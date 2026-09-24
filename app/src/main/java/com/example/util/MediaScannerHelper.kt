package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.CategoryOverviewStats
import com.example.model.FileItem
import com.example.model.FileType
import com.example.model.ViewCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaScannerHelper {

    private val documentExtensions = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "csv", "epub"
    )

    private val archiveExtensions = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz"
    )

    private val apkExtensions = setOf(
        "apk", "xapk", "apks"
    )

    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "svg"
    )

    private val videoExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "3gp"
    )

    private val audioExtensions = setOf(
        "mp3", "wav", "flac", "m4a", "aac", "ogg", "wma", "opus"
    )

    suspend fun scanCategoryStatsFast(context: Context): Pair<CategoryOverviewStats, Map<ViewCategory, List<FileItem>>> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val categoryMap = mutableMapOf<ViewCategory, MutableList<FileItem>>()
        ViewCategory.entries.forEach { categoryMap[it] = mutableListOf() }
        val visitedPaths = HashSet<String>()

        // 1. Query Images via MediaStore
        queryMediaStore(
            resolver = resolver,
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            fileType = FileType.IMAGE,
            targetCategory = ViewCategory.IMAGES,
            categoryMap = categoryMap,
            visitedPaths = visitedPaths
        )

        // 2. Query Videos via MediaStore
        queryMediaStore(
            resolver = resolver,
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            fileType = FileType.VIDEO,
            targetCategory = ViewCategory.VIDEOS,
            categoryMap = categoryMap,
            visitedPaths = visitedPaths
        )

        // 3. Query Audio via MediaStore
        queryMediaStore(
            resolver = resolver,
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            fileType = FileType.AUDIO,
            targetCategory = ViewCategory.AUDIO,
            categoryMap = categoryMap,
            visitedPaths = visitedPaths
        )

        // 4. Query MediaStore Files for Documents, Archives, APKs, Downloads
        queryMediaStoreFiles(
            resolver = resolver,
            categoryMap = categoryMap,
            visitedPaths = visitedPaths
        )

        // 5. Fast scan known user directories for any files not yet indexed in MediaStore
        scanStandardDirectories(categoryMap, visitedPaths)

        val images = categoryMap[ViewCategory.IMAGES] ?: emptyList()
        val videos = categoryMap[ViewCategory.VIDEOS] ?: emptyList()
        val audio = categoryMap[ViewCategory.AUDIO] ?: emptyList()
        val docs = categoryMap[ViewCategory.DOCUMENTS] ?: emptyList()
        val archives = categoryMap[ViewCategory.ARCHIVES] ?: emptyList()
        val apks = categoryMap[ViewCategory.APKS] ?: emptyList()
        val downloads = categoryMap[ViewCategory.DOWNLOADS] ?: emptyList()

        val stats = CategoryOverviewStats(
            imagesCount = images.size,
            imagesSize = images.sumOf { it.size },
            videosCount = videos.size,
            videosSize = videos.sumOf { it.size },
            audioCount = audio.size,
            audioSize = audio.sumOf { it.size },
            docsCount = docs.size,
            docsSize = docs.sumOf { it.size },
            archivesCount = archives.size,
            archivesSize = archives.sumOf { it.size },
            apksCount = apks.size,
            apksSize = apks.sumOf { it.size },
            downloadsCount = downloads.size,
            downloadsSize = downloads.sumOf { it.size }
        )

        Pair(stats, categoryMap)
    }

    private fun queryMediaStore(
        resolver: ContentResolver,
        uri: Uri,
        fileType: FileType,
        targetCategory: ViewCategory,
        categoryMap: MutableMap<ViewCategory, MutableList<FileItem>>,
        visitedPaths: HashSet<String>
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        try {
            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val path = if (dataCol >= 0) cursor.getString(dataCol) else null ?: continue
                    if (visitedPaths.contains(path)) continue
                    val file = File(path)
                    if (!file.exists() || file.isDirectory) continue

                    visitedPaths.add(path)
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(file.length()) else file.length()
                    val lastMod = if (dateCol >= 0) cursor.getLong(dateCol) * 1000L else file.lastModified()

                    val item = FileItem(
                        file = file,
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = false,
                        size = size,
                        lastModified = lastMod,
                        extension = file.extension.lowercase(),
                        fileType = fileType,
                        isHidden = file.isHidden || file.name.startsWith(".")
                    )

                    categoryMap[targetCategory]?.add(item)
                    if (file.absolutePath.contains("/Download/", ignoreCase = true) ||
                        file.absolutePath.contains("/Downloads/", ignoreCase = true)
                    ) {
                        categoryMap[ViewCategory.DOWNLOADS]?.add(item)
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun queryMediaStoreFiles(
        resolver: ContentResolver,
        categoryMap: MutableMap<ViewCategory, MutableList<FileItem>>,
        visitedPaths: HashSet<String>
    ) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        try {
            val uri = MediaStore.Files.getContentUri("external")
            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val path = if (dataCol >= 0) cursor.getString(dataCol) else null ?: continue
                    if (visitedPaths.contains(path)) continue
                    val file = File(path)
                    if (!file.exists() || file.isDirectory) continue

                    val ext = file.extension.lowercase()
                    val targetCategory = when {
                        documentExtensions.contains(ext) -> ViewCategory.DOCUMENTS
                        archiveExtensions.contains(ext) -> ViewCategory.ARCHIVES
                        apkExtensions.contains(ext) -> ViewCategory.APKS
                        imageExtensions.contains(ext) -> ViewCategory.IMAGES
                        videoExtensions.contains(ext) -> ViewCategory.VIDEOS
                        audioExtensions.contains(ext) -> ViewCategory.AUDIO
                        else -> null
                    }

                    val isDownload = file.absolutePath.contains("/Download/", ignoreCase = true) ||
                            file.absolutePath.contains("/Downloads/", ignoreCase = true)

                    if (targetCategory != null || isDownload) {
                        visitedPaths.add(path)
                        val size = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(file.length()) else file.length()
                        val lastMod = if (dateCol >= 0) cursor.getLong(dateCol) * 1000L else file.lastModified()
                        val fileType = when (targetCategory) {
                            ViewCategory.DOCUMENTS -> FileType.DOCUMENT
                            ViewCategory.ARCHIVES -> FileType.ARCHIVE
                            ViewCategory.APKS -> FileType.APK
                            ViewCategory.IMAGES -> FileType.IMAGE
                            ViewCategory.VIDEOS -> FileType.VIDEO
                            ViewCategory.AUDIO -> FileType.AUDIO
                            else -> FileUtils.getFileType(file)
                        }

                        val item = FileItem(
                            file = file,
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = false,
                            size = size,
                            lastModified = lastMod,
                            extension = ext,
                            fileType = fileType,
                            isHidden = file.isHidden || file.name.startsWith(".")
                        )

                        if (targetCategory != null) {
                            categoryMap[targetCategory]?.add(item)
                        }
                        if (isDownload) {
                            categoryMap[ViewCategory.DOWNLOADS]?.add(item)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun scanStandardDirectories(
        categoryMap: MutableMap<ViewCategory, MutableList<FileItem>>,
        visitedPaths: HashSet<String>
    ) {
        val root = Environment.getExternalStorageDirectory() ?: return
        val targetFolders = listOf(
            File(root, "Download"),
            File(root, "Downloads"),
            File(root, "Documents"),
            File(root, "DCIM"),
            File(root, "Pictures"),
            File(root, "Movies"),
            File(root, "Music"),
            File(root, "WhatsApp/Media"),
            File(root, "Android/media/com.whatsapp/WhatsApp/Media"),
            File(root, "Telegram")
        )

        for (folder in targetFolders) {
            if (folder.exists() && folder.isDirectory) {
                try {
                    folder.walkTopDown().maxDepth(5).forEach { file ->
                        if (file.isFile && !visitedPaths.contains(file.absolutePath)) {
                            visitedPaths.add(file.absolutePath)
                            val ext = file.extension.lowercase()
                            val category = when {
                                documentExtensions.contains(ext) -> ViewCategory.DOCUMENTS
                                archiveExtensions.contains(ext) -> ViewCategory.ARCHIVES
                                apkExtensions.contains(ext) -> ViewCategory.APKS
                                imageExtensions.contains(ext) -> ViewCategory.IMAGES
                                videoExtensions.contains(ext) -> ViewCategory.VIDEOS
                                audioExtensions.contains(ext) -> ViewCategory.AUDIO
                                else -> null
                            }

                            val isDownload = file.absolutePath.contains("/Download/", ignoreCase = true) ||
                                    file.absolutePath.contains("/Downloads/", ignoreCase = true)

                            if (category != null || isDownload) {
                                val item = FileItem(
                                    file = file,
                                    name = file.name,
                                    path = file.absolutePath,
                                    isDirectory = false,
                                    size = file.length(),
                                    lastModified = file.lastModified(),
                                    extension = ext,
                                    fileType = when (category) {
                                        ViewCategory.DOCUMENTS -> FileType.DOCUMENT
                                        ViewCategory.ARCHIVES -> FileType.ARCHIVE
                                        ViewCategory.APKS -> FileType.APK
                                        ViewCategory.IMAGES -> FileType.IMAGE
                                        ViewCategory.VIDEOS -> FileType.VIDEO
                                        ViewCategory.AUDIO -> FileType.AUDIO
                                        else -> FileUtils.getFileType(file)
                                    },
                                    isHidden = file.isHidden || file.name.startsWith(".")
                                )

                                if (category != null) {
                                    categoryMap[category]?.add(item)
                                }
                                if (isDownload) {
                                    categoryMap[ViewCategory.DOWNLOADS]?.add(item)
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }
}
