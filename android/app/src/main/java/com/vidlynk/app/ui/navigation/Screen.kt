package com.vidlynk.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Player : Screen("player/{slug}") {
        fun createRoute(slug: String) = "player/$slug"
    }
}
