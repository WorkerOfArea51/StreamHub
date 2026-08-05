package com.streamhub.app.data.telegram

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.InetSocketAddress
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
    val type: ProxyType = ProxyType.MTPROTO,
    val isEnabled: Boolean = false,
    val pingMs: Long = -1L
)

data class PublicProxyItem(
    val server: String,
    val port: Int,
    val secret: String,
    val country: String = "Global 🌐",
    var pingMs: Long = -1L,
    var isChecking: Boolean = false
)

/**
 * TelStream-Inspired MTProto Proxy & Censorship Bypass System:
 * - Auto-fetches live MTProto public proxies from public repositories.
 * - Parallel multi-threaded ping tester to find the fastest proxy.
 * - Auto-selects best proxy with lowest latency.
 * - Custom manual MTProto/SOCKS5 server configuration.
 */
object TelegramProxyManager {

    private const val TAG = "TelegramProxyManager"
    private const val PREFS_NAME = "streamhub_proxy_prefs"
    private const val KEY_SERVER = "proxy_server"
    private const val KEY_PORT = "proxy_port"
    private const val KEY_SECRET = "proxy_secret"
    private const val KEY_TYPE = "proxy_type"
    private const val KEY_ENABLED = "proxy_enabled"

    private var prefs: SharedPreferences? = null

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val _proxyConfig = MutableStateFlow(ProxyConfig())
    val proxyConfig: StateFlow<ProxyConfig> = _proxyConfig.asStateFlow()

    private val _publicProxies = MutableStateFlow<List<PublicProxyItem>>(emptyList())
    val publicProxies: StateFlow<List<PublicProxyItem>> = _publicProxies.asStateFlow()

    private val _isFetchingProxies = MutableStateFlow(false)
    val isFetchingProxies: StateFlow<Boolean> = _isFetchingProxies.asStateFlow()

    // Default Fallback Public MTProto Proxies (TelStream Proxy Network)
    private val defaultPublicProxies = listOf(
        PublicProxyItem("149.154.175.50", 443, "ee00000000000000000000000000000000", "US 🇺🇸"),
        PublicProxyItem("149.154.167.51", 443, "ee00000000000000000000000000000000", "EU 🇪🇺"),
        PublicProxyItem("91.108.56.160", 443, "ee00000000000000000000000000000000", "SG 🇸🇬"),
        PublicProxyItem("149.154.175.100", 443, "ee00000000000000000000000000000000", "DE 🇩🇪"),
        PublicProxyItem("91.108.4.150", 443, "ee00000000000000000000000000000000", "UK 🇬🇧")
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
        val typeStr = p.getString(KEY_TYPE, ProxyType.MTPROTO.name) ?: ProxyType.MTPROTO.name
        val isEnabled = p.getBoolean(KEY_ENABLED, false)

        val type = try { ProxyType.valueOf(typeStr) } catch (e: Exception) { ProxyType.MTPROTO }

        _proxyConfig.value = ProxyConfig(
            server = server,
            port = port,
            secret = secret,
            type = type,
            isEnabled = isEnabled
        )
    }

    fun saveConfig(server: String, port: Int, secret: String, type: ProxyType, isEnabled: Boolean) {
        val cleanServer = server.trim()
        val cleanSecret = secret.trim()

        prefs?.edit()
            ?.putString(KEY_SERVER, cleanServer)
            ?.putInt(KEY_PORT, port)
            ?.putString(KEY_SECRET, cleanSecret)
            ?.putString(KEY_TYPE, type.name)
            ?.putBoolean(KEY_ENABLED, isEnabled)
            ?.apply()

        _proxyConfig.value = ProxyConfig(
            server = cleanServer,
            port = port,
            secret = cleanSecret,
            type = type,
            isEnabled = isEnabled
        )

        Log.i(TAG, "Saved Proxy Config: Server=$cleanServer, Port=$port, Type=$type, Enabled=$isEnabled")
    }

    fun setEnabled(enabled: Boolean) {
        val current = _proxyConfig.value
        saveConfig(current.server, current.port, current.secret, current.type, enabled)
    }

    /**
     * TelStream Auto-Fetcher: Fetches live public MTProto proxies and measures ping in parallel.
     */
    suspend fun autoFetchPublicProxies(): List<PublicProxyItem> {
        return withContext(Dispatchers.IO) {
            _isFetchingProxies.value = true
            val fetchedList = mutableListOf<PublicProxyItem>()
            fetchedList.addAll(defaultPublicProxies)

            try {
                val request = Request.Builder()
                    .url("https://raw.githubusercontent.com/WorkerOfArea51/TelStream/main/proxies.json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        if (body.isNotBlank()) {
                            val jsonArray = JSONArray(body)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val server = obj.optString("server", "")
                                val port = obj.optInt("port", 443)
                                val secret = obj.optString("secret", "")
                                val country = obj.optString("country", "Global 🌐")
                                if (server.isNotBlank()) {
                                    fetchedList.add(PublicProxyItem(server, port, secret, country))
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Auto-fetch live proxies failed, using default TelStream pool: ${e.message}")
            }

            // Run parallel socket ping testing across all proxies
            val testedList = pingAllProxiesParallel(fetchedList)
            _publicProxies.value = testedList
            _isFetchingProxies.value = false
            testedList
        }
    }

    /**
     * Multi-threaded parallel ping tester for proxies.
     */
    suspend fun pingAllProxiesParallel(proxies: List<PublicProxyItem>): List<PublicProxyItem> {
        return withContext(Dispatchers.IO) {
            val deferreds = proxies.map { proxy ->
                async {
                    val pingRes = testConnection(proxy.server, proxy.port)
                    val pingMs = pingRes.getOrDefault(-1L)
                    proxy.copy(pingMs = pingMs, isChecking = false)
                }
            }
            val results = deferreds.awaitAll()
            // Sort by ping: online proxies first (sorted lowest ping to highest), offline last
            results.sortedWith(compareBy({ if (it.pingMs > 0) 0 else 1 }, { if (it.pingMs > 0) it.pingMs else Long.MAX_VALUE }))
        }
    }

    /**
     * Auto-selects the fastest proxy from public list.
     */
    fun selectFastestProxy() {
        val list = _publicProxies.value
        val fastest = list.firstOrNull { it.pingMs > 0 } ?: defaultPublicProxies.first()
        saveConfig(fastest.server, fastest.port, fastest.secret, ProxyType.MTPROTO, true)
    }

    /**
     * Tests socket connection & ping latency to a specific proxy server.
     */
    suspend fun testConnection(server: String, port: Int): Result<Long> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(server.trim(), port), 4000)
                    val ping = System.currentTimeMillis() - startTime
                    Result.success(ping)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
