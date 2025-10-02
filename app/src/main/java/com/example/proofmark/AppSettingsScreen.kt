package com.example.proofmark.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
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
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title
        item {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // Language
        item {
            SectionTitle(stringResource(R.string.language))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Chip(
                    selected = prefs.language == "en",
                    onClick = { settingsViewModel.setLanguage("en") },
                    label = stringResource(R.string.english)
                )
                Chip(
                    selected = prefs.language == "ur",
                    onClick = { settingsViewModel.setLanguage("ur") },
                    label = stringResource(R.string.urdu)
                )
            }
        }

        // Quality
        item {
            SectionTitle(stringResource(R.string.quality))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("LOW","MED","HIGH").forEach { q ->
                    Chip(
                        selected = prefs.quality == q,
                        onClick = { settingsViewModel.setQuality(q) },
                        label = q
                    )
                }
            }
        }

        // Max MP
        item {
            SectionTitle(stringResource(R.string.max_mp))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(8,12,16).forEach { mp ->
                    Chip(
                        selected = prefs.maxMp == mp,
                        onClick = { settingsViewModel.setMaxMp(mp) },
                        label = "$mp"
                    )
                }
            }
        }

        // Watermark
        item {
            SectionTitle(stringResource(R.string.watermark))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Chip(
                    selected = prefs.watermark,
                    onClick = { settingsViewModel.setWatermark(true) },
                    label = stringResource(R.string.on)
                )
                Chip(
                    selected = !prefs.watermark,
                    onClick = { settingsViewModel.setWatermark(false) },
                    label = stringResource(R.string.off)
                )
            }
        }

        // Flash
        item {
            SectionTitle(stringResource(R.string.flash))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("AUTO","OFF","ON").forEach { f ->
                    val label = when (f) {
                        "AUTO" -> stringResource(R.string.auto)
                        "OFF"  -> stringResource(R.string.off)
                        else   -> stringResource(R.string.on)
                    }
                    Chip(
                        selected = prefs.flash == f,
                        onClick = { settingsViewModel.setFlash(f) },
                        label = label
                    )
                }
            }
        }

        // --- Spacer so last button keyboard/nav se clip na ho
        item { Spacer(Modifier.height(12.dp)) }

        // Privacy Policy button (always visible; scroll karke mil jayega)
        item {
            TextButton(onClick = { navController.navigate("privacy") }) {
                Text(text = stringResource(R.string.privacy_policy))
            }
        }

        // Bottom safe space
        item { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun Chip(selected: Boolean, onClick: () -> Unit, label: String) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) { Text(label) }
}
