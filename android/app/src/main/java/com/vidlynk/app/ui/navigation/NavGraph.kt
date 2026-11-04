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
        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                onVideoSelected = { slug ->
                    navController.navigate(Screen.Player.createRoute(slug))
                }
            )
        }

        // Video Player Screen with Deep Links
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("slug") {
                    type = NavType.StringType
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://vidlynk.app/watch/{slug}" },
                navDeepLink { uriPattern = "http://vidlynk.app/watch/{slug}" },
                navDeepLink { uriPattern = "vidlynk://watch/{slug}" }
            )
        ) {
            val viewModel: VideoPlayerViewModel = hiltViewModel()
            VideoPlayerScreen(
                exoPlayer = exoPlayer,
                viewModel = viewModel,
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0)
                        }
                    }
                }
            )
        }
    }
}
