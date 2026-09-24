package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.FileItem
import com.example.model.FileType
import com.example.model.SizeFilter
import com.example.model.SortBy
import com.example.model.SortOrder
import com.example.model.ViewCategory
import com.example.ui.components.AudioPlayerDialog
import com.example.ui.components.CategoryDashboard
import com.example.ui.components.CompressZipDialog
import com.example.ui.components.ConciseCategoryHomeView
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.DuplicateScannerDialog
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.FileGridItem
import com.example.ui.components.FileListItem
import com.example.ui.components.ImageEditorDialog
import com.example.ui.components.ImageViewerDialog
import com.example.ui.components.MoveDestinationDialog
import com.example.ui.components.PdfReaderDialog
import com.example.ui.components.PinInputDialog
import com.example.ui.components.RecentFoldersDialog
import com.example.ui.components.RenameFileDialog
import com.example.ui.components.StorageHeader
import com.example.ui.components.TextEditorDialog
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.components.ZipPreviewDialog
import com.example.util.FileUtils
import com.example.util.StoragePermissionHelper
import com.example.viewmodel.FileManagerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDir by viewModel.currentDir.collectAsState()
    val availableStorages by viewModel.availableStorages.collectAsState()
    val selectedStorageIndex by viewModel.selectedStorageIndex.collectAsState()
    val canNavigateBack by viewModel.canNavigateBack.collectAsState()
    val canNavigateForward by viewModel.canNavigateForward.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val categoryStats by viewModel.categoryOverviewStats.collectAsState()
    val isGridView by viewModel.preferences.isGridView.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sizeFilter by viewModel.sizeFilter.collectAsState()
    val duplicateResult by viewModel.duplicateResult.collectAsState()
    val recentFolders by viewModel.recentFolders.collectAsState()
    val isDeepSearching by viewModel.isDeepSearching.collectAsState()
    val isScanningCategories by viewModel.isScanningCategories.collectAsState()
    val searchEntireStorage by viewModel.searchEntireStorage.collectAsState()
    val extensionFilter by viewModel.extensionFilter.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val files = viewModel.getFilteredAndSortedFiles()

    // Dialogs state
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateTextFileDialog by remember { mutableStateOf(false) }
    var editingTextFile by remember { mutableStateOf<File?>(null) }
    var editingTextContent by remember { mutableStateOf("") }
    var renameTargetFile by remember { mutableStateOf<File?>(null) }
    var detailsTargetItem by remember { mutableStateOf<FileItem?>(null) }
    var zipTargetFiles by remember { mutableStateOf<List<File>?>(null) }
    var showDuplicateScanner by remember { mutableStateOf(false) }
    var editingImageFile by remember { mutableStateOf<File?>(null) }
    var viewingImageFile by remember { mutableStateOf<File?>(null) }
    var playingAudioFile by remember { mutableStateOf<File?>(null) }
    var playingVideoFile by remember { mutableStateOf<File?>(null) }
    var viewingPdfFile by remember { mutableStateOf<File?>(null) }
    var previewingZipFile by remember { mutableStateOf<File?>(null) }
    var movingTargetFile by remember { mutableStateOf<File?>(null) }
    var showRecentFoldersDialog by remember { mutableStateOf(false) }
    var vaultLockTargetFile by remember { mutableStateOf<File?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }

    var isBrowsingFolder by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching = searchQuery.isNotEmpty() || extensionFilter != null || searchEntireStorage
    val shouldShowExplorer = isBrowsingFolder || isSearching || selectedCategory != ViewCategory.ALL
    val hasStoragePermission = StoragePermissionHelper.hasStoragePermission(context)

    BackHandler(enabled = (isBrowsingFolder || selectedCategory != ViewCategory.ALL) && !isSearching) {
        if (selectedCategory != ViewCategory.ALL) {
            viewModel.setCategory(ViewCategory.ALL)
            isBrowsingFolder = false
        } else if (canNavigateBack) {
            viewModel.navigateBack()
        } else {
            isBrowsingFolder = false
            viewModel.setCategory(ViewCategory.ALL)
        }
    }

    val activeStorageRoot = availableStorages.getOrNull(selectedStorageIndex)?.rootDir
    val isAtRoot = activeStorageRoot != null && currentDir.absolutePath == activeStorageRoot.absolutePath

    Box(modifier = modifier.fillMaxSize()) {
        if (!shouldShowExplorer) {
            ConciseCategoryHomeView(
                hasStoragePermission = hasStoragePermission,
                onRequestPermission = { StoragePermissionHelper.requestStoragePermission(context) },
                storages = availableStorages,
                categoryStats = categoryStats,
                recentFolders = recentFolders,
                onOpenStorage = { storage ->
                    viewModel.openStorageVolume(storage)
                    isBrowsingFolder = true
                },
                onOpenCategory = { cat ->
                    viewModel.openCategoryDirectory(cat)
                    isBrowsingFolder = true
                },
                onOpenDuplicates = {
                    showDuplicateScanner = true
                    viewModel.scanDuplicates()
                },
                onOpenRecentFolder = { folder ->
                    viewModel.navigateTo(folder)
                    isBrowsingFolder = true
                },
                onShowAllRecentFolders = { showRecentFoldersDialog = true },
                onSdCardNotAvailable = {
                    viewModel.notifySnackbar("Kartu SD atau USB OTG tidak terpasang di perangkat")
                }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (selectedCategory != ViewCategory.ALL) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.setCategory(ViewCategory.ALL)
                                    isBrowsingFolder = false
                                    viewModel.setSearchQuery("")
                                    viewModel.setExtensionFilter(null)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali ke Beranda"
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val title = when (selectedCategory) {
                                    ViewCategory.IMAGES -> "Semua Gambar"
                                    ViewCategory.AUDIO -> "Semua Musik & Audio"
                                    ViewCategory.VIDEOS -> "Semua Video"
                                    ViewCategory.DOCUMENTS -> "Semua Dokumen"
                                    ViewCategory.ARCHIVES -> "Semua Arsip Zip/Rar"
                                    ViewCategory.APKS -> "Semua File APK"
                                    ViewCategory.DOWNLOADS -> "Semua Unduhan"
                                    else -> "Semua File"
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isScanningCategories) "Memindai file perangkat..." else "${files.size} file di seluruh penyimpanan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isScanningCategories) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            IconButton(onClick = { viewModel.refreshCategoryStats() }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Muat Ulang Kategori",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    // 1. Unified Breadcrumb & Navigation Header
                    BreadcrumbHeader(
                        currentDir = currentDir,
                        isAtRoot = isAtRoot,
                        canNavigateBack = canNavigateBack,
                        canNavigateForward = canNavigateForward,
                        onNavigateBack = { viewModel.navigateBack() },
                        onNavigateForward = { viewModel.navigateForward() },
                        onNavigateUp = {
                            if (isAtRoot) {
                                isBrowsingFolder = false
                                viewModel.setCategory(ViewCategory.ALL)
                            } else {
                                viewModel.navigateUp()
                            }
                        },
                        onBackToCategories = {
                            isBrowsingFolder = false
                            viewModel.setCategory(ViewCategory.ALL)
                            viewModel.setSearchQuery("")
                            viewModel.setExtensionFilter(null)
                        },
                        onNavigateToPath = { viewModel.navigateTo(it) },
                        onShowRecentFolders = { showRecentFoldersDialog = true }
                    )
                }

                // 2. Storage Overview bar (only if multi-storage available and not in category view)
                if (availableStorages.size > 1 && selectedCategory == ViewCategory.ALL) {
                    StorageHeader(
                        storages = availableStorages,
                        selectedStorageIndex = selectedStorageIndex,
                        onSelectStorage = { viewModel.switchStorage(it) },
                        storageStats = storageStats
                    )
                }

                // 3. Quick filter chips row (if category active or size filter active)
                if (selectedCategory != ViewCategory.ALL || sizeFilter != SizeFilter.ANY) {
                    FilterChipsRow(
                        selectedCategory = selectedCategory,
                        onSelectCategory = { viewModel.setCategory(it) },
                        sizeFilter = sizeFilter,
                        onSelectSizeFilter = { viewModel.setSizeFilter(it) }
                    )
                }

            // Deep Search Status Banner
            if (isDeepSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mencari di seluruh penyimpanan...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (searchEntireStorage && files.isNotEmpty()) {
                Text(
                    text = "Ditemukan ${files.size} file di seluruh penyimpanan",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            // 5. Clipboard Banner (if files copied/cut)
            clipboard?.let { clip ->
                ClipboardBanner(
                    itemCount = clip.files.size,
                    isCut = clip.isCut,
                    onPaste = { viewModel.pasteClipboard() },
                    onCancel = { viewModel.clearClipboard() }
                )
            }

            // 6. Files List / Grid View
            if (files.isEmpty() && !isDeepSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Search else Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isSearching) "Tidak Ada Hasil Pencarian" else "Folder Ini Kosong",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isSearching) "Coba kata kunci lain atau periksa filter ekstensi"
                            else "Belum ada berkas atau sub-folder di direktori ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isSearching) {
                            Button(
                                onClick = {
                                    viewModel.setSearchQuery("")
                                    viewModel.setExtensionFilter(null)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("Bersihkan Pencarian", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showCreateFolderDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buat Folder Baru", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 105.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(files, key = { it.path }) { item ->
                            FileGridItem(
                                item = item,
                                isSelected = selectedPaths.contains(item.path),
                                isMultiSelectMode = isMultiSelectMode,
                                onClick = {
                                    handleFileClick(
                                        context = context,
                                        item = item,
                                        coroutineScope = coroutineScope,
                                        viewModel = viewModel,
                                        onViewImage = { viewingImageFile = it },
                                        onPlayAudio = { playingAudioFile = it },
                                        onPlayVideo = { playingVideoFile = it },
                                        onViewPdf = { viewingPdfFile = it },
                                        onPreviewZip = { previewingZipFile = it },
                                        onEditText = { f, content ->
                                            editingTextFile = f
                                            editingTextContent = content
                                        },
                                        onEditImage = { f -> editingImageFile = f }
                                    )
                                },
                                onLongClick = { viewModel.togglePathSelection(item.path) },
                                onToggleSelect = { viewModel.togglePathSelection(item.path) },
                                onRename = { renameTargetFile = item.file },
                                onDelete = { viewModel.deleteFile(item.file) },
                                onCopy = {
                                    viewModel.togglePathSelection(item.path)
                                    viewModel.copySelected()
                                },
                                onCut = {
                                    viewModel.togglePathSelection(item.path)
                                    viewModel.cutSelected()
                                },
                                onDetails = { detailsTargetItem = item },
                                onZip = { zipTargetFiles = listOf(item.file) },
                                onExtractZip = { viewModel.extractZip(item.file) },
                                onMoveToVault = { vaultLockTargetFile = item.file },
                                onEditImage = { editingImageFile = item.file },
                                onShare = { FileUtils.shareFile(context, item.file) },
                                onMove = { movingTargetFile = item.file },
                                onPreviewZip = { previewingZipFile = item.file },
                                onOpenFileLocation = {
                                    item.file.parentFile?.let { p ->
                                        viewModel.setCategory(ViewCategory.ALL)
                                        viewModel.navigateTo(p)
                                        isBrowsingFolder = true
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(files, key = { it.path }) { item ->
                            FileListItem(
                                item = item,
                                isSelected = selectedPaths.contains(item.path),
                                isMultiSelectMode = isMultiSelectMode,
                                onClick = {
                                    handleFileClick(
                                        context = context,
                                        item = item,
                                        coroutineScope = coroutineScope,
                                        viewModel = viewModel,
                                        onViewImage = { viewingImageFile = it },
                                        onPlayAudio = { playingAudioFile = it },
                                        onPlayVideo = { playingVideoFile = it },
                                        onViewPdf = { viewingPdfFile = it },
                                        onPreviewZip = { previewingZipFile = it },
                                        onEditText = { f, content ->
                                            editingTextFile = f
                                            editingTextContent = content
                                        },
                                        onEditImage = { f -> editingImageFile = f }
                                    )
                                },
                                onLongClick = { viewModel.togglePathSelection(item.path) },
                                onToggleSelect = { viewModel.togglePathSelection(item.path) },
                                onRename = { renameTargetFile = item.file },
                                onDelete = { viewModel.deleteFile(item.file) },
                                onCopy = {
                                    viewModel.togglePathSelection(item.path)
                                    viewModel.copySelected()
                                },
                                onCut = {
                                    viewModel.togglePathSelection(item.path)
                                    viewModel.cutSelected()
                                },
                                onDetails = { detailsTargetItem = item },
                                onZip = { zipTargetFiles = listOf(item.file) },
                                onExtractZip = { viewModel.extractZip(item.file) },
                                onMoveToVault = { vaultLockTargetFile = item.file },
                                onEditImage = { editingImageFile = item.file },
                                onShare = { FileUtils.shareFile(context, item.file) },
                                onMove = { movingTargetFile = item.file },
                                onPreviewZip = { previewingZipFile = item.file },
                                onOpenFileLocation = {
                                    item.file.parentFile?.let { p ->
                                        viewModel.setCategory(ViewCategory.ALL)
                                        viewModel.navigateTo(p)
                                        isBrowsingFolder = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
            }
        }

        // Floating Action Button with quick actions
        if (shouldShowExplorer) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                AnimatedVisibility(visible = showFabMenu) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                showDuplicateScanner = true
                                viewModel.scanDuplicates()
                            },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "Scan Duplikat")
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                showCreateFolderDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Folder Baru")
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                showCreateTextFileDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "File Teks Baru")
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("main_fab")
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Clear else Icons.Default.Add,
                        contentDescription = "Menu Tambah"
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = {
                viewModel.createFolder(it)
                showCreateFolderDialog = false
            }
        )
    }

    if (showCreateTextFileDialog) {
        TextEditorDialog(
            fileName = "Dokumen Baru.txt",
            initialContent = "",
            isNewFile = true,
            onDismiss = { showCreateTextFileDialog = false },
            onSave = { name, content ->
                viewModel.createTextFile(name, content)
                showCreateTextFileDialog = false
            }
        )
    }

    editingTextFile?.let { file ->
        TextEditorDialog(
            fileName = file.name,
            initialContent = editingTextContent,
            isNewFile = false,
            onDismiss = { editingTextFile = null },
            onSave = { _, content ->
                viewModel.saveTextFile(file, content) {
                    editingTextFile = null
                }
            }
        )
    }

    renameTargetFile?.let { file ->
        RenameFileDialog(
            currentName = file.name,
            onDismiss = { renameTargetFile = null },
            onConfirm = {
                viewModel.renameFile(file, it)
                renameTargetFile = null
            }
        )
    }

    detailsTargetItem?.let { item ->
        FileDetailsDialog(
            item = item,
            onDismiss = { detailsTargetItem = null }
        )
    }

    zipTargetFiles?.let { filesList ->
        val defaultName = if (filesList.size == 1) "${filesList[0].nameWithoutExtension}.zip" else "Arsip.zip"
        CompressZipDialog(
            defaultZipName = defaultName,
            onDismiss = { zipTargetFiles = null },
            onConfirm = {
                viewModel.compressToZip(filesList, it)
                zipTargetFiles = null
            }
        )
    }

    if (showDuplicateScanner) {
        val rootDir = availableStorages.firstOrNull()?.rootDir ?: currentDir
        DuplicateScannerDialog(
            currentDir = currentDir,
            storageRoot = rootDir,
            result = duplicateResult,
            onStartScan = { folders, desc -> viewModel.scanDuplicates(folders, desc) },
            onToggleSelect = { checksum, path -> viewModel.toggleDuplicateSelection(checksum, path) },
            onCleanDuplicates = {
                viewModel.deleteSelectedDuplicates()
                showDuplicateScanner = false
            },
            onDismiss = { showDuplicateScanner = false }
        )
    }

    viewingImageFile?.let { imgFile ->
        ImageViewerDialog(
            file = imgFile,
            onDismiss = { viewingImageFile = null },
            onEditImage = {
                viewingImageFile = null
                editingImageFile = imgFile
            }
        )
    }

    playingAudioFile?.let { audioFile ->
        AudioPlayerDialog(
            file = audioFile,
            onDismiss = { playingAudioFile = null }
        )
    }

    playingVideoFile?.let { videoFile ->
        VideoPlayerDialog(
            file = videoFile,
            onDismiss = { playingVideoFile = null }
        )
    }

    viewingPdfFile?.let { pdfFile ->
        PdfReaderDialog(
            file = pdfFile,
            onDismiss = { viewingPdfFile = null }
        )
    }

    previewingZipFile?.let { zipFile ->
        ZipPreviewDialog(
            file = zipFile,
            onDismiss = { previewingZipFile = null },
            onExtract = {
                viewModel.extractZip(zipFile)
                previewingZipFile = null
            }
        )
    }

    movingTargetFile?.let { fileToMove ->
        val rootDir = availableStorages.firstOrNull()?.rootDir ?: currentDir
        MoveDestinationDialog(
            initialDirectory = currentDir,
            rootStorage = rootDir,
            title = "Pindahkan '${fileToMove.name}'",
            onDismiss = { movingTargetFile = null },
            onSelectDestination = { destDir ->
                viewModel.moveFile(fileToMove, destDir)
                movingTargetFile = null
            }
        )
    }

    if (showRecentFoldersDialog) {
        RecentFoldersDialog(
            recentPaths = recentFolders,
            onDismiss = { showRecentFoldersDialog = false },
            onClearHistory = { viewModel.clearRecentFolders() },
            onSelectFolder = { folder ->
                viewModel.navigateTo(folder)
                showRecentFoldersDialog = false
            }
        )
    }

    editingImageFile?.let { imgFile ->
        ImageEditorDialog(
            imageFile = imgFile,
            onDismiss = { editingImageFile = null },
            onSaved = {
                editingImageFile = null
                viewModel.refreshCurrentDir()
                viewModel.notifySnackbar("Gambar berhasil disimpan!")
            }
        )
    }

    vaultLockTargetFile?.let { file ->
        PinInputDialog(
            title = "Kunci '${file.name}' ke Brankas",
            subtitle = "Masukkan PIN untuk mengenkripsi file ini dengan AES-256",
            confirmText = "Enkripsi & Kunci",
            onDismiss = { vaultLockTargetFile = null },
            onConfirm = { pin ->
                viewModel.moveToSafeVault(file, pin)
                vaultLockTargetFile = null
            }
        )
    }
}

private fun handleFileClick(
    context: Context,
    item: FileItem,
    coroutineScope: CoroutineScope,
    viewModel: FileManagerViewModel,
    onViewImage: (File) -> Unit,
    onPlayAudio: (File) -> Unit,
    onPlayVideo: (File) -> Unit,
    onViewPdf: (File) -> Unit,
    onPreviewZip: (File) -> Unit,
    onEditText: (File, String) -> Unit,
    onEditImage: (File) -> Unit
) {
    if (item.isDirectory) {
        viewModel.navigateTo(item.file)
    } else {
        when {
            item.extension.equals("pdf", ignoreCase = true) -> {
                onViewPdf(item.file)
            }
            item.fileType == FileType.IMAGE -> {
                onViewImage(item.file)
            }
            item.fileType == FileType.AUDIO -> {
                onPlayAudio(item.file)
            }
            item.fileType == FileType.VIDEO -> {
                onPlayVideo(item.file)
            }
            item.fileType == FileType.ARCHIVE || item.extension.equals("zip", ignoreCase = true) -> {
                onPreviewZip(item.file)
            }
            item.fileType == FileType.DOCUMENT || item.fileType == FileType.CODE -> {
                if (item.extension.lowercase() in listOf("txt", "log", "json", "xml", "csv", "kt", "java", "py", "md", "html", "js", "css", "ini", "properties")) {
                    coroutineScope.launch {
                        val content = FileUtils.readTextFile(item.file)
                        onEditText(item.file, content)
                    }
                } else {
                    openWithExternalApp(context, item.file)
                }
            }
            else -> {
                openWithExternalApp(context, item.file)
            }
        }
    }
}

private fun openWithExternalApp(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "com.aistudio.lishfile.kpmv.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, FileUtils.getMimeType(file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Buka dengan"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun BreadcrumbHeader(
    currentDir: File,
    isAtRoot: Boolean,
    canNavigateBack: Boolean,
    canNavigateForward: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onNavigateUp: () -> Unit,
    onBackToCategories: () -> Unit,
    onNavigateToPath: (File) -> Unit,
    onShowRecentFolders: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Row 1: Back to Categories pill + navigation arrows + history
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onBackToCategories() }
                        .testTag("back_to_categories_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Kategori",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Kategori",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onNavigateUp,
                    enabled = !isAtRoot,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Ke Atas",
                        modifier = Modifier.size(18.dp),
                        tint = if (!isAtRoot) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }

                IconButton(
                    onClick = onNavigateBack,
                    enabled = canNavigateBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        modifier = Modifier.size(18.dp),
                        tint = if (canNavigateBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }

                IconButton(
                    onClick = onNavigateForward,
                    enabled = canNavigateForward,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Maju",
                        modifier = Modifier.size(18.dp),
                        tint = if (canNavigateForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onShowRecentFolders,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Folder Terakhir",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Breadcrumb path
            val pathSegments = getPathSegments(currentDir)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(14.dp)
                    )
                }

                pathSegments.forEachIndexed { index, (name, file) ->
                    val isCurrent = index == pathSegments.lastIndex
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onNavigateToPath(file) }
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    if (index < pathSegments.lastIndex) {
                        Text(
                            text = "›",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getPathSegments(dir: File): List<Pair<String, File>> {
    val segments = mutableListOf<Pair<String, File>>()
    var curr: File? = dir
    while (curr != null) {
        val name = if (curr.parent == null || curr.name.isEmpty()) "Root" else curr.name
        segments.add(0, Pair(name, curr))
        curr = curr.parentFile
    }
    return segments
}

@Composable
fun FilterChipsRow(
    selectedCategory: ViewCategory,
    onSelectCategory: (ViewCategory) -> Unit,
    sizeFilter: SizeFilter,
    onSelectSizeFilter: (SizeFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ViewCategory.values().forEach { cat ->
            FilterChip(
                selected = selectedCategory == cat,
                onClick = { onSelectCategory(cat) },
                label = {
                    Text(
                        text = when (cat) {
                            ViewCategory.ALL -> "Semua"
                            ViewCategory.IMAGES -> "Gambar"
                            ViewCategory.AUDIO -> "Audio"
                            ViewCategory.VIDEOS -> "Video"
                            ViewCategory.DOCUMENTS -> "Dokumen"
                            ViewCategory.ARCHIVES -> "Arsip"
                            ViewCategory.APKS -> "APK"
                            ViewCategory.DOWNLOADS -> "Unduhan"
                            else -> "Semua"
                        },
                        fontSize = 11.sp
                    )
                }
            )
        }
    }
}

@Composable
fun ClipboardBanner(
    itemCount: Int,
    isCut: Boolean,
    onPaste: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$itemCount item ${if (isCut) "dipotong" else "disalin"}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
            Row {
                Button(onClick = onPaste, modifier = Modifier.height(34.dp)) {
                    Text("Tempel di Sini", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(onClick = onCancel, modifier = Modifier.height(34.dp)) {
                    Text("Batal", fontSize = 12.sp)
                }
            }
        }
    }
}
