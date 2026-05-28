package com.reelgrab.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reelgrab.feature.history.HistoryScreen
import com.reelgrab.feature.home.HomeScreen
import com.reelgrab.feature.preview.PreviewScreen
import com.reelgrab.feature.settings.SettingsScreen
import com.reelgrab.feature.settings.disclaimer.DisclaimerScreen

/**
 * Root composable: Scaffold + bottom nav + NavHost.
 *
 * Why route the start destination through a ViewModel? The disclaimer flag is the single
 * source of truth for whether the user has been onboarded. Resolving it via a Flow lets the
 * NavHost re-render to home as soon as the user taps Accept, with no manual nav scripting.
 */
@Composable
fun ReelGrabApp(viewModel: AppRootViewModel = hiltViewModel()) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
    if (startDestination == null) {
        SplashPlaceholder()
        return
    }
    AppNavGraph(start = startDestination!!)
}

@Composable
private fun SplashPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AppNavGraph(start: String) {
    val navController = rememberNavController()
    val context = LocalContext.current
    Scaffold(
        bottomBar = { ReelGrabBottomBar(navController) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onNavigateToPreview = { encoded -> navController.navigate(Destination.Preview.build(encoded)) },
                )
            }
            composable(Destination.History.route) {
                HistoryScreen(
                    onOpenUri = { uri, mime -> openUri(context, uri, mime) },
                    onShareUri = { uri, mime -> shareUri(context, uri, mime) },
                )
            }
            composable(Destination.Settings.route) {
                SettingsScreen(onShowDisclaimer = { navController.navigate(Destination.Disclaimer.route) })
            }
            composable(Destination.Disclaimer.route) {
                DisclaimerScreen(
                    onAccept = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Disclaimer.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onDecline = {
                        if (!navController.popBackStack()) (context as? Activity)?.finish()
                    },
                )
            }
            composable(
                route = Destination.Preview.route,
                arguments = listOf(navArgument(Destination.Preview.ARG_ENCODED_URL) { type = NavType.StringType }),
            ) {
                PreviewScreen(
                    onBack = { navController.popBackStack() },
                    onOpenUri = { uri ->
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, uri).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ReelGrabBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    if (currentRoute == Destination.Preview.route || currentRoute == Destination.Disclaimer.route) return
    NavigationBar {
        Destination.bottomNavDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}

private fun openUri(context: android.content.Context, uri: String, mimeType: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(uri), mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}

private fun shareUri(context: android.content.Context, uri: String, mimeType: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
