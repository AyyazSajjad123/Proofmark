package com.example.proofmark.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.proofmark.feature.capture.CaptureScreen

private object Routes {
    const val Capture = "capture"
    const val Library = "library"
    const val Verify  = "verify"
    const val Report  = "report"
    const val Settings = "settings"
}

@Composable
fun ProofMarkNavHost(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    val backstack by nav.currentBackStackEntryAsState()
    val currentRoute = backstack?.destination?.route

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                listOf(
                    Triple(Routes.Capture,  "Capture",  Icons.Filled.CameraAlt),
                    Triple(Routes.Library,  "Library",  Icons.Filled.Collections),
                    Triple(Routes.Verify,   "Verify",   Icons.Filled.Verified),
                    Triple(Routes.Report,   "Report",   Icons.Filled.Article),
                    Triple(Routes.Settings, "Settings", Icons.Filled.Settings),
                ).forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            if (currentRoute != route) {
                                nav.navigate(route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->                     // ✅ padding is used
        NavHost(
            navController = nav,
            startDestination = Routes.Capture,
            modifier = Modifier.padding(innerPadding) // ✅ apply content padding
        ) {
            composable(Routes.Capture)  { CaptureScreen() }
            composable(Routes.Library)  { CenterText("Library") }
            composable(Routes.Verify)   { CenterText("Verify") }
            composable(Routes.Report)   { CenterText("Report") }
            composable(Routes.Settings) { CenterText("Settings") }
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp), // leave a little room above bottom bar
        contentAlignment = Alignment.Center
    ) {
        Text(text)
    }
}
