package com.streamhub.app.data

/**
 * Dedicated in-memory scroll state coordinator for HomeScreen.
 * Persists vertical and horizontal shelf scroll offsets across navigation,
 * bottom-bar tab switching, and app foregrounding without unwanted resets.
 */
object HomeScreenScrollManager {
    // Retained vertical scroll position
    var verticalScrollIndex: Int = 0
    var verticalScrollOffset: Int = 0

    // Retained horizontal scroll positions keyed by shelf identifier
    private val horizontalScrollPositions = mutableMapOf<String, Pair<Int, Int>>()

    fun getHorizontalScroll(shelfKey: String): Pair<Int, Int> {
        return horizontalScrollPositions[shelfKey] ?: (0 to 0)
    }

    fun setHorizontalScroll(shelfKey: String, index: Int, offset: Int) {
        horizontalScrollPositions[shelfKey] = index to offset
    }

    fun resetVertical() {
        verticalScrollIndex = 0
        verticalScrollOffset = 0
    }

    fun resetAll() {
        verticalScrollIndex = 0
        verticalScrollOffset = 0
        horizontalScrollPositions.clear()
    }
}
