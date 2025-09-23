package com.example.proofmark.feature.capture

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@Composable
fun CaptureScreen() {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // 👇 rename to avoid shadowing PreviewView.controller
    val cameraController = remember {
        LifecycleCameraController(ctx).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                        CameraController.IMAGE_ANALYSIS or
                        CameraController.VIDEO_CAPTURE
            )
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) cameraController.bindToLifecycle(lifecycleOwner)
        else requestPermission.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize()) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PreviewView(context).apply {
                        // ✅ explicitly assign to the PreviewView property
                        this.controller = cameraController
                        this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission required")
            }
        }

        // ...same imports & code you have now...

        ExtendedFloatingActionButton(
            text = { Text("Shutter") },
            icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
            onClick = { /* capture code */ },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)   // a bit more space
                .navigationBarsPadding()
        )

    }
}
