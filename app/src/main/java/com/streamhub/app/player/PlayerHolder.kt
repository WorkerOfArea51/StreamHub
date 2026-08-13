package com.streamhub.app.player

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * RC2 FIX: Dedicated Singleton holder for active ExoPlayer instance.
 * Decouples player state management from ViewModel companion objects
 * and provides lifecycle-aware StateFlow access across app components.
 */
object PlayerHolder {

    private val _currentPlayerFlow = MutableStateFlow<ExoPlayer?>(null)
    val currentPlayerFlow: StateFlow<ExoPlayer?> = _currentPlayerFlow.asStateFlow()

    var currentPlayer: ExoPlayer?
        get() = _currentPlayerFlow.value
        set(value) {
            _currentPlayerFlow.value = value
        }

    fun setPlayer(player: ExoPlayer?) {
        _currentPlayerFlow.value = player
    }

    fun clear() {
        _currentPlayerFlow.value = null
    }
}
