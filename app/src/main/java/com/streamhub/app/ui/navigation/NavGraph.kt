package com.streamhub.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Splash : Screen("splash", "Splash")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Explore", Icons.Default.Search)
    object Downloads : Screen("downloads", "Downloads", Icons.Default.Download)
    object MyList : Screen("mylist", "My List", Icons.Default.Bookmark)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Settings : Screen("settings", "Settings")
    object VideoSettings : Screen("video-settings", "Video Settings")
    object AppearanceSettings : Screen("appearance-settings", "Appearance")
    object GestureSettings : Screen("gesture-settings", "Gestures")
    object AudioSettings : Screen("audio-settings", "Audio & Sound")
    object AdvancedSettings : Screen("advanced-settings", "Advanced & Backup")
    object StorageManagement : Screen("storage-management", "Storage & Cache")
    object History : Screen("history", "Watch History")
    object Admin : Screen("admin", "Admin Panel")
    object About : Screen("about", "About StreamHub")
    
    object Details : Screen("details/{mediaId}?episodeIndex={episodeIndex}", "Details") {
        fun createRoute(mediaId: String, episodeIndex: Int = -1): String {
            val encoded = android.net.Uri.encode(mediaId)
            return if (episodeIndex >= 0) "details/$encoded?episodeIndex=$episodeIndex" else "details/$encoded"
        }
    }
    
    object Player : Screen("player/{mediaId}/{episodeIndex}", "Player") {
        fun createRoute(mediaId: String, episodeIndex: Int = 0) = "player/${android.net.Uri.encode(mediaId)}/$episodeIndex"
    }
}
