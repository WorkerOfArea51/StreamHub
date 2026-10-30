package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DownloadSettings(
    val autoResumeOnWifi: Boolean = true,
    val downloadOverWifiOnly: Boolean = false,
    val downloadTtlDays: Int = 30 // Default: 30 Days (Options: 0 [Never], 1, 3, 7, 14, 30, 60, 90)
)

/**
 * Production Download Preferences & Wi-Fi Auto-Recovery Manager:
 * - Allows users to toggle auto-resume on Wi-Fi recovery on/off
 * - Allows users to restrict downloads strictly to Wi-Fi
 * - Allows users to set Download Auto-Delete TTL retention lifespan (Never [0], 1d, 3d, 7d, 14d, 30d, 60d, 90d)
 * - Dynamically registers a ConnectivityManager network callback for seamless auto-resume
 */
object DownloadSettingsManager {

    private const val TAG = "DownloadSettingsManager"
    private const val PREFS_NAME = "streamhub_download_settings"
    private const val KEY_AUTO_RESUME_WIFI = "auto_resume_wifi"
    private const val KEY_WIFI_ONLY = "download_wifi_only"
    private const val KEY_DOWNLOAD_TTL_DAYS = "download_ttl_days"
    val VALID_TTL_DAYS = listOf(0, 1, 3, 7, 14, 30, 60, 90)

    private lateinit var appContext: Context
    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _settingsFlow = MutableStateFlow(DownloadSettings())
    val settingsFlow: StateFlow<DownloadSettings> = _settingsFlow.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun init(context: Context) {
        if (prefs != null) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromDisk()
        registerNetworkObserver()
    }

    private fun loadFromDisk() {
        val p = prefs ?: return
        val autoResume = p.getBoolean(KEY_AUTO_RESUME_WIFI, true)
        val wifiOnly = p.getBoolean(KEY_WIFI_ONLY, false)
        val ttlDays = p.getInt(KEY_DOWNLOAD_TTL_DAYS, 30)
        _settingsFlow.value = DownloadSettings(
            autoResumeOnWifi = autoResume,
            downloadOverWifiOnly = wifiOnly,
            downloadTtlDays = if (ttlDays in VALID_TTL_DAYS) ttlDays else 30
        )
    }

    fun updateAutoResumeOnWifi(enabled: Boolean) {
        _settingsFlow.value = _settingsFlow.value.copy(autoResumeOnWifi = enabled)
        prefs?.edit()?.putBoolean(KEY_AUTO_RESUME_WIFI, enabled)?.apply()
        Log.i(TAG, "Auto-resume on Wi-Fi updated: $enabled")
    }

    fun updateDownloadOverWifiOnly(enabled: Boolean) {
        _settingsFlow.value = _settingsFlow.value.copy(downloadOverWifiOnly = enabled)
        prefs?.edit()?.putBoolean(KEY_WIFI_ONLY, enabled)?.apply()
        Log.i(TAG, "Download over Wi-Fi only updated: $enabled")

        if (enabled && !isWifiConnected()) {
            pauseActiveDownloadsForCellular()
        }
    }

    fun updateDownloadTtlDays(days: Int) {
        val safeDays = if (days in VALID_TTL_DAYS) days else 30
        _settingsFlow.value = _settingsFlow.value.copy(downloadTtlDays = safeDays)
        prefs?.edit()?.putInt(KEY_DOWNLOAD_TTL_DAYS, safeDays)?.apply()
        Log.i(TAG, "Download auto-delete TTL updated: $safeDays days")
        DownloadManager.enforceDownloadTtlPolicy()
    }

    fun restoreSettings(settings: DownloadSettings) {
        updateAutoResumeOnWifi(settings.autoResumeOnWifi)
        updateDownloadOverWifiOnly(settings.downloadOverWifiOnly)
        updateDownloadTtlDays(settings.downloadTtlDays)
    }

    private fun registerNetworkObserver() {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val caps = connectivityManager.getNetworkCapabilities(network) ?: return
                val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

                Log.d(TAG, "Network became available. isWifi=$isWifi")
                if (isWifi) {
                    onWifiConnected()
                }
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost. Checking Wi-Fi status...")
                if (_settingsFlow.value.downloadOverWifiOnly && !isWifiConnected()) {
                    pauseActiveDownloadsForCellular()
                }
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register NetworkCallback", e)
        }
    }

    private fun isWifiConnected(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private fun onWifiConnected() {
        if (!_settingsFlow.value.autoResumeOnWifi) {
            Log.d(TAG, "Wi-Fi connected but autoResumeOnWifi is disabled by user.")
            return
        }

        scope.launch {
            Log.i(TAG, "Wi-Fi connected — auto-resuming paused and interrupted downloads...")
            runCatching {
                DownloadManager.resumeAllInterruptedDownloads(appContext)
            }.onFailure {
                Log.e(TAG, "Failed to auto-resume downloads on Wi-Fi recovery", it)
            }
        }
    }

    private fun pauseActiveDownloadsForCellular() {
        scope.launch {
            Log.i(TAG, "Wi-Fi disconnected and downloadOverWifiOnly is enabled — pausing downloads...")
            runCatching {
                DownloadManager.pauseAllActiveDownloads()
            }.onFailure {
                Log.e(TAG, "Failed to pause downloads on cellular switch", it)
            }
        }
    }

    fun unregisterNetworkObserver() {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        networkCallback?.let {
            runCatching { connectivityManager.unregisterNetworkCallback(it) }
            networkCallback = null
        }
    }
}
