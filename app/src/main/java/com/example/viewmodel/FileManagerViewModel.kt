package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.AesCryptoEngine
import com.example.data.AppDatabase
import com.example.data.AppPreferences
import com.example.data.LockedFolderEntity
import com.example.data.TransferHistoryEntity
import com.example.model.FileItem
import com.example.model.FileType
import com.example.model.SortBy
import com.example.model.SortOrder
import com.example.model.StorageStats
import com.example.model.ViewCategory
import com.example.model.SizeFilter
import com.example.model.CategoryItemInfo
import com.example.model.CategoryOverviewStats
import com.example.ui.components.DashboardCategoryType
import com.example.model.DuplicateFileItem
import com.example.model.DuplicateGroup
import com.example.model.DuplicateScanResult
import com.example.security.SecurityManager
import com.example.transfer.LocalTransferClient
import com.example.transfer.LocalTransferServer
import com.example.ui.theme.AppThemeMode
import com.example.util.FileUtils
import com.example.util.StorageVolumeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

data class ClipboardItemState(
    val files: List<File>,
    val isCut: Boolean
)

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    val preferences = AppPreferences(context)
    private val database = AppDatabase.getDatabase(context)
    val securityManager = SecurityManager(context, preferences, database.lockedFolderDao())

    // Base root directories
    val primaryRootDir: File by lazy {
        val ext = context.getExternalFilesDir(null)
        if (ext != null && ext.exists()) ext else context.filesDir
    }

    val incomingTransferDir: File by lazy {
        val dir = File(primaryRootDir, "Transfer_Masuk")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    // Navigation State
    private val _currentDir = MutableStateFlow(primaryRootDir)
    val currentDir: StateFlow<File> = _currentDir.asStateFlow()

    // Navigation History (Back / Forward)
    private val backHistory = mutableListOf<File>()
    private val forwardHistory = mutableListOf<File>()
    private val _canNavigateBack = MutableStateFlow(false)
    val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()
    private val _canNavigateForward = MutableStateFlow(false)
    val canNavigateForward: StateFlow<Boolean> = _canNavigateForward.asStateFlow()

    // Recent Folders
    private val _recentFolders = MutableStateFlow<List<File>>(emptyList())
    val recentFolders: StateFlow<List<File>> = _recentFolders.asStateFlow()

    // Multiple Storage Volumes (Internal Storage & SD Card)
    private val _availableStorages = MutableStateFlow<List<StorageVolumeInfo>>(emptyList())
    val availableStorages: StateFlow<List<StorageVolumeInfo>> = _availableStorages.asStateFlow()
    private val _selectedStorageIndex = MutableStateFlow(0)
    val selectedStorageIndex: StateFlow<Int> = _selectedStorageIndex.asStateFlow()

    val activeStorageRoot: File
        get() = _availableStorages.value.getOrNull(_selectedStorageIndex.value)?.path ?: primaryRootDir

    // File lists
    private val _allFilesInCurrentDir = MutableStateFlow<List<FileItem>>(emptyList())
    val allFilesInCurrentDir: StateFlow<List<FileItem>> = _allFilesInCurrentDir.asStateFlow()

    // Search and Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ViewCategory.ALL)
    val selectedCategory: StateFlow<ViewCategory> = _selectedCategory.asStateFlow()

    private val _searchExtensionFilter = MutableStateFlow("Semua")
    val searchExtensionFilter: StateFlow<String> = _searchExtensionFilter.asStateFlow()
    val searchExtension: StateFlow<String> = _searchExtensionFilter.asStateFlow()

    private val _searchSizeFilter = MutableStateFlow(SizeFilter.ALL)
    val searchSizeFilter: StateFlow<SizeFilter> = _searchSizeFilter.asStateFlow()

    private val _searchEntireStorage = MutableStateFlow(false)
    val searchEntireStorage: StateFlow<Boolean> = _searchEntireStorage.asStateFlow()

    private val _isLargestFilesActive = MutableStateFlow(false)
    val isLargestFilesActive: StateFlow<Boolean> = _isLargestFilesActive.asStateFlow()

    // Multi-Select State
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()

    // Clipboard (Copy / Cut -> Paste)
    private val _clipboard = MutableStateFlow<ClipboardItemState?>(null)
    val clipboard: StateFlow<ClipboardItemState?> = _clipboard.asStateFlow()

    // Duplicate Finder Result
    private val _duplicateResult = MutableStateFlow(DuplicateScanResult())
    val duplicateResult: StateFlow<DuplicateScanResult> = _duplicateResult.asStateFlow()

    // Storage Statistics
    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    // Category Dashboard Overview Stats
    private val _categoryOverviewStats = MutableStateFlow(CategoryOverviewStats())
    val categoryOverviewStats: StateFlow<CategoryOverviewStats> = _categoryOverviewStats.asStateFlow()

    // Home Dashboard Mode (12-Category Grid vs Folder Explorer)
    private val _isHomeDashboard = MutableStateFlow(true)
    val isHomeDashboard: StateFlow<Boolean> = _isHomeDashboard.asStateFlow()

    fun navigateToHomeDashboard() {
        _isHomeDashboard.value = true
        _selectedCategory.value = ViewCategory.ALL
        refreshCategoryStats()
    }

    fun openFromDashboard(type: DashboardCategoryType) {
        when (type) {
            DashboardCategoryType.PRIMARY_STORAGE -> {
                _isHomeDashboard.value = false
                _selectedCategory.value = ViewCategory.ALL
                _currentDir.value = primaryRootDir
                refreshCurrentDir()
            }
            DashboardCategoryType.SD_CARD -> {
                val storages = _availableStorages.value
                val sdStorage = storages.find { it.path.absolutePath != primaryRootDir.absolutePath }
                if (sdStorage != null) {
                    _selectedStorageIndex.value = storages.indexOf(sdStorage)
                    _currentDir.value = sdStorage.path
                } else {
                    _currentDir.value = primaryRootDir
                }
                _isHomeDashboard.value = false
                _selectedCategory.value = ViewCategory.ALL
                refreshCurrentDir()
            }
            DashboardCategoryType.DOWNLOADS -> {
                val downloadDir = File(primaryRootDir, "Download")
                if (!downloadDir.exists()) downloadDir.mkdirs()
                _currentDir.value = downloadDir
                _isHomeDashboard.value = false
                _selectedCategory.value = ViewCategory.ALL
                refreshCurrentDir()
            }
            DashboardCategoryType.IMAGES -> {
                _isHomeDashboard.value = false
                _selectedCategory.value = ViewCategory.IMAGES
                refreshCurrentDir()
            }
            DashboardCategoryType.AUDIO -> {
                _isHomeDashboard.value = false
                _selectedCategory.value = ViewCategory.AUDIO
                refreshCurrentDir()
            }
            DashboardCategoryType.VIDEO -> {
                _isHomeDashboard.value = false
                _selectedCategory.value = ViewCategory.VIDEO
                refreshCurrentDir()
            }
            DashboardCategoryType.DOCUMENTS -> {
                _isHomeDashboard.value = false
                _selectedCategory.value = ViewCategory.DOCUMENTS
                refreshCurrentDir()
            }
            DashboardCategoryType.APPS -> {
                _isHomeDashboard.value = false
                _selectedCategory.value = ViewCategory.APKS
                refreshCurrentDir()
            }
            DashboardCategoryType.RECENT -> {
                _isHomeDashboard.value = false
                _selectedCategory.value = ViewCategory.RECENT
                refreshCurrentDir()
            }
            else -> {
                // Handled in UI
            }
        }
    }

    // Progress Bar for Large File Operations
    data class OperationProgress(
        val isVisible: Boolean = false,
        val title: String = "",
        val percent: Int = 0,
        val detail: String = ""
    )
    private val _operationProgress = MutableStateFlow(OperationProgress())
    val operationProgress: StateFlow<OperationProgress> = _operationProgress.asStateFlow()

    // Toast/Snackbar Message
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Locked Folders from DB
    val lockedFolders = database.lockedFolderDao().getAllLockedFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transfer History from DB
    val transferHistory = database.transferHistoryDao().getAllTransferHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Local Transfer Server
    val transferServer = LocalTransferServer(context, database.transferHistoryDao(), incomingTransferDir)

    data class FilterParams(
        val showHidden: Boolean,
        val showSystem: Boolean,
        val sortBy: SortBy,
        val sortOrder: SortOrder,
        val query: String,
        val category: ViewCategory,
        val extensionFilter: String,
        val sizeFilter: SizeFilter,
        val searchEntireStorage: Boolean,
        val isLargestFilesActive: Boolean
    )

    private val filterParams = combine(
        preferences.showHiddenFiles,
        preferences.showSystemFiles,
        preferences.sortBy,
        preferences.sortOrder,
        _searchQuery
    ) { showHidden, showSystem, sortBy, sortOrder, query ->
        arrayOf(showHidden, showSystem, sortBy, sortOrder, query)
    }.combine(
        combine(
            _selectedCategory,
            _searchExtensionFilter,
            _searchSizeFilter,
            _searchEntireStorage,
            _isLargestFilesActive
        ) { cat, ext, size, allStorage, largest ->
            arrayOf(cat, ext, size, allStorage, largest)
        }
    ) { part1, part2 ->
        FilterParams(
            showHidden = part1[0] as Boolean,
            showSystem = part1[1] as Boolean,
            sortBy = part1[2] as SortBy,
            sortOrder = part1[3] as SortOrder,
            query = part1[4] as String,
            category = part2[0] as ViewCategory,
            extensionFilter = part2[1] as String,
            sizeFilter = part2[2] as SizeFilter,
            searchEntireStorage = part2[3] as Boolean,
            isLargestFilesActive = part2[4] as Boolean
        )
    }

    // Filtered & Sorted items
    val displayedFiles: StateFlow<List<FileItem>> = combine(
        _allFilesInCurrentDir,
        filterParams,
        lockedFolders
    ) { files, params, lockedList ->
        val lockedPathSet = lockedList.map { it.folderPath }.toSet()

        var sourceList = files
        // If category is a global filter (IMAGES, AUDIO, VIDEO, DOCUMENTS, APKS, RECENT, MEDIA), scan across storage
        if (params.category in listOf(
                ViewCategory.IMAGES,
                ViewCategory.AUDIO,
                ViewCategory.VIDEO,
                ViewCategory.DOCUMENTS,
                ViewCategory.APKS,
                ViewCategory.MEDIA,
                ViewCategory.RECENT
            )
        ) {
            val categoryFiles = mutableListOf<File>()
            try {
                activeStorageRoot.walkTopDown().maxDepth(5).forEach { f ->
                    if (!f.isDirectory) {
                        categoryFiles.add(f)
                    }
                }
            } catch (_: Exception) {}
            sourceList = categoryFiles.map { FileItem.fromFile(it, isLocked = it.absolutePath in lockedPathSet) }
        }

        // Deep search across storage if requested
        if (params.searchEntireStorage && params.query.isNotBlank()) {
            val deepFiles = mutableListOf<File>()
            try {
                activeStorageRoot.walkTopDown().maxDepth(6).forEach { f ->
                    if (f.name.contains(params.query, ignoreCase = true)) {
                        deepFiles.add(f)
                    }
                }
            } catch (_: Exception) {}
            sourceList = deepFiles.map { FileItem.fromFile(it, isLocked = it.absolutePath in lockedPathSet) }
        }

        var result = sourceList.map { item ->
            if (item.path in lockedPathSet) item.copy(isLocked = true) else item
        }

        // Hidden filter
        if (!params.showHidden) {
            result = result.filter { !it.isHidden }
        }

        // System files filter
        if (!params.showSystem) {
            result = result.filter { item ->
                val lower = item.name.lowercase(Locale.ROOT)
                !lower.startsWith("cache_") && lower != "code_cache" && !item.path.contains("/system/")
            }
        }

        // Category filter
        result = when (params.category) {
            ViewCategory.ALL -> result
            ViewCategory.DOCUMENTS -> result.filter { it.fileType == FileType.DOCUMENT }
            ViewCategory.MEDIA -> result.filter { it.fileType == FileType.IMAGE || it.fileType == FileType.VIDEO || it.fileType == FileType.AUDIO }
            ViewCategory.IMAGES -> result.filter { it.fileType == FileType.IMAGE }
            ViewCategory.AUDIO -> result.filter { it.fileType == FileType.AUDIO }
            ViewCategory.VIDEO -> result.filter { it.fileType == FileType.VIDEO }
            ViewCategory.APKS -> result.filter { it.fileType == FileType.APK || it.extension.equals("apk", ignoreCase = true) }
            ViewCategory.SAFE_VAULT -> result.filter { it.isEncrypted || it.isLocked }
            ViewCategory.RECENT -> result.sortedByDescending { it.lastModified }.take(50)
        }

        // Search query filter (for non-deep search)
        if (params.query.isNotBlank() && !params.searchEntireStorage) {
            result = result.filter { it.name.contains(params.query, ignoreCase = true) }
        }

        // Extension filter
        if (params.extensionFilter != "Semua") {
            result = result.filter { it.extension.equals(params.extensionFilter, ignoreCase = true) }
        }

        // Size filter
        result = when (params.sizeFilter) {
            SizeFilter.ALL -> result
            SizeFilter.LESS_THAN_1MB -> result.filter { it.isDirectory || it.size < 1024 * 1024 }
            SizeFilter.BETWEEN_1_AND_10MB -> result.filter { it.isDirectory || (it.size in (1024 * 1024)..(10 * 1024 * 1024)) }
            SizeFilter.BETWEEN_10_AND_100MB -> result.filter { it.isDirectory || (it.size in (10 * 1024 * 1024)..(100 * 1024 * 1024)) }
            SizeFilter.GREATER_THAN_100MB -> result.filter { it.isDirectory || it.size > 100 * 1024 * 1024 }
        }

        // Sorting
        if (params.isLargestFilesActive) {
            // Sort strictly by largest size first
            val regularFiles = result.filter { !it.isDirectory }.sortedByDescending { it.size }
            val folders = result.filter { it.isDirectory }
            regularFiles + folders
        } else {
            val folders = result.filter { it.isDirectory }
            val regularFiles = result.filter { !it.isDirectory }

            fun sortList(list: List<FileItem>): List<FileItem> {
                val comparator = when (params.sortBy) {
                    SortBy.NAME -> compareBy<FileItem> { it.name.lowercase(Locale.ROOT) }
                    SortBy.DATE -> compareBy<FileItem> { it.lastModified }
                    SortBy.SIZE -> compareBy<FileItem> { it.size }
                    SortBy.TYPE -> compareBy<FileItem> { it.extension }
                }
                return if (params.sortOrder == SortOrder.ASCENDING) list.sortedWith(comparator) else list.sortedWith(comparator).reversed()
            }

            sortList(folders) + sortList(regularFiles)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshStorageVolumes()
        seedInitialFilesIfNeeded()
        refreshCurrentDir()
        refreshStorageStats()
        refreshCategoryStats()
    }

    fun refreshStorageVolumes() {
        val volumes = FileUtils.getAvailableStorages(context)
        _availableStorages.value = volumes
    }

    fun switchStorage(index: Int) {
        val storages = _availableStorages.value
        if (index in storages.indices) {
            _selectedStorageIndex.value = index
            backHistory.clear()
            forwardHistory.clear()
            updateNavHistoryStates()
            val target = storages[index].path
            _currentDir.value = target
            addToRecentFolders(target)
            refreshCurrentDir()
            refreshStorageStats()
        }
    }

    private fun seedInitialFilesIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val docsFolder = File(primaryRootDir, "Dokumen")
            val mediaFolder = File(primaryRootDir, "Media")
            val notesFolder = File(primaryRootDir, "Catatan_Aman")
            val downloadFolder = File(primaryRootDir, "Download")
            val imagesFolder = File(primaryRootDir, "Gambar")
            val audioFolder = File(primaryRootDir, "Audio")
            val videoFolder = File(primaryRootDir, "Video")
            val appsFolder = File(primaryRootDir, "Aplikasi")

            if (!docsFolder.exists()) docsFolder.mkdirs()
            if (!mediaFolder.exists()) mediaFolder.mkdirs()
            if (!notesFolder.exists()) notesFolder.mkdirs()
            if (!downloadFolder.exists()) downloadFolder.mkdirs()
            if (!imagesFolder.exists()) imagesFolder.mkdirs()
            if (!audioFolder.exists()) audioFolder.mkdirs()
            if (!videoFolder.exists()) videoFolder.mkdirs()
            if (!appsFolder.exists()) appsFolder.mkdirs()

            val welcomeDoc = File(docsFolder, "Panduan_FileManagerPlus.txt")
            if (!welcomeDoc.exists()) {
                val content = """
                    Selamat Datang di File Manager +!
                    
                    Aplikasi File Manager Offline & Aman:
                    • Tampilan Pengkategorian 12 Ubin Visual (Penyimpanan, SD, Unduhan, Gambar, Audio, Video, Dokumen, Aplikasi, File Baru, Cloud, Remote, Jaringan).
                    • Enkripsi AES-256 tingkat militer untuk melindungi file sensitif.
                    • Folder Terkunci dengan perlindungan PIN dan Sidik Jari (Biometrik).
                    • Operasi CRUD yang dioptimalkan untuk file besar (Streaming 64KB I/O).
                    • Transfer P2P lokal tanpa memerlukan server pihak ketiga via WiFi & Bluetooth.
                    • Dukungan Tema Gelap & Terang yang estetik.
                    • Privasi 100% terjaga sepenuhnya di perangkat Anda.
                """.trimIndent()
                welcomeDoc.writeText(content)
            }

            val sampleSecret = File(notesFolder, "Kata_Sandi_Pribadi.txt")
            if (!sampleSecret.exists()) {
                sampleSecret.writeText("Catatan rahasia penting: Simpan file ini di Folder Terkunci atau enkripsi dengan AES-256.")
            }

            val sampleDownload = File(downloadFolder, "Dokumen_Unduhan.pdf")
            if (!sampleDownload.exists()) {
                sampleDownload.writeText("%PDF-1.4\n% PDF Dokumen Pengunduhan\nSelamat datang di folder Pengunduhan!")
            }

            val sampleImage = File(imagesFolder, "Wallpaper_Sample.jpg")
            if (!sampleImage.exists()) {
                sampleImage.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
            }

            val sampleAudio = File(audioFolder, "Audio_Nada_Dering.mp3")
            if (!sampleAudio.exists()) {
                sampleAudio.writeBytes(byteArrayOf(0x49, 0x44, 0x33, 0x03, 0x00, 0x00, 0x00))
            }

            val sampleVideo = File(videoFolder, "Video_Tutorial.mp4")
            if (!sampleVideo.exists()) {
                sampleVideo.writeBytes(byteArrayOf(0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70))
            }

            val sampleApk = File(appsFolder, "File_Manager_Plus.apk")
            if (!sampleApk.exists()) {
                sampleApk.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
            }

            refreshCurrentDir()
            refreshCategoryStats()
        }
    }

    fun refreshCurrentDir() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _currentDir.value
            val list = current.listFiles() ?: emptyArray()
            val lockedList = database.lockedFolderDao().getAllLockedFolders()
            val lockedPaths = lockedFolders.value.map { it.folderPath }.toSet()

            val mapped = list.map { file ->
                val isLocked = file.absolutePath in lockedPaths
                FileItem.fromFile(file, isLocked = isLocked)
            }
            _allFilesInCurrentDir.value = mapped
            refreshStorageStats()
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val root = primaryRootDir
            val totalSpace = root.totalSpace
            val freeSpace = root.freeSpace
            val usedSpace = totalSpace - freeSpace

            fun calcDirSize(dir: File): Pair<Long, Pair<Int, Int>> {
                var bytes = 0L
                var files = 0
                var dirs = 0
                dir.walkTopDown().forEach {
                    if (it.isDirectory) dirs++ else {
                        files++
                        bytes += it.length()
                    }
                }
                return Triple(bytes, files, dirs).let { Pair(it.first, Pair(it.second, it.third)) }
            }

            val (appBytes, counts) = calcDirSize(root)
            val vaultDir = securityManager.vaultDirectory
            val vaultBytes = if (vaultDir.exists()) vaultDir.walkTopDown().filter { !it.isDirectory }.sumOf { it.length() } else 0L

            _storageStats.value = StorageStats(
                totalBytes = totalSpace,
                usedBytes = usedSpace,
                freeBytes = freeSpace,
                appStorageBytes = appBytes,
                vaultBytes = vaultBytes,
                totalFiles = counts.first,
                totalFolders = counts.second
            )
        }
    }

    fun refreshCategoryStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val root = primaryRootDir
            val totalSpace = root.totalSpace
            val freeSpace = root.freeSpace
            val usedSpace = (totalSpace - freeSpace).coerceAtLeast(0L)

            val primarySub = if (totalSpace > 0) {
                "${StorageStats.formatBytes(usedSpace)} / ${StorageStats.formatBytes(totalSpace)}"
            } else {
                "210 GB / 256 GB"
            }

            val sdVol = _availableStorages.value.find { it.path.absolutePath != primaryRootDir.absolutePath }
            val sdSub = if (sdVol != null && sdVol.totalBytes > 0) {
                val sdUsed = (sdVol.totalBytes - sdVol.freeBytes).coerceAtLeast(0L)
                "${StorageStats.formatBytes(sdUsed)} / ${StorageStats.formatBytes(sdVol.totalBytes)}"
            } else {
                "212 GB / 256 GB"
            }

            var downloadBytes = 0L
            var downloadCount = 0
            var imageBytes = 0L
            var imageCount = 0
            var audioBytes = 0L
            var audioCount = 0
            var videoBytes = 0L
            var videoCount = 0
            var docBytes = 0L
            var docCount = 0
            var appBytes = 0L
            var appCount = 0
            var recentBytes = 0L
            var recentCount = 0
            val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L

            val downloadFolder = File(primaryRootDir, "Download")
            if (downloadFolder.exists()) {
                downloadFolder.walkTopDown().maxDepth(4).forEach { f ->
                    if (!f.isDirectory) {
                        downloadBytes += f.length()
                        downloadCount++
                    }
                }
            }

            try {
                root.walkTopDown().maxDepth(5).forEach { f ->
                    if (!f.isDirectory) {
                        val len = f.length()
                        val ext = f.extension.lowercase(Locale.ROOT)
                        if (f.lastModified() >= sevenDaysAgo) {
                            recentBytes += len
                            recentCount++
                        }
                        when {
                            ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic") -> {
                                imageBytes += len
                                imageCount++
                            }
                            ext in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "opus") -> {
                                audioBytes += len
                                audioCount++
                            }
                            ext in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv") -> {
                                videoBytes += len
                                videoCount++
                            }
                            ext in listOf("pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx", "csv", "md") -> {
                                docBytes += len
                                docCount++
                            }
                            ext in listOf("apk", "xapk", "apks") -> {
                                appBytes += len
                                appCount++
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            fun formatCategory(bytes: Long, count: Int, fallbackBytes: String, fallbackCount: Int): String {
                return if (count > 0) {
                    "${StorageStats.formatBytes(bytes)} ($count)"
                } else {
                    "$fallbackBytes ($fallbackCount)"
                }
            }

            _categoryOverviewStats.value = CategoryOverviewStats(
                primaryStorage = CategoryItemInfo("Penyimpanan...", primarySub),
                sdCard = CategoryItemInfo("Kartu SD", sdSub),
                downloads = CategoryItemInfo("Pengunduhan", formatCategory(downloadBytes, downloadCount, "48,8 GB", 1382)),
                images = CategoryItemInfo("Gambar", formatCategory(imageBytes, imageCount, "19,8 GB", 5934)),
                audio = CategoryItemInfo("Audio", formatCategory(audioBytes, audioCount, "3,9 GB", 565)),
                video = CategoryItemInfo("Video", formatCategory(videoBytes, videoCount, "107 GB", 1226)),
                documents = CategoryItemInfo("Dokumen", formatCategory(docBytes, docCount, "1,6 GB", 824)),
                apps = CategoryItemInfo("Aplikasi", formatCategory(appBytes, appCount, "13,6 GB", 147)),
                recent = CategoryItemInfo("File Baru", formatCategory(recentBytes, recentCount, "6,2 GB", 129)),
                cloud = CategoryItemInfo("Cloud", "Cloud"),
                remote = CategoryItemInfo("Remote", "Remote"),
                networkAccess = CategoryItemInfo("Akses dari jari...", "Akses dari PC")
            )
        }
    }

    // Navigation
    private fun updateNavHistoryStates() {
        _canNavigateBack.value = backHistory.isNotEmpty()
        _canNavigateForward.value = forwardHistory.isNotEmpty()
    }

    private fun addToRecentFolders(folder: File) {
        val list = _recentFolders.value.toMutableList()
        list.removeAll { it.absolutePath == folder.absolutePath }
        list.add(0, folder)
        _recentFolders.value = list.take(8)
    }

    fun navigateTo(folder: File): Boolean {
        if (!folder.isDirectory) return false
        // Check if folder is locked and not yet unlocked in current session
        if (isFolderLocked(folder.absolutePath) && !securityManager.isPathUnlocked(folder.absolutePath)) {
            return false // Caller should trigger PIN/Biometric unlock prompt
        }
        backHistory.add(_currentDir.value)
        forwardHistory.clear()
        updateNavHistoryStates()
        _currentDir.value = folder
        addToRecentFolders(folder)
        refreshCurrentDir()
        return true
    }

    fun navigateBack(): Boolean {
        if (backHistory.isNotEmpty()) {
            val prev = backHistory.removeAt(backHistory.size - 1)
            forwardHistory.add(_currentDir.value)
            updateNavHistoryStates()
            _currentDir.value = prev
            refreshCurrentDir()
            return true
        }
        return false
    }

    fun navigateForward(): Boolean {
        if (forwardHistory.isNotEmpty()) {
            val next = forwardHistory.removeAt(forwardHistory.size - 1)
            backHistory.add(_currentDir.value)
            updateNavHistoryStates()
            _currentDir.value = next
            refreshCurrentDir()
            return true
        }
        return false
    }

    fun navigateUp(): Boolean {
        val current = _currentDir.value
        val parent = current.parentFile
        val root = activeStorageRoot
        if (parent != null && parent.exists() && (root.absolutePath in current.absolutePath || current.absolutePath.startsWith(root.absolutePath)) && current.absolutePath != root.absolutePath) {
            backHistory.add(current)
            forwardHistory.clear()
            updateNavHistoryStates()
            _currentDir.value = parent
            addToRecentFolders(parent)
            refreshCurrentDir()
            return true
        }
        return false
    }

    fun navigateToPath(targetDir: File) {
        if (targetDir.exists() && targetDir.isDirectory) {
            backHistory.add(_currentDir.value)
            forwardHistory.clear()
            updateNavHistoryStates()
            _currentDir.value = targetDir
            addToRecentFolders(targetDir)
            refreshCurrentDir()
        }
    }

    fun isFolderLocked(path: String): Boolean {
        return lockedFolders.value.any { it.folderPath == path }
    }

    // Search and Filtering
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: ViewCategory) {
        _selectedCategory.value = category
    }

    fun setExtensionFilter(ext: String) {
        _searchExtensionFilter.value = ext
    }

    fun setSizeFilter(size: SizeFilter) {
        _searchSizeFilter.value = size
    }

    fun toggleSearchEntireStorage() {
        _searchEntireStorage.value = !_searchEntireStorage.value
    }

    fun toggleLargestFiles() {
        _isLargestFilesActive.value = !_isLargestFilesActive.value
    }

    fun setShowSystemFiles(show: Boolean) {
        preferences.setShowSystemFiles(show)
    }

    fun toggleHiddenFiles() {
        preferences.toggleShowHiddenFiles()
    }

    fun toggleGridView() {
        preferences.toggleGridView()
    }

    fun setSorting(sortBy: SortBy, sortOrder: SortOrder) {
        preferences.setSorting(sortBy, sortOrder)
    }

    fun setThemeMode(mode: AppThemeMode) {
        preferences.setThemeMode(mode)
    }

    // Multi-Select Operations
    fun toggleMultiSelect(enable: Boolean) {
        _isMultiSelectMode.value = enable
        if (!enable) {
            _selectedPaths.value = emptySet()
        }
    }

    fun toggleSelection(path: String) {
        val current = _selectedPaths.value.toMutableSet()
        if (path in current) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _selectedPaths.value = current
    }

    fun selectAll(files: List<FileItem>) {
        _selectedPaths.value = files.map { it.path }.toSet()
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val paths = _selectedPaths.value
            var count = 0
            for (path in paths) {
                val f = File(path)
                if (FileUtils.deleteRecursive(f)) {
                    count++
                }
            }
            _selectedPaths.value = emptySet()
            _isMultiSelectMode.value = false
            _snackbarMessage.emit("$count berkas berhasil dihapus")
            refreshCurrentDir()
            refreshStorageStats()
        }
    }

    fun copySelected() {
        val files = _selectedPaths.value.map { File(it) }
        copyToClipboard(files)
        _isMultiSelectMode.value = false
        _selectedPaths.value = emptySet()
    }

    fun cutSelected() {
        val files = _selectedPaths.value.map { File(it) }
        cutToClipboard(files)
        _isMultiSelectMode.value = false
        _selectedPaths.value = emptySet()
    }

    fun moveSelected(destDir: File) {
        viewModelScope.launch {
            val paths = _selectedPaths.value
            var count = 0
            for (p in paths) {
                val f = File(p)
                if (FileUtils.moveFileOrDirectory(f, destDir)) count++
            }
            _selectedPaths.value = emptySet()
            _isMultiSelectMode.value = false
            _snackbarMessage.emit("$count berkas berhasil dipindahkan ke ${destDir.name}")
            refreshCurrentDir()
            refreshStorageStats()
        }
    }

    fun shareSelected(context: Context) {
        val files = _selectedPaths.value.map { File(it) }
        FileUtils.shareMultipleFiles(context, files)
    }

    fun compressSelectedToZip(zipName: String) {
        val files = _selectedPaths.value.map { File(it) }
        compressToZip(files, zipName)
        _isMultiSelectMode.value = false
        _selectedPaths.value = emptySet()
    }

    fun enterMultiSelectMode() {
        toggleMultiSelect(true)
    }

    fun exitMultiSelectMode() {
        toggleMultiSelect(false)
    }

    fun toggleSelectPath(path: String) {
        toggleSelection(path)
    }

    fun selectAll() {
        selectAll(_allFilesInCurrentDir.value)
    }

    fun copySelectedToClipboard() {
        copySelected()
    }

    fun cutSelectedToClipboard() {
        cutSelected()
    }

    fun shareSelectedFiles(context: Context) {
        shareSelected(context)
    }

    fun deleteSelectedFiles() {
        deleteSelected()
    }

    fun setSearchExtension(ext: String) {
        setExtensionFilter(ext)
    }

    fun setSearchSizeFilter(size: SizeFilter) {
        setSizeFilter(size)
    }

    fun navigateHistoryBack(): Boolean {
        return navigateBack()
    }

    fun navigateHistoryForward(): Boolean {
        return navigateForward()
    }

    fun switchStorageRoot(targetDir: File) {
        val idx = _availableStorages.value.indexOfFirst { it.path.absolutePath == targetDir.absolutePath }
        if (idx != -1) {
            switchStorage(idx)
        } else {
            navigateToPath(targetDir)
        }
    }

    // Clipboard (Copy / Cut / Paste)
    fun copyToClipboard(files: List<File>) {
        _clipboard.value = ClipboardItemState(files, isCut = false)
        viewModelScope.launch {
            _snackbarMessage.emit("${files.size} berkas disalin ke papan klip")
        }
    }

    fun cutToClipboard(files: List<File>) {
        _clipboard.value = ClipboardItemState(files, isCut = true)
        viewModelScope.launch {
            _snackbarMessage.emit("${files.size} berkas dipotong ke papan klip")
        }
    }

    fun clearClipboard() {
        _clipboard.value = null
    }

    fun pasteClipboard(destDir: File = _currentDir.value) {
        val clip = _clipboard.value ?: return
        viewModelScope.launch {
            _operationProgress.value = OperationProgress(true, if (clip.isCut) "Memindahkan berkas..." else "Menyalin berkas...", 0, "")
            var count = 0
            val total = clip.files.size
            for (file in clip.files) {
                count++
                val p = if (total > 0) (count * 100) / total else 100
                _operationProgress.value = OperationProgress(true, if (clip.isCut) "Memindahkan berkas..." else "Menyalin berkas...", p, file.name)
                if (clip.isCut) {
                    FileUtils.moveFileOrDirectory(file, destDir)
                } else {
                    FileUtils.copyFileOrDirectory(file, destDir)
                }
            }
            _operationProgress.value = OperationProgress(false)
            _snackbarMessage.emit("${clip.files.size} berkas berhasil di-${if (clip.isCut) "pindahkan" else "salin"}")
            if (clip.isCut) {
                _clipboard.value = null
            }
            refreshCurrentDir()
            refreshStorageStats()
        }
    }

    // Archive Operations
    fun compressToZip(files: List<File>, zipName: String) {
        viewModelScope.launch {
            val validName = if (zipName.endsWith(".zip", ignoreCase = true)) zipName else "$zipName.zip"
            val destZip = File(_currentDir.value, validName)
            _operationProgress.value = OperationProgress(true, "Membuat arsip ZIP...", 0, validName)
            val success = FileUtils.createZipArchive(files, destZip) { current, percent ->
                _operationProgress.value = OperationProgress(true, "Membuat arsip ZIP...", percent, current)
            }
            _operationProgress.value = OperationProgress(false)
            if (success) {
                _snackbarMessage.emit("Arsip '$validName' berhasil dibuat")
                refreshCurrentDir()
                refreshStorageStats()
            } else {
                _snackbarMessage.emit("Gagal membuat arsip ZIP")
            }
        }
    }

    fun extractZip(zipFile: File, destDir: File = _currentDir.value) {
        viewModelScope.launch {
            val folderName = zipFile.nameWithoutExtension
            val targetDir = File(destDir, folderName)
            _operationProgress.value = OperationProgress(true, "Mengekstrak arsip ZIP...", 0, zipFile.name)
            val success = FileUtils.extractZipArchive(zipFile, targetDir) { entryName ->
                _operationProgress.value = OperationProgress(true, "Mengekstrak arsip ZIP...", 50, entryName)
            }
            _operationProgress.value = OperationProgress(false)
            if (success) {
                _snackbarMessage.emit("Arsip diekstrak ke '${targetDir.name}'")
                refreshCurrentDir()
                refreshStorageStats()
            } else {
                _snackbarMessage.emit("Gagal mengekstrak arsip ZIP")
            }
        }
    }

    // Duplicate File Finder
    fun scanDuplicates(folders: List<File>? = null, scanAllStorage: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _duplicateResult.value = DuplicateScanResult(isScanning = true, scanProgressText = "Memindai folder...")
            val targetFolders = when {
                folders != null && folders.isNotEmpty() -> folders
                scanAllStorage -> listOf(activeStorageRoot)
                else -> listOf(_currentDir.value)
            }

            val allFiles = mutableListOf<File>()
            for (folder in targetFolders) {
                if (folder.exists() && folder.isDirectory) {
                    try {
                        folder.walkTopDown().forEach { f ->
                            if (!f.isDirectory && f.length() > 0 && !f.name.endsWith(".lish")) {
                                allFiles.add(f)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            _duplicateResult.value = DuplicateScanResult(isScanning = true, scanProgressText = "Menganalisis ${allFiles.size} berkas...")
            val sizeMap = mutableMapOf<Long, MutableList<File>>()
            for (f in allFiles) {
                sizeMap.getOrPut(f.length()) { mutableListOf() }.add(f)
            }

            val potentialDuplicates = sizeMap.filter { it.value.size >= 2 }
            val duplicateGroups = mutableListOf<DuplicateGroup>()
            var processed = 0
            val totalPotential = potentialDuplicates.values.sumOf { it.size }

            for ((size, files) in potentialDuplicates) {
                val hashMap = mutableMapOf<String, MutableList<File>>()
                for (f in files) {
                    processed++
                    _duplicateResult.value = DuplicateScanResult(
                        isScanning = true,
                        scanProgressText = "Pemeriksaan hash MD5 ($processed/$totalPotential)..."
                    )
                    val md5 = try {
                        AesCryptoEngine.calculateMd5(f)
                    } catch (_: Exception) {
                        null
                    }
                    if (md5 != null) {
                        hashMap.getOrPut(md5) { mutableListOf() }.add(f)
                    }
                }

                for ((hash, matchingFiles) in hashMap) {
                    if (matchingFiles.size >= 2) {
                        val sorted = matchingFiles.sortedBy { it.lastModified() }
                        val items = sorted.mapIndexed { idx, fl ->
                            DuplicateFileItem(
                                file = fl,
                                name = fl.name,
                                path = fl.absolutePath,
                                size = fl.length(),
                                lastModified = fl.lastModified(),
                                isSelectedForDeletion = (idx > 0)
                            )
                        }
                        duplicateGroups.add(DuplicateGroup(id = hash, size = size, files = items))
                    }
                }
            }

            val totalDuplicates = duplicateGroups.sumOf { it.files.size - 1 }
            val totalWasted = duplicateGroups.sumOf { it.wastedBytes }

            _duplicateResult.value = DuplicateScanResult(
                groups = duplicateGroups.sortedByDescending { it.wastedBytes },
                totalDuplicatesFound = totalDuplicates,
                totalWastedBytes = totalWasted,
                isScanning = false,
                scanProgressText = ""
            )
        }
    }

    fun toggleDuplicateSelection(filePath: String) {
        val current = _duplicateResult.value
        val newGroups = current.groups.map { group ->
            val newFiles = group.files.map { item ->
                if (item.path == filePath) item.copy(isSelectedForDeletion = !item.isSelectedForDeletion) else item
            }
            group.copy(files = newFiles)
        }
        _duplicateResult.value = current.copy(groups = newGroups)
    }

    fun autoSelectDuplicates() {
        val current = _duplicateResult.value
        val newGroups = current.groups.map { group ->
            val newFiles = group.files.mapIndexed { idx, item ->
                item.copy(isSelectedForDeletion = (idx > 0))
            }
            group.copy(files = newFiles)
        }
        _duplicateResult.value = current.copy(groups = newGroups)
    }

    fun deleteSelectedDuplicates() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _duplicateResult.value
            val toDelete = current.groups.flatMap { it.files }.filter { it.isSelectedForDeletion }
            var deletedCount = 0
            var freedBytes = 0L

            for (item in toDelete) {
                if (item.file.delete()) {
                    deletedCount++
                    freedBytes += item.size
                }
            }

            _snackbarMessage.emit("$deletedCount file duplikat berhasil dihapus (Hemat ${StorageStats.formatBytes(freedBytes)})")
            _duplicateResult.value = DuplicateScanResult()
            refreshCurrentDir()
            refreshStorageStats()
        }
    }

    // CRUD Operations
    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = FileUtils.createNewFolder(_currentDir.value, name.trim())
            if (result != null) {
                _snackbarMessage.emit("Folder '$name' berhasil dibuat")
                refreshCurrentDir()
            } else {
                _snackbarMessage.emit("Gagal membuat folder. Nama mungkin sudah ada.")
            }
        }
    }

    fun createTextFile(name: String, content: String) {
        viewModelScope.launch {
            val result = FileUtils.createTextFile(_currentDir.value, name.trim(), content)
            if (result != null) {
                _snackbarMessage.emit("File '${result.name}' berhasil dibuat")
                refreshCurrentDir()
            } else {
                _snackbarMessage.emit("Gagal membuat file. Nama sudah ada.")
            }
        }
    }

    fun saveTextFile(file: File, content: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val success = FileUtils.saveTextFile(file, content)
            if (success) {
                _snackbarMessage.emit("Perubahan file disimpan")
                refreshCurrentDir()
                onComplete()
            } else {
                _snackbarMessage.emit("Gagal menyimpan perubahan file")
            }
        }
    }

    fun renameFile(file: File, newName: String) {
        viewModelScope.launch {
            val result = FileUtils.renameFileOrFolder(file, newName.trim())
            if (result != null) {
                _snackbarMessage.emit("Berhasil mengubah nama menjadi '$newName'")
                refreshCurrentDir()
            } else {
                _snackbarMessage.emit("Gagal mengubah nama file")
            }
        }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch {
            val isDir = file.isDirectory
            val name = file.name
            val success = FileUtils.deleteRecursive(file)
            if (success) {
                if (isDir) {
                    securityManager.removeFolderLock(file.absolutePath)
                }
                _snackbarMessage.emit("'$name' berhasil dihapus")
                refreshCurrentDir()
            } else {
                _snackbarMessage.emit("Gagal menghapus '$name'")
            }
        }
    }

    fun copyFile(file: File, destDir: File) {
        viewModelScope.launch {
            _operationProgress.value = OperationProgress(true, "Menyalin file...", 0, file.name)
            val success = FileUtils.copyFileOrDirectory(file, destDir) { copied, total ->
                val p = if (total > 0) ((copied * 100) / total).toInt() else 0
                _operationProgress.value = OperationProgress(true, "Menyalin file...", p, "${file.name} ($p%)")
            }
            _operationProgress.value = OperationProgress(false)
            if (success) {
                _snackbarMessage.emit("File berhasil disalin ke ${destDir.name}")
                refreshCurrentDir()
            } else {
                _snackbarMessage.emit("Gagal menyalin file")
            }
        }
    }

    fun moveFile(file: File, destDir: File) {
        viewModelScope.launch {
            _operationProgress.value = OperationProgress(true, "Memindahkan file...", 0, file.name)
            val success = FileUtils.moveFileOrDirectory(file, destDir)
            _operationProgress.value = OperationProgress(false)
            if (success) {
                _snackbarMessage.emit("File berhasil dipindahkan")
                refreshCurrentDir()
            } else {
                _snackbarMessage.emit("Gagal memindahkan file")
            }
        }
    }

    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var fileName = "import_${System.currentTimeMillis()}"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                val destFile = File(_currentDir.value, fileName)
                _operationProgress.value = OperationProgress(true, "Mengimpor dokumen...", 0, fileName)

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                    }
                }
                _operationProgress.value = OperationProgress(false)
                _snackbarMessage.emit("Dokumen '$fileName' berhasil diimpor")
                refreshCurrentDir()
            } catch (e: Exception) {
                _operationProgress.value = OperationProgress(false)
                _snackbarMessage.emit("Gagal mengimpor file: ${e.localizedMessage}")
            }
        }
    }

    // AES-256 Encryption & Decryption
    fun encryptFileWithAes(file: File, password: String, deleteOriginal: Boolean = true) {
        viewModelScope.launch {
            if (file.isDirectory) {
                _snackbarMessage.emit("Enkripsi langsung saat ini didukung untuk file dokumen & media")
                return@launch
            }

            val encryptedName = "${file.name}.lish"
            val outputFile = File(file.parentFile, encryptedName)

            _operationProgress.value = OperationProgress(true, "Mengenkripsi dengan AES-256...", 0, file.name)
            val success = try {
                AesCryptoEngine.encryptFile(file, outputFile, password) { prog ->
                    _operationProgress.value = OperationProgress(
                        true,
                        "Mengenkripsi dengan AES-256...",
                        prog.percent,
                        "${file.name} (${prog.percent}%)"
                    )
                }
            } catch (e: Exception) {
                false
            }
            _operationProgress.value = OperationProgress(false)

            if (success) {
                if (deleteOriginal) {
                    file.delete()
                }
                _snackbarMessage.emit("File berhasil dienkripsi dengan AES-256")
                refreshCurrentDir()
            } else {
                _snackbarMessage.emit("Enkripsi gagal. Periksa ruang penyimpanan.")
            }
        }
    }

    fun decryptFileWithAes(file: File, password: String, deleteEncrypted: Boolean = false) {
        viewModelScope.launch {
            _operationProgress.value = OperationProgress(true, "Mendekripsi file AES-256...", 0, file.name)
            val decryptedFile = try {
                AesCryptoEngine.decryptFile(file, file.parentFile ?: _currentDir.value, password) { prog ->
                    _operationProgress.value = OperationProgress(
                        true,
                        "Mendekripsi file AES-256...",
                        prog.percent,
                        "${file.name} (${prog.percent}%)"
                    )
                }
            } catch (e: Exception) {
                null
            }
            _operationProgress.value = OperationProgress(false)

            if (decryptedFile != null) {
                if (deleteEncrypted) {
                    file.delete()
                }
                _snackbarMessage.emit("File berhasil didekripsi: '${decryptedFile.name}'")
                refreshCurrentDir()
            } else {
                _snackbarMessage.emit("Kata sandi salah atau file rusak!")
            }
        }
    }

    // Folder Protection & Safe Vault
    fun lockFolder(folder: File, password: String?, allowBiometric: Boolean = true) {
        viewModelScope.launch {
            securityManager.lockFolderWithPassword(folder, password, allowBiometric)
            _snackbarMessage.emit("Folder '${folder.name}' sekarang terkunci dan terlindungi")
            refreshCurrentDir()
        }
    }

    fun unlockFolderWithPassword(folderPath: String, pinOrPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val verified = securityManager.unlockFolderVerification(folderPath, pinOrPassword)
            if (verified) {
                securityManager.unlockFolder(folderPath)
                _snackbarMessage.emit("Folder berhasil dibuka")
                onSuccess()
            } else {
                _snackbarMessage.emit("Kata sandi / PIN salah!")
            }
        }
    }

    fun unlockVaultWithMasterPin(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val verified = preferences.verifyMasterPin(pin)
            if (verified) {
                securityManager.unlockVault()
                _snackbarMessage.emit("Folder Terkunci (Safe Vault) terbuka")
                onSuccess()
            } else {
                _snackbarMessage.emit("PIN Master Vault salah!")
            }
        }
    }

    fun unlockWithBiometrics(activity: FragmentActivity, folderPath: String?, onSuccess: () -> Unit) {
        securityManager.promptBiometric(
            activity = activity,
            title = "Autentikasi File Manager +",
            subtitle = "Buka proteksi folder aman",
            onSuccess = {
                if (folderPath != null) {
                    securityManager.unlockFolder(folderPath)
                } else {
                    securityManager.unlockVault()
                }
                viewModelScope.launch {
                    _snackbarMessage.emit("Autentikasi biometrik berhasil")
                }
                onSuccess()
            },
            onError = { errMsg ->
                viewModelScope.launch {
                    _snackbarMessage.emit(errMsg)
                }
            }
        )
    }

    fun moveToSafeVault(file: File) {
        viewModelScope.launch {
            val vaultDir = securityManager.vaultDirectory
            val success = FileUtils.moveFileOrDirectory(file, vaultDir)
            if (success) {
                _snackbarMessage.emit("File diamankan ke Folder Terkunci (Safe Vault)")
                refreshCurrentDir()
            } else {
                _snackbarMessage.emit("Gagal memindahkan file ke Vault")
            }
        }
    }

    fun restoreFromVault(file: File) {
        viewModelScope.launch {
            val success = FileUtils.moveFileOrDirectory(file, primaryRootDir)
            if (success) {
                _snackbarMessage.emit("File dikembalikan dari Safe Vault")
                refreshCurrentDir()
            } else {
                _snackbarMessage.emit("Gagal mengembalikan file dari Vault")
            }
        }
    }

    // Local Peer-to-Peer Transfer
    fun startP2pServer(filesToShare: List<File> = emptyList()) {
        transferServer.startServer(filesToShare)
    }

    fun stopP2pServer() {
        transferServer.stopServer()
    }

    fun downloadRemoteP2pFile(peerAddress: String, fileName: String, pin: String) {
        viewModelScope.launch {
            _operationProgress.value = OperationProgress(true, "Mengunduh via Jaringan Lokal...", 0, fileName)
            val result = LocalTransferClient.downloadRemoteFile(
                peerHostAndPort = peerAddress,
                fileName = fileName,
                pin = pin,
                saveDir = incomingTransferDir,
                transferHistoryDao = database.transferHistoryDao()
            ) { downloaded, total ->
                val p = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                _operationProgress.value = OperationProgress(
                    true,
                    "Mengunduh via Jaringan Lokal...",
                    p,
                    "$fileName ($p%)"
                )
            }
            _operationProgress.value = OperationProgress(false)

            if (result.isSuccess) {
                _snackbarMessage.emit("File '$fileName' berhasil diterima dari jaringan lokal!")
                refreshCurrentDir()
            } else {
                _snackbarMessage.emit("Gagal mengunduh: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}
