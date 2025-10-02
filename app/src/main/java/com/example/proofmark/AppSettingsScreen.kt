package com.example.proofmark.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.proofmark.R

@Composable
fun AppSettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val prefs by settingsViewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(text = stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Language
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row {
            Chip(
                selected = prefs.language == "en",
                onClick = { settingsViewModel.setLanguage("en") },
                label = stringResource(R.string.english)
            )
            Spacer(Modifier.width(12.dp))     // <-- FIXED (.dp)
            Chip(
                selected = prefs.language == "ur",
                onClick = { settingsViewModel.setLanguage("ur") },
                label = stringResource(R.string.urdu)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Quality
        Text(stringResource(R.string.quality), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row {
            listOf("LOW","MED","HIGH").forEachIndexed { i, q ->
                if (i > 0) Spacer(Modifier.width(12.dp))
                Chip(
                    selected = prefs.quality == q,
                    onClick = { settingsViewModel.setQuality(q) },
                    label = q
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Max MP
        Text(stringResource(R.string.max_mp), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row {
            listOf(8,12,16).forEachIndexed { i, mp ->
                if (i > 0) Spacer(Modifier.width(12.dp))
                Chip(
                    selected = prefs.maxMp == mp,
                    onClick = { settingsViewModel.setMaxMp(mp) },
                    label = "$mp"
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Watermark
        Text(stringResource(R.string.watermark), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row {
            Chip(
                selected = prefs.watermark,
                onClick = { settingsViewModel.setWatermark(true) },
                label = stringResource(R.string.on)
            )
            Spacer(Modifier.width(12.dp))
            Chip(
                selected = !prefs.watermark,
                onClick = { settingsViewModel.setWatermark(false) },
                label = stringResource(R.string.off)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Flash
        Text(stringResource(R.string.flash), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row {
            listOf("AUTO","OFF","ON").forEachIndexed { i, f ->
                if (i > 0) Spacer(Modifier.width(12.dp))
                Chip(
                    selected = prefs.flash == f,
                    onClick = { settingsViewModel.setFlash(f) },
                    label = when (f) {
                        "AUTO" -> stringResource(R.string.auto)
                        "OFF"  -> stringResource(R.string.off)
                        else   -> stringResource(R.string.on)
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { navController.navigate("privacy") }) {
            Text(stringResource(R.string.privacy_policy))
        }
    }
}

@Composable
private fun Chip(selected: Boolean, onClick: () -> Unit, label: String) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) { Text(label) }
}
