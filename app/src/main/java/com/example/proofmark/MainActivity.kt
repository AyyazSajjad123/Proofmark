package com.example.proofmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.proofmark.nav.ProofMarkNavHost
import com.example.proofmark.ui.theme.ProofTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // IMPORTANT: remove any delayed Runnable or "force crash" test code you had here.

        setContent {
            ProofTheme {
                ProofMarkNavHost()
            }
        }
    }
}
