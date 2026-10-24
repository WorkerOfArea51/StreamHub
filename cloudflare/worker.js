/**
 * StreamHub Cloudflare Edge Worker (100% Free Tier)
 *
 * Responsibilities:
 * 1. High-Throughput Video Streaming Proxy (/dl/*):
 *    Preserves HTTP Range headers, passes through upstream chunks, adds CORS headers.
 * 2. Edge-Cached Catalog API (/api/catalog):
 *    Serves pre-compiled JSON from KV with ETag & 304 Not Modified support.
 * 3. Creator Studio CRUD Endpoints (/api/catalog/item, /api/catalog/import):
 *    Direct PUT, DELETE, and batch import with catalog hash updating.
 * 4. User Presence & Telemetry (/api/telemetry/heartbeat, /api/telemetry/audience):
 *    Stores active viewer sessions with 180s auto-expiry in KV.
 *    Piggybacks remote commands (NOTIFICATION, REFRESH, KICK) onto 60s heartbeats.
 * 5. Global Announcements (/api/broadcasts):
 *    Publishes and delivers self-expiring announcements.
 */

// Upstream VPS host for video file streaming
const UPSTREAM_STREAMING_HOST = "midnighthawk.serv00.net";

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
    // 1. VIDEO STREAMING PROXY (/dl/* or /stream/*)
    // ─────────────────────────────────────────────────────────────
    if (pathname.startsWith("/dl/") || pathname.startsWith("/stream/")) {
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
    // 2. CATALOG API
    // ─────────────────────────────────────────────────────────────

    // GET /api/catalog
    if (pathname === "/api/catalog" && request.method === "GET") {
      if (!env.CATALOG_KV) {
        return new Response(JSON.stringify({ error: "CATALOG_KV binding missing" }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }

      const versionInfo = (await env.CATALOG_KV.get("catalog_version", "json")) || {
        version: "1",
        updatedAt: 0,
      };
      const etag = `W/"${versionInfo.version}_${versionInfo.updatedAt}"`;

      // Check client If-None-Match header for instant 304 (0 bytes transferred)
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

      const catalogData = await env.CATALOG_KV.get("catalog_full");
      if (!catalogData) {
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

    // GET /api/catalog/version
    if (pathname === "/api/catalog/version" && request.method === "GET") {
      const versionInfo = (await env.CATALOG_KV?.get("catalog_version", "json")) || {
        version: "1",
        count: 0,
        updatedAt: 0,
      };
      return new Response(JSON.stringify(versionInfo), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // PUT /api/catalog/item
    if (pathname === "/api/catalog/item" && request.method === "PUT") {
      try {
        const item = await request.json();
        if (!item || !item.id) {
          return new Response(JSON.stringify({ error: "Missing item id" }), {
            status: 400,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }

        // 1. Save single show in KV
        await env.CATALOG_KV.put(`item:${item.id}`, JSON.stringify(item));

        // 2. Update catalog_full
        let catalog = [];
        const rawFull = await env.CATALOG_KV.get("catalog_full");
        if (rawFull) {
          try {
            const parsed = JSON.parse(rawFull);
            catalog = Array.isArray(parsed) ? parsed : (parsed.items || []);
          } catch (e) {}
        }

        const idx = catalog.findIndex((x) => x.id === item.id);
        if (idx >= 0) {
          catalog[idx] = item;
        } else {
          catalog.unshift(item);
        }

        const now = Date.now();
        await env.CATALOG_KV.put("catalog_full", JSON.stringify({ items: catalog }));
        await env.CATALOG_KV.put(
          "catalog_version",
          JSON.stringify({
            version: `${now}`,
            count: catalog.length,
            updatedAt: now,
          })
        );

        return new Response(JSON.stringify({ ok: true, count: catalog.length }), {
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

    // DELETE /api/catalog/item/:id
    if (pathname.startsWith("/api/catalog/item/") && request.method === "DELETE") {
      const id = pathname.substring("/api/catalog/item/".length);
      try {
        await env.CATALOG_KV.delete(`item:${id}`);

        let catalog = [];
        const rawFull = await env.CATALOG_KV.get("catalog_full");
        if (rawFull) {
          try {
            const parsed = JSON.parse(rawFull);
            catalog = Array.isArray(parsed) ? parsed : (parsed.items || []);
          } catch (e) {}
        }

        catalog = catalog.filter((x) => x.id !== id);
        const now = Date.now();
        await env.CATALOG_KV.put("catalog_full", JSON.stringify({ items: catalog }));
        await env.CATALOG_KV.put(
          "catalog_version",
          JSON.stringify({
            version: `${now}`,
            count: catalog.length,
            updatedAt: now,
          })
        );

        return new Response(JSON.stringify({ ok: true, count: catalog.length }), {
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

    // POST /api/catalog/import (Batch import)
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

        let currentCatalog = [];
        const rawFull = await env.CATALOG_KV.get("catalog_full");
        if (rawFull) {
          try {
            const parsed = JSON.parse(rawFull);
            currentCatalog = Array.isArray(parsed) ? parsed : (parsed.items || []);
          } catch (e) {}
        }

        const map = new Map(currentCatalog.map((x) => [x.id, x]));
        for (const item of incomingItems) {
          map.set(item.id, item);
        }

        const merged = Array.from(map.values());
        const now = Date.now();
        await env.CATALOG_KV.put("catalog_full", JSON.stringify({ items: merged }));
        await env.CATALOG_KV.put(
          "catalog_version",
          JSON.stringify({
            version: `${now}`,
            count: merged.length,
            updatedAt: now,
          })
        );

        return new Response(JSON.stringify({ ok: true, imported: incomingItems.length, total: merged.length }), {
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

    // POST /api/catalog/refresh
    if (pathname === "/api/catalog/refresh" && request.method === "POST") {
      return new Response(JSON.stringify({ ok: true, message: "Catalog refreshed" }), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // ─────────────────────────────────────────────────────────────
    // 3. USER TELEMETRY & HEARTBEATS
    // ─────────────────────────────────────────────────────────────

    // POST /api/telemetry/heartbeat
    if (pathname === "/api/telemetry/heartbeat" && request.method === "POST") {
      try {
        const session = await request.json();
        const clientId = session.clientId;
        if (!clientId) {
          return new Response(JSON.stringify({ error: "Missing clientId" }), {
            status: 400,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }

        // Store session in KV with 180-second TTL (auto-purges after 3 minutes)
        await env.CATALOG_KV.put(`session:${clientId}`, JSON.stringify(session), {
          expirationTtl: 180,
        });

        // Check if there is any pending command for this client
        let pendingCommand = null;
        const cmdKey = `cmd:${clientId}`;
        const cmdRaw = await env.CATALOG_KV.get(cmdKey);
        if (cmdRaw) {
          try {
            pendingCommand = JSON.parse(cmdRaw);
            await env.CATALOG_KV.delete(cmdKey); // Consume command
          } catch (e) {}
        }

        // Check if there is an active broadcast
        let activeBroadcast = null;
        const bcastRaw = await env.CATALOG_KV.get("broadcast:latest");
        if (bcastRaw) {
          try {
            const parsed = JSON.parse(bcastRaw);
            if (parsed.expiresAt && Date.now() < parsed.expiresAt) {
              activeBroadcast = parsed;
            }
          } catch (e) {}
        }

        return new Response(
          JSON.stringify({
            ok: true,
            command: pendingCommand,
            broadcast: activeBroadcast,
          }),
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

    // GET /api/telemetry/audience
    if (pathname === "/api/telemetry/audience" && request.method === "GET") {
      try {
        // List active session keys
        const listResult = await env.CATALOG_KV.list({ prefix: "session:" });
        const sessions = [];

        // Fetch up to 50 active sessions concurrently
        const fetchPromises = listResult.keys.slice(0, 50).map(async (key) => {
          const raw = await env.CATALOG_KV.get(key.name);
          if (raw) {
            try {
              return JSON.parse(raw);
            } catch (e) {}
          }
          return null;
        });

        const resolved = await Promise.all(fetchPromises);
        for (const s of resolved) {
          if (s) sessions.push(s);
        }

        return new Response(JSON.stringify({ activeWatchers: sessions }), {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message, activeWatchers: [] }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // POST /api/telemetry/command (Admin dispatch command)
    if (pathname === "/api/telemetry/command" && request.method === "POST") {
      try {
        const body = await request.json();
        const { targetClientId, command, title, message } = body;
        if (!targetClientId || !command) {
          return new Response(JSON.stringify({ error: "Missing targetClientId or command" }), {
            status: 400,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          });
        }

        const cmdPayload = {
          type: command,
          title: title || "",
          message: message || "",
          timestamp: Date.now(),
        };

        await env.CATALOG_KV.put(`cmd:${targetClientId}`, JSON.stringify(cmdPayload), {
          expirationTtl: 300, // 5 min TTL
        });

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

    // ─────────────────────────────────────────────────────────────
    // 4. GLOBAL BROADCAST ANNOUNCEMENTS
    // ─────────────────────────────────────────────────────────────

    // GET /api/broadcasts/latest
    if (pathname === "/api/broadcasts/latest" && request.method === "GET") {
      const bcastRaw = await env.CATALOG_KV.get("broadcast:latest");
      if (!bcastRaw) {
        return new Response(JSON.stringify({}), {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
      return new Response(bcastRaw, {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // POST /api/broadcasts
    if (pathname === "/api/broadcasts" && request.method === "POST") {
      try {
        const body = await request.json();
        const { title, message, expiryHours } = body;
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
        };

        await env.CATALOG_KV.put("broadcast:latest", JSON.stringify(bcast), {
          expirationTtl: hours * 3600,
        });

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
      await env.CATALOG_KV.delete("broadcast:latest");
      return new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Default route: Health check
    return new Response(
      JSON.stringify({
        status: "healthy",
        service: "StreamHub Edge Proxy & Catalog Server",
        version: "4.8.423",
      }),
      {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  },
};
