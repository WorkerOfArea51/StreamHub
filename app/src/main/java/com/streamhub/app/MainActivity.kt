package com.streamhub.app

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streamhub.app.data.repository.CatalogState
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.player.StreamPlayerViewModel
import com.streamhub.app.ui.navigation.Screen
import com.streamhub.app.ui.screens.DetailsScreen
import com.streamhub.app.ui.screens.DownloadsScreen
import com.streamhub.app.ui.screens.HomeScreen
import com.streamhub.app.ui.screens.MyListScreen
import com.streamhub.app.ui.screens.PlayerScreen
import com.streamhub.app.ui.screens.ProfileScreen
import com.streamhub.app.ui.screens.SearchScreen
import com.streamhub.app.ui.screens.SettingsScreen
import com.streamhub.app.ui.screens.SplashScreen
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.StreamHubTheme
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // All managers are initialized in StreamHubApplication.onCreate().
        // No manager init calls belong here — see StreamHubApplication KDoc.

        setContent {
            StreamHubTheme {
                StreamHubApp()
            }
        }
    }

    /**
     * Called when the user leaves the activity (presses Home, recents, etc.).
     *
     * If we are currently on the Player route and the player is playing,
     * enter PiP automatically. This matches YouTube/Netflix behavior.
     *
     * Note: we cannot directly check ExoPlayer's.isPlaying from here (the
     * player is owned by StreamPlayerViewModel). The PlayerScreen sets
     * an instance field [shouldAutoEnterPip] via the activity cast.
     * A future refactor will replace this with a proper MediaSessionService
     * that exposes playback state to the activity via a Flow.
     */
    @Volatile
    var shouldAutoEnterPip: Boolean = false
        internal set

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (shouldAutoEnterPip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "PiP entry failed: ${e.message}")
            }
        }
    }

    /**
     * Called when the system enters or exits PiP mode.
     *
     * We log the transition for debugging. Future work will use this to:
     *   - Hide player controls on enter, show on exit
     *   - Pause background downloads on enter
     *   - Switch to compact HUD layout on enter
     *
     * The actual UI reaction happens in PlayerScreen via its DisposableEffect,
     * which reads `resources.configuration.uiMode` to detect PiP. This override
     * is the system-level hook for any activity-wide side effects.
     */
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        Log.d(TAG, "PiP mode changed: isInPip=$isInPictureInPictureMode")
        if (isInPictureInPictureMode) {
            // Disable auto-PiP re-entry while already in PiP
            shouldAutoEnterPip = false
        }
    }
}

@Composable
fun StreamHubApp() {
    val navController = rememberNavController()
    val repository = remember { FirebaseRepository.getInstance() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        repository.connect()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarScreens = remember {
        listOf(
            Screen.Home,
            Screen.Search,
            Screen.Downloads,
            Screen.MyList,
            Screen.Profile
        )
    }

    val showBottomBar = bottomBarScreens.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    contentColor = TextPrimary
                ) {
                    bottomBarScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = screen.title,
                                        tint = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.primary else TextSecondary
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    color = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.primary else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        containerColor = BackgroundDark,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    repository = repository,
                    onSplashFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    repository = repository,
                    onMediaClick = { media ->
                        navController.navigate(Screen.Details.createRoute(media.id))
                    },
                    onPlayEpisode = { media, episodeIndex ->
                        navController.navigate(Screen.Player.createRoute(media.id, episodeIndex))
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    repository = repository,
                    onMediaClick = { media ->
                        navController.navigate(Screen.Details.createRoute(media.id))
                    }
                )
            }

            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    onPlayEpisode = { media, episodeIndex ->
                        navController.navigate(Screen.Player.createRoute(media.id, episodeIndex))
                    }
                )
            }

            composable(Screen.MyList.route) {
                MyListScreen(
                    repository = repository,
                    onMediaClick = { media ->
                        navController.navigate(Screen.Details.createRoute(media.id))
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onOpenAdminPanel = {
                        navController.navigate(Screen.Admin.route)
                    }
                )
            }

            composable(Screen.Admin.route) {
                HomeScreen(
                    repository = repository,
                    onMediaClick = { media ->
                        navController.navigate(Screen.Details.createRoute(media.id))
                    },
                    onPlayEpisode = { media, episodeIndex ->
                        navController.navigate(Screen.Player.createRoute(media.id, episodeIndex))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToVideoSettings = {
                        navController.navigate(Screen.VideoSettings.route)
                    }
                )
            }

            composable(Screen.VideoSettings.route) {
                com.streamhub.app.ui.screens.VideoSettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Details.route,
                arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val rawMediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
                val mediaId = java.net.URLDecoder.decode(rawMediaId, "UTF-8")
                DetailsScreen(
                    mediaId = mediaId,
                    repository = repository,
                    onBackClick = { navController.popBackStack() },
                    onPlayEpisode = { media, episodeIndex ->
                        navController.navigate(Screen.Player.createRoute(media.id, episodeIndex))
                    }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.StringType },
                    navArgument("episodeIndex") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val playerViewModel: StreamPlayerViewModel = viewModel()
                val rawMediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
                val mediaId = java.net.URLDecoder.decode(rawMediaId, "UTF-8")
                val episodeIndex = backStackEntry.arguments?.getInt("episodeIndex") ?: 0
                val catalogState by repository.catalogState.collectAsState()
                val catalog by repository.mediaCatalog.collectAsState()
                val mediaItem = remember(mediaId, catalog) {
                    catalog.firstOrNull { it.id == mediaId }
                }

                when {
                    // Catalog still loading — show spinner, will recompute when state changes
                    catalogState is CatalogState.Loading && mediaItem == null -> {
                        PlayerLoadingScreen()
                    }
                    // Catalog loaded but mediaId not found — show error
                    mediaItem == null -> {
                        PlayerNotFoundScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    // Happy path — render PlayerScreen
                    else -> {
                        PlayerScreen(
                            mediaItem = mediaItem,
                            initialEpisodeIndex = episodeIndex,
                            viewModel = playerViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Loading state for PlayerScreen — shown when the catalog is still loading
 * from Firestore and the requested mediaId has not been found yet.
 *
 * This fixes the "black screen on player open" bug where navigating to
 * player/{mediaId}/{episodeIndex} before Firestore returns its first
 * snapshot would render nothing.
 */
@Composable
private fun PlayerLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = PrimaryRed)
            Text(
                text = "Loading media...",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/**
 * Error state for PlayerScreen — shown when the catalog has finished loading
 * but the requested mediaId was not found. This usually means the user
 * navigated to a deleted media item via a stale deep link.
 */
@Composable
private fun PlayerNotFoundScreen(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Media not found",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "This item may have been removed.",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryRed
                )
            ) {
                Text("Go back", color = Color.White)
            }
        }
    }
}
