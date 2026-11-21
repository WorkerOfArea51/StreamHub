/**
 * StreamHub Cloudflare Edge Worker (100% Free Tier)
 * Hybrid D1 Relational SQLite (5 GB) + Auto-Sharded KV Edge Cache (1 GB)
 *
 * Responsibilities:
 * 1. High-Throughput Video Streaming Proxy (/dl/*, /stream/*):
 *    Preserves HTTP Range headers, passes through upstream chunks, adds CORS headers.
 * 2. Auto-Sharded Category Edge Cache (/api/catalog):
 *    Shards by category (catalog_animes_1, catalog_movies_1, catalog_series_1)
 *    Enforces an 80% capacity ceiling (20 MB) per shard with 5 MB safety runway.
 *    Auto-rolls over to _2, _3, etc. without truncating any show.
 *    Serves in parallel with ETag & 304 Not Modified support (0 D1 reads for regular users).
 * 3. Master Relational SQLite Storage in D1 (shows, vouchers, broadcasts, system_meta):
 *    Full ACID relational persistence with 5 GB free capacity.
 * 4. VIP Vouchers & Device Hardware Binding (/api/vouchers):
 *    Indexed SQL lookup + instant edge caching.
 * 5. Global Announcements (/api/broadcasts):
 *    Self-expiring push alerts.
 */

// Upstream VPS host for video file streaming
const UPSTREAM_STREAMING_HOST = "midnighthawk.serv00.net";

// 80% of 25 MiB KV limit = 20 MiB safe threshold (5 MiB runway to prevent truncation)
const MAX_SHARD_BYTES = 20 * 1024 * 1024;

/**
 * Shards an array of media items by category with an 80% (20 MB) ceiling per shard.
 * Never truncates or fragments any individual show.
 */
function shardCatalogItems(items) {
  const buckets = {
    animes: [],
    movies: [],
    series: [],
  };

  for (const item of items) {
    const cat = (item.category || item.type || "").toLowerCase();
    if (cat.includes("anime")) {
      buckets.animes.push(item);
    } else if (cat.includes("movie") || item.isMovie) {
      buckets.movies.push(item);
    } else {
      buckets.series.push(item);
    }
  }

  const shards = {}; // shardKey -> JSON string
  const shardKeys = [];
  const shardStats = [];

  for (const [catName, catItems] of Object.entries(buckets)) {
    let shardIndex = 1;
    let currentItems = [];
    let currentBytes = 25; // Base wrapper bytes {"items":[]}

    for (const show of catItems) {
      const showJson = JSON.stringify(show);
      const showBytes = new TextEncoder().encode(showJson).length + 1; // + comma

      if (currentItems.length > 0 && (currentBytes + showBytes) > MAX_SHARD_BYTES) {
        // 80% ceiling reached — cleanly seal current shard and roll over to next index
        const shardKey = `catalog_${catName}_${shardIndex}`;
        shards[shardKey] = JSON.stringify({ items: currentItems });
        shardKeys.push(shardKey);
        shardStats.push({
          key: shardKey,
          category: catName,
          count: currentItems.length,
          sizeBytes: currentBytes,
          fillPercent: ((currentBytes / MAX_SHARD_BYTES) * 100).toFixed(1) + "%",
        });

        shardIndex++;
        currentItems = [show];
        currentBytes = 25 + showBytes;
      } else {
        currentItems.push(show);
        currentBytes += showBytes;
      }
    }

    if (currentItems.length > 0) {
      const shardKey = `catalog_${catName}_${shardIndex}`;
      shards[shardKey] = JSON.stringify({ items: currentItems });
      shardKeys.push(shardKey);
      shardStats.push({
        key: shardKey,
        category: catName,
        count: currentItems.length,
        sizeBytes: currentBytes,
        fillPercent: ((currentBytes / MAX_SHARD_BYTES) * 100).toFixed(1) + "%",
      });
    }
  }

  return { shards, shardKeys, shardStats };
}

/**
 * Rebuilds all KV category shards from D1 SQLite and updates catalog_manifest.
 */
async function rebuildAndSaveKVShards(env, now = Date.now()) {
  if (!env.DB) return { ok: false, error: "No DB bound" };

  const { results } = await env.DB.prepare(
    "SELECT data_json FROM shows ORDER BY updated_at DESC"
  ).all();
  const allItems = (results || []).map((r) => JSON.parse(r.data_json));

  if (!env.CATALOG_KV) {
    return { ok: true, total: allItems.length, shards: [] };
  }

  // Get previous manifest to clean up any obsolete shard keys if count shrank
  const prevManifest = await env.CATALOG_KV.get("catalog_manifest", "json");
  const prevShards = (prevManifest && prevManifest.shards) ? prevManifest.shards : [];

  const { shards, shardKeys, shardStats } = shardCatalogItems(allItems);

  // 1. Write new/updated shards to KV in parallel
  const putPromises = Object.entries(shards).map(([key, jsonStr]) =>
    env.CATALOG_KV.put(key, jsonStr)
  );
  await Promise.all(putPromises);

  // 2. Delete obsolete shards if any
  const obsoleteShards = prevShards.filter((k) => !shardKeys.includes(k));
  if (obsoleteShards.length > 0) {
    await Promise.all(obsoleteShards.map((k) => env.CATALOG_KV.delete(k)));
  }

  // 3. Save catalog_manifest
  const manifest = {
    shards: shardKeys,
    totalShows: allItems.length,
    version: `${now}`,
    updatedAt: now,
    stats: shardStats,
  };
  await env.CATALOG_KV.put("catalog_manifest", JSON.stringify(manifest));

  // 4. Save catalog_version
  const versionPayload = {
    version: `${now}`,
    count: allItems.length,
    updatedAt: now,
  };
  await env.CATALOG_KV.put("catalog_version", JSON.stringify(versionPayload));

  // 5. Delete old legacy monolithic catalog_full if it exists
  try {
    await env.CATALOG_KV.delete("catalog_full");
  } catch (_) {}

  return { ok: true, total: allItems.length, manifest };
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const { pathname } = url;

    // CORS headers for all responses
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, HEAD, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Range, Authorization, User-Agent",
      "Access-Control-Expose-Headers": "Content-Range, Content-Length, Accept-Ranges, ETag",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    // ─────────────────────────────────────────────────────────────
    // 1. VIDEO STREAMING & F2L API PROXY (/dl/*, /stream/*, /api/batch/*, /api/file/*, /api/movies)
    // ─────────────────────────────────────────────────────────────
    if (
      pathname.startsWith("/dl/") ||
      pathname.startsWith("/stream/") ||
      pathname.startsWith("/api/batch/") ||
      pathname.startsWith("/api/file/") ||
      pathname.startsWith("/api/movies")
    ) {
      const upstreamUrl = new URL(request.url);
      upstreamUrl.host = env.UPSTREAM_HOST || UPSTREAM_STREAMING_HOST;
      upstreamUrl.protocol = "https:";

      const forwardHeaders = new Headers(request.headers);
      forwardHeaders.set("Host", upstreamUrl.host);
      forwardHeaders.set("User-Agent", "StreamHub/4.8 (Android; Cloudflare-Worker-Proxy)");

      try {
        const upstreamResponse = await fetch(upstreamUrl.toString(), {
          method: request.method,
          headers: forwardHeaders,
          redirect: "follow",
        });

        const responseHeaders = new Headers(upstreamResponse.headers);
        for (const [key, value] of Object.entries(corsHeaders)) {
          responseHeaders.set(key, value);
        }
        responseHeaders.set("Accept-Ranges", "bytes");

        return new Response(upstreamResponse.body, {
          status: upstreamResponse.status,
          statusText: upstreamResponse.statusText,
          headers: responseHeaders,
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: "Streaming proxy error", message: err.message }), {
          status: 502,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. REBUILD / INITIALIZE SHARDS (/api/rebuild-shards)
    // Splits all D1 shows into category shards with 80% ceiling.
    // ─────────────────────────────────────────────────────────────
    if (pathname === "/api/rebuild-shards" && (request.method === "GET" || request.method === "POST")) {
      const key = url.searchParams.get("key") || request.headers.get("X-Migration-Key");
      if (key !== "streamhub_migrate_2026") {
        return new Response(JSON.stringify({ error: "Unauthorized: Invalid key" }), {
          status: 401,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }

      try {
        const result = await rebuildAndSaveKVShards(env);
        return new Response(JSON.stringify({
          ok: true,
          message: "Successfully partitioned catalog into 80%-capped category shards!",
          manifest: result.manifest,
        }, null, 2), {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      } catch (err) {
        return new Response(JSON.stringify({ ok: false, error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. CATALOG API
    // ─────────────────────────────────────────────────────────────

    // GET /api/catalog (Served from KV category shards in parallel; 0 D1 reads for regular users)
    if (pathname === "/api/catalog" && request.method === "GET") {
      let etag = `W/"1_0"`;
      if (env.CATALOG_KV) {
        const versionInfo = (await env.CATALOG_KV.get("catalog_version", "json")) || {
          version: "1",
          updatedAt: 0,
        };
        etag = `W/"${versionInfo.version}_${versionInfo.updatedAt}"`;

        // Check client If-None-Match header for instant 304 Not Modified (0 bytes transferred)
        const clientEtag = request.headers.get("If-None-Match");
        if (clientEtag && clientEtag === etag) {
          return new Response(null, {
            status: 304,
            headers: {
              ...corsHeaders,
              ETag: etag,
              "Cache-Control": "public, max-age=60, s-maxage=3600",
            },
          });
        }

        // Optional category filter parameter: ?category=animes | movies | series
        const categoryFilter = url.searchParams.get("category");

        // Read manifest to get active category shards
        const manifest = await env.CATALOG_KV.get("catalog_manifest", "json");
        if (manifest && Array.isArray(manifest.shards) && manifest.shards.length > 0) {
          let targetShards = manifest.shards;
          if (categoryFilter) {
            const filterLower = categoryFilter.toLowerCase();
            targetShards = targetShards.filter((k) => k.toLowerCase().includes(filterLower));
          }

          // Fetch shards in parallel (~10-15ms edge read)
          const shardResponses = await Promise.all(
            targetShards.map((k) => env.CATALOG_KV.get(k, "json"))
          );
          let combinedItems = [];
          for (const s of shardResponses) {
            if (s && Array.isArray(s.items)) {
              combinedItems = combinedItems.concat(s.items);
            }
          }

          return new Response(JSON.stringify({ items: combinedItems }), {
            status: 200,
            headers: {
              ...corsHeaders,
              "Content-Type": "application/json",
              ETag: etag,
              "Cache-Control": "public, max-age=60, s-maxage=3600",
            },
          });
        }

        // Backward compatibility fallback: check legacy catalog_full if manifest not yet built
        const catalogData = await env.CATALOG_KV.get("catalog_full");
        if (catalogData) {
          return new Response(catalogData, {
            status: 200,
            headers: {
              ...corsHeaders,
              "Content-Type": "application/json",
              ETag: etag,
              "Cache-Control": "public, max-age=60, s-maxage=3600",
            },
          });
        }
      }

      // Self-healing fallback: If KV is empty but D1 has data, rebuild shards from D1
      if (env.DB) {
        try {
          const result = await rebuildAndSaveKVShards(env);
          if (result && result.ok) {
            const { results } = await env.DB.prepare(
              "SELECT data_json FROM shows ORDER BY updated_at DESC"
            ).all();
            const items = (results || []).map((r) => JSON.parse(r.data_json));
            const now = Date.now();
            return new Response(JSON.stringify({ items }), {
              status: 200,
              headers: {
                ...corsHeaders,
                "Content-Type": "application/json",
                ETag: `W/"${now}_${now}"`,
                "Cache-Control": "public, max-age=60, s-maxage=3600",
              },
            });
          }
        } catch (err) {
          return new Response(JSON.stringify({ error: err.message }), {
            status: 500,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }
      }

      return new Response(JSON.stringify({ items: [] }), {
        status: 200,
        headers: {
          ...corsHeaders,
          "Content-Type": "application/json",
          ETag: etag,
          "Cache-Control": "public, max-age=60, s-maxage=3600",
        },
      });
    }

    // GET /api/catalog/manifest (Metadata inspect: view active shards & fill percentages)
    if (pathname === "/api/catalog/manifest" && request.method === "GET") {
      let manifest = null;
      if (env.CATALOG_KV) {
        manifest = await env.CATALOG_KV.get("catalog_manifest", "json");
      }
      return new Response(JSON.stringify(manifest || {}), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // GET /api/catalog/version
    if (pathname === "/api/catalog/version" && request.method === "GET") {
      let versionInfo = null;
      if (env.CATALOG_KV) {
        versionInfo = await env.CATALOG_KV.get("catalog_version", "json");
      }
      if (!versionInfo && env.DB) {
        try {
          const row = await env.DB.prepare(
            "SELECT value FROM system_meta WHERE key = 'catalog_version'"
          ).first();
          if (row && row.value) versionInfo = JSON.parse(row.value);
        } catch (_) {}
      }
      return new Response(
        JSON.stringify(versionInfo || { version: "1", count: 0, updatedAt: 0 }),
        {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    // PUT /api/catalog/item (Creator Studio: Save/Update show)
    if (pathname === "/api/catalog/item" && request.method === "PUT") {
      try {
        const item = await request.json();
        if (!item || !item.id) {
          return new Response(JSON.stringify({ error: "Missing item id" }), {
            status: 400,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }

        const now = Date.now();

        // 1. Write to D1 Relational SQLite Master
        if (env.DB) {
          await env.DB.prepare(`
            INSERT OR REPLACE INTO shows 
            (id, title, type, category, release_year, season_number, part_number, franchise_order, is_movie, episode_count, data_json, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          `).bind(
            item.id,
            item.title || "",
            item.type || "",
            item.category || "",
            Number(item.releaseYear || item.year) || 0,
            Number(item.seasonNumber) || 0,
            Number(item.partNumber) || 0,
            Number(item.franchiseOrder) || 0.0,
            item.isMovie ? 1 : 0,
            Array.isArray(item.episodes) ? item.episodes.length : 0,
            JSON.stringify(item),
            Number(item.createdAt) || now,
            now
          ).run();
        }

        // 2. Rebuild KV category shards with 80% ceiling check
        const result = await rebuildAndSaveKVShards(env, now);

        return new Response(JSON.stringify({ ok: true, count: result.total || 0 }), {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // DELETE /api/catalog/item/:id (Creator Studio: Delete show)
    if (pathname.startsWith("/api/catalog/item/") && request.method === "DELETE") {
      const id = pathname.substring("/api/catalog/item/".length);
      try {
        const now = Date.now();

        // 1. Delete from D1
        if (env.DB) {
          await env.DB.prepare("DELETE FROM shows WHERE id = ?").bind(id).run();
        }

        // 2. Rebuild KV category shards
        const result = await rebuildAndSaveKVShards(env, now);

        return new Response(JSON.stringify({ ok: true, count: result.total || 0 }), {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // POST /api/catalog/import (Batch import shows)
    if (pathname === "/api/catalog/import" && request.method === "POST") {
      try {
        const body = await request.json();
        const incomingItems = Array.isArray(body) ? body : (body.items || []);
        if (!incomingItems.length) {
          return new Response(JSON.stringify({ error: "Empty items list" }), {
            status: 400,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }

        const now = Date.now();

        // 1. Batch insert into D1
        if (env.DB) {
          for (let i = 0; i < incomingItems.length; i += 50) {
            const chunk = incomingItems.slice(i, i + 50);
            const statements = chunk.map((item) => {
              return env.DB.prepare(`
                INSERT OR REPLACE INTO shows 
                (id, title, type, category, release_year, season_number, part_number, franchise_order, is_movie, episode_count, data_json, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              `).bind(
                item.id,
                item.title || "",
                item.type || "",
                item.category || "",
                Number(item.releaseYear || item.year) || 0,
                Number(item.seasonNumber) || 0,
                Number(item.partNumber) || 0,
                Number(item.franchiseOrder) || 0.0,
                item.isMovie ? 1 : 0,
                Array.isArray(item.episodes) ? item.episodes.length : 0,
                JSON.stringify(item),
                Number(item.createdAt) || now,
                now
              );
            });
            await env.DB.batch(statements);
          }
        }

        // 2. Rebuild KV category shards
        const result = await rebuildAndSaveKVShards(env, now);

        return new Response(
          JSON.stringify({ ok: true, imported: incomingItems.length, total: result.total || 0 }),
          {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          }
        );
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // POST /api/catalog/refresh (Force rebuild KV shards directly from D1)
    if (pathname === "/api/catalog/refresh" && request.method === "POST") {
      try {
        const result = await rebuildAndSaveKVShards(env);
        return new Response(
          JSON.stringify({ ok: true, message: "KV category shards rebuilt from D1", result }),
          {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          }
        );
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // ─────────────────────────────────────────────────────────────
    // 4. GLOBAL BROADCAST ANNOUNCEMENTS
    // ─────────────────────────────────────────────────────────────

    // GET /api/broadcasts/latest
    if (pathname === "/api/broadcasts/latest" && request.method === "GET") {
      if (env.CATALOG_KV) {
        const bcastRaw = await env.CATALOG_KV.get("broadcast:latest");
        if (bcastRaw) {
          return new Response(bcastRaw, {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }
      }

      if (env.DB) {
        try {
          const row = await env.DB.prepare(`
            SELECT * FROM broadcasts 
            WHERE is_active = 1 AND expires_at > ? 
            ORDER BY created_at DESC LIMIT 1
          `).bind(Date.now()).first();
          if (row) {
            return new Response(JSON.stringify(row), {
              status: 200,
              headers: { ...corsHeaders, "Content-Type": "application/json" },
            });
          }
        } catch (_) {}
      }

      return new Response(JSON.stringify({}), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // POST /api/broadcasts (Publish announcement)
    if (pathname === "/api/broadcasts" && request.method === "POST") {
      try {
        const body = await request.json();
        const { title, message, expiryHours, category, actionType, actionTarget, actionLabel } = body;
        const now = Date.now();
        const hours = Math.max(1, Math.min(168, expiryHours || 24));
        const expiresAt = now + hours * 3600_000;

        const bcast = {
          id: `bcast_${now}`,
          title: title || "StreamHub Announcement 📢",
          message: message || "",
          timestamp: now,
          expiresAt: expiresAt,
          sender: "Owner Admin",
          category: category || "INFO",
          actionType: actionType || "NONE",
          actionTarget: actionTarget || "",
          actionLabel: actionLabel || "",
        };

        // 1. Write to D1
        if (env.DB) {
          await env.DB.prepare(`
            INSERT OR REPLACE INTO broadcasts
            (id, title, message, category, action_type, action_target, action_label, sender, created_at, expires_at, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
          `).bind(
            bcast.id,
            bcast.title,
            bcast.message,
            bcast.category,
            bcast.actionType,
            bcast.actionTarget,
            bcast.actionLabel,
            bcast.sender,
            now,
            expiresAt
          ).run();
        }

        // 2. Write to KV with TTL
        if (env.CATALOG_KV) {
          await env.CATALOG_KV.put("broadcast:latest", JSON.stringify(bcast), {
            expirationTtl: hours * 3600,
          });
        }

        return new Response(JSON.stringify({ ok: true, broadcast: bcast }), {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // DELETE /api/broadcasts
    if (pathname === "/api/broadcasts" && request.method === "DELETE") {
      if (env.DB) {
        try {
          await env.DB.prepare("UPDATE broadcasts SET is_active = 0").run();
        } catch (_) {}
      }
      if (env.CATALOG_KV) {
        await env.CATALOG_KV.delete("broadcast:latest");
      }
      return new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // ─────────────────────────────────────────────────────────────
    // 5. VIP VOUCHERS API (/api/vouchers)
    // ─────────────────────────────────────────────────────────────

    // Helper: fetch or build the consolidated voucher index
    async function getOrBuildVouchersIndex(workerEnv) {
      if (workerEnv.DB) {
        try {
          const { results } = await workerEnv.DB.prepare(
            "SELECT data_json FROM vouchers ORDER BY created_at DESC"
          ).all();
          if (results && results.length > 0) {
            return results.map((r) => JSON.parse(r.data_json));
          }
        } catch (_) {}
      }

      if (workerEnv.CATALOG_KV) {
        const cached = await workerEnv.CATALOG_KV.get("vouchers:all");
        if (cached) {
          try {
            return JSON.parse(cached);
          } catch (_) {}
        }
        const list = await workerEnv.CATALOG_KV.list({ prefix: "voucher:code:" });
        const rawList = await Promise.all(list.keys.map((k) => workerEnv.CATALOG_KV.get(k.name)));
        const vouchers = [];
        for (const raw of rawList) {
          if (raw) {
            try {
              vouchers.push(JSON.parse(raw));
            } catch (_) {}
          }
        }
        vouchers.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
        await workerEnv.CATALOG_KV.put("vouchers:all", JSON.stringify(vouchers));
        return vouchers;
      }
      return [];
    }

    // GET /api/vouchers (Admin: List all vouchers)
    if (pathname === "/api/vouchers" && request.method === "GET") {
      try {
        const vouchers = await getOrBuildVouchersIndex(env);
        return new Response(JSON.stringify({ ok: true, vouchers }), {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // POST /api/vouchers (Admin: Create voucher)
    if (pathname === "/api/vouchers" && request.method === "POST") {
      try {
        const body = await request.json();
        const code = (body.code || "").trim().toUpperCase();
        if (!code) {
          return new Response(JSON.stringify({ error: "Missing voucher code" }), {
            status: 400,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }
        const now = Date.now();
        const voucher = {
          code: code,
          label: (body.label || "").trim(),
          status: "AVAILABLE",
          createdAt: now,
          activatedAt: 0,
          expiresAt: 0,
          durationDays: body.durationDays || 30,
          boundDeviceId: "",
          deviceModel: "",
        };

        // 1. Write to D1
        if (env.DB) {
          await env.DB.prepare(`
            INSERT OR REPLACE INTO vouchers
            (code, bound_device_id, label, status, duration_days, created_at, activated_at, expires_at, device_model, data_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          `).bind(
            code,
            "",
            voucher.label,
            "AVAILABLE",
            voucher.durationDays,
            now,
            0,
            0,
            "",
            JSON.stringify(voucher)
          ).run();
        }

        // 2. Write to KV for instant edge retrieval
        if (env.CATALOG_KV) {
          await env.CATALOG_KV.put("voucher:code:" + code, JSON.stringify(voucher));
          try {
            const currentList = await getOrBuildVouchersIndex(env);
            const updatedList = [voucher, ...currentList.filter((v) => v.code !== code)];
            await env.CATALOG_KV.put("vouchers:all", JSON.stringify(updatedList));
          } catch (_) {}
        }

        return new Response(JSON.stringify({ ok: true, voucher }), {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // POST /api/vouchers/batch (Admin: Batch import vouchers)
    if (pathname === "/api/vouchers/batch" && request.method === "POST") {
      try {
        const body = await request.json();
        const vouchers = Array.isArray(body) ? body : (body.vouchers || []);
        let count = 0;
        const d1Statements = [];

        for (const v of vouchers) {
          if (!v || !v.code) continue;
          const code = v.code.trim().toUpperCase();
          const cleanVoucher = {
            code: code,
            label: (v.label || "").trim(),
            status: v.status || "AVAILABLE",
            createdAt: Number(v.createdAt) || Date.now(),
            activatedAt: Number(v.activatedAt) || 0,
            expiresAt: Number(v.expiresAt) || 0,
            durationDays: Number(v.durationDays) || 30,
            boundDeviceId: (v.boundDeviceId || "").trim(),
            deviceModel: (v.deviceModel || "").trim(),
          };

          if (env.DB) {
            d1Statements.push(
              env.DB.prepare(`
                INSERT OR REPLACE INTO vouchers
                (code, bound_device_id, label, status, duration_days, created_at, activated_at, expires_at, device_model, data_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              `).bind(
                cleanVoucher.code,
                cleanVoucher.boundDeviceId,
                cleanVoucher.label,
                cleanVoucher.status,
                cleanVoucher.durationDays,
                cleanVoucher.createdAt,
                cleanVoucher.activatedAt,
                cleanVoucher.expiresAt,
                cleanVoucher.deviceModel,
                JSON.stringify(cleanVoucher)
              )
            );
          }

          if (env.CATALOG_KV) {
            await env.CATALOG_KV.put("voucher:code:" + code, JSON.stringify(cleanVoucher));
            if (cleanVoucher.boundDeviceId) {
              await env.CATALOG_KV.put("voucher:device:" + cleanVoucher.boundDeviceId, JSON.stringify(cleanVoucher));
            }
          }
          count++;
        }

        if (env.DB && d1Statements.length > 0) {
          for (let i = 0; i < d1Statements.length; i += 50) {
            await env.DB.batch(d1Statements.slice(i, i + 50));
          }
        }

        if (env.CATALOG_KV) {
          try {
            const allVouchers = await getOrBuildVouchersIndex(env);
            await env.CATALOG_KV.put("vouchers:all", JSON.stringify(allVouchers));
          } catch (_) {}
        }

        return new Response(JSON.stringify({ ok: true, imported: count }), {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // POST /api/vouchers/redeem (Device: Redeem code & bind hardware)
    if (pathname === "/api/vouchers/redeem" && request.method === "POST") {
      try {
        const body = await request.json();
        const code = (body.code || "").trim().toUpperCase();
        const deviceId = (body.deviceId || "").trim();
        const deviceModel = (body.deviceModel || "").trim();

        if (!code || !deviceId) {
          return new Response(JSON.stringify({ result: "INVALID_CODE" }), {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }

        let voucher = null;
        if (env.CATALOG_KV) {
          const raw = await env.CATALOG_KV.get("voucher:code:" + code);
          if (raw) {
            try { voucher = JSON.parse(raw); } catch (_) {}
          }
        }
        if (!voucher && env.DB) {
          try {
            const row = await env.DB.prepare("SELECT data_json FROM vouchers WHERE code = ?").bind(code).first();
            if (row && row.data_json) voucher = JSON.parse(row.data_json);
          } catch (_) {}
        }

        if (!voucher) {
          return new Response(JSON.stringify({ result: "INVALID_CODE" }), {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }

        const now = Date.now();

        if (voucher.status === "AVAILABLE") {
          const durationMs = (voucher.durationDays || 30) * 24 * 60 * 60 * 1000;
          const updated = {
            ...voucher,
            status: "ACTIVE",
            activatedAt: now,
            expiresAt: now + durationMs,
            boundDeviceId: deviceId,
            deviceModel: deviceModel,
          };

          // 1. Update D1
          if (env.DB) {
            await env.DB.prepare(`
              UPDATE vouchers 
              SET bound_device_id = ?, status = 'ACTIVE', activated_at = ?, expires_at = ?, device_model = ?, data_json = ?
              WHERE code = ?
            `).bind(deviceId, now, now + durationMs, deviceModel, JSON.stringify(updated), code).run();
          }

          // 2. Update KV
          if (env.CATALOG_KV) {
            await env.CATALOG_KV.put("voucher:code:" + code, JSON.stringify(updated));
            await env.CATALOG_KV.put("voucher:device:" + deviceId, JSON.stringify(updated));
            try {
              const currentList = await getOrBuildVouchersIndex(env);
              const updatedList = currentList.map((v) => (v.code === code ? updated : v));
              await env.CATALOG_KV.put("vouchers:all", JSON.stringify(updatedList));
            } catch (_) {}
          }

          return new Response(
            JSON.stringify({
              result: "SUCCESS",
              daysRemaining: voucher.durationDays || 30,
              isReactivation: false,
            }),
            {
              status: 200,
              headers: { ...corsHeaders, "Content-Type": "application/json" },
            }
          );
        } else if (voucher.status === "ACTIVE") {
          if (now >= voucher.expiresAt) {
            if (env.DB) {
              await env.DB.prepare("UPDATE vouchers SET status = 'EXPIRED' WHERE code = ?").bind(code).run();
            }
            if (env.CATALOG_KV) {
              await env.CATALOG_KV.delete("voucher:code:" + code);
              await env.CATALOG_KV.delete("voucher:device:" + deviceId);
              try {
                const currentList = await getOrBuildVouchersIndex(env);
                const updatedList = currentList.filter((v) => v.code !== code);
                await env.CATALOG_KV.put("vouchers:all", JSON.stringify(updatedList));
              } catch (_) {}
            }
            return new Response(JSON.stringify({ result: "EXPIRED" }), {
              status: 200,
              headers: { ...corsHeaders, "Content-Type": "application/json" },
            });
          }
          if (voucher.boundDeviceId === deviceId) {
            const daysRemaining = Math.max(
              1,
              Math.ceil((voucher.expiresAt - now) / (24 * 60 * 60 * 1000))
            );
            if (env.CATALOG_KV) {
              await env.CATALOG_KV.put("voucher:device:" + deviceId, JSON.stringify(voucher));
            }
            return new Response(
              JSON.stringify({
                result: "SUCCESS",
                daysRemaining: daysRemaining,
                isReactivation: true,
              }),
              {
                status: 200,
                headers: { ...corsHeaders, "Content-Type": "application/json" },
              }
            );
          } else {
            return new Response(JSON.stringify({ result: "BOUND_TO_ANOTHER_DEVICE" }), {
              status: 200,
              headers: { ...corsHeaders, "Content-Type": "application/json" },
            });
          }
        } else {
          return new Response(JSON.stringify({ result: "EXPIRED" }), {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }
      } catch (err) {
        return new Response(JSON.stringify({ result: "ERROR", error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // GET /api/vouchers/check/:deviceId (Device: Check active voucher status on startup)
    if (pathname.startsWith("/api/vouchers/check/") && request.method === "GET") {
      try {
        const deviceId = pathname.substring("/api/vouchers/check/".length);
        if (!deviceId) {
          return new Response(JSON.stringify({ status: "REVOKED" }), {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }

        let voucher = null;
        if (env.CATALOG_KV) {
          const raw = await env.CATALOG_KV.get("voucher:device:" + deviceId);
          if (raw) {
            try { voucher = JSON.parse(raw); } catch (_) {}
          }
        }

        if (!voucher && env.DB) {
          try {
            const row = await env.DB.prepare(
              "SELECT data_json FROM vouchers WHERE bound_device_id = ? AND status = 'ACTIVE'"
            ).bind(deviceId).first();
            if (row && row.data_json) voucher = JSON.parse(row.data_json);
          } catch (_) {}
        }

        if (!voucher) {
          return new Response(JSON.stringify({ status: "REVOKED" }), {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }

        const now = Date.now();
        if (now >= voucher.expiresAt) {
          if (env.DB) {
            await env.DB.prepare("UPDATE vouchers SET status = 'EXPIRED' WHERE code = ?").bind(voucher.code).run();
          }
          if (env.CATALOG_KV) {
            await env.CATALOG_KV.delete("voucher:device:" + deviceId);
            await env.CATALOG_KV.delete("voucher:code:" + voucher.code);
          }
          return new Response(JSON.stringify({ status: "REVOKED" }), {
            status: 200,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }

        return new Response(JSON.stringify({ status: "ACTIVE", voucher }), {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      } catch (err) {
        return new Response(JSON.stringify({ status: "VERIFICATION_FAILED", reason: err.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // DELETE /api/vouchers/:code (Admin: Delete voucher)
    if (pathname.startsWith("/api/vouchers/") && request.method === "DELETE") {
      try {
        const code = pathname.substring("/api/vouchers/".length).trim().toUpperCase();

        if (env.DB) {
          await env.DB.prepare("DELETE FROM vouchers WHERE code = ?").bind(code).run();
        }

        if (env.CATALOG_KV) {
          const raw = await env.CATALOG_KV.get("voucher:code:" + code);
          if (raw) {
            try {
              const voucher = JSON.parse(raw);
              if (voucher.boundDeviceId) {
                await env.CATALOG_KV.delete("voucher:device:" + voucher.boundDeviceId);
              }
            } catch (_) {}
          }
          await env.CATALOG_KV.delete("voucher:code:" + code);
          try {
            const currentList = await getOrBuildVouchersIndex(env);
            const updatedList = currentList.filter((v) => v.code !== code);
            await env.CATALOG_KV.put("vouchers:all", JSON.stringify(updatedList));
          } catch (_) {}
        }

        return new Response(JSON.stringify({ ok: true }), {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // Default route: Health check
    return new Response(
      JSON.stringify({
        status: "healthy",
        service: "StreamHub Edge Proxy & Hybrid D1/KV Sharded Catalog Server",
        version: "4.8.452",
        database: env.DB ? "D1 Connected" : "KV Fallback",
      }),
      {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  },
};
