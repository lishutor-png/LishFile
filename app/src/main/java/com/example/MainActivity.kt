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
        // Fast refresh without hanging disk crawls
        viewModel.refreshCurrentDir()
        viewModel.refreshStorageStats()
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                viewModel.notifySnackbar("Perangkat ini tidak memiliki sensor sidik jari. Silakan gunakan PIN.")
                return
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                viewModel.notifySnackbar("Sensor biometrik sedang sibuk / tidak tersedia. Silakan gunakan PIN.")
                return
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                viewModel.notifySnackbar("Belum ada sidik jari yang terdaftar di HP. Silakan daftarkan di Pengaturan Keamanan HP atau gunakan PIN.")
                try {
                    val enrollIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        android.content.Intent(android.provider.Settings.ACTION_BIOMETRIC_ENROLL).apply {
                            putExtra(
                                android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                            )
                        }
                    } else {
                        android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                    }
                    startActivity(enrollIntent)
                } catch (_: Exception) {}
                return
            }
            else -> {}
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                viewModel.unlockVaultWithBiometrics()
                viewModel.notifySnackbar("Brankas berhasil dibuka dengan Sidik Jari!")
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
            .setSubtitle("Pindai sidik jari Anda untuk mengakses file terenkripsi")
            .setNegativeButtonText("Gunakan PIN")
            .build()

        prompt.authenticate(promptInfo)
    }
}
