package com.streamhub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamHubTheme {
                StreamHubApp()
            }
        }
    }
}

@Composable
fun StreamHubApp() {
    val navController = rememberNavController()
    val repository = remember { FirebaseRepository() }
    val playerViewModel: StreamPlayerViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarScreens = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Downloads,
        Screen.MyList,
        Screen.Profile
    )

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
                                        tint = if (selected) PrimaryRed else TextSecondary
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    color = if (selected) PrimaryRed else TextSecondary,
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
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Details.route,
                arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
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
                val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
                val episodeIndex = backStackEntry.arguments?.getInt("episodeIndex") ?: 0
                val mediaItem = repository.mediaCatalog.value.firstOrNull { it.id == mediaId }

                if (mediaItem != null) {
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
