package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.crypto.AesCryptoEngine
import com.example.data.AppPreferences
import com.example.data.LockedFolderDao
import com.example.data.LockedFolderEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class SecurityManager(
    private val context: Context,
    private val preferences: AppPreferences,
    private val lockedFolderDao: LockedFolderDao
) {

    // Vault root directory
    val vaultDirectory: File by lazy {
        val dir = File(context.filesDir, ".lish_vault")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    // Unlocked folders in current session (memory-only)
    private val _unlockedPaths = MutableStateFlow<Set<String>>(emptySet())
    val unlockedPaths: StateFlow<Set<String>> = _unlockedPaths.asStateFlow()

    // Master Vault session state
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    fun isPathUnlocked(path: String): Boolean {
        if (_isVaultUnlocked.value) return true
        return _unlockedPaths.value.contains(path)
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        _unlockedPaths.value = emptySet()
    }

    fun unlockVault() {
        _isVaultUnlocked.value = true
    }

    fun unlockFolder(path: String) {
        _unlockedPaths.value = _unlockedPaths.value + path
    }

    fun canUseBiometric(): Boolean {
        if (!preferences.isBiometricEnabled.value) return false
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "Autentikasi Sidik Jari",
        subtitle: String = "Gunakan sidik jari untuk membuka folder aman",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Sidik jari tidak dikenali. Silakan coba lagi.")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Gunakan Kata Sandi / PIN")
            .build()

        prompt.authenticate(promptInfo)
    }

    suspend fun lockFolderWithPassword(
        folderFile: File,
        password: String?,
        allowBiometric: Boolean = true
    ) {
        val path = folderFile.absolutePath
        val entity = if (password.isNullOrBlank()) {
            LockedFolderEntity(
                folderPath = path,
                folderName = folderFile.name,
                passwordHash = null,
                passwordSalt = null,
                isBiometricAllowed = allowBiometric
            )
        } else {
            val salt = AesCryptoEngine.generateSalt()
            val hash = AesCryptoEngine.hashPassword(password, salt)
            LockedFolderEntity(
                folderPath = path,
                folderName = folderFile.name,
                passwordHash = hash,
                passwordSalt = salt,
                isBiometricAllowed = allowBiometric
            )
        }
        lockedFolderDao.insertLockedFolder(entity)
    }

    suspend fun unlockFolderVerification(
        folderPath: String,
        inputPinOrPassword: String
    ): Boolean {
        val entity = lockedFolderDao.getLockedFolderByPath(folderPath)
        if (entity == null) {
            // Might be inside the Safe Vault root
            return preferences.verifyMasterPin(inputPinOrPassword)
        }
        return if (entity.passwordHash == null || entity.passwordSalt == null) {
            // Inherits master vault pin
            preferences.verifyMasterPin(inputPinOrPassword)
        } else {
            val computed = AesCryptoEngine.hashPassword(inputPinOrPassword, entity.passwordSalt)
            computed == entity.passwordHash
        }
    }

    suspend fun unlockFolderDirectly(folderPath: String) {
        unlockFolder(folderPath)
    }

    suspend fun removeFolderLock(folderPath: String) {
        lockedFolderDao.deleteLockedFolder(folderPath)
        _unlockedPaths.value = _unlockedPaths.value - folderPath
    }
}
