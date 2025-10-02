package com.example.proofmark.feature.settings

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PrivacyPolicyScreen() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = false
                webViewClient = WebViewClient()
                // ✅ Loads the asset you created at app/src/main/assets/privacy.html
                loadUrl("file:///android_asset/privacy.html")
            }
        }
    )
}
