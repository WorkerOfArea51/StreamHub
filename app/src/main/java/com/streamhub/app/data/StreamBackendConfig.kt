package com.streamhub.app.data

/**
 * Centralized Streaming Backend Configuration & Domain Migration Utility.
 *
 * Manages the transition from legacy Alwaysdata hosting (streamhub69.alwaysdata.net)
 * to the high-performance Serv00 VPS (midnighthawk.serv00.net), and provides future-proof
 * dynamic base URL rewriting across playback, downloads, and batch imports.
 */
object StreamBackendConfig {

    /** The active production Serv00 VPS streaming domain */
    const val DEFAULT_STREAMING_HOST = "midnighthawk.serv00.net"

    /** Legacy Alwaysdata domain being migrated */
    const val LEGACY_STREAMING_HOST = "streamhub69.alwaysdata.net"

    /** Production Base URL for media endpoints */
    const val DEFAULT_BASE_URL = "https://$DEFAULT_STREAMING_HOST"

    /** Production API endpoint for batch episode resolution */
    const val DEFAULT_BATCH_API_BASE_URL = "https://$DEFAULT_STREAMING_HOST/api/batch/"

    /**
     * Regex matching any variant of the Alwaysdata host, e.g.:
     * streamhub69.alwaysdata.net, streamhub.alwaysdata.net, or *.alwaysdata.net
     */
    private val LEGACY_HOST_REGEX = Regex("""(?i)\b[a-z0-9_.-]*alwaysdata\.net\b""")

    /**
     * Checks if a URL or host belongs to our Telegram streaming backend
     * (either the current Serv00 VPS or legacy Alwaysdata).
     */
    fun isBackendHost(urlOrHost: String): Boolean {
        if (urlOrHost.isBlank()) return false
        return urlOrHost.contains("serv00.net", ignoreCase = true) ||
               urlOrHost.contains("alwaysdata.net", ignoreCase = true)
    }

    /**
     * Checks if a URL or host specifically belongs to legacy Alwaysdata hosting.
     */
    fun isLegacyBackendHost(urlOrHost: String): Boolean {
        if (urlOrHost.isBlank()) return false
        return urlOrHost.contains("alwaysdata.net", ignoreCase = true)
    }

    /**
     * Migrates a URL pointing to the legacy backend (alwaysdata.net) to the new
     * Serv00 VPS domain (midnighthawk.serv00.net).
     *
     * - Preserves path (/dl/...), query params, and anchors.
     * - Normalizes `/stream/<hash>` to `/dl/<hash>` for backend direct playback.
     * - Upgrades `http://` to `https://` for backend traffic.
     * - Preserves external direct links (YouTube, custom CDNs, localhost, local file paths).
     */
    fun migrateUrl(
        url: String,
        targetHost: String = DEFAULT_STREAMING_HOST,
        normalizeRoute: Boolean = true
    ): String {
        if (url.isBlank()) return url
        var migrated = url.trim()
        if (migrated.contains("alwaysdata.net", ignoreCase = true)) {
            migrated = migrated.replace(LEGACY_HOST_REGEX, targetHost)
            if (migrated.startsWith("http://", ignoreCase = true)) {
                migrated = "https://" + migrated.substring(7)
            }
        }
        if (normalizeRoute && isBackendHost(migrated) && migrated.contains("/stream/")) {
            migrated = migrated.replace("/stream/", "/dl/")
        }
        return migrated
    }

    /**
     * Resolves an F2L / batch API endpoint URL.
     * If the input contains a legacy host, rewrites to [targetHost].
     */
    fun resolveBatchApiUrl(input: String, targetHost: String = DEFAULT_STREAMING_HOST): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return ""
        val migrated = migrateUrl(trimmed, targetHost)
        return if (migrated.startsWith("http://") || migrated.startsWith("https://")) {
            migrated
        } else {
            "https://$targetHost/api/batch/$migrated"
        }
    }
}
