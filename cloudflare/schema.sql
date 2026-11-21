-- =====================================================================
-- StreamHub D1 SQLite Master Database Schema
-- 100% Free Tier Cloudflare Edge Database (5 GB Storage)
-- =====================================================================

-- 1. Shows & Episodes Table (Master Media Catalog)
-- Stores full media documents with indexed columns for ultra-fast filtering
CREATE TABLE IF NOT EXISTS shows (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    type TEXT,
    category TEXT,
    release_year INTEGER DEFAULT 0,
    season_number INTEGER DEFAULT 0,
    part_number INTEGER DEFAULT 0,
    franchise_order REAL DEFAULT 0.0,
    is_movie INTEGER DEFAULT 0,
    episode_count INTEGER DEFAULT 0,
    data_json TEXT NOT NULL,
    created_at INTEGER,
    updated_at INTEGER
);

CREATE INDEX IF NOT EXISTS idx_shows_category ON shows(category);
CREATE INDEX IF NOT EXISTS idx_shows_type ON shows(type);
CREATE INDEX IF NOT EXISTS idx_shows_updated_at ON shows(updated_at);
CREATE INDEX IF NOT EXISTS idx_shows_franchise ON shows(franchise_order);

-- 2. VIP Vouchers Table (Access Codes & Single-Device Bindings)
-- Replaces linear KV key-scanning with indexed, ACID relational records
CREATE TABLE IF NOT EXISTS vouchers (
    code TEXT PRIMARY KEY,
    bound_device_id TEXT DEFAULT '',
    label TEXT DEFAULT '',
    status TEXT DEFAULT 'AVAILABLE',
    duration_days INTEGER DEFAULT 30,
    created_at INTEGER,
    activated_at INTEGER DEFAULT 0,
    expires_at INTEGER DEFAULT 0,
    device_model TEXT DEFAULT '',
    data_json TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vouchers_device ON vouchers(bound_device_id);
CREATE INDEX IF NOT EXISTS idx_vouchers_status ON vouchers(status);
CREATE INDEX IF NOT EXISTS idx_vouchers_created ON vouchers(created_at);

-- 3. Broadcast Announcements Table (Admin Notifications / Push Alerts)
CREATE TABLE IF NOT EXISTS broadcasts (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    message TEXT DEFAULT '',
    category TEXT DEFAULT 'INFO',
    action_type TEXT DEFAULT 'NONE',
    action_target TEXT DEFAULT '',
    action_label TEXT DEFAULT '',
    sender TEXT DEFAULT 'Owner Admin',
    created_at INTEGER,
    expires_at INTEGER,
    is_active INTEGER DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_broadcasts_active ON broadcasts(is_active, expires_at);

-- 4. System Metadata Table (Catalog Version, ETags, Migration Flags)
CREATE TABLE IF NOT EXISTS system_meta (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at INTEGER
);
