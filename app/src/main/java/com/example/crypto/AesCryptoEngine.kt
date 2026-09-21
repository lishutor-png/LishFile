package com.example.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
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

    private const val MAGIC_HEADER = "LISHFILE_V1"
    private val MAGIC_BYTES = MAGIC_HEADER.toByteArray(StandardCharsets.UTF_8)
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 16
    private const val KEY_LENGTH_BITS = 256
    private const val PBKDF2_ITERATIONS = 10_000
    private const val BUFFER_SIZE = 64 * 1024 // 64 KB streaming buffer for large file optimization

    data class CryptoProgress(
        val bytesProcessed: Long,
        val totalBytes: Long,
        val percent: Int
    )

    suspend fun encryptFile(
        inputFile: File,
        outputFile: File,
        password: String,
        onProgress: ((CryptoProgress) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val totalSize = inputFile.length()
        var processedSize = 0L

        val salt = ByteArray(SALT_LENGTH)
        val iv = ByteArray(IV_LENGTH)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(salt)
        secureRandom.nextBytes(iv)

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

        val origNameBytes = inputFile.name.toByteArray(StandardCharsets.UTF_8)

        BufferedOutputStream(FileOutputStream(outputFile), BUFFER_SIZE).use { bos ->
            // Write Magic header
            bos.write(MAGIC_BYTES)
            // Write Salt & IV
            bos.write(salt)
            bos.write(iv)
            // Write original filename length (2 bytes) + original filename
            bos.write((origNameBytes.size shr 8) and 0xFF)
            bos.write(origNameBytes.size and 0xFF)
            bos.write(origNameBytes)

            // Write Cipher Stream
            CipherOutputStream(bos, cipher).use { cos ->
                BufferedInputStream(FileInputStream(inputFile), BUFFER_SIZE).use { bis ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (bis.read(buffer).also { read = it } != -1) {
                        cos.write(buffer, 0, read)
                        processedSize += read
                        val percent = if (totalSize > 0) ((processedSize * 100) / totalSize).toInt() else 100
                        onProgress?.invoke(CryptoProgress(processedSize, totalSize, percent))
                    }
                    cos.flush()
                }
            }
        }
        true
    }

    suspend fun decryptFile(
        inputFile: File,
        outputDirectory: File,
        password: String,
        customOutputName: String? = null,
        onProgress: ((CryptoProgress) -> Unit)? = null
    ): File? = withContext(Dispatchers.IO) {
        val totalSize = inputFile.length()
        var processedSize = 0L

        BufferedInputStream(FileInputStream(inputFile), BUFFER_SIZE).use { bis ->
            // Check Magic Header
            val header = ByteArray(MAGIC_BYTES.size)
            val readHeader = bis.read(header)
            if (readHeader != MAGIC_BYTES.size || !header.contentEquals(MAGIC_BYTES)) {
                throw IllegalArgumentException("Not a valid LishFile encrypted file or corrupt header")
            }

            val salt = ByteArray(SALT_LENGTH)
            if (bis.read(salt) != SALT_LENGTH) throw IllegalArgumentException("Invalid file: Salt missing")

            val iv = ByteArray(IV_LENGTH)
            if (bis.read(iv) != IV_LENGTH) throw IllegalArgumentException("Invalid file: IV missing")

            // Read original filename
            val high = bis.read()
            val low = bis.read()
            if (high == -1 || low == -1) throw IllegalArgumentException("Invalid file: filename length missing")
            val nameLen = (high shl 8) or low
            val nameBytes = ByteArray(nameLen)
            if (bis.read(nameBytes) != nameLen) throw IllegalArgumentException("Invalid file: corrupt filename")
            val originalName = String(nameBytes, StandardCharsets.UTF_8)

            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

            val targetFileName = customOutputName ?: originalName
            var targetFile = File(outputDirectory, targetFileName)
            if (targetFile.exists()) {
                val base = targetFile.nameWithoutExtension
                val ext = if (targetFile.extension.isNotEmpty()) ".${targetFile.extension}" else ""
                targetFile = File(outputDirectory, "${base}_decrypted_${System.currentTimeMillis() % 10000}$ext")
            }

            CipherInputStream(bis, cipher).use { cis ->
                BufferedOutputStream(FileOutputStream(targetFile), BUFFER_SIZE).use { bos ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (cis.read(buffer).also { read = it } != -1) {
                        bos.write(buffer, 0, read)
                        processedSize += read
                        val percent = if (totalSize > 0) ((processedSize * 100) / totalSize).toInt().coerceIn(0, 100) else 100
                        onProgress?.invoke(CryptoProgress(processedSize, totalSize, percent))
                    }
                    bos.flush()
                }
            }

            targetFile
        }
    }

    fun isLishEncrypted(file: File): Boolean {
        if (!file.exists() || file.isDirectory || file.length() < MAGIC_BYTES.size + SALT_LENGTH + IV_LENGTH + 2) {
            return false
        }
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(MAGIC_BYTES.size)
                val read = fis.read(header)
                read == MAGIC_BYTES.size && header.contentEquals(MAGIC_BYTES)
            }
        } catch (_: Exception) {
            file.name.endsWith(".lish", ignoreCase = true)
        }
    }

    fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(StandardCharsets.UTF_8))
        val digest = md.digest(password.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(digest)
    }

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytesToHex(bytes)
    }

    suspend fun calculateChecksum(file: File, algorithm: String = "SHA-256"): String = withContext(Dispatchers.IO) {
        if (!file.exists() || file.isDirectory) return@withContext ""
        val md = MessageDigest.getInstance(algorithm)
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(BUFFER_SIZE)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        bytesToHex(md.digest())
    }

    suspend fun calculateMd5(file: File): String = calculateChecksum(file, "MD5")

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789ABCDEF".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
