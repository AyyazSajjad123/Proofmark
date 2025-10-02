package com.example.proofmark.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

@Composable
fun PermissionsGate(
    onAllGranted: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val needed = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }

    fun allGranted(): Boolean = needed.all {
        ContextCompat.checkSelfPermission(ctx, it) == PermissionChecker.PERMISSION_GRANTED
    }

    var requestedOnce by remember { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { result ->
        val anyDenied = result.values.any { !it }
        permanentlyDenied = anyDenied && requestedOnce
    }

    LaunchedEffect(Unit) {
        if (!allGranted()) {
            requestedOnce = true
            launcher.launch(needed)
        }
    }

    if (allGranted()) {
        onAllGranted()
    } else {
        PermissionScreen(
            permanentlyDenied = permanentlyDenied,
            onRequest = { launcher.launch(needed) },
            onOpenSettings = {
                val i = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", ctx.packageName, null)
                )
                ctx.startActivity(i)
            }
        )
    }
}

@Composable
private fun PermissionScreen(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Permissions needed", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(
                "Camera, Location, and Notifications (Android 13+) zaroori hain.",
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            if (permanentlyDenied) {
                Button(onClick = onOpenSettings) { Text("Open App Settings") }
            } else {
                Button(onClick = onRequest) { Text("Continue") }
            }
        }
    }
}
