package com.streamhub.app

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.streamhub.app.ui.screens.HistoryScreen
import com.streamhub.app.ui.screens.HomeScreen
import com.streamhub.app.ui.screens.MyListScreen
import com.streamhub.app.ui.screens.PlayerScreen
import com.streamhub.app.ui.screens.ProfileScreen
import com.streamhub.app.ui.screens.SearchScreen
import com.streamhub.app.ui.screens.SettingsScreen
import com.streamhub.app.ui.screens.SplashScreen
import com.streamhub.app.ui.screens.StorageManagementScreen
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.StreamHubTheme
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
        // FIX: PiP action broadcast IDs
        const val ACTION_PIP_PLAY_PAUSE = "com.streamhub.app.PIP_PLAY_PAUSE"
        const val ACTION_PIP_NEXT = "com.streamhub.app.PIP_NEXT"
        const val ACTION_PIP_PREV = "com.streamhub.app.PIP_PREV"
    }

    // FIX: BroadcastReceiver to handle PiP media button actions
    private var pipActionReceiver: BroadcastReceiver? = null

    private fun registerPipActionReceiver() {
        if (pipActionReceiver != null) return
        pipActionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_PIP_PLAY_PAUSE -> {
                        val player = StreamPlayerViewModel.currentPlayer
                        player?.let { if (it.isPlaying) it.pause() else it.play() }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
                            try {
                                setPictureInPictureParams(buildPipParams())
                            } catch (_: Exception) {}
                        }
                    }
                    ACTION_PIP_NEXT -> {
                        val player = StreamPlayerViewModel.currentPlayer
                        player?.seekToNextMediaItem()
                    }
                    ACTION_PIP_PREV -> {
                        val player = StreamPlayerViewModel.currentPlayer
                        player?.seekToPreviousMediaItem()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_PIP_PLAY_PAUSE)
            addAction(ACTION_PIP_NEXT)
            addAction(ACTION_PIP_PREV)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.RECEIVER_NOT_EXPORTED
        } else {
            0
        }
        ContextCompat.registerReceiver(
            this,
            pipActionReceiver!!,
            filter,
            flags
        )
    }

    private fun unregisterPipActionReceiver() {
        pipActionReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        pipActionReceiver = null
    }

    /**
     * FIX: Use a single mutableStateOf for the deep link, scoped to the Activity.
     */
    val deepLinkMediaId = mutableStateOf<String?>(null)

    @Volatile
    var shouldAutoEnterPip: Boolean = false
        internal set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState != null) {
            savedInstanceState.getString("deep_link_media_id")?.let { deepLinkMediaId.value = it }
        }
        handleDeepLink(intent)
        registerPipActionReceiver()

        setContent {
            StreamHubTheme {
                StreamHubApp(deepLinkMediaId = deepLinkMediaId)
            }
        }

        // FIX: Defer notification permission request until AFTER first frame renders.
        // Asking during onCreate blocks the splash animation and confuses users.
        window.decorView.post {
            requestNotificationPermissionIfNeeded()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterPipActionReceiver()
    }

    /**
     * FIX: Separate method — only asks for permission if not already granted,
     * and only after the first frame is rendered (via decorView.post).
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) return
        try {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        } catch (e: Exception) {
            Log.w(TAG, "Notification permission request failed: ${e.message}")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("deep_link_media_id", deepLinkMediaId.value)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // FIX: setIntent so subsequent onResume can re-read it if needed.
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri: Uri? = intent.data
        if (uri == null || uri.scheme != "streamhub" || uri.host != "media") return
        val mediaId = uri.lastPathSegment
        if (mediaId.isNullOrBlank() || !mediaId.matches(Regex("^[a-zA-Z0-9_-]{1,64}$"))) {
            Log.w(TAG, "Invalid deep link mediaId rejected: $mediaId")
            return
        }
        Log.d(TAG, "Deep link: streamhub://media/$mediaId")
        // FIX: Only update if different — prevents re-navigation when the same deep link
        // is delivered twice (e.g. via onNewIntent after onCreate already handled it).
        if (deepLinkMediaId.value != mediaId) {
            deepLinkMediaId.value = mediaId
        }
    }

    /**
     * Called when the user leaves the activity (presses Home, recents, etc.).
     * If we are currently on the Player route and the player is playing,
     * enter PiP automatically. This matches YouTube/Netflix behavior.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!shouldAutoEnterPip || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val params = buildPipParams()
            enterPictureInPictureMode(params)
        } catch (e: Exception) {
            Log.w(TAG, "PiP entry failed: ${e.message}")
        }
    }

    /**
     * FIX: Build PiP params with custom RemoteActions for play/pause, next, prev.
     * These appear as buttons in the PiP window — matching YouTube/Netflix behavior.
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    fun buildPipParams(): PictureInPictureParams {
        val player = StreamPlayerViewModel.currentPlayer
        val videoSize = player?.videoSize
        val ratio = if (videoSize != null && videoSize.width > 0 && videoSize.height > 0) {
            val w = videoSize.width
            val h = videoSize.height
            val r = w.toFloat() / h.toFloat()
            when {
                r > 2.39f -> Rational(239, 100)
                r < 0.42f -> Rational(42, 100)
                else -> Rational(w, h)
            }
        } else {
            Rational(16, 9)
        }

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(ratio)

        // FIX: Add custom RemoteActions for play/pause, next, prev
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val actions = mutableListOf<RemoteAction>()

            // Previous button
            val prevIntent = PendingIntent.getBroadcast(
                this, 1,
                Intent(ACTION_PIP_PREV).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                RemoteAction(
                    Icon.createWithResource(this, android.R.drawable.ic_media_previous),
                    "Previous",
                    "Previous episode",
                    prevIntent
                )
            )

            // Play/Pause button — icon depends on current state
            val isPlaying = player?.isPlaying == true
            val playPauseDrawable = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            val playPauseIntent = PendingIntent.getBroadcast(
                this, 2,
                Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                RemoteAction(
                    Icon.createWithResource(this, playPauseDrawable),
                    if (isPlaying) "Pause" else "Play",
                    "Play or pause",
                    playPauseIntent
                )
            )

            // Next button
            val nextIntent = PendingIntent.getBroadcast(
                this, 3,
                Intent(ACTION_PIP_NEXT).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                RemoteAction(
                    Icon.createWithResource(this, android.R.drawable.ic_media_next),
                    "Next",
                    "Next episode",
                    nextIntent
                )
            )

            builder.setActions(actions)
        }

        return builder.build()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
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
fun StreamHubApp(deepLinkMediaId: androidx.compose.runtime.MutableState<String?>? = null) {
    val navController = rememberNavController()
    val repository = remember { FirebaseRepository.getInstance() }

    LaunchedEffect(Unit) {
        repository.connect()
    }

    // FIX: Handle deep link with retry — if the catalog isn't loaded yet, wait for it
    // instead of dropping the deep link. Previously, if user navigated back before
    // catalog loaded, the deep link was lost forever.
    LaunchedEffect(deepLinkMediaId?.value) {
        val mediaId = deepLinkMediaId?.value ?: return@LaunchedEffect
        // Wait up to 10 seconds for catalog to load before navigating.
        var attempts = 0
        while (repository.catalogState.value is CatalogState.Loading && attempts < 20) {
            delay(500L)
            attempts++
        }
        if (attempts >= 20) {
            Log.w("StreamHubApp", "Catalog still loading after 10s — navigating anyway with deep link $mediaId")
        }
        navController.navigate(Screen.Details.createRoute(mediaId))
        deepLinkMediaId.value = null  // Consumed
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
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
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
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    },
                    onNavigateToStorage = {
                        navController.navigate(Screen.StorageManagement.route)
                    },
                    onOpenAdminPanel = {
                        navController.navigate(Screen.Admin.route)
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    repository = repository,
                    onBackClick = { navController.popBackStack() },
                    onMediaClick = { media ->
                        navController.navigate(Screen.Details.createRoute(media.id))
                    },
                    onPlayEpisode = { media, episodeIndex ->
                        navController.navigate(Screen.Player.createRoute(media.id, episodeIndex))
                    }
                )
            }

            composable(Screen.StorageManagement.route) {
                StorageManagementScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Admin.route) {
                com.streamhub.app.ui.components.AdminEditorDialog(
                    onDismiss = { navController.popBackStack() },
                    onSave = { item ->
                        repository.saveMediaItem(item)
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToVideoSettings = {
                        navController.navigate(Screen.VideoSettings.route)
                    },
                    onNavigateToStorage = {
                        navController.navigate(Screen.StorageManagement.route)
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
                val mediaId = runCatching { java.net.URLDecoder.decode(rawMediaId, "UTF-8") }.getOrDefault(rawMediaId)
                DetailsScreen(
                    mediaId = mediaId,
                    repository = repository,
                    onBackClick = { navController.popBackStack() },
                    onPlayEpisode = { media, episodeIndex ->
                        navController.navigate(Screen.Player.createRoute(media.id, episodeIndex))
                    },
                    onMediaClick = { media ->
                        navController.navigate(Screen.Details.createRoute(media.id))
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
                val mediaId = runCatching { java.net.URLDecoder.decode(rawMediaId, "UTF-8") }.getOrDefault(rawMediaId)
                val episodeIndex = backStackEntry.arguments?.getInt("episodeIndex") ?: 0
                val catalogState by repository.catalogState.collectAsState()
                val catalog by repository.mediaCatalog.collectAsState()
                val mediaItem = remember(mediaId, catalog) {
                    catalog.firstOrNull { it.id == mediaId }
                }

                when {
                    catalogState is CatalogState.Loading && mediaItem == null -> {
                        PlayerLoadingScreen()
                    }
                    mediaItem == null -> {
                        PlayerNotFoundScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
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
