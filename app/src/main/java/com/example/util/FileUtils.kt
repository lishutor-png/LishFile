package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class StorageVolumeInfo(
    val name: String,
    val path: File,
    val isRemovable: Boolean,
    val totalBytes: Long,
    val freeBytes: Long
)

object FileUtils {

    private const val BUFFER_SIZE = 64 * 1024 // 64 KB buffered I/O

    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase(Locale.ROOT)
        if (extension.isEmpty()) return "*/*"
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return mime ?: when (extension) {
            "pdf" -> "application/pdf"
            "txt", "log", "md" -> "text/plain"
            "json" -> "application/json"
            "xml" -> "text/xml"
            "html", "htm" -> "text/html"
            "csv" -> "text/csv"
            "lish", "aes" -> "application/octet-stream"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "apk" -> "application/vnd.android.package-archive"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/x-wav"
            "mp4" -> "video/mp4"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "*/*"
        }
    }

    fun openFileWithExternalApp(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "File tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.provider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val mimeType = getMimeType(file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Buka dengan aplikasi")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Tidak ada aplikasi yang dapat membuka file ini", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFileViaBluetoothOrSystem(context: Context, file: File) {
        if (!file.exists()) return

        try {
            val authority = "${context.packageName}.provider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val mimeType = getMimeType(file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Kirim via Bluetooth / Jaringan")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun copyFileOrDirectory(
        src: File,
        destDir: File,
        onProgress: ((copied: Long, total: Long) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!src.exists()) return@withContext false
        if (!destDir.exists()) destDir.mkdirs()

        if (src.isDirectory) {
            val targetDir = File(destDir, src.name)
            if (!targetDir.exists()) targetDir.mkdirs()
            val children = src.listFiles() ?: return@withContext true
            for (child in children) {
                copyFileOrDirectory(child, targetDir, onProgress)
            }
            true
        } else {
            var targetFile = File(destDir, src.name)
            if (targetFile.exists()) {
                val base = src.nameWithoutExtension
                val ext = if (src.extension.isNotEmpty()) ".${src.extension}" else ""
                targetFile = File(destDir, "${base}_copy$ext")
            }

            val totalSize = src.length()
            var copied = 0L

            BufferedInputStream(FileInputStream(src), BUFFER_SIZE).use { bis ->
                BufferedOutputStream(FileOutputStream(targetFile), BUFFER_SIZE).use { bos ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (bis.read(buffer).also { read = it } != -1) {
                        bos.write(buffer, 0, read)
                        copied += read
                        onProgress?.invoke(copied, totalSize)
                    }
                    bos.flush()
                }
            }
            true
        }
    }

    suspend fun moveFileOrDirectory(src: File, destDir: File): Boolean = withContext(Dispatchers.IO) {
        if (!src.exists()) return@withContext false
        if (!destDir.exists()) destDir.mkdirs()

        val destFile = File(destDir, src.name)
        // Try atomic rename first
        if (src.renameTo(destFile)) {
            return@withContext true
        }

        // Fallback: Copy and delete original
        val copied = copyFileOrDirectory(src, destDir)
        if (copied) {
            deleteRecursive(src)
            true
        } else {
            false
        }
    }

    suspend fun renameFileOrFolder(src: File, newName: String): File? = withContext(Dispatchers.IO) {
        if (!src.exists() || newName.isBlank()) return@withContext null
        val target = File(src.parentFile, newName)
        if (target.exists()) return@withContext null
        if (src.renameTo(target)) target else null
    }

    suspend fun deleteRecursive(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext true
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }

    suspend fun createNewFolder(parentDir: File, folderName: String): File? = withContext(Dispatchers.IO) {
        val folder = File(parentDir, folderName)
        if (folder.exists()) return@withContext null
        if (folder.mkdirs()) folder else null
    }

    suspend fun createTextFile(parentDir: File, fileName: String, content: String): File? = withContext(Dispatchers.IO) {
        val fullFileName = if (fileName.contains(".")) fileName else "$fileName.txt"
        val file = File(parentDir, fullFileName)
        if (file.exists()) return@withContext null
        FileOutputStream(file).use { fos ->
            fos.write(content.toByteArray())
            fos.flush()
        }
        file
    }

    suspend fun readTextFile(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() > 5 * 1024 * 1024) {
            return@withContext "File terlalu besar untuk ditampilkan di editor teks (maks 5MB)."
        }
        file.readText()
    }

    suspend fun saveTextFile(file: File, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createZipArchive(
        files: List<File>,
        destZipFile: File,
        onProgress: ((currentFile: String, percent: Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (destZipFile.exists()) destZipFile.delete()
            val allFiles = mutableListOf<Pair<File, String>>()
            for (rootFile in files) {
                if (rootFile.isDirectory) {
                    rootFile.walkTopDown().forEach { f ->
                        val relPath = rootFile.name + "/" + f.relativeTo(rootFile).path.replace('\\', '/')
                        allFiles.add(Pair(f, if (f.isDirectory) "$relPath/" else relPath))
                    }
                } else {
                    allFiles.add(Pair(rootFile, rootFile.name))
                }
            }

            val total = allFiles.size
            var done = 0

            ZipOutputStream(BufferedOutputStream(FileOutputStream(destZipFile))).use { zos ->
                val buffer = ByteArray(BUFFER_SIZE)
                for ((f, entryName) in allFiles) {
                    done++
                    val p = if (total > 0) ((done * 100) / total) else 100
                    onProgress?.invoke(f.name, p)

                    if (f.isDirectory) {
                        val entry = ZipEntry(if (entryName.endsWith("/")) entryName else "$entryName/")
                        zos.putNextEntry(entry)
                        zos.closeEntry()
                    } else {
                        val entry = ZipEntry(entryName)
                        entry.time = f.lastModified()
                        entry.size = f.length()
                        zos.putNextEntry(entry)
                        BufferedInputStream(FileInputStream(f), BUFFER_SIZE).use { bis ->
                            var read: Int
                            while (bis.read(buffer).also { read = it } != -1) {
                                zos.write(buffer, 0, read)
                            }
                        }
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun extractZipArchive(
        zipFile: File,
        destDir: File,
        onProgress: ((currentEntry: String) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!destDir.exists()) destDir.mkdirs()
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(BUFFER_SIZE)
                while (entry != null) {
                    val newFile = File(destDir, entry.name)
                    // Zip Slip vulnerability prevention
                    if (!newFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                        throw SecurityException("Zip Slip detected: ${entry.name}")
                    }
                    onProgress?.invoke(entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(newFile), BUFFER_SIZE).use { bos ->
                            var read: Int
                            while (zis.read(buffer).also { read = it } != -1) {
                                bos.write(buffer, 0, read)
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

    fun shareMultipleFiles(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        try {
            val authority = "${context.packageName}.provider"
            val uris = ArrayList<Uri>()
            for (f in files) {
                if (f.exists()) {
                    uris.add(FileProvider.getUriForFile(context, authority, f))
                }
            }
            if (uris.isEmpty()) return

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Bagikan ${files.size} berkas")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan berkas: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getAvailableStorages(context: Context): List<StorageVolumeInfo> {
        val list = mutableListOf<StorageVolumeInfo>()
        val dirs = context.getExternalFilesDirs(null)
        dirs.forEachIndexed { index, dir ->
            if (dir != null) {
                val isPrimary = index == 0
                val isRemovable = !isPrimary || android.os.Environment.isExternalStorageRemovable()
                val name = if (isPrimary) "Penyimpanan Internal" else "Kartu SD (Penyimpanan $index)"
                list.add(
                    StorageVolumeInfo(
                        name = name,
                        path = dir,
                        isRemovable = isRemovable,
                        totalBytes = dir.totalSpace,
                        freeBytes = dir.freeSpace
                    )
                )
            }
        }
        if (list.isEmpty()) {
            val internal = context.filesDir
            list.add(
                StorageVolumeInfo(
                    name = "Penyimpanan Internal",
                    path = internal,
                    isRemovable = false,
                    totalBytes = internal.totalSpace,
                    freeBytes = internal.freeSpace
                )
            )
        }
        return list
    }
}
