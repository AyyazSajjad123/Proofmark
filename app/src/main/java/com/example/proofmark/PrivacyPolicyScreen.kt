package com.example.proofmark.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.proofmark.R

@Composable
fun PrivacyPolicyScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.privacy_placeholder))
    }
}
