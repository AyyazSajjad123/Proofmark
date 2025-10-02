package com.example.proofmark.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ---------- Pure UI models kept INSIDE the feature module ----------
data class SettingsState(
    val language: String = "en",   // "en" | "ur"
    val quality: String = "MED",   // "LOW" | "MED" | "HIGH"
    val maxMp: Int = 12,           // 8 | 12 | 16
    val watermark: Boolean = true, // true => ON, false => OFF
    val flash: String = "AUTO"     // "AUTO" | "OFF" | "ON"
)

data class SettingsLabels(
    val title: String,
    val language: String,
    val english: String,
    val urdu: String,
    val quality: String,
    val maxMp: String,
    val watermark: String,
    val flash: String,
    val on: String,
    val off: String,
    val auto: String,
    val privacyPolicy: String,
)

// ---------- Pure UI composable: no NavController/ViewModel/DataStore ----------
@Composable
fun SettingsScreen(
    state: SettingsState,
    labels: SettingsLabels,
    onLanguage: (String) -> Unit,
    onQuality: (String) -> Unit,
    onMaxMp: (Int) -> Unit,
    onWatermark: (Boolean) -> Unit,
    onFlash: (String) -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(labels.title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        // Language
        Text(labels.language, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row {
            Chip(state.language == "en", { onLanguage("en") }, labels.english)
            Spacer(Modifier.width(12.dp))
            Chip(state.language == "ur", { onLanguage("ur") }, labels.urdu)
        }

        Spacer(Modifier.height(24.dp))

        // Quality
        Text(labels.quality, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row {
            Chip(state.quality == "LOW",  { onQuality("LOW") }, "LOW")
            Spacer(Modifier.width(12.dp))
            Chip(state.quality == "MED",  { onQuality("MED") }, "MED")
            Spacer(Modifier.width(12.dp))
            Chip(state.quality == "HIGH", { onQuality("HIGH") }, "HIGH")
        }

        Spacer(Modifier.height(24.dp))

        // Max MP
        Text(labels.maxMp, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row {
            Chip(state.maxMp == 8,  { onMaxMp(8)  }, "8")
            Spacer(Modifier.width(12.dp))
            Chip(state.maxMp == 12, { onMaxMp(12) }, "12")
            Spacer(Modifier.width(12.dp))
            Chip(state.maxMp == 16, { onMaxMp(16) }, "16")
        }

        Spacer(Modifier.height(24.dp))

        // Watermark
        Text(labels.watermark, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row {
            Chip(state.watermark,          { onWatermark(true)  }, labels.on)
            Spacer(Modifier.width(12.dp))
            Chip(!state.watermark,         { onWatermark(false) }, labels.off)
        }

        Spacer(Modifier.height(24.dp))

        // Flash
        Text(labels.flash, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row {
            Chip(state.flash == "AUTO", { onFlash("AUTO") }, labels.auto)
            Spacer(Modifier.width(12.dp))
            Chip(state.flash == "OFF",  { onFlash("OFF")  }, labels.off)
            Spacer(Modifier.width(12.dp))
            Chip(state.flash == "ON",   { onFlash("ON")   }, labels.on)
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onOpenPrivacy) {
            Text(labels.privacyPolicy)
        }
    }
}

@Composable
private fun Chip(selected: Boolean, onClick: () -> Unit, text: String) {
    OutlinedButton(onClick = onClick) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
