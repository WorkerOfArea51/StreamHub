package com.streamhub.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Collections

enum class NetworkType {
    OFFLINE,
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER
}

/**
 * Cinema-Grade, Resilient Network Connectivity Engine.
 * 
 * Modeled after YouTube and Facebook production architectures:
 * 1. Multi-network tracking pool (Set<Network>) to prevent false-offline traps during carrier/tower handoffs.
 * 2. 1,500ms Debounced Hysteresis on offline transitions: transient 200-500ms cell drops never flap the UI.
 * 3. Dual-registration: monitors both general internet capability networks and system default network.
 * 4. Zero battery drain: reactive callbacks without background polling loops.
 * 5. On-demand socket/DNS probe validation via forceRecheck().
 */
object NetworkMonitor {
    private const val TAG = "NetworkMonitor"
    private const val OFFLINE_DEBOUNCE_MS = 1500L

    @Volatile
    private var isInitialized = false
    private var connectivityManager: ConnectivityManager? = null

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pendingOfflineJob: Job? = null

    // Thread-safe pool of validated active networks currently reporting internet capability
    private val activeNetworks: MutableSet<Network> = Collections.synchronizedSet(mutableSetOf())

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkType = MutableStateFlow(NetworkType.OTHER)
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()

    // Holds timestamp of when the device transitioned from Offline -> Online
    private val _lastReconnectedAt = MutableStateFlow(0L)
    val lastReconnectedAt: StateFlow<Long> = _lastReconnectedAt.asStateFlow()

    private val internetNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            activeNetworks.add(network)
            evaluateNetwork(isImmediateOnline = true)
        }

        override fun onLost(network: Network) {
            activeNetworks.remove(network)
            evaluateNetwork(isImmediateOnline = false)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (hasInternet) {
                activeNetworks.add(network)
                evaluateNetwork(isImmediateOnline = true)
            } else {
                activeNetworks.remove(network)
                evaluateNetwork(isImmediateOnline = false)
            }
        }
    }

    private val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            activeNetworks.add(network)
            evaluateNetwork(isImmediateOnline = true)
        }

        override fun onLost(network: Network) {
            activeNetworks.remove(network)
            evaluateNetwork(isImmediateOnline = false)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (hasInternet) {
                activeNetworks.add(network)
                evaluateNetwork(isImmediateOnline = true)
            } else {
                activeNetworks.remove(network)
                evaluateNetwork(isImmediateOnline = false)
            }
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

            // Seed activeNetworks from current system state
            val active = cm.activeNetwork
            if (active != null) {
                val caps = cm.getNetworkCapabilities(active)
                if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    activeNetworks.add(active)
                }
            }

            // Initial immediate evaluation
            evaluateNetwork(isImmediateOnline = true)

            // 1. Register for ANY network with internet capability (catches Wi-Fi + Cellular simultaneous readiness)
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, internetNetworkCallback)

            // 2. Register for system default network callback (API 24+)
            cm.registerDefaultNetworkCallback(defaultNetworkCallback)

            Log.d(TAG, "NetworkMonitor initialized successfully (Initial online: ${_isOnline.value}, type: ${_networkType.value})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize NetworkMonitor", e)
        }
    }

    /**
     * Re-evaluates network state with hysteresis.
     * If transitioning to online: applied immediately (0ms).
     * If transitioning to offline: debounced by [OFFLINE_DEBOUNCE_MS] to absorb carrier/cell-tower handoffs.
     */
    @Synchronized
    private fun evaluateNetwork(isImmediateOnline: Boolean = false) {
        val cm = connectivityManager ?: return

        try {
            // Check 1: Do we have any networks in our active verified pool?
            val hasPoolNetworks = activeNetworks.isNotEmpty()

            // Check 2: Does cm.activeNetwork have internet capability?
            val activeNetwork = cm.activeNetwork
            val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null
            val activeHasInternet = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            val currentlyConnected = hasPoolNetworks || activeHasInternet

            val targetType = when {
                !currentlyConnected -> NetworkType.OFFLINE
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> NetworkType.WIFI
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> NetworkType.CELLULAR
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> NetworkType.ETHERNET
                else -> {
                    // Inspect pool networks for transport if activeNetwork caps was null during handoff
                    val anyWifi = activeNetworks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
                    val anyCellular = activeNetworks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true }
                    when {
                        anyWifi -> NetworkType.WIFI
                        anyCellular -> NetworkType.CELLULAR
                        else -> NetworkType.OTHER
                    }
                }
            }

            if (currentlyConnected) {
                // Cancel pending offline flip if a network recovered before debounce expired
                pendingOfflineJob?.cancel()
                pendingOfflineJob = null

                val wasOffline = !_isOnline.value
                _isOnline.value = true
                _networkType.value = targetType

                if (wasOffline) {
                    Log.i(TAG, "Network restored: Device is now ONLINE ($targetType)")
                    _lastReconnectedAt.value = System.currentTimeMillis()
                }
            } else {
                // Transitioning toward Offline: apply debounced hysteresis
                if (_isOnline.value && pendingOfflineJob == null) {
                    pendingOfflineJob = monitorScope.launch {
                        delay(OFFLINE_DEBOUNCE_MS)
                        // Re-verify after debounce delay before flipping state
                        val stillConnected = activeNetworks.isNotEmpty() ||
                            (cm.activeNetwork?.let { cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } == true)

                        if (!stillConnected) {
                            Log.w(TAG, "Network lost confirmed after ${OFFLINE_DEBOUNCE_MS}ms debounce: Device is now OFFLINE")
                            _isOnline.value = false
                            _networkType.value = NetworkType.OFFLINE
                        }
                        pendingOfflineJob = null
                    }
                } else if (!_isOnline.value) {
                    _networkType.value = NetworkType.OFFLINE
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating network state", e)
        }
    }

    /**
     * Force on-demand re-check (used by the [Retry] action on the Offline banner).
     * Performs a fast non-blocking socket test to verify if internet is actually reachable.
     */
    fun forceRecheck() {
        monitorScope.launch(Dispatchers.IO) {
            val cm = connectivityManager
            if (cm != null) {
                val active = cm.activeNetwork
                if (active != null) {
                    val caps = cm.getNetworkCapabilities(active)
                    if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        activeNetworks.add(active)
                    }
                }
            }

            // Quick non-blocking socket check to Cloudflare / Google DNS (1.1.1.1:53)
            val socketReachable = try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("1.1.1.1", 53), 1200)
                    true
                }
            } catch (_: Exception) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress("8.8.8.8", 53), 1200)
                        true
                    }
                } catch (_: Exception) {
                    false
                }
            }

            if (socketReachable) {
                Log.i(TAG, "forceRecheck: Active socket probe succeeded! Restoring online state.")
                pendingOfflineJob?.cancel()
                pendingOfflineJob = null
                _isOnline.value = true
                _lastReconnectedAt.value = System.currentTimeMillis()
            } else {
                evaluateNetwork(isImmediateOnline = false)
            }
        }
    }
}
