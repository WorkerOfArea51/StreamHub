package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeLayoutConfig(
    val showHeroCarousel: Boolean = true,
    val showContinueWatching: Boolean = true,
    val continueWatchingFirst: Boolean = false,
    val showTrendingSection: Boolean = true,
    val showAnimeSection: Boolean = true,
    val showMoviesSection: Boolean = true
)

/**
 * Production Home Screen Layout Preferences Manager:
 * - Allows users to customize Home Screen section order and visibility
 * - Persists preferences in SharedPreferences (streamhub_home_layout_prefs)
 */
object HomeScreenLayoutManager {

    private const val PREFS_NAME = "streamhub_home_layout_prefs"
    private const val KEY_SHOW_HERO = "show_hero"
    private const val KEY_SHOW_CONTINUE = "show_continue"
    private const val KEY_CONTINUE_FIRST = "continue_first"
    private const val KEY_SHOW_TRENDING = "show_trending"
    private const val KEY_SHOW_ANIME = "show_anime"
    private const val KEY_SHOW_MOVIES = "show_movies"

    private var prefs: SharedPreferences? = null

    private val _layoutConfig = MutableStateFlow(HomeLayoutConfig())
    val layoutConfig: StateFlow<HomeLayoutConfig> = _layoutConfig.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val p = prefs ?: return
        try {
            _layoutConfig.value = HomeLayoutConfig(
                showHeroCarousel = p.getBoolean(KEY_SHOW_HERO, true),
                showContinueWatching = p.getBoolean(KEY_SHOW_CONTINUE, true),
                continueWatchingFirst = p.getBoolean(KEY_CONTINUE_FIRST, false),
                showTrendingSection = p.getBoolean(KEY_SHOW_TRENDING, true),
                showAnimeSection = p.getBoolean(KEY_SHOW_ANIME, true),
                showMoviesSection = p.getBoolean(KEY_SHOW_MOVIES, true)
            )
        } catch (e: Exception) {
            p.edit().clear().apply()
            _layoutConfig.value = HomeLayoutConfig()
        }
    }

    fun updateConfig(newConfig: HomeLayoutConfig) {
        _layoutConfig.value = newConfig
        prefs?.edit()?.apply {
            putBoolean(KEY_SHOW_HERO, newConfig.showHeroCarousel)
            putBoolean(KEY_SHOW_CONTINUE, newConfig.showContinueWatching)
            putBoolean(KEY_CONTINUE_FIRST, newConfig.continueWatchingFirst)
            putBoolean(KEY_SHOW_TRENDING, newConfig.showTrendingSection)
            putBoolean(KEY_SHOW_ANIME, newConfig.showAnimeSection)
            putBoolean(KEY_SHOW_MOVIES, newConfig.showMoviesSection)
            apply()
        }
    }
}
