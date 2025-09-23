package com.example.proofmark.nav

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.remoteconfig.remoteConfig

@Composable
fun DevDebugPanel() {
    if (!BuildConfig.DEBUG) return

    val rc = Firebase.remoteConfig
    var quality by remember { mutableStateOf(rc.getString("pdfDefaultQuality")) }
    var mp by remember { mutableStateOf(rc.getLong("maxMpDefault")) }
    var warn by remember { mutableStateOf(rc.getBoolean("warnJpgShare")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("RC pdfDefaultQuality: $quality")
        Text("RC maxMpDefault: $mp")
        Text("RC warnJpgShare: $warn")
        Spacer(Modifier.height(12.dp))
        Row {
            Button(onClick = {
                rc.fetchAndActivate().addOnCompleteListener {
                    quality = rc.getString("pdfDefaultQuality")
                    mp = rc.getLong("maxMpDefault")
                    warn = rc.getBoolean("warnJpgShare")
                }
            }) { Text("Fetch RC") }
            Spacer(Modifier.width(12.dp))
            Button(onClick = {
                // TEST CRASH (debug only)
                Firebase.crashlytics.log("Manual test crash pressed")
                throw RuntimeException("Test Crash — ProofMark devDebug")
            }) { Text("Force Crash") }
        }
    }
}
