package com.reelgrab.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation graph as a closed taxonomy.
 *
 * Why a sealed class? Routes are typed, exhaustively matchable in `when`, and decoupled
 * from the string keys actually used by NavController — refactors don't silently break
 * navigation. The companion `bottomNavDestinations` keeps the bottom-bar order stable.
 */
sealed class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : Destination("home", "Home", Icons.Outlined.Home)
    data object History : Destination("history", "History", Icons.Outlined.History)
    data object Settings : Destination("settings", "Settings", Icons.Outlined.Settings)

    /** First-launch + Settings-revisitable legal screen. Not part of bottom nav. */
    data object Disclaimer : Destination(AppRootViewModel.ROUTE_DISCLAIMER, "Disclaimer", Icons.Outlined.Home)

    /** Preview route is parameterised by a URL-encoded source URL. */
    data object Preview : Destination("preview/{encodedUrl}", "Preview", Icons.Outlined.Download) {
        const val ARG_ENCODED_URL: String = "encodedUrl"
        fun build(encodedUrl: String): String = "preview/$encodedUrl"
    }

    companion object {
        val bottomNavDestinations: List<Destination> by lazy { listOf(Home, History, Settings) }
    }
}
