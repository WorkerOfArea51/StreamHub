package com.streamhub.app.data

/**
 * Visual presentation style for the in-player video seekbar, matching mpvEx 1:1.
 */
enum class SeekbarStyle(val label: String, val description: String) {
    Standard("Standard", "Classic clean progress bar with thumb"),
    Wavy("Wavy", "Modern undulating sinusoidal wave animation"),
    Thick("Thick", "Chunky rounded pill seekbar with tactile endpoints")
}
