package com.streamhub.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkType {
    OFFLINE,
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER
}

/**
 * High-performance, zero-overhead network connectivity monitor.
 * Driven purely by Android OS ConnectivityManager callbacks (zero polling).
 * Provides reactive StateFlows for the UI and Player layers.
 */
object NetworkMonitor {
    private const val TAG = "NetworkMonitor"

    @Volatile
    private var isInitialized = false
    private var connectivityManager: ConnectivityManager? = null

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkType = MutableStateFlow(NetworkType.OTHER)
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()

    // Holds timestamp of when the device transitioned from Offline -> Online
    private val _lastReconnectedAt = MutableStateFlow(0L)
    val lastReconnectedAt: StateFlow<Long> = _lastReconnectedAt.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            evaluateNetwork()
        }

        override fun onLost(network: Network) {
            evaluateNetwork()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            evaluateNetwork()
        }
    }

    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            isInitialized = true
        }

        try {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm == null) {
                Log.w(TAG, "ConnectivityManager unavailable")
                return
            }
            connectivityManager = cm

            // Initial evaluation
            evaluateNetwork()

            // Register system network callback (API 24+ supported)
            cm.registerDefaultNetworkCallback(networkCallback)
            Log.d(TAG, "NetworkMonitor initialized successfully (Initial online: ${_isOnline.value}, type: ${_networkType.value})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize NetworkMonitor", e)
        }
    }

    @Synchronized
    private fun evaluateNetwork() {
        val cm = connectivityManager ?: return
        try {
            val activeNetwork = cm.activeNetwork
            val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null

            val hasInternet = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            val resolvedType = when {
                !hasInternet -> NetworkType.OFFLINE
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> NetworkType.WIFI
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> NetworkType.CELLULAR
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }

            val wasOffline = !_isOnline.value
            _isOnline.value = hasInternet
            _networkType.value = resolvedType

            if (wasOffline && hasInternet) {
                Log.d(TAG, "Network connection restored! Previous: Offline -> Now: $resolvedType")
                _lastReconnectedAt.value = System.currentTimeMillis()
            } else if (!hasInternet && !wasOffline) {
                Log.d(TAG, "Network connection lost! Device is now OFFLINE")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating network state", e)
        }
    }
}
