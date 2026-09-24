package com.example

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.LishFileTheme
import com.example.viewmodel.FileManagerViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: FileManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.preferences.themeMode.collectAsState()

            LishFileTheme(themeMode = themeMode) {
                MainAppScreen(
                    viewModel = viewModel,
                    onTriggerBiometrics = { showBiometricPrompt() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAll()
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                viewModel.unlockVaultWithBiometrics()
                viewModel.notifySnackbar("Brankas berhasil dibuka dengan Biometrik")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    viewModel.notifySnackbar("Otentikasi biometrik: $errString")
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka Brankas Aman")
            .setSubtitle("Gunakan sensor sidik jari atau wajah Anda")
            .setNegativeButtonText("Gunakan PIN")
            .build()

        prompt.authenticate(promptInfo)
    }
}
