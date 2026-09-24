package com.example.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.AesCryptoEngine
import com.example.data.AppPreferences
import com.example.model.CategoryOverviewStats
import com.example.model.ClipboardItemState
import com.example.model.DashboardCategoryType
import com.example.model.DuplicateFileItem
import com.example.model.DuplicateGroup
import com.example.model.DuplicateScanResult
import com.example.model.FileItem
import com.example.model.FileType
import com.example.model.SizeFilter
import com.example.model.SortBy
import com.example.model.SortOrder
import com.example.model.StorageStats
import com.example.model.StorageVolumeInfo
import com.example.model.TransferItem
import com.example.model.ViewCategory
import com.example.transfer.LocalTransferClient
import com.example.transfer.LocalTransferServer
import com.example.ui.theme.AppThemeMode
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.Stack

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val preferences = AppPreferences(context)
    val transferServer = LocalTransferServer(port = 8080)
    val transferClient = LocalTransferClient()

    // Navigation & Directories
    private val backHistory = Stack<File>()
    private val forwardHistory = Stack<File>()

    private val _availableStorages = MutableStateFlow<List<StorageVolumeInfo>>(emptyList())
    val availableStorages: StateFlow<List<StorageVolumeInfo>> = _availableStorages.asStateFlow()

    private val _selectedStorageIndex = MutableStateFlow(0)
    val selectedStorageIndex: StateFlow<Int> = _selectedStorageIndex.asStateFlow()

    private val _currentDir = MutableStateFlow<File>(Environment.getExternalStorageDirectory())
    val currentDir: StateFlow<File> = _currentDir.asStateFlow()

    private val _canNavigateBack = MutableStateFlow(false)
    val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()

    private val _canNavigateForward = MutableStateFlow(false)
    val canNavigateForward: StateFlow<Boolean> = _canNavigateForward.asStateFlow()

    // Files in Current Directory
    private val _allFilesInCurrentDir = MutableStateFlow<List<FileItem>>(emptyList())
    val allFilesInCurrentDir: StateFlow<List<FileItem>> = _allFilesInCurrentDir.asStateFlow()

    // Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _extensionFilter = MutableStateFlow<String?>(null)
    val extensionFilter: StateFlow<String?> = _extensionFilter.asStateFlow()

    private val _isDeepSearching = MutableStateFlow(false)
    val isDeepSearching: StateFlow<Boolean> = _isDeepSearching.asStateFlow()

    private val _deepSearchResults = MutableStateFlow<List<FileItem>>(emptyList())
    val deepSearchResults: StateFlow<List<FileItem>> = _deepSearchResults.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ViewCategory.ALL)
    val selectedCategory: StateFlow<ViewCategory> = _selectedCategory.asStateFlow()

    private val _sizeFilter = MutableStateFlow(SizeFilter.ANY)
    val sizeFilter: StateFlow<SizeFilter> = _sizeFilter.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.NAME)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _searchEntireStorage = MutableStateFlow(false)
    val searchEntireStorage: StateFlow<Boolean> = _searchEntireStorage.asStateFlow()

    // Multi-Select & Clipboard
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()

    private val _clipboard = MutableStateFlow<ClipboardItemState?>(null)
    val clipboard: StateFlow<ClipboardItemState?> = _clipboard.asStateFlow()

    // Stats
    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _categoryOverviewStats = MutableStateFlow(CategoryOverviewStats())
    val categoryOverviewStats: StateFlow<CategoryOverviewStats> = _categoryOverviewStats.asStateFlow()

    // Duplicates
    private val _duplicateResult = MutableStateFlow(DuplicateScanResult())
    val duplicateResult: StateFlow<DuplicateScanResult> = _duplicateResult.asStateFlow()

    // Recent Folders
    val recentFolders: StateFlow<List<String>> = preferences.recentFolders

    // Safe Vault State
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _vaultFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val vaultFiles: StateFlow<List<FileItem>> = _vaultFiles.asStateFlow()

    // P2P Transfer Remote State
    private val _remotePeerFiles = MutableStateFlow<List<JSONObject>>(emptyList())
    val remotePeerFiles: StateFlow<List<JSONObject>> = _remotePeerFiles.asStateFlow()

    private val _transferHistory = MutableStateFlow<List<TransferItem>>(emptyList())
    val transferHistory: StateFlow<List<TransferItem>> = _transferHistory.asStateFlow()

    // Snackbar Messages
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val vaultDir: File
        get() {
            val dir = File(context.filesDir, "SafeVault")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    init {
        initializeStorageVolumes()
        seedSampleFilesIfNeeded()
        refreshCurrentDir()
        refreshStorageStats()
        refreshCategoryStats()
    }

    fun refreshAll() {
        initializeStorageVolumes()
        refreshCurrentDir()
        refreshStorageStats()
        refreshCategoryStats()
    }

    private fun initializeStorageVolumes() {
        val volumes = mutableListOf<StorageVolumeInfo>()
        val primary = Environment.getExternalStorageDirectory()
        val total = primary.totalSpace
        val free = primary.freeSpace
        volumes.add(
            StorageVolumeInfo(
                name = "Penyimpanan Internal",
                rootDir = primary,
                totalSpace = total,
                freeSpace = free,
                isPrimary = true,
                isRemovable = false
            )
        )
        // Check for secondary storage via context.getExternalFilesDirs
        try {
            context.getExternalFilesDirs(null).drop(1).forEachIndexed { index, file ->
                if (file != null) {
                    var root = file
                    var parent = file.parentFile
                    while (parent != null && parent.name != "storage") {
                        if (parent.name.equals("Android", ignoreCase = true)) {
                            root = parent.parentFile ?: file
                            break
                        }
                        parent = parent.parentFile
                    }
                    if (volumes.none { it.rootDir.absolutePath == root.absolutePath }) {
                        volumes.add(
                            StorageVolumeInfo(
                                name = "Kartu SD ${index + 1}",
                                rootDir = root,
                                totalSpace = root.totalSpace,
                                freeSpace = root.freeSpace,
                                isPrimary = false,
                                isRemovable = true
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // Check for secondary storage via /storage
        try {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.isDirectory) {
                storageDir.listFiles()?.forEach { dir ->
                    if (dir.isDirectory && dir.name != "emulated" && dir.name != "self" && !dir.name.startsWith(".")) {
                        if (volumes.none { it.rootDir.absolutePath == dir.absolutePath }) {
                            val name = "Kartu SD (${dir.name})"
                            volumes.add(
                                StorageVolumeInfo(
                                    name = name,
                                    rootDir = dir,
                                    totalSpace = dir.totalSpace,
                                    freeSpace = dir.freeSpace,
                                    isPrimary = false,
                                    isRemovable = true
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        _availableStorages.value = volumes
    }

    private fun seedSampleFilesIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val baseDir = _currentDir.value
            val docs = File(baseDir, "Documents")
            if (!docs.exists()) docs.mkdirs()
            val welcomeFile = File(docs, "Selamat Datang di LishFile.txt")
            if (!welcomeFile.exists()) {
                welcomeFile.writeText(
                    "Selamat Datang di LishFile (File Manager +)!\n\n" +
                            "Fitur Utama:\n" +
                            "- Manajemen file lengkap: Salin, Pindahkan, Ubah Nama, Hapus, ZIP & Ekstrak.\n" +
                            "- Safe Vault (Brankas Aman): Enkripsi file Anda dengan AES-256 dan PIN/Biometrik.\n" +
                            "- Transfer P2P Lokal: Berbagi file melalui Wi-Fi tanpa kuota internet.\n" +
                            "- Pembersih Duplikat: Temukan dan bersihkan file ganda dengan cepat.\n" +
                            "- Editor Gambar: Edit gambar dengan crop, rotasi, dan preset filter warna.\n"
                )
            }
        }
    }

    fun refreshCurrentDir() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = _currentDir.value
            val showHidden = preferences.showHiddenFiles.value
            val rawFiles = dir.listFiles() ?: emptyArray()

            val fileItems = rawFiles
                .filter { showHidden || (!it.isHidden && !it.name.startsWith(".")) }
                .map { file ->
                    FileItem(
                        file = file,
                        fileType = FileUtils.getFileType(file),
                        isLocked = preferences.isFolderLocked(file.absolutePath),
                        isHidden = file.isHidden || file.name.startsWith(".")
                    )
                }

            _allFilesInCurrentDir.value = fileItems
            updateNavHistoryStates()
        }
    }

    fun navigateTo(folder: File) {
        if (!folder.exists() || !folder.isDirectory) return
        backHistory.push(_currentDir.value)
        forwardHistory.clear()
        _currentDir.value = folder
        _selectedPaths.value = emptySet()
        preferences.addRecentFolder(folder.absolutePath)
        refreshCurrentDir()
    }

    fun navigateBack(): Boolean {
        if (backHistory.isNotEmpty()) {
            forwardHistory.push(_currentDir.value)
            _currentDir.value = backHistory.pop()
            _selectedPaths.value = emptySet()
            refreshCurrentDir()
            return true
        }
        return false
    }

    fun navigateForward(): Boolean {
        if (forwardHistory.isNotEmpty()) {
            backHistory.push(_currentDir.value)
            _currentDir.value = forwardHistory.pop()
            _selectedPaths.value = emptySet()
            refreshCurrentDir()
            return true
        }
        return false
    }

    fun navigateUp(): Boolean {
        val parent = _currentDir.value.parentFile ?: return false
        val activeRoot = _availableStorages.value.getOrNull(_selectedStorageIndex.value)?.rootDir
        if (activeRoot != null && _currentDir.value.absolutePath == activeRoot.absolutePath) {
            return false
        }
        navigateTo(parent)
        return true
    }

    fun navigateToRoot() {
        val root = _availableStorages.value.getOrNull(_selectedStorageIndex.value)?.rootDir
            ?: Environment.getExternalStorageDirectory()
        navigateTo(root)
    }

    fun switchStorage(index: Int) {
        val storage = _availableStorages.value.getOrNull(index) ?: return
        _selectedStorageIndex.value = index
        backHistory.clear()
        forwardHistory.clear()
        _currentDir.value = storage.rootDir
        _selectedPaths.value = emptySet()
        refreshCurrentDir()
        refreshStorageStats()
    }

    private fun updateNavHistoryStates() {
        _canNavigateBack.value = backHistory.isNotEmpty()
        _canNavigateForward.value = forwardHistory.isNotEmpty()
    }

    // Filtering & Sorting
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (_searchEntireStorage.value && (query.isNotBlank() || _extensionFilter.value != null)) {
            triggerDeepSearch()
        }
    }

    fun setExtensionFilter(ext: String?) {
        _extensionFilter.value = ext
        if (_searchEntireStorage.value && (_searchQuery.value.isNotBlank() || ext != null)) {
            triggerDeepSearch()
        }
    }

    fun setCategory(category: ViewCategory) {
        _selectedCategory.value = category
    }

    fun setSizeFilter(filter: SizeFilter) {
        _sizeFilter.value = filter
    }

    fun setSorting(sortBy: SortBy, sortOrder: SortOrder) {
        _sortBy.value = sortBy
        _sortOrder.value = sortOrder
    }

    fun setSortBy(sortBy: SortBy) {
        if (_sortBy.value == sortBy) {
            toggleSortOrder()
        } else {
            _sortBy.value = sortBy
            _sortOrder.value = if (sortBy == SortBy.SIZE || sortBy == SortBy.DATE) SortOrder.DESCENDING else SortOrder.ASCENDING
        }
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
    }

    fun showLargestFilesInCategory(category: ViewCategory) {
        _selectedCategory.value = category
        _sortBy.value = SortBy.SIZE
        _sortOrder.value = SortOrder.DESCENDING
    }

    fun toggleSearchEntireStorage() {
        val next = !_searchEntireStorage.value
        _searchEntireStorage.value = next
        if (next && (_searchQuery.value.isNotBlank() || _extensionFilter.value != null)) {
            triggerDeepSearch()
        }
    }

    fun triggerDeepSearch() {
        viewModelScope.launch(Dispatchers.IO) {
            _isDeepSearching.value = true
            val root = _availableStorages.value.getOrNull(_selectedStorageIndex.value)?.rootDir ?: _currentDir.value
            val showHidden = preferences.showHiddenFiles.value
            val rawResults = FileUtils.searchFilesRecursively(
                root = root,
                query = _searchQuery.value,
                extensionFilter = _extensionFilter.value,
                maxResults = 250,
                showHidden = showHidden
            )
            _deepSearchResults.value = rawResults.map { file ->
                FileItem(
                    file = file,
                    fileType = FileUtils.getFileType(file),
                    isLocked = preferences.isFolderLocked(file.absolutePath),
                    isHidden = file.isHidden || file.name.startsWith(".")
                )
            }
            _isDeepSearching.value = false
        }
    }

    fun openStorageVolume(storage: StorageVolumeInfo) {
        val index = _availableStorages.value.indexOfFirst { it.rootDir.absolutePath == storage.rootDir.absolutePath }
        if (index != -1) {
            _selectedStorageIndex.value = index
        }
        _selectedCategory.value = ViewCategory.ALL
        _searchQuery.value = ""
        _extensionFilter.value = null
        navigateTo(storage.rootDir)
    }

    fun openCategoryDirectory(category: ViewCategory, largestFirst: Boolean = false) {
        val root = _availableStorages.value.firstOrNull()?.rootDir ?: Environment.getExternalStorageDirectory()
        _selectedCategory.value = category
        if (largestFirst) {
            setSorting(SortBy.SIZE, SortOrder.DESCENDING)
        }
        when (category) {
            ViewCategory.DOWNLOADS -> {
                val dl = File(root, "Download")
                if (dl.exists() && dl.isDirectory) navigateTo(dl)
                else navigateTo(root)
            }
            ViewCategory.IMAGES -> {
                val dcim = File(root, "DCIM")
                val pictures = File(root, "Pictures")
                if (dcim.exists() && dcim.isDirectory) navigateTo(dcim)
                else if (pictures.exists() && pictures.isDirectory) navigateTo(pictures)
                else navigateTo(root)
            }
            ViewCategory.VIDEOS -> {
                val movies = File(root, "Movies")
                val dcim = File(root, "DCIM")
                if (movies.exists() && movies.isDirectory) navigateTo(movies)
                else if (dcim.exists() && dcim.isDirectory) navigateTo(dcim)
                else navigateTo(root)
            }
            ViewCategory.AUDIO -> {
                val music = File(root, "Music")
                if (music.exists() && music.isDirectory) navigateTo(music)
                else navigateTo(root)
            }
            ViewCategory.DOCUMENTS -> {
                val docs = File(root, "Documents")
                if (docs.exists() && docs.isDirectory) navigateTo(docs)
                else navigateTo(root)
            }
            ViewCategory.ARCHIVES -> {
                _searchEntireStorage.value = true
                _extensionFilter.value = "zip"
                triggerDeepSearch()
            }
            ViewCategory.APKS -> {
                _searchEntireStorage.value = true
                _extensionFilter.value = "apk"
                triggerDeepSearch()
            }
            else -> {
                navigateTo(root)
            }
        }
    }

    fun getFilteredAndSortedFiles(): List<FileItem> {
        val query = _searchQuery.value.trim().lowercase()
        val cat = _selectedCategory.value
        val sizeF = _sizeFilter.value
        val extFilter = _extensionFilter.value?.lowercase()?.removePrefix(".")

        var list = if (_searchEntireStorage.value && (query.isNotEmpty() || extFilter != null)) {
            _deepSearchResults.value
        } else {
            _allFilesInCurrentDir.value
        }

        if (query.isNotEmpty()) {
            list = list.filter { it.name.lowercase().contains(query) }
        }

        if (!extFilter.isNullOrEmpty()) {
            list = list.filter { it.extension.equals(extFilter, ignoreCase = true) }
        }

        if (cat != ViewCategory.ALL) {
            list = list.filter {
                when (cat) {
                    ViewCategory.IMAGES -> it.fileType == FileType.IMAGE
                    ViewCategory.AUDIO -> it.fileType == FileType.AUDIO
                    ViewCategory.VIDEOS -> it.fileType == FileType.VIDEO
                    ViewCategory.DOCUMENTS -> it.fileType == FileType.DOCUMENT
                    ViewCategory.ARCHIVES -> it.fileType == FileType.ARCHIVE
                    ViewCategory.APKS -> it.fileType == FileType.APK
                    ViewCategory.DOWNLOADS -> it.file.parent?.contains("Download", ignoreCase = true) == true
                    else -> true
                }
            }
        }

        if (sizeF != SizeFilter.ANY) {
            list = list.filter { item ->
                if (item.isDirectory) true
                else {
                    val mb = item.size / (1024 * 1024)
                    when (sizeF) {
                        SizeFilter.SMALL -> mb < 1
                        SizeFilter.MEDIUM -> mb in 1..50
                        SizeFilter.LARGE -> mb in 51..500
                        SizeFilter.HUGE -> mb > 500
                        else -> true
                    }
                }
            }
        }

        // Sorting: Folders first, then sort by criteria
        val comparator = Comparator<FileItem> { a, b ->
            if (a.isDirectory && !b.isDirectory) return@Comparator -1
            if (!a.isDirectory && b.isDirectory) return@Comparator 1
            val res = when (_sortBy.value) {
                SortBy.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                SortBy.DATE -> a.lastModified.compareTo(b.lastModified)
                SortBy.SIZE -> a.size.compareTo(b.size)
                SortBy.TYPE -> a.extension.compareTo(b.extension, ignoreCase = true)
            }
            if (_sortOrder.value == SortOrder.DESCENDING) -res else res
        }

        return list.sortedWith(comparator)
    }

    // Storage & Category Statistics
    fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val root = _currentDir.value
            val total = root.totalSpace
            val free = root.freeSpace
            val used = total - free
            val percent = if (total > 0) (used.toFloat() / total.toFloat()) else 0f
            _storageStats.value = StorageStats(
                totalBytes = total,
                freeBytes = free,
                usedBytes = used,
                usedPercent = percent
            )
        }
    }

    fun refreshCategoryStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val root = _availableStorages.value.getOrNull(_selectedStorageIndex.value)?.rootDir ?: _currentDir.value
            var imgCount = 0; var imgSize = 0L
            var audCount = 0; var audSize = 0L
            var vidCount = 0; var vidSize = 0L
            var docCount = 0; var docSize = 0L
            var arcCount = 0; var arcSize = 0L
            var apkCount = 0; var apkSize = 0L
            var dlCount = 0; var dlSize = 0L

            root.walkTopDown().maxDepth(3).forEach { file ->
                if (file.isFile) {
                    val size = file.length()
                    when (FileUtils.getFileType(file)) {
                        FileType.IMAGE -> { imgCount++; imgSize += size }
                        FileType.AUDIO -> { audCount++; audSize += size }
                        FileType.VIDEO -> { vidCount++; vidSize += size }
                        FileType.DOCUMENT -> { docCount++; docSize += size }
                        FileType.ARCHIVE -> { arcCount++; arcSize += size }
                        FileType.APK -> { apkCount++; apkSize += size }
                        else -> {}
                    }
                    if (file.parent?.contains("Download", ignoreCase = true) == true) {
                        dlCount++; dlSize += size
                    }
                }
            }

            _categoryOverviewStats.value = CategoryOverviewStats(
                imagesCount = imgCount, imagesSize = imgSize,
                audioCount = audCount, audioSize = audSize,
                videosCount = vidCount, videosSize = vidSize,
                docsCount = docCount, docsSize = docSize,
                archivesCount = arcCount, archivesSize = arcSize,
                apksCount = apkCount, apksSize = apkSize,
                downloadsCount = dlCount, downloadsSize = dlSize
            )
        }
    }

    // Multi-Select Actions
    fun toggleMultiSelect(enable: Boolean) {
        _isMultiSelectMode.value = enable
        if (!enable) _selectedPaths.value = emptySet()
    }

    fun togglePathSelection(path: String) {
        val set = _selectedPaths.value.toMutableSet()
        if (set.contains(path)) set.remove(path) else set.add(path)
        _selectedPaths.value = set
        if (set.isEmpty()) _isMultiSelectMode.value = false else _isMultiSelectMode.value = true
    }

    fun selectAll(files: List<FileItem>) {
        _selectedPaths.value = files.map { it.path }.toSet()
        _isMultiSelectMode.value = true
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
        _isMultiSelectMode.value = false
    }

    // Clipboard Actions (Copy, Cut, Paste)
    fun copySelected() {
        val files = _selectedPaths.value.map { File(it) }.filter { it.exists() }
        _clipboard.value = ClipboardItemState(files = files, isCut = false)
        clearSelection()
        notifySnackbar("${files.size} item disalin ke papan klip")
    }

    fun cutSelected() {
        val files = _selectedPaths.value.map { File(it) }.filter { it.exists() }
        _clipboard.value = ClipboardItemState(files = files, isCut = true)
        clearSelection()
        notifySnackbar("${files.size} item dipotong ke papan klip")
    }

    fun clearClipboard() {
        _clipboard.value = null
    }

    fun pasteClipboard(targetDir: File = _currentDir.value) {
        val clip = _clipboard.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            var successCount = 0
            clip.files.forEach { file ->
                val ok = if (clip.isCut) {
                    FileUtils.moveFileOrDirectory(file, targetDir)
                } else {
                    FileUtils.copyFileOrDirectory(file, targetDir)
                }
                if (ok) successCount++
            }
            _clipboard.value = null
            refreshCurrentDir()
            notifySnackbar("Berhasil menempelkan $successCount item")
        }
    }

    // File Operations
    fun createFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = FileUtils.createFolder(_currentDir.value, name)
            if (ok) {
                refreshCurrentDir()
                notifySnackbar("Folder '$name' berhasil dibuat")
            } else {
                notifySnackbar("Gagal membuat folder")
            }
        }
    }

    fun createTextFile(name: String, content: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val finalName = if (!name.contains(".")) "$name.txt" else name
            val ok = FileUtils.createTextFile(_currentDir.value, finalName, content)
            if (ok) {
                refreshCurrentDir()
                notifySnackbar("File '$finalName' berhasil dibuat")
            } else {
                notifySnackbar("Gagal membuat file")
            }
        }
    }

    fun saveTextFile(file: File, content: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = FileUtils.saveTextFile(file, content)
            if (ok) {
                notifySnackbar("File tersimpan")
                withContext(Dispatchers.Main) { onComplete() }
            } else {
                notifySnackbar("Gagal menyimpan file")
            }
        }
    }

    fun renameFile(file: File, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = FileUtils.renameFile(file, newName)
            if (ok) {
                refreshCurrentDir()
                notifySnackbar("Nama berhasil diubah menjadi '$newName'")
            } else {
                notifySnackbar("Gagal mengubah nama file")
            }
        }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = FileUtils.deleteRecursive(file)
            if (ok) {
                refreshCurrentDir()
                notifySnackbar("'${file.name}' berhasil dihapus")
            } else {
                notifySnackbar("Gagal menghapus file")
            }
        }
    }

    fun deleteSelected() {
        viewModelScope.launch(Dispatchers.IO) {
            val paths = _selectedPaths.value
            var count = 0
            paths.forEach { path ->
                val f = File(path)
                if (FileUtils.deleteRecursive(f)) count++
            }
            clearSelection()
            refreshCurrentDir()
            notifySnackbar("Berhasil menghapus $count item")
        }
    }

    fun moveFile(file: File, destinationFolder: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val target = File(destinationFolder, file.name)
            if (target.exists()) {
                notifySnackbar("File '${file.name}' sudah ada di folder tujuan!")
                return@launch
            }
            if (file.renameTo(target)) {
                notifySnackbar("Berhasil memindahkan '${file.name}'")
                refreshCurrentDir()
            } else {
                try {
                    file.copyRecursively(target, overwrite = true)
                    file.deleteRecursively()
                    notifySnackbar("Berhasil memindahkan '${file.name}'")
                    refreshCurrentDir()
                } catch (e: Exception) {
                    notifySnackbar("Gagal memindahkan: ${e.message}")
                }
            }
        }
    }

    fun clearRecentFolders() {
        preferences.clearRecentFolders()
    }

    fun compressToZip(files: List<File>, zipName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalName = if (zipName.endsWith(".zip", ignoreCase = true)) zipName else "$zipName.zip"
            val targetZip = File(_currentDir.value, finalName)
            val ok = FileUtils.createZipArchive(files, targetZip)
            if (ok) {
                refreshCurrentDir()
                notifySnackbar("Arsip '$finalName' berhasil dibuat")
            } else {
                notifySnackbar("Gagal membuat arsip ZIP")
            }
        }
    }

    fun extractZip(zipFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val folderName = zipFile.nameWithoutExtension
            val targetDir = File(_currentDir.value, folderName)
            val ok = FileUtils.extractZipArchive(zipFile, targetDir)
            if (ok) {
                refreshCurrentDir()
                notifySnackbar("Ekstraksi '$folderName' berhasil")
            } else {
                notifySnackbar("Gagal mengekstrak ZIP")
            }
        }
    }

    fun moveFileTo(source: File, targetDir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = FileUtils.moveFileOrDirectory(source, targetDir)
            if (ok) {
                refreshCurrentDir()
                notifySnackbar("Berhasil memindahkan '${source.name}' ke '${targetDir.name}'")
            } else {
                notifySnackbar("Gagal memindahkan '${source.name}'")
            }
        }
    }

    fun moveSelectedTo(targetDir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val paths = _selectedPaths.value.toList()
            var movedCount = 0
            paths.forEach { path ->
                val f = File(path)
                if (f.exists() && FileUtils.moveFileOrDirectory(f, targetDir)) {
                    movedCount++
                }
            }
            clearSelection()
            refreshCurrentDir()
            notifySnackbar("Berhasil memindahkan $movedCount file ke '${targetDir.name}'")
        }
    }

    fun loadZipPreview(zipFile: File, onLoaded: (List<com.example.model.ZipEntryItem>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = FileUtils.listZipEntries(zipFile)
            withContext(Dispatchers.Main) {
                onLoaded(entries)
            }
        }
    }

    // Duplicate File Scanner (Supports Current Folder, Multiple Custom Folders, or Entire Storage)
    fun scanDuplicates(targetFolders: List<File> = listOf(_currentDir.value), scopeDescription: String = "Folder Terpilih") {
        viewModelScope.launch(Dispatchers.IO) {
            _duplicateResult.value = DuplicateScanResult(isScanning = true, scopeDescription = scopeDescription)
            val fileMap = mutableMapOf<Long, MutableList<File>>()

            // Step 1: Group by file size across all designated target folders
            targetFolders.forEach { folder ->
                if (folder.exists() && folder.isDirectory) {
                    folder.walkTopDown().maxDepth(5).forEach { file ->
                        if (file.isFile && file.length() > 0) {
                            val size = file.length()
                            fileMap.getOrPut(size) { mutableListOf() }.add(file)
                        }
                    }
                }
            }

            // Step 2: Compute hash for files with identical size
            val potentialDuplicates = fileMap.filter { it.value.size > 1 }
            val groups = mutableListOf<DuplicateGroup>()
            var totalWasted = 0L

            potentialDuplicates.forEach { (size, files) ->
                val hashMap = mutableMapOf<String, MutableList<File>>()
                files.forEach { file ->
                    val hash = AesCryptoEngine.calculateChecksum(file, "MD5")
                    if (hash.isNotEmpty()) {
                        hashMap.getOrPut(hash) { mutableListOf() }.add(file)
                    }
                }
                hashMap.filter { it.value.size > 1 }.forEach { (hash, dupList) ->
                    val items = dupList.mapIndexed { index, file ->
                        DuplicateFileItem(file = file, isSelectedForDelete = index > 0)
                    }
                    groups.add(DuplicateGroup(checksum = hash, fileSize = size, files = items))
                    totalWasted += size * (dupList.size - 1)
                }
            }

            _duplicateResult.value = DuplicateScanResult(
                duplicateGroups = groups,
                totalWastedBytes = totalWasted,
                isScanning = false,
                scopeDescription = scopeDescription
            )
        }
    }

    fun toggleDuplicateSelection(checksum: String, filePath: String) {
        val current = _duplicateResult.value
        val updatedGroups = current.duplicateGroups.map { group ->
            if (group.checksum == checksum) {
                val updatedFiles = group.files.map { item ->
                    if (item.file.absolutePath == filePath) {
                        item.copy(isSelectedForDelete = !item.isSelectedForDelete)
                    } else item
                }
                group.copy(files = updatedFiles)
            } else group
        }
        _duplicateResult.value = current.copy(duplicateGroups = updatedGroups)
    }

    fun deleteSelectedDuplicates() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _duplicateResult.value
            var deletedCount = 0
            current.duplicateGroups.forEach { group ->
                group.files.filter { it.isSelectedForDelete }.forEach { item ->
                    if (item.file.delete()) deletedCount++
                }
            }
            _duplicateResult.value = DuplicateScanResult()
            refreshCurrentDir()
            notifySnackbar("Berhasil membersihkan $deletedCount file duplikat")
        }
    }

    // Safe Vault
    fun unlockVaultWithPin(pin: String): Boolean {
        if (preferences.verifyMasterPin(pin)) {
            _isVaultUnlocked.value = true
            loadVaultFiles()
            return true
        }
        return false
    }

    fun unlockVaultWithBiometrics() {
        _isVaultUnlocked.value = true
        loadVaultFiles()
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        _vaultFiles.value = emptyList()
    }

    fun setupVaultPin(newPin: String) {
        preferences.setMasterPin(newPin)
        _isVaultUnlocked.value = true
        loadVaultFiles()
        notifySnackbar("PIN Brankas Aman berhasil dibuat")
    }

    fun loadVaultFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = vaultDir.listFiles() ?: emptyArray()
            _vaultFiles.value = list.map { file ->
                FileItem(
                    file = file,
                    fileType = FileUtils.getFileType(file),
                    isLocked = true
                )
            }
        }
    }

    fun moveToSafeVault(file: File, pin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val encryptedTarget = File(vaultDir, "${file.name}.lish")
            val ok = AesCryptoEngine.encryptFile(file, encryptedTarget, pin)
            if (ok) {
                file.delete()
                refreshCurrentDir()
                loadVaultFiles()
                notifySnackbar("'${file.name}' berhasil diamankan ke Brankas")
            } else {
                notifySnackbar("Gagal mengenkripsi file")
            }
        }
    }

    fun restoreFromSafeVault(encryptedFile: File, pin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val originalName = encryptedFile.name.removeSuffix(".lish")
            val target = File(_currentDir.value, originalName)
            val ok = AesCryptoEngine.decryptFile(encryptedFile, target, pin)
            if (ok) {
                encryptedFile.delete()
                refreshCurrentDir()
                loadVaultFiles()
                notifySnackbar("File '$originalName' berhasil dipulihkan")
            } else {
                notifySnackbar("PIN salah atau gagal mendekripsi file")
            }
        }
    }

    // Local P2P Wi-Fi Transfer
    fun startP2pServer(filesToShare: List<File>) {
        viewModelScope.launch {
            transferServer.setFiles(filesToShare)
            val ok = transferServer.startServer(context)
            if (ok) {
                notifySnackbar("Server P2P aktif. Bagikan alamat IP ke rekan Anda.")
            } else {
                notifySnackbar("Gagal mengaktifkan Server P2P")
            }
        }
    }

    fun stopP2pServer() {
        transferServer.stopServer()
        notifySnackbar("Server P2P dimatikan")
    }

    fun connectToRemotePeer(host: String, port: Int = 8080) {
        viewModelScope.launch(Dispatchers.IO) {
            val files = transferClient.fetchRemoteFileList(host, port)
            _remotePeerFiles.value = files
            if (files.isNotEmpty()) {
                notifySnackbar("Terhubung ke $host ($files item tersedia)")
            } else {
                notifySnackbar("Tidak dapat terhubung ke $host:$port")
            }
        }
    }

    fun downloadRemotePeerFile(host: String, port: Int = 8080, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetDir = File(_currentDir.value, "LishTransfer")
            val ok = transferClient.downloadRemoteFile(host, port, fileName, targetDir)
            if (ok) {
                val newHistory = _transferHistory.value.toMutableList()
                newHistory.add(
                    TransferItem(
                        id = System.currentTimeMillis().toString(),
                        fileName = fileName,
                        fileSize = 0L,
                        isDownload = true,
                        peerAddress = "$host:$port"
                    )
                )
                _transferHistory.value = newHistory
                refreshCurrentDir()
                notifySnackbar("Berhasil mengunduh '$fileName'")
            } else {
                notifySnackbar("Gagal mengunduh '$fileName'")
            }
        }
    }

    fun notifySnackbar(message: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(message)
        }
    }
}
