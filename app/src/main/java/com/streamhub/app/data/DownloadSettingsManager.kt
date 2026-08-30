package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.streamhub.app.player.StreamDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DownloadSettings(
    val autoResumeOnWifi: Boolean = true,
    val downloadOverWifiOnly: Boolean = false
)

/**
 * Production Download Preferences & Wi-Fi Auto-Recovery Manager:
 * - Allows users to toggle auto-resume on Wi-Fi recovery on/off
 * - Allows users to restrict downloads strictly to Wi-Fi
 * - Dynamically registers a ConnectivityManager network callback for seamless auto-resume
 */
object DownloadSettingsManager {

    private const val TAG = "DownloadSettingsManager"
    private const val PREFS_NAME = "streamhub_download_settings"
    private const val KEY_AUTO_RESUME_WIFI = "auto_resume_wifi"
    private const val KEY_WIFI_ONLY = "download_wifi_only"

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
        _settingsFlow.value = DownloadSettings(
            autoResumeOnWifi = autoResume,
            downloadOverWifiOnly = wifiOnly
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
                StreamDownloadManager.resumeDownloads(appContext)
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
                StreamDownloadManager.pauseDownloads()
                DownloadManager.pauseAllActiveDownloads()
            }.onFailure {
                Log.e(TAG, "Failed to pause downloads on cellular switch", it)
            }
        }
    }
}
