# StreamHub Cloudflare Edge Worker Deployment Guide (100% Free Tier)

This directory contains the production Cloudflare Edge Worker script (`worker.js`) that replaces Firebase Firestore for StreamHub.

---

## 🚀 Free Tier Guarantees ($0.00/month)

| Feature | Cloudflare Free Tier Allocation | StreamHub Usage |
| :--- | :--- | :--- |
| **Worker Requests** | 100,000 requests / day | ~15,000 – 25,000 requests / day |
| **KV Storage** | 1 GB Free | ~15 MB for 500 shows + sessions |
| **KV Read Operations** | 100,000 reads / day | Edge-cached via ETag (304 Not Modified) |
| **KV Write Operations** | 1,000 writes / day | Only on show edits / imports |
| **Viewer Heartbeats** | 180s auto-purged KV sessions | 60s client interval with command piggybacking |
| **Video Streaming** | Unlimited Bandwidth | Cloudflare Anycast edge routing (10–15ms ping) |

---

## 🛠️ Step-by-Step Setup (Takes < 2 Minutes)

### Option A: Via Cloudflare Web Dashboard (No Terminal Required)

1. **Log in to Cloudflare Dashboard**:
   - Go to [dash.cloudflare.com](https://dash.cloudflare.com/) and navigate to **Workers & Pages**.
2. **Create KV Namespace**:
   - On the left sidebar, click **KV**.
   - Click **Create namespace**. Name it `streamhub-catalog` and click **Add**.
   - Note down the **Namespace ID** (e.g. `a1b2c3d4...`).
3. **Open or Create Worker**:
   - Under **Workers & Pages**, select your worker (e.g. `stream-proxy`).
   - Go to **Settings** > **Variables and KV**.
   - Under **KV Namespace Bindings**, click **Add binding**:
     - **Variable name**: `CATALOG_KV` *(must match exactly)*
     - **KV namespace**: select `streamhub-catalog`
   - Click **Save and Deploy**.
4. **Paste Worker Code**:
   - Click **Edit Code** (or **Quick Edit**) on your Worker.
   - Replace everything with the contents of [worker.js](file:///d:/Study%20Material/Programming%20Languages/Project%20StreamHub/cloudflare/worker.js).
   - Click **Save and Deploy**.

---

### Option B: Via Wrangler CLI

If you have Node.js and Wrangler installed:
```bash
cd cloudflare
# 1. Login to Cloudflare
npx wrangler login

# 2. Create the KV namespace
npx wrangler kv:namespace create CATALOG_KV

# 3. Paste the generated namespace ID into wrangler.toml under `id = "..."`

# 4. Deploy worker to production
npx wrangler deploy
```

---

## 📲 Catalog Management & Native Cloudflare KV Operation

The StreamHub catalog is **100% native on Cloudflare Edge KV**:
- **Zero Firebase Dependency**: Reads, writes, updates, and batch imports route directly through the Edge Worker endpoints (`/api/catalog`, `/api/catalog/item`, `/api/catalog/import`).
- **Creator Studio Integration**: Adding or editing shows from Creator Studio updates Cloudflare KV in real-time, automatically updating ETags and cache hashes.
- **Backup & Restore**: Use **Catalog Backup & Restore** in Creator Studio (Tab 3: Tools) to export full JSON snapshots or restore previous catalog backups directly to Cloudflare Edge KV.

