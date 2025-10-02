package com.example.proofmark.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.proofmark.R
import com.example.proofmark.feature.capture.CaptureScreen
import com.example.proofmark.feature.library.LibraryScreen
import com.example.proofmark.feature.verify.VerifyScreen
import com.example.proofmark.feature.report.ReportScreen
import com.example.proofmark.feature.settings.AppSettingsScreen
import com.example.proofmark.feature.settings.PrivacyPolicyScreen

private object Routes {
    const val Capture  = "capture"
    const val Library  = "library"
    const val Verify   = "verify"
    const val Report   = "report"
    const val Settings = "settings"
    const val Privacy  = "privacy"   // ✅ Day-21: privacy route
}

@Composable
fun ProofNavHost() {
    val nav = rememberNavController()
    var current by remember { mutableStateOf(Routes.Capture) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = current == Routes.Capture,
                    onClick = {
                        current = Routes.Capture
                        nav.navigate(Routes.Capture) { launchSingleTop = true }
                    },
                    icon = { Icon(Icons.Filled.CameraAlt, null) },
                    label = { Text(stringResource(R.string.capture)) }
                )
                NavigationBarItem(
                    selected = current == Routes.Library,
                    onClick = {
                        current = Routes.Library
                        nav.navigate(Routes.Library) { launchSingleTop = true }
                    },
                    icon = { Icon(Icons.Filled.List, null) },
                    label = { Text(stringResource(R.string.library)) }
                )
                NavigationBarItem(
                    selected = current == Routes.Verify,
                    onClick = {
                        current = Routes.Verify
                        nav.navigate(Routes.Verify) { launchSingleTop = true }
                    },
                    icon = { Icon(Icons.Filled.Verified, null) },
                    label = { Text(stringResource(R.string.verify)) }
                )
                NavigationBarItem(
                    selected = current == Routes.Report,
                    onClick = {
                        current = Routes.Report
                        nav.navigate(Routes.Report) { launchSingleTop = true }
                    },
                    icon = { Icon(Icons.Filled.PictureAsPdf, null) },
                    label = { Text(stringResource(R.string.report)) }
                )
                NavigationBarItem(
                    selected = current == Routes.Settings,
                    onClick = {
                        current = Routes.Settings
                        nav.navigate(Routes.Settings) { launchSingleTop = true }
                    },
                    icon = { Icon(Icons.Filled.Settings, null) },
                    label = { Text(stringResource(R.string.settings)) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.Capture,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Capture)  { CaptureScreen() }
            composable(Routes.Library)  { LibraryScreen() }
            composable(Routes.Verify)   { VerifyScreen() }
            composable(Routes.Report)   { ReportScreen() }
            // ✅ Use the app-level settings wrapper so it can navigate
            composable(Routes.Settings) { AppSettingsScreen(nav) }
            // ✅ Privacy screen registered
            composable(Routes.Privacy)  { PrivacyPolicyScreen() }
        }
    }
}
