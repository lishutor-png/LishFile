package com.example

import com.example.crypto.AesCryptoEngine
import com.example.util.FileUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileUtilsAndCryptoTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", FileUtils.formatFileSize(0))
        assertEquals("100 B", FileUtils.formatFileSize(100))
        assertEquals("1.0 KB", FileUtils.formatFileSize(1024))
        assertEquals("1.5 MB", FileUtils.formatFileSize((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.0 GB", FileUtils.formatFileSize((2L * 1024 * 1024 * 1024)))
    }

    @Test
    fun testGetMimeType() {
        val pngFile = File("image.png")
        assertEquals("image/png", FileUtils.getMimeType(pngFile))

        val pdfFile = File("document.pdf")
        assertEquals("application/pdf", FileUtils.getMimeType(pdfFile))

        val zipFile = File("archive.zip")
        assertEquals("application/zip", FileUtils.getMimeType(zipFile))

        val txtFile = File("notes.txt")
        assertEquals("text/plain", FileUtils.getMimeType(txtFile))
    }

    @Test
    fun testAesEncryptionAndDecryption() = runBlocking {
        val testFile = tempFolder.newFile("plain_secret.txt")
        val secretContent = "LishFile Secret Data for Testing 12345!@#$"
        testFile.writeText(secretContent)

        val encryptedFile = tempFolder.newFile("secret.lish")
        val pin = "987654"

        // Encrypt
        val encryptResult = AesCryptoEngine.encryptFile(testFile, encryptedFile, pin)
        assertTrue("Encryption must succeed", encryptResult)

        // Decrypt
        val decryptedFile = tempFolder.newFile("decrypted.txt")
        val decryptResult = AesCryptoEngine.decryptFile(encryptedFile, decryptedFile, pin)
        assertTrue("Decryption must succeed", decryptResult)

        assertEquals("Decrypted content must match original", secretContent, decryptedFile.readText())
    }

    @Test
    fun testChecksumCalculation() = runBlocking {
        val testFile = tempFolder.newFile("checksum_sample.txt")
        testFile.writeText("LishFile Checksum Validation Sample")

        val md5 = AesCryptoEngine.calculateChecksum(testFile, "MD5")
        assertNotNull(md5)
        assertEquals(32, md5.length)

        val sha256 = AesCryptoEngine.calculateChecksum(testFile, "SHA-256")
        assertNotNull(sha256)
        assertEquals(64, sha256.length)
    }

    @Test
    fun testZipAndUnzip() = runBlocking {
        val sourceDir = tempFolder.newFolder("source_dir")
        val file1 = File(sourceDir, "file1.txt")
        file1.writeText("Content 1")
        val file2 = File(sourceDir, "file2.txt")
        file2.writeText("Content 2")

        val zipFile = tempFolder.newFile("test_output.zip")
        val zipSuccess = FileUtils.createZipArchive(listOf(file1, file2), zipFile)
        assertTrue("Zipping files should succeed", zipSuccess)

        val destDir = tempFolder.newFolder("extracted_dir")
        val unzipSuccess = FileUtils.extractZipArchive(zipFile, destDir)
        assertTrue("Unzipping should succeed", unzipSuccess)

        val extracted1 = File(destDir, "file1.txt")
        val extracted2 = File(destDir, "file2.txt")
        assertTrue("Extracted file 1 exists", extracted1.exists())
        assertTrue("Extracted file 2 exists", extracted2.exists())
        assertEquals("Content 1", extracted1.readText())
        assertEquals("Content 2", extracted2.readText())
    }
}
