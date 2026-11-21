# StreamHub Cloudflare Edge Worker Deployment Guide (100% Free Tier)
## Hybrid D1 Relational SQLite (5 GB) + Auto-Sharded KV Edge Cache (1 GB)

This directory contains the production Cloudflare Edge Worker script (`worker.js`), Wrangler configuration (`wrangler.toml`), and SQLite database schema (`schema.sql`) for StreamHub.

---

## 🚀 Free Tier Guarantees ($0.00/month)

| Service | Cloudflare Free Tier Allocation | StreamHub Usage | Purpose |
| :--- | :--- | :--- | :--- |
| **Cloudflare D1** | **5 GB Storage** (5,000,000 row reads / day) | ~6.14 MB for 560 shows | Master ACID relational SQLite database |
| **Workers KV** | **1 GB Storage** (100,000 reads / day) | ~3.5 MB sharded cache | High-speed edge delivery (~15ms) via ETags |
| **Worker Requests** | **100,000 requests / day** | ~15,000 – 25,000 / day | API endpoints and streaming proxy |
| **Video Streaming** | **Unlimited Bandwidth** | Cloudflare Anycast edge | 10–15ms edge ping to nearest server |

---

## 🗂️ Auto-Sharded KV Architecture (80% / 20 MB Ceiling)

Instead of a monolithic single key, KV stores category-sharded caches with an **80% capacity ceiling (20 MB)** and a **5 MB safety buffer** to permanently eliminate any truncation or data loss:

1. **`catalog_manifest`**:
   Tracks all active shard keys, total item counts, version timestamps, and fill percentages.
2. **`catalog_animes_1`**, **`catalog_animes_2`**, ...:
   Stores anime titles. Auto-rolls over to the next shard index as soon as 20 MB is reached.
3. **`catalog_movies_1`**, **`catalog_movies_2`**, ...:
   Stores movies with identical 20 MB rollover guards.
4. **`catalog_series_1`**, **`catalog_series_2`**, ...:
   Stores web series and multi-season TV shows.
5. **`catalog_version`**:
   ETag and version metadata for 304 Not Modified edge caching.

---

## 🛠️ Step-by-Step Setup Guide

### Step 1: Deploy Updated Worker Code
1. On your Cloudflare Worker page (`stream-proxy`), click **Edit Code**.
2. Replace all existing code with the contents of [`worker.js`](./worker.js).
3. Click **Save and Deploy**.

---

### Step 2: Initialize / Rebuild Category Shards
1. Open your browser and navigate to:
   ```
   https://stream-proxy.area-51-ancientworkers.workers.dev/api/rebuild-shards?key=streamhub_migrate_2026
   ```
2. You will receive an instant JSON response confirming that all 560 shows from D1 have been cleanly partitioned into `catalog_animes_1`, `catalog_movies_1`, and `catalog_series_1`, with fill percentages:
   ```json
   {
     "ok": true,
     "message": "Successfully partitioned catalog into 80%-capped category shards!",
     "manifest": {
       "shards": [
         "catalog_animes_1",
         "catalog_movies_1",
         "catalog_series_1"
       ],
       "totalShows": 560,
       "stats": [...]
     }
   }
   ```
3. Refresh your Cloudflare KV dashboard — you will now see the partitioned category shards alongside your VIP vouchers!
