package com.streamhub.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Adaptive navigation shell: bottom bar on phones, navigation rail on
 * tablets/foldables/unfolded. Wrap the existing NavHost content with this and
 * pass the current WindowSizeClass from the activity.
 */
@Composable
fun AdaptiveNavShell(
    windowSizeClass: WindowSizeClass,
    navController: NavHostController,
    bottomBarScreens: List<Screen>,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showNav = bottomBarScreens.any { it.route == currentRoute }
    val useRail = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    val showRail = useRail && showNav
    val showBottomBar = !useRail && showNav

    Row(modifier = modifier.fillMaxSize()) {
        if (showRail) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                header = { Spacer(Modifier.statusBarsPadding()) }
            ) {
                bottomBarScreens.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationRailItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            screen.icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = screen.title,
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        alwaysShowLabel = false,
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationRailItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            val contentModifier = if (showNav) {
                if (showRail) {
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                } else {
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                }
            } else {
                Modifier.fillMaxSize()
            }
            content(contentModifier)

            if (showBottomBar) {
                com.streamhub.app.ui.components.ExpressiveFloatingNavBar(
                    screens = bottomBarScreens,
                    currentRoute = currentRoute,
                    onScreenSelected = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
