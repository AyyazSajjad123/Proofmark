package com.example.proofmark.security

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun AppLockGate(content: @Composable () -> Unit) {
    // If we are not inside an AppCompatActivity (e.g., previews), show content (fail-open)
    val maybeAct = LocalContext.current as? AppCompatActivity
    if (maybeAct == null) {
        content()
        return
    }
    val activity = maybeAct
    val lifecycleOwner = LocalLifecycleOwner.current

    val lock = remember { AppLockManager(activity) }
    var unlocked by remember { mutableStateOf(false) }

    // First composition: decide whether to prompt
    LaunchedEffect(Unit) {
        if (lock.shouldLock()) {
            lock.showPromptIfPossible(
                activity,
                onUnlock = { unlocked = true },
                onFailOpen = { unlocked = true }
            )
        } else {
            unlocked = true
        }
    }

    // Observe lifecycle for idle -> lock
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (lock.shouldLock()) {
                        lock.showPromptIfPossible(
                            activity,
                            onUnlock = { unlocked = true },
                            onFailOpen = { unlocked = true }
                        )
                    }
                }
                Lifecycle.Event.ON_PAUSE -> lock.markUsed()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    if (unlocked) content()
}
