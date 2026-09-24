package com.example.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object AesCryptoEngine {
    private const val MAGIC_HEADER = "LISHFILE_ENC_V1"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 16

    suspend fun encryptFile(
        inputFile: File,
        outputFile: File,
        pinOrPassword: String,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val salt = ByteArray(SALT_LENGTH)
            val iv = ByteArray(IV_LENGTH)
            val random = SecureRandom()
            random.nextBytes(salt)
            random.nextBytes(iv)

            val secretKey = deriveKey(pinOrPassword, salt)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

            outputFile.parentFile?.mkdirs()
            val totalBytes = inputFile.length()
            var bytesProcessed = 0L

            FileOutputStream(outputFile).use { fos ->
                fos.write(MAGIC_HEADER.toByteArray(Charsets.UTF_8))
                fos.write(salt)
                fos.write(iv)

                CipherOutputStream(fos, cipher).use { cos ->
                    FileInputStream(inputFile).use { fis ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (fis.read(buffer).also { read = it } != -1) {
                            cos.write(buffer, 0, read)
                            bytesProcessed += read
                            if (totalBytes > 0) {
                                onProgress?.invoke(bytesProcessed.toFloat() / totalBytes)
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            outputFile.delete()
            false
        }
    }

    suspend fun decryptFile(
        inputFile: File,
        outputFile: File,
        pinOrPassword: String,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            outputFile.parentFile?.mkdirs()
            val totalBytes = inputFile.length()
            var bytesProcessed = 0L

            FileInputStream(inputFile).use { fis ->
                val headerBytes = ByteArray(MAGIC_HEADER.toByteArray(Charsets.UTF_8).size)
                fis.read(headerBytes)
                val header = String(headerBytes, Charsets.UTF_8)
                if (header != MAGIC_HEADER) {
                    return@withContext false
                }

                val salt = ByteArray(SALT_LENGTH)
                val iv = ByteArray(IV_LENGTH)
                fis.read(salt)
                fis.read(iv)

                val secretKey = deriveKey(pinOrPassword, salt)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

                CipherInputStream(fis, cipher).use { cis ->
                    FileOutputStream(outputFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (cis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                            bytesProcessed += read
                            if (totalBytes > 0) {
                                onProgress?.invoke(bytesProcessed.toFloat() / totalBytes)
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            outputFile.delete()
            false
        }
    }

    fun isEncryptedFile(file: File): Boolean {
        if (!file.exists() || file.isDirectory || file.length() < 50) return false
        return try {
            FileInputStream(file).use { fis ->
                val headerBytes = ByteArray(MAGIC_HEADER.toByteArray(Charsets.UTF_8).size)
                val read = fis.read(headerBytes)
                if (read == headerBytes.size) {
                    String(headerBytes, Charsets.UTF_8) == MAGIC_HEADER
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun calculateChecksum(file: File, algorithm: String = "SHA-256"): String = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance(algorithm)
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$password:$saltHex".toByteArray(Charsets.UTF_8)
        return digest.digest(combined).joinToString("") { "%02x".format(it) }
    }

    private fun deriveKey(pinOrPassword: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(pinOrPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
