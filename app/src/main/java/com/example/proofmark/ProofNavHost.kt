package com.example.proofmark.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.compose.*
import com.example.proofmark.feature.capture.CaptureScreen
import com.example.proofmark.feature.library.LibraryScreen
import com.example.proofmark.feature.verify.VerifyScreen
import com.example.proofmark.feature.report.ReportScreen
import com.example.proofmark.feature.settings.AppSettingsScreen
import com.example.proofmark.feature.settings.PrivacyPolicyScreen

private object Routes {
    const val Capture = "capture"
    const val Library = "library"
    const val Verify  = "verify"
    const val Report  = "report"
    const val Settings= "settings"
    const val Privacy = "privacy"
}

@Composable
fun ProofNavHost() {
    val nav = rememberNavController()
    var current by remember { mutableStateOf(Routes.Capture) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.semantics { contentDescription = "Bottom navigation" }
            ) {
                NavigationBarItem(
                    selected = current == Routes.Capture,
                    onClick = { current = Routes.Capture; nav.navigate(Routes.Capture){launchSingleTop=true} },
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = "Capture tab") },
                    label = { Text("Capture") }
                )
                NavigationBarItem(
                    selected = current == Routes.Library,
                    onClick = { current = Routes.Library; nav.navigate(Routes.Library){launchSingleTop=true} },
                    icon = { Icon(Icons.Filled.List, contentDescription = "Library tab") },
                    label = { Text("Library") }
                )
                NavigationBarItem(
                    selected = current == Routes.Verify,
                    onClick = { current = Routes.Verify; nav.navigate(Routes.Verify){launchSingleTop=true} },
                    icon = { Icon(Icons.Filled.Verified, contentDescription = "Verify tab") },
                    label = { Text("Verify") }
                )
                NavigationBarItem(
                    selected = current == Routes.Report,
                    onClick = { current = Routes.Report; nav.navigate(Routes.Report){launchSingleTop=true} },
                    icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = "Report tab") },
                    label = { Text("Report") }
                )
                NavigationBarItem(
                    selected = current == Routes.Settings,
                    onClick = { current = Routes.Settings; nav.navigate(Routes.Settings){launchSingleTop=true} },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings tab") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.Capture, modifier = Modifier.padding(padding)) {
            composable(Routes.Capture)  { CaptureScreen() }
            composable(Routes.Library)  { LibraryScreen() }
            composable(Routes.Verify)   { VerifyScreen() }
            composable(Routes.Report)   { ReportScreen() }
            composable(Routes.Settings) { AppSettingsScreen(nav) }
            composable(Routes.Privacy)  { PrivacyPolicyScreen() }
        }
    }
}
