package com.vidlynk.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home?slug={slug}") {
        fun createRoute(slug: String? = null): String {
            return if (!slug.isNullOrBlank()) "home?slug=$slug" else "home"
        }
    }
    data object Player : Screen("player/{slug}") {
        fun createRoute(slug: String) = "player/$slug"
    }
}
