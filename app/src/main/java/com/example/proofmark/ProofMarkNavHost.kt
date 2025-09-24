package com.example.proofmark.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.proofmark.feature.capture.CaptureScreen
import com.example.proofmark.feature.library.WatermarkGalleryScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.saveable.rememberSaveable

private object Routes {
    const val Capture = "capture"
    const val Gallery = "gallery"   // Day-6 screen
    const val Verify  = "verify"
    const val Report  = "report"
    const val Settings = "settings"
}

@Composable
fun ProofMarkNavHost() {
    val nav = rememberNavController()

    // bottom bar destinations
    val items = listOf(
        Triple(Routes.Capture, "Capture", Icons.Filled.CameraAlt),
        Triple(Routes.Gallery, "Library", Icons.Filled.PhotoLibrary),
        Triple(Routes.Verify,  "Verify",  Icons.Filled.Verified),
        Triple(Routes.Report,  "Report",  Icons.Filled.Assessment),
        Triple(Routes.Settings,"Settings",Icons.Filled.Settings),
    )

    // keep selection stable (no jump back to start)
    var selected by rememberSaveable { mutableStateOf(Routes.Capture) }

    // track backstack (for highlight)
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: selected

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            selected = route
                            nav.navigate(route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.Capture,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Capture)  { CaptureScreen() }
            composable(Routes.Gallery)  { WatermarkGalleryScreen() } // Day-6
            composable(Routes.Verify)   { CenterText("Verify") }
            composable(Routes.Report)   { CenterText("Report") }
            composable(Routes.Settings) { CenterText("Settings") }
        }
    }
}

@Composable private fun CenterText(text: String) {
    Surface { Text(text) }
}
