package com.streamhub.app.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.streamhub.app.BuildConfig
import com.streamhub.app.data.api.Secrets
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UnityAdsManager — Handles Unity Ads SDK lifecycle, pre-caching, and rewarded ad presentations.
 */
object UnityAdsManager {

    private const val TAG = "UnityAdsManager"

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isRewardedAdLoaded = MutableStateFlow(false)
    val isRewardedAdLoaded: StateFlow<Boolean> = _isRewardedAdLoaded.asStateFlow()

    private var isInitializing = false
    private var isAdLoading = false

    /**
     * Initializes the Unity Ads SDK.
     * Called on app startup in StreamHubApp.
     */
    fun init(context: Context) {
        if (_isInitialized.value || isInitializing) return

        val gameId = Secrets.UNITY_GAME_ID.trim()
        if (gameId.isBlank()) {
            Log.w(TAG, "Unity Game ID is not configured — Unity Ads disabled.")
            return
        }

        isInitializing = true
        val isTestMode = BuildConfig.DEBUG

        Log.d(TAG, "Initializing Unity Ads SDK: gameId=$gameId, testMode=$isTestMode")

        UnityAds.initialize(
            context.applicationContext,
            gameId,
            isTestMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    isInitializing = false
                    _isInitialized.value = true
                    Log.d(TAG, "Unity Ads initialized successfully. Preloading rewarded ad...")
                    preloadRewardedAd()
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError?,
                    message: String?
                ) {
                    isInitializing = false
                    _isInitialized.value = false
                    Log.e(TAG, "Unity Ads initialization failed: $error — $message")
                }
            }
        )
    }

    /**
     * Preloads the Rewarded Video placement in background so it opens with zero latency when requested.
     */
    fun preloadRewardedAd() {
        if (!_isInitialized.value || isAdLoading || _isRewardedAdLoaded.value) return

        val placementId = Secrets.UNITY_REWARDED_AD_UNIT_ID.trim().ifBlank { "Rewarded_Android" }
        isAdLoading = true

        Log.d(TAG, "Preloading Rewarded Ad: placementId=$placementId")
        UnityAds.load(
            placementId,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    isAdLoading = false
                    _isRewardedAdLoaded.value = true
                    Log.d(TAG, "Rewarded Ad preloaded & ready: $placementId")
                }

                override fun onUnityAdsFailedToLoad(
                    placementId: String?,
                    error: UnityAds.UnityAdsLoadError?,
                    message: String?
                ) {
                    isAdLoading = false
                    _isRewardedAdLoaded.value = false
                    Log.w(TAG, "Failed to load Rewarded Ad ($placementId): $error — $message")
                }
            }
        )
    }

    /**
     * Shows the rewarded video ad to unlock a 12-hour pass.
     *
     * @param activity Hosting Activity
     * @param onUserEarnedReward Callback invoked when video completes and 12-hour pass should be granted
     * @param onAdDismissed Callback invoked when ad closes
     * @param onAdError Callback invoked if showing fails (with reason message)
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdDismissed: () -> Unit = {},
        onAdError: (String) -> Unit
    ) {
        val placementId = Secrets.UNITY_REWARDED_AD_UNIT_ID.trim().ifBlank { "Rewarded_Android" }

        if (!_isInitialized.value) {
            Log.w(TAG, "Unity Ads not initialized. Attempting emergency re-init & granting grace pass.")
            init(activity.applicationContext)
            onAdError("Ad service initializing. Please try again in a moment.")
            return
        }

        Log.d(TAG, "Displaying Rewarded Ad: placementId=$placementId")

        UnityAds.show(
            activity,
            placementId,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowStart(placementId: String?) {
                    Log.d(TAG, "Rewarded Ad presentation started: $placementId")
                    _isRewardedAdLoaded.value = false
                }

                override fun onUnityAdsShowClick(placementId: String?) {
                    Log.d(TAG, "Rewarded Ad clicked: $placementId")
                }

                override fun onUnityAdsShowComplete(
                    placementId: String?,
                    state: UnityAds.UnityAdsShowCompletionState?
                ) {
                    Log.d(TAG, "Rewarded Ad finished with state: $state")
                    _isRewardedAdLoaded.value = false
                    preloadRewardedAd() // Preload next ad immediately

                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        AdPassManager.grant12HourPass()
                        onUserEarnedReward()
                    } else {
                        onAdDismissed()
                    }
                }

                override fun onUnityAdsShowFailure(
                    placementId: String?,
                    error: UnityAds.UnityAdsShowError?,
                    message: String?
                ) {
                    Log.e(TAG, "Rewarded Ad display failure ($placementId): $error — $message")
                    _isRewardedAdLoaded.value = false
                    preloadRewardedAd()
                    onAdError(message ?: "Failed to display sponsor video")
                }
            }
        )
    }
}
