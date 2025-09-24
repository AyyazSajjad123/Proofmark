package com.example.proofmark.feature.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun CaptureScreen() {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    // Camera permission state
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val askPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // One controller for the screen
    val controller = remember(ctx) {
        LifecycleCameraController(ctx).apply {
            // Preview is automatic once this controller is set on a PreviewView.
            // Just enable IMAGE_CAPTURE use case (enough for preview + capture with this controller).
            setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
        }
    }

    // Bind to lifecycle only when permission is granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) controller.bindToLifecycle(lifecycle)
    }

    val scope = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize()) {
        if (!hasPermission) {
            // Simple permission UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is required to show the preview.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { askPermission.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Camera Permission")
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Preview area
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { context ->
                        PreviewView(context).apply {
                            // Attach our controller (this wires the preview surface automatically)
                            this.controller = controller
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                // Single shutter button
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val file = takePictureToFile(ctx, controller)
                                Toast.makeText(ctx, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
                            } catch (e: Throwable) {
                                Toast.makeText(ctx, "Capture failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text("Shutter")
                }
            }
        }
    }
}

/* ---------- Capture to a File using LifecycleCameraController ---------- */
private suspend fun takePictureToFile(
    ctx: Context,
    controller: LifecycleCameraController
): File {
    val file = File(ctx.getExternalFilesDir(null), "cap_${System.currentTimeMillis()}.jpg")
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
