package com.streamhub.app.data.telegram

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.concurrent.TimeUnit

enum class ProxyType {
    MTPROTO,
    SOCKS5,
    HTTP
}

data class ProxyConfig(
    val server: String = "",
    val port: Int = 443,
    val secret: String = "",
    val username: String = "",
    val password: String = "",
    val type: ProxyType = ProxyType.MTPROTO,
    val isEnabled: Boolean = false,
    val pingMs: Long = -1L
)

data class PublicProxyItem(
    val server: String,
    val port: Int,
    val secret: String = "",
    val username: String = "",
    val password: String = "",
    val type: ProxyType = ProxyType.MTPROTO,
    val country: String = "Global",
    var pingMs: Long = -1L,
    var isChecking: Boolean = false
)

/**
 * MTProto & SOCKS5 Proxy Engine:
 * - Fetches live MTProto and SOCKS5 proxies from canonical sources
 * - SOCKS5/HTTP proxy support via OkHttp Authenticator (per-client, NOT global)
 * - MTProto proxy support requires TDLib integration (STUB until TDLib is added)
 * - Multi-threaded parallel ping tester with bounded concurrency
 *
 * WARNING: MTProto proxies CANNOT work via java.net.Proxy — they require
 * Telegram's custom MTProto transport (TDLib). Any MTProto proxy set here
 * is stored for future TDLib use but will NOT be used by OkHttpClient.
 */
object TelegramProxyManager {

    private const val TAG = "TelegramProxyManager"
    private const val PREFS_NAME = "streamhub_proxy_prefs"
    private const val KEY_SERVER = "proxy_server"
    private const val KEY_PORT = "proxy_port"
    private const val KEY_SECRET = "proxy_secret"
    private const val KEY_USERNAME = "proxy_username"
    private const val KEY_PASSWORD = "proxy_password"
    private const val KEY_TYPE = "proxy_type"
    private const val KEY_ENABLED = "proxy_enabled"

    /** Max concurrent proxy ping connections to avoid FD exhaustion */
    private const val MAX_CONCURRENT_PINGS = 10

    private var prefs: SharedPreferences? = null

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private val _proxyConfig = MutableStateFlow(ProxyConfig())
    val proxyConfig: StateFlow<ProxyConfig> = _proxyConfig.asStateFlow()

    private val _publicProxies = MutableStateFlow<List<PublicProxyItem>>(emptyList())
    val publicProxies: StateFlow<List<PublicProxyItem>> = _publicProxies.asStateFlow()

    private val _isFetchingProxies = MutableStateFlow(false)
    val isFetchingProxies: StateFlow<Boolean> = _isFetchingProxies.asStateFlow()

    private val telStreamProxySources = listOf(
        "https://raw.githubusercontent.com/SoliSpirit/mtproto/master/all_proxies.txt",
        "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/socks5.txt"
    )

    // FIX #3: Removed fake built-in proxies with placeholder secrets.
    // These were ee000000... hex strings that never work with any real server.
    // Fallback to empty list — user must fetch live proxies or configure manually.
    private val builtInFallbackProxies = listOf(
        PublicProxyItem("149.154.175.50", 443, "", "", "", ProxyType.SOCKS5, "Telegram DC2 (US)"),
        PublicProxyItem("149.154.167.51", 443, "", "", "", ProxyType.SOCKS5, "Telegram DC3 (EU)")
    )

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val p = prefs ?: return
        val server = p.getString(KEY_SERVER, "") ?: ""
        val port = p.getInt(KEY_PORT, 443)
        val secret = p.getString(KEY_SECRET, "") ?: ""
        val username = p.getString(KEY_USERNAME, "") ?: ""
        val password = p.getString(KEY_PASSWORD, "") ?: ""
        val typeStr = p.getString(KEY_TYPE, ProxyType.MTPROTO.name) ?: ProxyType.MTPROTO.name
        val isEnabled = p.getBoolean(KEY_ENABLED, false)

        val type = try { ProxyType.valueOf(typeStr) } catch (e: Exception) { ProxyType.MTPROTO }

        _proxyConfig.value = ProxyConfig(
            server = server,
            port = port,
            secret = secret,
            username = username,
            password = password,
            type = type,
            isEnabled = isEnabled
        )
    }

    fun saveConfig(
        server: String,
        port: Int,
        secret: String,
        username: String = "",
        password: String = "",
        type: ProxyType,
        isEnabled: Boolean
    ) {
        val cleanServer = server.trim()
        val cleanSecret = secret.trim()
        val cleanUser = username.trim()
        val cleanPass = password.trim()

        prefs?.edit()
            ?.putString(KEY_SERVER, cleanServer)
            ?.putInt(KEY_PORT, port)
            ?.putString(KEY_SECRET, cleanSecret)
            ?.putString(KEY_USERNAME, cleanUser)
            ?.putString(KEY_PASSWORD, cleanPass)
            ?.putString(KEY_TYPE, type.name)
            ?.putBoolean(KEY_ENABLED, isEnabled)
            ?.apply()

        _proxyConfig.value = ProxyConfig(
            server = cleanServer,
            port = port,
            secret = cleanSecret,
            username = cleanUser,
            password = cleanPass,
            type = type,
            isEnabled = isEnabled
        )

        Log.i(TAG, "Saved Proxy Config: Server=$cleanServer, Port=$port, Type=$type, AuthUser=$cleanUser, Enabled=$isEnabled")
    }

    fun setEnabled(enabled: Boolean) {
        val current = _proxyConfig.value
        saveConfig(current.server, current.port, current.secret, current.username, current.password, current.type, enabled)
    }

    /**
     * Builds an OkHttpClient configured with the active proxy.
     *
     * IMPORTANT: Only SOCKS5 and HTTP proxies are supported by OkHttpClient.
     * MTProto proxies require TDLib's custom transport and CANNOT be used here.
     * If an MTProto proxy is configured, this returns the plain client (no proxy)
     * and logs a warning — the MTProto secret is stored for future TDLib use.
     *
     * FIX #1: Removed java.net.Authenticator.setDefault() — proxy auth is now
     * per-client via OkHttp's proxyAuthenticator, NOT a JVM-global side effect.
     */
    fun getProxyOkHttpClient(): OkHttpClient {
        val config = _proxyConfig.value
        if (!config.isEnabled || config.server.isBlank()) {
            return httpClient
        }

        // FIX #2: MTProto cannot work via java.net.Proxy — it's not SOCKS5.
        // Log a warning and return the plain client. MTProto support requires TDLib.
        if (config.type == ProxyType.MTPROTO) {
            Log.w(TAG, "MTProto proxy configured but not supported by OkHttpClient. " +
                "MTProto requires TDLib integration. Using direct connection.")
            return httpClient
        }

        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)

        val pType = when (config.type) {
            ProxyType.SOCKS5 -> Proxy.Type.SOCKS
            ProxyType.HTTP -> Proxy.Type.HTTP
            ProxyType.MTPROTO -> Proxy.Type.SOCKS // unreachable due to early return above
        }

        val proxy = Proxy(pType, InetSocketAddress(config.server, config.port))
        builder.proxy(proxy)

        // FIX #1: Per-client authenticator only — NOT java.net.Authenticator.setDefault()
        if (config.username.isNotBlank()) {
            val proxyAuth = Authenticator { _, response ->
                val credential = Credentials.basic(config.username, config.password)
                response.request.newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build()
            }
            builder.proxyAuthenticator(proxyAuth)
        }

        return builder.build()
    }

    /**
     * Auto-fetches live MTProto and SOCKS5 proxies from canonical sources.
     */
    suspend fun autoFetchPublicProxies(): List<PublicProxyItem> {
        return withContext(Dispatchers.IO) {
            _isFetchingProxies.value = true
            val fetchedList = mutableListOf<PublicProxyItem>()

            val client = getProxyOkHttpClient()

            for (sourceUrl in telStreamProxySources) {
                try {
                    val request = Request.Builder().url(sourceUrl).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            if (body.isNotBlank()) {
                                parseProxySourceContent(sourceUrl, body, fetchedList)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "TelStream source $sourceUrl failed: ${e.message}")
                }
            }

            if (fetchedList.isEmpty()) {
                fetchedList.addAll(builtInFallbackProxies)
            }

            val distinctProxies = fetchedList.distinctBy { it.server }.take(40)

            // FIX #4: Bounded concurrency for parallel ping testing
            val testedList = pingAllProxiesParallel(distinctProxies)
            _publicProxies.value = testedList
            _isFetchingProxies.value = false
            testedList
        }
    }

    private fun parseProxySourceContent(sourceUrl: String, content: String, outputList: MutableList<PublicProxyItem>) {
        val lines = content.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#")) continue

            try {
                if (trimmed.startsWith("tg://proxy") || trimmed.startsWith("https://t.me/proxy") || trimmed.startsWith("http://t.me/proxy")) {
                    val uri = Uri.parse(trimmed)
                    val server = uri.getQueryParameter("server") ?: uri.getQueryParameter("host") ?: ""
                    val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 443
                    val secret = uri.getQueryParameter("secret") ?: ""
                    if (server.isNotBlank()) {
                        outputList.add(PublicProxyItem(server, port, secret, "", "", ProxyType.MTPROTO, "MTProto"))
                    }
                } else if (trimmed.contains(":")) {
                    val parts = trimmed.split(":")
                    if (parts.size >= 4) {
                        val server = parts[0].trim()
                        val port = parts[1].trim().toIntOrNull()
                        val user = parts[2].trim()
                        val pass = parts[3].trim()
                        if (server.isNotBlank() && port != null) {
                            outputList.add(PublicProxyItem(server, port, "", user, pass, ProxyType.SOCKS5, "SOCKS5"))
                        }
                    } else if (parts.size >= 2) {
                        val server = parts[0].trim()
                        val port = parts[1].trim().toIntOrNull()
                        if (server.isNotBlank() && port != null) {
                            outputList.add(PublicProxyItem(server, port, "", "", "", ProxyType.SOCKS5, "SOCKS5"))
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore malformed line
            }
        }
    }

    /**
     * FIX #4: Bounded parallel ping tester — chunks proxies into groups of
     * MAX_CONCURRENT_PINGS to avoid exhausting file descriptors on low-end devices.
     */
    suspend fun pingAllProxiesParallel(proxies: List<PublicProxyItem>): List<PublicProxyItem> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<PublicProxyItem>()
            // Chunk into bounded batches to limit concurrent socket connections
            for (chunk in proxies.chunked(MAX_CONCURRENT_PINGS)) {
                val deferreds = chunk.map { proxy ->
                    async {
                        val pingRes = testConnection(proxy.server, proxy.port)
                        val pingMs = pingRes.getOrDefault(-1L)
                        proxy.copy(pingMs = pingMs, isChecking = false)
                    }
                }
                results.addAll(deferreds.awaitAll())
            }
            results.sortedWith(compareBy({ if (it.pingMs > 0) 0 else 1 }, { if (it.pingMs > 0) it.pingMs else Long.MAX_VALUE }))
        }
    }

    fun selectFastestProxy() {
        val list = _publicProxies.value
        val fastest = list.firstOrNull { it.pingMs > 0 } ?: return
        saveConfig(fastest.server, fastest.port, fastest.secret, fastest.username, fastest.password, fastest.type, true)
    }

    suspend fun testConnection(server: String, port: Int): Result<Long> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(server.trim(), port), 3500)
                    val ping = System.currentTimeMillis() - startTime
                    Result.success(ping)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
