package com.example.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileType {
    FOLDER,
    DOCUMENT,
    IMAGE,
    VIDEO,
    AUDIO,
    ARCHIVE,
    APK,
    ENCRYPTED,
    CODE,
    OTHER
}

enum class SortBy(val label: String) {
    NAME("Nama"),
    DATE("Tanggal"),
    SIZE("Ukuran"),
    TYPE("Tipe")
}

enum class SortOrder(val label: String) {
    ASCENDING("Menaik (A-Z / Kecil-Besar)"),
    DESCENDING("Menurun (Z-A / Besar-Kecil)")
}

enum class ViewCategory {
    ALL,
    DOCUMENTS,
    MEDIA,
    IMAGES,
    AUDIO,
    VIDEO,
    APKS,
    SAFE_VAULT,
    RECENT
}

enum class SizeFilter(val label: String) {
    ALL("Semua Ukuran"),
    LESS_THAN_1MB("< 1 MB"),
    BETWEEN_1_AND_10MB("1 - 10 MB"),
    BETWEEN_10_AND_100MB("10 - 100 MB"),
    GREATER_THAN_100MB("> 100 MB")
}

data class CategoryItemInfo(
    val title: String,
    val subtitle: String,
    val totalBytes: Long = 0L,
    val count: Int = 0
)

data class CategoryOverviewStats(
    val primaryStorage: CategoryItemInfo = CategoryItemInfo("Penyimpanan...", "210 GB / 256 GB"),
    val sdCard: CategoryItemInfo = CategoryItemInfo("Kartu SD", "212 GB / 256 GB"),
    val downloads: CategoryItemInfo = CategoryItemInfo("Pengunduhan", "48,8 GB (1382)"),
    val images: CategoryItemInfo = CategoryItemInfo("Gambar", "19,8 GB (5934)"),
    val audio: CategoryItemInfo = CategoryItemInfo("Audio", "3,9 GB (565)"),
    val video: CategoryItemInfo = CategoryItemInfo("Video", "107 GB (1226)"),
    val documents: CategoryItemInfo = CategoryItemInfo("Dokumen", "1,6 GB (824)"),
    val apps: CategoryItemInfo = CategoryItemInfo("Aplikasi", "13,6 GB (147)"),
    val recent: CategoryItemInfo = CategoryItemInfo("File Baru", "6,2 GB (129)"),
    val cloud: CategoryItemInfo = CategoryItemInfo("Cloud", "Cloud"),
    val remote: CategoryItemInfo = CategoryItemInfo("Remote", "Remote"),
    val networkAccess: CategoryItemInfo = CategoryItemInfo("Akses dari jari...", "Akses dari PC")
)

data class FileItem(
    val file: File,
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val isHidden: Boolean,
    val isEncrypted: Boolean,
    val isLocked: Boolean = false,
    val extension: String,
    val fileType: FileType,
    val childCount: Int = 0
) {
    val formattedSize: String
        get() {
            if (isDirectory) {
                return if (childCount == 1) "1 item" else "$childCount items"
            }
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = size / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    companion object {
        fun fromFile(file: File, isLocked: Boolean = false): FileItem {
            val isDir = file.isDirectory
            val name = file.name
            val ext = if (isDir) "" else file.extension.lowercase(Locale.ROOT)
            val isEnc = name.endsWith(".lish", ignoreCase = true) || name.endsWith(".aes", ignoreCase = true)
            val childCount = if (isDir) {
                file.listFiles()?.size ?: 0
            } else 0

            val type = when {
                isDir -> FileType.FOLDER
                isEnc -> FileType.ENCRYPTED
                ext in listOf("pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx", "csv", "md") -> FileType.DOCUMENT
                ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic") -> FileType.IMAGE
                ext in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv") -> FileType.VIDEO
                ext in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "opus") -> FileType.AUDIO
                ext in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") -> FileType.ARCHIVE
                ext in listOf("apk", "xapk", "apks") -> FileType.APK
                ext in listOf("kt", "java", "py", "js", "html", "css", "json", "xml", "c", "cpp", "ts") -> FileType.CODE
                else -> FileType.OTHER
            }

            return FileItem(
                file = file,
                name = name,
                path = file.absolutePath,
                size = if (isDir) 0L else file.length(),
                lastModified = file.lastModified(),
                isDirectory = isDir,
                isHidden = file.isHidden || name.startsWith("."),
                isEncrypted = isEnc,
                isLocked = isLocked,
                extension = ext,
                fileType = type,
                childCount = childCount
            )
        }
    }
}

data class StorageStats(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val appStorageBytes: Long = 0L,
    val vaultBytes: Long = 0L,
    val totalFiles: Int = 0,
    val totalFolders: Int = 0
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedUsed: String
        get() = formatBytes(usedBytes)

    val formattedTotal: String
        get() = formatBytes(totalBytes)

    val formattedFree: String
        get() = formatBytes(freeBytes)

    val formattedAppStorage: String
        get() = formatBytes(appStorageBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }
}
