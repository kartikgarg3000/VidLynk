package com.vidlynk.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.vidlynk.app.ui.screens.home.HomeScreen
import com.vidlynk.app.ui.screens.player.VideoPlayerScreen
import com.vidlynk.app.ui.screens.player.VideoPlayerViewModel

@Composable
fun VidLynkNavGraph(
    navController: NavHostController,
    exoPlayer: ExoPlayer,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Home Screen with Deep Links (Tapping Telegram link opens Home Screen with video hero & floating ad)
        composable(
            route = Screen.Home.route,
            arguments = listOf(
                navArgument("slug") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://vidlynk.app/watch/{slug}" },
                navDeepLink { uriPattern = "http://vidlynk.app/watch/{slug}" },
                navDeepLink { uriPattern = "https://vidlynk-production.up.railway.app/watch/{slug}" },
                navDeepLink { uriPattern = "http://vidlynk-production.up.railway.app/watch/{slug}" },
                navDeepLink { uriPattern = "vidlynk://watch/{slug}" }
            )
        ) { backStackEntry ->
            val incomingSlug = backStackEntry.arguments?.getString("slug")
            HomeScreen(
                incomingSlug = incomingSlug,
                onVideoSelected = { slug ->
                    navController.navigate(Screen.Player.createRoute(slug))
                }
            )
        }

        // Video Player Screen
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("slug") {
                    type = NavType.StringType
                }
            )
        ) {
            val viewModel: VideoPlayerViewModel = hiltViewModel()
            VideoPlayerScreen(
                exoPlayer = exoPlayer,
                viewModel = viewModel,
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Home.createRoute()) {
                            popUpTo(0)
                        }
                    }
                }
            )
        }
    }
}
