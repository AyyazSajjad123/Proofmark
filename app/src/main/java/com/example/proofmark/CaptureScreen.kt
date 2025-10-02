package com.example.proofmark.feature.capture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkManager
import com.example.proofmark.feature.settings.SettingsState
import com.example.proofmark.feature.settings.SettingsViewModel
import com.example.proofmark.work.QueueOrchestrator
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/* ---------------- Permission helpers ---------------- */

private fun capturePermissions(): Array<String> = buildList {
    add(Manifest.permission.CAMERA)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

@Composable
private fun PermissionGate(
    requiredPermissions: Array<String>,
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current

    fun allGranted(): Boolean = requiredPermissions.all {
        ContextCompat.checkSelfPermission(ctx, it) == PermissionChecker.PERMISSION_GRANTED
    }

    var requestedOnce by remember { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val anyDenied = result.values.any { granted -> !granted }
        permanentlyDenied = anyDenied && requestedOnce
    }

    LaunchedEffect(Unit) {
        if (!allGranted()) { requestedOnce = true; launcher.launch(requiredPermissions) }
    }

    if (allGranted()) {
        content()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(24.dp).semantics { contentDescription = "Permissions explanation" },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Permissions needed", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Camera, Location (GPS), Notifications (Android 13+).")
                Spacer(Modifier.height(16.dp))
                if (permanentlyDenied) {
                    Button(
                        onClick = {
                            val i = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", ctx.packageName, null)
                            )
                            ctx.startActivity(i)
                        },
                        modifier = Modifier.semantics { contentDescription = "Open app settings" }
                    ) { Text("Open App Settings") }
                } else {
                    Button(
                        onClick = { launcher.launch(requiredPermissions) },
                        modifier = Modifier.semantics { contentDescription = "Grant permissions" }
                    ) { Text("Grant") }
                }
            }
        }
    }
}

/* --------------------------------- Capture UI ---------------------------------- */

@Composable
fun CaptureScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    PermissionGate(requiredPermissions = capturePermissions()) {
        val ctx = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        val prefs by settingsViewModel.state.collectAsState(initial = SettingsState())

        val controller = remember {
            LifecycleCameraController(ctx).apply {
                setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
            }
        }
        LaunchedEffect(Unit) { controller.bindToLifecycle(lifecycleOwner) }

        var isFront by remember { mutableStateOf(false) }
        LaunchedEffect(isFront) {
            controller.cameraSelector = if (isFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        }

        // Apply flash mode from settings
        LaunchedEffect(prefs.flash) {
            controller.imageCaptureFlashMode = when (prefs.flash) {
                "ON" -> ImageCapture.FLASH_MODE_ON
                "OFF" -> ImageCapture.FLASH_MODE_OFF
                else -> ImageCapture.FLASH_MODE_AUTO
            }
        }

        val scope = rememberCoroutineScope()

        Column(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .semantics { contentDescription = "Camera preview" },
                factory = { PreviewView(it).also { pv -> pv.controller = controller } }
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { isFront = !isFront },
                    modifier = Modifier.semantics { contentDescription = if (isFront) "Switch to back camera" else "Switch to front camera" }
                ) { Text(if (isFront) "Switch to Back" else "Switch to Front") }

                OutlinedButton(
                    onClick = {
                        val next = when (prefs.flash) { "AUTO" -> "OFF"; "OFF" -> "ON"; else -> "AUTO" }
                        settingsViewModel.setFlash(next)
                    },
                    modifier = Modifier.semantics { contentDescription = "Toggle flash mode" }
                ) { Text("Flash: ${prefs.flash}") }
            }

            Button(
                onClick = {
                    scope.launch {
                        runCatching {
                            val photo = takePictureToFile(ctx, controller)
                            Toast.makeText(ctx, "Saved: ${photo.name}", Toast.LENGTH_SHORT).show()
                            val wm = WorkManager.getInstance(ctx)
                            QueueOrchestrator.enqueue(
                                wm = wm,
                                proofId = photo.nameWithoutExtension,
                                inputPath = photo.absolutePath,
                                maxMP = prefs.maxMp,
                                quality = prefs.quality
                            )
                        }.onFailure {
                            Toast.makeText(ctx, "Capture failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
                    .semantics { contentDescription = "Capture photo shutter" }
            ) { Text("Shutter") }
        }
    }
}

/* ------------------------------ IO helpers -------------------------------- */

private suspend fun takePictureToFile(ctx: Context, controller: LifecycleCameraController): File {
    val out = File(ctx.getExternalFilesDir(null), "proofs").apply { mkdirs() }
    val file = File(out, "${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    val mainExec = ContextCompat.getMainExecutor(ctx)

    return suspendCancellableCoroutine { cont ->
        controller.takePicture(
            outputOptions,
            mainExec,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (!cont.isCompleted) cont.resume(file)
                }
                override fun onError(exception: ImageCaptureException) {
                    if (!cont.isCompleted) cont.resumeWithException(exception)
                }
            }
        )
    }
}
