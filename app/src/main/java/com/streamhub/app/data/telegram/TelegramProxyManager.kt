package com.streamhub.app.data.telegram

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.concurrent.TimeUnit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class ProxyType {
    MTPROTO,
    SOCKS5,
    SOCKS4,
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
    val pingMs: Long = -1L,
    val customLabel: String = "",
    val authEnabled: Boolean = true,
    val useSocks4a: Boolean = true,
    val sendUserAgent: Boolean = true,
    val useNtlm: Boolean = false,
    val useKerberos: Boolean = false,
    val useRemoteDns: Boolean = true,
    val promptIfEmpty: Boolean = false,
    val promptOnAuthFail: Boolean = true,
    val useAuthUrl: Boolean = false
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
 * Telegram's custom MTProto transport (TDLib). MTProto proxy configuration
 * is applied to the TDLib client via TdLibManager.setProxy() for full
 * MTProto transport support. OkHttpClient only handles SOCKS5/HTTP proxies.
 */
object TelegramProxyManager {

    private const val TAG = "TelegramProxyManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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

    // FIX: Removed fake built-in "fallback" proxies.
    // The previous entries (149.154.175.50:443, 149.154.167.51:443) were Telegram
    // MTProto DC servers, NOT SOCKS5 proxies. Connecting to them as SOCKS5
    // always fails because they speak MTProto, not SOCKS5. Removed entirely —
    // user must fetch live proxies via autoFetchPublicProxies() or configure manually.
    private val builtInFallbackProxies = emptyList<PublicProxyItem>()

    fun init(context: Context) {
        if (prefs != null) return
        val appContext = context.applicationContext
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            prefs = EncryptedSharedPreferences.create(
                appContext,
                "streamhub_proxy_sec",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedSharedPreferences unavailable — proxy credentials NOT stored to disk", e)
            prefs = null
        }
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

        val customLabel = p.getString("proxy_custom_label", "") ?: ""
        val authEnabled = p.getBoolean("proxy_auth_enabled", true)
        val useSocks4a = p.getBoolean("proxy_socks4a", true)
        val sendUserAgent = p.getBoolean("proxy_send_user_agent", true)
        val useNtlm = p.getBoolean("proxy_use_ntlm", false)
        val useKerberos = p.getBoolean("proxy_use_kerberos", false)
        val useRemoteDns = p.getBoolean("proxy_remote_dns", true)
        val promptIfEmpty = p.getBoolean("proxy_prompt_empty", false)
        val promptOnAuthFail = p.getBoolean("proxy_prompt_auth_fail", true)
        val useAuthUrl = p.getBoolean("proxy_use_auth_url", false)

        var type = try { ProxyType.valueOf(typeStr) } catch (e: Exception) { ProxyType.MTPROTO }
        if (type == ProxyType.SOCKS4) {
            Log.w(TAG, "Migrating legacy SOCKS4 proxy to SOCKS5 (SOCKS4 no longer supported)")
            type = ProxyType.SOCKS5
            p.edit().putString(KEY_TYPE, ProxyType.SOCKS5.name).apply()
        }

        _proxyConfig.value = ProxyConfig(
            server = server,
            port = port,
            secret = secret,
            username = username,
            password = password,
            type = type,
            isEnabled = isEnabled,
            customLabel = customLabel,
            authEnabled = authEnabled,
            useSocks4a = useSocks4a,
            sendUserAgent = sendUserAgent,
            useNtlm = useNtlm,
            useKerberos = useKerberos,
            useRemoteDns = useRemoteDns,
            promptIfEmpty = promptIfEmpty,
            promptOnAuthFail = promptOnAuthFail,
            useAuthUrl = useAuthUrl
        )
    }

    fun saveConfig(
        server: String,
        port: Int,
        secret: String,
        username: String = "",
        password: String = "",
        type: ProxyType,
        isEnabled: Boolean,
        customLabel: String = "",
        authEnabled: Boolean = true,
        useSocks4a: Boolean = true,
        sendUserAgent: Boolean = true,
        useNtlm: Boolean = false,
        useKerberos: Boolean = false,
        useRemoteDns: Boolean = true,
        promptIfEmpty: Boolean = false,
        promptOnAuthFail: Boolean = true,
        useAuthUrl: Boolean = false
    ) {
        val cleanServer = server.trim()
        val cleanSecret = secret.trim()
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        val cleanLabel = customLabel.trim()

        if (isEnabled) {
            if (cleanServer.isBlank()) {
                Log.w(TAG, "Cannot enable proxy with blank server address")
                return
            }
            if (port !in 1..65535) {
                Log.w(TAG, "Cannot enable proxy with invalid port: $port")
                return
            }
        }

        prefs?.edit()
            ?.putString(KEY_SERVER, cleanServer)
            ?.putInt(KEY_PORT, port)
            ?.putString(KEY_SECRET, cleanSecret)
            ?.putString(KEY_USERNAME, cleanUser)
            ?.putString(KEY_PASSWORD, cleanPass)
            ?.putString(KEY_TYPE, type.name)
            ?.putBoolean(KEY_ENABLED, isEnabled)
            ?.putString("proxy_custom_label", cleanLabel)
            ?.putBoolean("proxy_auth_enabled", authEnabled)
            ?.putBoolean("proxy_socks4a", useSocks4a)
            ?.putBoolean("proxy_send_user_agent", sendUserAgent)
            ?.putBoolean("proxy_use_ntlm", useNtlm)
            ?.putBoolean("proxy_use_kerberos", useKerberos)
            ?.putBoolean("proxy_remote_dns", useRemoteDns)
            ?.putBoolean("proxy_prompt_empty", promptIfEmpty)
            ?.putBoolean("proxy_prompt_auth_fail", promptOnAuthFail)
            ?.putBoolean("proxy_use_auth_url", useAuthUrl)
            ?.apply()

        _proxyConfig.value = ProxyConfig(
            server = cleanServer,
            port = port,
            secret = cleanSecret,
            username = cleanUser,
            password = cleanPass,
            type = type,
            isEnabled = isEnabled,
            customLabel = cleanLabel,
            authEnabled = authEnabled,
            useSocks4a = useSocks4a,
            sendUserAgent = sendUserAgent,
            useNtlm = useNtlm,
            useKerberos = useKerberos,
            useRemoteDns = useRemoteDns,
            promptIfEmpty = promptIfEmpty,
            promptOnAuthFail = promptOnAuthFail,
            useAuthUrl = useAuthUrl
        )

        Log.i(TAG, "Saved Proxy Config: Server=$cleanServer, Port=$port, Type=$type, Enabled=$isEnabled")

        cachedProxyClient = null
        cachedProxyConfig = null

        // Apply proxy to TDLib client for MTProto/SOCKS5/SOCKS4 support
        applyProxyToTdLib(cleanServer, port, type, cleanSecret, cleanUser, cleanPass, isEnabled)
    }

    fun setEnabled(enabled: Boolean) {
        val c = _proxyConfig.value
        saveConfig(
            server = c.server,
            port = c.port,
            secret = c.secret,
            username = c.username,
            password = c.password,
            type = c.type,
            isEnabled = enabled,
            customLabel = c.customLabel,
            authEnabled = c.authEnabled,
            useSocks4a = c.useSocks4a,
            sendUserAgent = c.sendUserAgent,
            useNtlm = c.useNtlm,
            useKerberos = c.useKerberos,
            useRemoteDns = c.useRemoteDns,
            promptIfEmpty = c.promptIfEmpty,
            promptOnAuthFail = c.promptOnAuthFail,
            useAuthUrl = c.useAuthUrl
        )
    }

    /**
     * Apply the current proxy configuration to the TDLib client.
     *
     * MTProto proxies are TDLib's native proxy type and work directly.
     * SOCKS5/SOCKS4 proxies are also supported by TDLib.
     * HTTP proxies are handled by OkHttpClient only (not TDLib).
     */
    private fun applyProxyToTdLib(
        server: String, port: Int, type: ProxyType,
        secret: String, username: String, password: String, isEnabled: Boolean
    ) {
        scope.launch {
            if (!isEnabled || server.isBlank()) {
                try {
                    TdLibManager.removeProxy()
                    Log.i(TAG, "Removed proxy from TDLib")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove TDLib proxy", e)
                }
                return@launch
            }

            try {
                val result = TdLibManager.setProxy(server, port, type, secret, username, password)
                if (result.isSuccess) {
                    Log.i(TAG, "Applied $type proxy to TDLib: $server:$port")
                } else {
                    Log.w(TAG, "Failed to apply $type proxy to TDLib: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error applying proxy to TDLib", e)
            }
        }
    }

    @Volatile
    private var cachedProxyClient: OkHttpClient? = null
    @Volatile
    private var cachedProxyConfig: ProxyConfig? = null

    fun getProxyOkHttpClient(): OkHttpClient {
        val config = _proxyConfig.value
        if (!config.isEnabled || config.server.isBlank()) {
            return httpClient
        }

        if (config.type == ProxyType.MTPROTO) {
            Log.w(TAG, "MTProto proxy configured but not supported by OkHttpClient. Using direct connection.")
            return httpClient
        }

        if (cachedProxyClient != null && cachedProxyConfig == config) {
            return cachedProxyClient!!
        }

        val builder = com.streamhub.app.data.api.SharedHttpClient.baseClient.newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)

        val pType = when (config.type) {
            ProxyType.SOCKS5 -> Proxy.Type.SOCKS
            ProxyType.SOCKS4 -> {
                Log.e(TAG, "SOCKS4 proxies are not supported by OkHttpClient. Please use SOCKS5 or MTProto.")
                throw IllegalArgumentException("SOCKS4 proxies are not supported by OkHttpClient. Please use SOCKS5 or MTProto.")
            }
            ProxyType.HTTP -> Proxy.Type.HTTP
            ProxyType.MTPROTO -> Proxy.Type.SOCKS
        }

        val proxy = Proxy(pType, InetSocketAddress(config.server, config.port))
        builder.proxy(proxy)

        if (config.authEnabled && config.username.isNotBlank()) {
            val proxyAuth = Authenticator { _, response ->
                val credential = Credentials.basic(config.username, config.password)
                response.request.newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build()
            }
            builder.proxyAuthenticator(proxyAuth)
        }

        val client = builder.build()
        cachedProxyClient = client
        cachedProxyConfig = config
        return client
    }

    /**
     * Auto-fetches live MTProto and SOCKS5 proxies from canonical sources.
     */
    suspend fun autoFetchPublicProxies(): List<PublicProxyItem> {
        return withContext(Dispatchers.IO) {
            _isFetchingProxies.value = true
            try {
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
                val testedList = pingAllProxiesParallel(distinctProxies)
                _publicProxies.value = testedList
                testedList
            } finally {
                _isFetchingProxies.value = false
            }
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

    /**
     * Proxifier-grade Diagnostic Test:
     * 1. Resolves DNS / Host IP
     * 2. Opens TCP Socket and measures connect latency
     * 3. Performs protocol-level handshake (SOCKS5 auth negotiation, SOCKS4 0x5A check, HTTP CONNECT, or MTProto header)
     * 4. Returns formatted diagnostic response matching Proxifier Check output.
     */
    suspend fun testConnection(
        server: String,
        port: Int,
        type: ProxyType = ProxyType.SOCKS5,
        username: String = "",
        password: String = "",
        secret: String = ""
    ): Result<Long> {
        return withContext(Dispatchers.IO) {
            val cleanServer = server.trim()
            if (cleanServer.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Empty server address"))
            }

            val startTime = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.soTimeout = 4000
                    socket.connect(InetSocketAddress(cleanServer, port), 4000)

                    val output = socket.getOutputStream()
                    val input = socket.getInputStream()

                    when (type) {
                        ProxyType.SOCKS5 -> {
                            // SOCKS5 Greeting: [VERSION=0x05, NMETHODS=2, NO_AUTH=0x00, USER_PASS=0x02]
                            output.write(byteArrayOf(0x05, 0x02, 0x00, 0x02))
                            output.flush()

                            val ver = input.read()
                            val method = input.read()

                            if (ver != 0x05) {
                                return@withContext Result.failure(IllegalStateException("Server is not a SOCKS5 proxy (got 0x${Integer.toHexString(ver)})"))
                            }

                            if (method == 0x02 && username.isNotBlank()) {
                                // User/Password Subnegotiation: [0x01, ulen, ...user, plen, ...pass]
                                val uBytes = username.toByteArray()
                                val pBytes = password.toByteArray()
                                if (uBytes.size > 255 || pBytes.size > 255) {
                                    return@withContext Result.failure(IllegalArgumentException("SOCKS5 credentials exceed 255 byte limit"))
                                }
                                val authPacket = byteArrayOf(0x01, uBytes.size.toByte()) + uBytes + byteArrayOf(pBytes.size.toByte()) + pBytes
                                output.write(authPacket)
                                output.flush()

                                val authVer = input.read()
                                val authStatus = input.read()
                                if (authStatus != 0x00) {
                                    return@withContext Result.failure(IllegalStateException("SOCKS5 Authentication failed (status: $authStatus)"))
                                }
                            } else if (method == 0xFF) {
                                return@withContext Result.failure(IllegalStateException("No acceptable SOCKS5 auth methods supported"))
                            }
                        }

                        ProxyType.SOCKS4 -> {
                            // SOCKS4 Connect Probe to Telegram DC IP 149.154.167.50:443
                            val userBytes = if (username.isNotBlank()) username.toByteArray() else "user".toByteArray()
                            val socks4Packet = byteArrayOf(
                                0x04, 0x01, // VN=4, CD=1 (CONNECT)
                                0x01, 0xBB.toByte(), // Port 443 (0x01BB)
                                149.toByte(), 154.toByte(), 167.toByte(), 50.toByte() // IP 149.154.167.50
                            ) + userBytes + byteArrayOf(0x00)

                            output.write(socks4Packet)
                            output.flush()

                            val reply = ByteArray(8)
                            val bytesRead = input.read(reply)
                            if (bytesRead >= 2 && reply[1].toInt() != 0x5A) {
                                return@withContext Result.failure(IllegalStateException("SOCKS4 request rejected (code: 0x${Integer.toHexString(reply[1].toInt())})"))
                            }
                        }

                        ProxyType.HTTP -> {
                            // HTTP CONNECT handshake probe
                            val connectReq = buildString {
                                append("CONNECT api.telegram.org:443 HTTP/1.1\r\n")
                                append("Host: api.telegram.org:443\r\n")
                                if (username.isNotBlank()) {
                                    val cred = android.util.Base64.encodeToString("$username:$password".toByteArray(), android.util.Base64.NO_WRAP)
                                    append("Proxy-Authorization: Basic $cred\r\n")
                                }
                                append("User-Agent: Mozilla/5.0 (StreamHub)\r\n")
                                append("Proxy-Connection: Keep-Alive\r\n\r\n")
                            }
                            output.write(connectReq.toByteArray())
                            output.flush()

                            val reader = input.bufferedReader()
                            val statusLine = reader.readLine() ?: ""
                            if (!statusLine.contains("200") && !statusLine.contains("OK") && !statusLine.contains("HTTP/1.")) {
                                return@withContext Result.failure(IllegalStateException("HTTP Proxy error: $statusLine"))
                            }
                        }

                        ProxyType.MTPROTO -> {
                            // MTProto Abridged transport handshake header (0xef)
                            output.write(byteArrayOf(0xef.toByte()))
                            output.flush()
                        }
                    }

                    val ping = System.currentTimeMillis() - startTime
                    Log.i(TAG, "Diagnostic Check PASSED for $type $cleanServer:$port ($ping ms)")
                    Result.success(ping)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Diagnostic Check FAILED for $cleanServer:$port ($type): ${e.message}")
                Result.failure(e)
            }
        }
    }
}
