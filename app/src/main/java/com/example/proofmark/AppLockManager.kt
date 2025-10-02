package com.example.proofmark.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class AppLockManager(private val context: Context) {
    private var lastUsed = System.currentTimeMillis()
    private var _locked = true
    private var prompting = false

    fun markUsed() { lastUsed = System.currentTimeMillis() }
    fun forceLock() { _locked = true }
    fun isLocked(): Boolean = _locked

    fun shouldLock(): Boolean {
        val idleTooLong = (System.currentTimeMillis() - lastUsed) > 30_000
        return idleTooLong || _locked
    }

    fun showPromptIfPossible(activity: FragmentActivity, onUnlock: () -> Unit, onFailOpen: () -> Unit) {
        if (prompting) return
        prompting = true
        try {
            val authFlags = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL

            val capability = BiometricManager.from(context).canAuthenticate(authFlags)
            if (capability != BiometricManager.BIOMETRIC_SUCCESS) {
                // No biometric/PIN available -> fail-open (policy choice)
                _locked = false
                markUsed()
                prompting = false
                onFailOpen()
                return
            }

            val executor = ContextCompat.getMainExecutor(context)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    _locked = false
                    markUsed()
                    prompting = false
                    onUnlock()
                }
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    // user canceled or hard error -> stay locked; stop prompting
                    prompting = false
                }
                override fun onAuthenticationFailed() {
                    // bad biometric; prompt remains
                }
            }

            val prompt = BiometricPrompt(activity, executor, callback)
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock App")
                .setSubtitle("Use fingerprint or device PIN")
                .setAllowedAuthenticators(authFlags)
                .build()

            prompt.authenticate(info)
        } catch (_: Throwable) {
            // Any OEM oddity -> fail-open (don’t crash)
            _locked = false
            markUsed()
            prompting = false
            onFailOpen()
        }
    }
}
