package com.example.util

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.model.FileType
import com.example.model.ZipEntryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.lastIndex)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[index])
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getFileType(file: File): FileType {
        if (file.isDirectory) return FileType.FOLDER
        val ext = file.extension.lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "svg" -> FileType.IMAGE
            "mp3", "wav", "flac", "m4a", "aac", "ogg", "wma", "opus" -> FileType.AUDIO
            "mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "3gp" -> FileType.VIDEO
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "csv" -> FileType.DOCUMENT
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> FileType.ARCHIVE
            "apk", "xapk", "apks" -> FileType.APK
            "kt", "java", "py", "js", "html", "css", "json", "xml", "cpp", "c", "h", "cs", "php", "sql", "sh" -> FileType.CODE
            else -> FileType.OTHER
        }
    }

    private val mimeTypeFallback = mapOf(
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif" to "image/gif",
        "webp" to "image/webp",
        "svg" to "image/svg+xml",
        "pdf" to "application/pdf",
        "zip" to "application/zip",
        "txt" to "text/plain",
        "json" to "application/json",
        "xml" to "application/xml",
        "mp3" to "audio/mpeg",
        "mp4" to "video/mp4",
        "apk" to "application/vnd.android.package-archive"
    )

    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        mimeTypeFallback[extension]?.let { return it }
        return try {
            MimeTypeMap.getSingleton()?.getMimeTypeFromExtension(extension) ?: "*/*"
        } catch (e: Throwable) {
            "*/*"
        }
    }

    suspend fun createFolder(parentDir: File, folderName: String): Boolean = withContext(Dispatchers.IO) {
        val newDir = File(parentDir, folderName)
        if (!newDir.exists()) newDir.mkdirs() else false
    }

    suspend fun createTextFile(parentDir: File, fileName: String, content: String = ""): Boolean = withContext(Dispatchers.IO) {
        val newFile = File(parentDir, fileName)
        try {
            if (!newFile.exists()) {
                newFile.createNewFile()
                newFile.writeText(content)
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun readTextFile(file: File): String = withContext(Dispatchers.IO) {
        try {
            file.readText()
        } catch (e: Exception) {
            "Gagal membaca file: ${e.message}"
        }
    }

    suspend fun saveTextFile(file: File, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameFile(file: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        val target = File(file.parentFile, newName)
        if (target.exists()) false else file.renameTo(target)
    }

    suspend fun deleteRecursive(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { deleteRecursive(it) }
            }
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun copyFileOrDirectory(source: File, destinationDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!destinationDir.exists()) destinationDir.mkdirs()
            val target = File(destinationDir, source.name)
            if (source.isDirectory) {
                target.mkdirs()
                source.listFiles()?.forEach { child ->
                    copyFileOrDirectory(child, target)
                }
            } else {
                source.copyTo(target, overwrite = true)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun moveFileOrDirectory(source: File, destinationDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (copyFileOrDirectory(source, destinationDir)) {
                deleteRecursive(source)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createZipArchive(filesToZip: List<File>, zipFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            zipFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                filesToZip.forEach { file ->
                    addFileToZip(file, file.name, zos)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addFileToZip(file: File, entryPath: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            val dirPath = if (entryPath.endsWith("/")) entryPath else "$entryPath/"
            zos.putNextEntry(ZipEntry(dirPath))
            zos.closeEntry()
            file.listFiles()?.forEach { child ->
                addFileToZip(child, "$dirPath${child.name}", zos)
            }
        } else {
            FileInputStream(file).use { fis ->
                zos.putNextEntry(ZipEntry(entryPath))
                val buffer = ByteArray(8192)
                var len: Int
                while (fis.read(buffer).also { len = it } > 0) {
                    zos.write(buffer, 0, len)
                }
                zos.closeEntry()
            }
        }
    }

    suspend fun extractZipArchive(zipFile: File, targetDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                val buffer = ByteArray(8192)
                while (entry != null) {
                    val newFile = File(targetDir, entry.name)
                    // Security check to avoid zip-slip
                    if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        throw IOException("Zip entry is outside target dir: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun shareFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(file)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Bagikan file via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareMultipleFiles(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        try {
            val uris = ArrayList(files.map { FileProvider.getUriForFile(context, "${context.packageName}.provider", it) })
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Bagikan ${files.size} file via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun listZipEntries(zipFile: File): List<ZipEntryItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<ZipEntryItem>()
        try {
            val zf = java.util.zip.ZipFile(zipFile)
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                items.add(
                    ZipEntryItem(
                        name = entry.name,
                        size = entry.size.coerceAtLeast(0L),
                        isDirectory = entry.isDirectory,
                        compressedSize = entry.compressedSize.coerceAtLeast(0L)
                    )
                )
            }
            zf.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        items
    }

    suspend fun searchFilesRecursively(
        root: File,
        query: String,
        extensionFilter: String? = null,
        maxResults: Int = 300,
        showHidden: Boolean = false
    ): List<File> = withContext(Dispatchers.IO) {
        val results = mutableListOf<File>()
        val q = query.lowercase().trim()
        val ext = extensionFilter?.lowercase()?.removePrefix(".")

        root.walkTopDown().onEnter { dir ->
            if (!showHidden && (dir.isHidden || dir.name.startsWith("."))) false
            else true
        }.forEach { file ->
            if (results.size >= maxResults) return@forEach
            if (!showHidden && (file.isHidden || file.name.startsWith("."))) return@forEach

            val nameMatch = q.isEmpty() || file.name.lowercase().contains(q)
            val extMatch = ext == null || ext.isEmpty() || file.extension.equals(ext, ignoreCase = true)

            if (nameMatch && extMatch && file.absolutePath != root.absolutePath) {
                results.add(file)
            }
        }
        results
    }
}
