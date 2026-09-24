package com.example.model

import java.io.File

enum class FileType {
    FOLDER,
    IMAGE,
    AUDIO,
    VIDEO,
    DOCUMENT,
    ARCHIVE,
    APK,
    CODE,
    ENCRYPTED,
    OTHER
}

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val extension: String = file.extension.lowercase(),
    val fileType: FileType = FileType.OTHER,
    val isLocked: Boolean = false,
    val isHidden: Boolean = com.example.util.FileUtils.isHiddenOrInHiddenFolder(file)
)

enum class SortBy {
    NAME,
    DATE,
    SIZE,
    TYPE
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

enum class ViewCategory {
    ALL,
    IMAGES,
    AUDIO,
    VIDEOS,
    DOCUMENTS,
    ARCHIVES,
    APKS,
    DOWNLOADS
}

enum class SizeFilter {
    ANY,
    SMALL,   // < 1 MB
    MEDIUM,  // 1 MB - 50 MB
    LARGE,   // 50 MB - 500 MB
    HUGE     // > 500 MB
}

data class StorageStats(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val usedPercent: Float = 0f
)

data class CategoryOverviewStats(
    val imagesCount: Int = 0,
    val imagesSize: Long = 0L,
    val audioCount: Int = 0,
    val audioSize: Long = 0L,
    val videosCount: Int = 0,
    val videosSize: Long = 0L,
    val docsCount: Int = 0,
    val docsSize: Long = 0L,
    val archivesCount: Int = 0,
    val archivesSize: Long = 0L,
    val apksCount: Int = 0,
    val apksSize: Long = 0L,
    val downloadsCount: Int = 0,
    val downloadsSize: Long = 0L
)

enum class DuplicateScopeType {
    CURRENT_FOLDER,
    SELECTED_FOLDERS,
    ENTIRE_STORAGE
}

data class DuplicateScanResult(
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val totalWastedBytes: Long = 0L,
    val isScanning: Boolean = false,
    val scopeDescription: String = "Folder Saat Ini"
)

data class ZipEntryItem(
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val compressedSize: Long
)

data class DuplicateGroup(
    val checksum: String,
    val fileSize: Long,
    val files: List<DuplicateFileItem>
)

data class DuplicateFileItem(
    val file: File,
    val isSelectedForDelete: Boolean = false
)

data class StorageVolumeInfo(
    val name: String,
    val rootDir: File,
    val totalSpace: Long,
    val freeSpace: Long,
    val isPrimary: Boolean = true,
    val isRemovable: Boolean = false
)

enum class DashboardCategoryType {
    IMAGES,
    VIDEOS,
    AUDIO,
    DOCUMENTS,
    DOWNLOADS,
    APKS,
    ARCHIVES,
    SAFE_VAULT,
    DUPLICATE_SCANNER
}

data class DashboardTileItem(
    val type: DashboardCategoryType,
    val title: String,
    val count: Int,
    val sizeText: String,
    val colorHex: Long
)

data class ClipboardItemState(
    val files: List<File> = emptyList(),
    val isCut: Boolean = false
)

data class TransferItem(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val isDownload: Boolean,
    val peerAddress: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = true
)

data class CategoryFolderBucket(
    val folder: File,
    val folderName: String,
    val displayPath: String,
    val files: List<FileItem>,
    val fileCount: Int = files.size,
    val totalSize: Long = files.sumOf { it.size },
    val previewFile: FileItem? = files.firstOrNull()
)
