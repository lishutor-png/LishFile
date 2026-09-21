package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.model.FileItem
import com.example.model.FileType
import com.example.ui.components.CategoryDashboard
import com.example.ui.components.ChecksumDialog
import com.example.ui.components.CloudServicesDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CreateZipDialog
import com.example.ui.components.DashboardCategoryType
import com.example.ui.components.DecryptDialog
import com.example.ui.components.DestinationPickerDialog
import com.example.ui.components.DuplicateScannerDialog
import com.example.ui.components.EncryptDialog
import com.example.ui.components.FileActionBottomSheet
import com.example.ui.components.FileGridItem
import com.example.ui.components.FileListItem
import com.example.ui.components.FileManagerDrawerSheet
import com.example.ui.components.FilePreviewDialog
import com.example.ui.components.ImageEditorDialog
import com.example.ui.components.LishFileTopBar
import com.example.ui.components.LockFolderDialog
import com.example.ui.components.NetworkAccessDialog
import com.example.ui.components.NewItemDialog
import com.example.ui.components.PinAuthDialog
import com.example.ui.components.PremiumVipDialog
import com.example.ui.components.RemoteConnectionsDialog
import com.example.ui.components.RenameDialog
import com.example.ui.components.StorageHeader
import com.example.ui.theme.AppThemeMode
import com.example.util.FileUtils
import com.example.viewmodel.FileManagerViewModel
import java.io.File

@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel,
    themeMode: AppThemeMode,
    onThemeToggle: () -> Unit,
    onOpenVault: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val currentDir by viewModel.currentDir.collectAsState()
    val displayedFiles by viewModel.displayedFiles.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val showHiddenFiles by viewModel.preferences.showHiddenFiles.collectAsState()
    val isGridView by viewModel.preferences.isGridView.collectAsState()
    val sortBy by viewModel.preferences.sortBy.collectAsState()
    val sortOrder by viewModel.preferences.sortOrder.collectAsState()
    val operationProgress by viewModel.operationProgress.collectAsState()

    // Advanced Navigation & Storage State
    val canNavigateBack by viewModel.canNavigateBack.collectAsState()
    val canNavigateForward by viewModel.canNavigateForward.collectAsState()
    val recentFolders by viewModel.recentFolders.collectAsState()
    val availableStorages by viewModel.availableStorages.collectAsState()
    val activeStorageRoot = viewModel.activeStorageRoot
    val isLargestFilesActive by viewModel.isLargestFilesActive.collectAsState()

    // Multi-select & Clipboard State
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()

    // Advanced Search Filters
    val searchExtension by viewModel.searchExtension.collectAsState()
    val searchSizeFilter by viewModel.searchSizeFilter.collectAsState()
    val searchEntireStorage by viewModel.searchEntireStorage.collectAsState()

    // Duplicate Scanner State
    val duplicateResult by viewModel.duplicateResult.collectAsState()
    var showDuplicateScanner by remember { mutableStateOf(false) }

    // Dialog & Sheet States
    var selectedFileForAction by remember { mutableStateOf<FileItem?>(null) }
    var fileToEncrypt by remember { mutableStateOf<File?>(null) }
    var fileToDecrypt by remember { mutableStateOf<File?>(null) }
    var folderToLock by remember { mutableStateOf<File?>(null) }
    var folderToUnlock by remember { mutableStateOf<File?>(null) }
    var fileToPreview by remember { mutableStateOf<File?>(null) }
    var fileToEditImage by remember { mutableStateOf<File?>(null) }
    var fileForChecksum by remember { mutableStateOf<File?>(null) }
    var fileToCopyOrMove by remember { mutableStateOf<Pair<File, Boolean>?>(null) } // (file, isCopy)
    var showNewItemDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    // Dashboard State & Dialogs
    val isHomeDashboard by viewModel.isHomeDashboard.collectAsState()
    val categoryOverviewStats by viewModel.categoryOverviewStats.collectAsState()

    var showCloudDialog by remember { mutableStateOf(false) }
    var showRemoteDialog by remember { mutableStateOf(false) }
    var showNetworkAccessDialog by remember { mutableStateOf(false) }
    var showVipDialog by remember { mutableStateOf(false) }
    var showDrawerSheet by remember { mutableStateOf(false) }

    // Zip, Rename, Delete Dialog States
    var fileToZip by remember { mutableStateOf<File?>(null) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var fileToDeletePermanent by remember { mutableStateOf<File?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    // File Picker for importing documents
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFileFromUri(it) }
    }

    val isRoot = currentDir.absolutePath == activeStorageRoot.absolutePath

    Scaffold(
        topBar = {
            LishFileTopBar(
                currentDir = currentDir,
                isRoot = isRoot,
                onNavigateUp = { viewModel.navigateUp() },
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                showHiddenFiles = showHiddenFiles,
                onToggleHiddenFiles = { viewModel.toggleHiddenFiles() },
                isGridView = isGridView,
                onToggleGridView = { viewModel.toggleGridView() },
                currentSortBy = sortBy,
                currentSortOrder = sortOrder,
                onSortChange = { sb, so -> viewModel.setSorting(sb, so) },
                themeMode = themeMode,
                onThemeToggle = onThemeToggle,
                onLogoClick = onOpenVault,
                // Multi-select
                isMultiSelectMode = isMultiSelectMode,
                selectedCount = selectedPaths.size,
                onCloseMultiSelect = { viewModel.exitMultiSelectMode() },
                onSelectAll = { viewModel.selectAll() },
                onDeleteSelected = { showBulkDeleteConfirm = true },
                onCopySelected = { viewModel.copySelectedToClipboard() },
                onCutSelected = { viewModel.cutSelectedToClipboard() },
                onShareSelected = { viewModel.shareSelectedFiles(context) },
                onZipSelected = { viewModel.compressSelectedToZip("Arsip_Terpilih_${System.currentTimeMillis() % 1000}.zip") },
                onStartMultiSelect = { viewModel.enterMultiSelectMode() },
                // Search Filters
                searchExtension = searchExtension,
                onExtensionFilterChange = { viewModel.setSearchExtension(it) },
                searchSizeFilter = searchSizeFilter,
                onSizeFilterChange = { viewModel.setSearchSizeFilter(it) },
                searchEntireStorage = searchEntireStorage,
                onToggleSearchEntireStorage = { viewModel.toggleSearchEntireStorage() },
                // Duplicate Scanner
                onOpenDuplicateScanner = { showDuplicateScanner = true },
                // Dashboard & Home
                isHomeDashboard = isHomeDashboard,
                onNavigateToDashboard = { viewModel.navigateToHomeDashboard() },
                onOpenDrawer = { showDrawerSheet = true },
                onOpenVip = { showVipDialog = true },
                onRefresh = { viewModel.refreshCategoryStats() }
            )
        },
        floatingActionButton = {
            if (!isMultiSelectMode && !isHomeDashboard) {
                Box {
                    FloatingActionButton(
                        onClick = { showFabMenu = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("main_add_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah dokumen / folder")
                    }

                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Buat Folder Baru") },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            onClick = {
                                showFabMenu = false
                                showNewItemDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Buat Catatan Teks (.txt)") },
                            leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                            onClick = {
                                showFabMenu = false
                                showNewItemDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Impor Dokumen dari Penyimpanan") },
                            leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                            onClick = {
                                showFabMenu = false
                                filePickerLauncher.launch("*/*")
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isHomeDashboard) {
                CategoryDashboard(
                    overviewStats = categoryOverviewStats,
                    onCategoryClick = { catType ->
                        when (catType) {
                            DashboardCategoryType.CLOUD -> showCloudDialog = true
                            DashboardCategoryType.REMOTE -> showRemoteDialog = true
                            DashboardCategoryType.NETWORK_ACCESS -> showNetworkAccessDialog = true
                            else -> viewModel.openFromDashboard(catType)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Header with Storage info & Category Chips & Breadcrumbs & History navigation
                StorageHeader(
                storageStats = storageStats,
                selectedCategory = selectedCategory,
                onSelectCategory = { viewModel.setCategory(it) },
                currentDir = currentDir,
                rootDir = viewModel.primaryRootDir,
                onNavigateToDir = { viewModel.navigateToPath(it) },
                onLogoClick = onOpenVault,
                canNavigateBack = canNavigateBack,
                canNavigateForward = canNavigateForward,
                onNavigateBack = { viewModel.navigateHistoryBack() },
                onNavigateForward = { viewModel.navigateHistoryForward() },
                recentFolders = recentFolders,
                availableStorages = availableStorages,
                activeStorageRoot = activeStorageRoot,
                onSwitchStorage = { viewModel.switchStorageRoot(it) },
                isLargestFilesActive = isLargestFilesActive,
                onToggleLargestFiles = { viewModel.toggleLargestFiles() }
            )

            // Large file operation progress indicator banner
            AnimatedVisibility(visible = operationProgress.isVisible) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = operationProgress.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${operationProgress.percent}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { operationProgress.percent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = operationProgress.detail,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Floating Clipboard Bar (shows when user copied/cut files and can paste in current directory)
            clipboard?.let { clip ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (clip.isCut) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${clip.files.size} berkas (${if (clip.isCut) "Dipindahkan" else "Disalin"})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { viewModel.pasteClipboard() },
                                modifier = Modifier.testTag("paste_clipboard_button")
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tempel di Sini", fontSize = 12.sp)
                            }
                            IconButton(
                                onClick = { viewModel.clearClipboard() },
                                modifier = Modifier.size(32.dp).testTag("cancel_clipboard_button")
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Batal",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // File Listing Area
            if (displayedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Tidak ada file yang cocok dengan '$searchQuery'" else "Folder ini kosong",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gunakan tombol '+' di bawah untuk membuat atau mengimpor file",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 130.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedFiles, key = { it.path }) { item ->
                        val isSelected = selectedPaths.contains(item.path)
                        FileGridItem(
                            fileItem = item,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = isSelected,
                            onToggleSelect = { viewModel.toggleSelectPath(item.path) },
                            onLongClick = {
                                viewModel.enterMultiSelectMode()
                                viewModel.toggleSelectPath(item.path)
                            },
                            onClick = {
                                if (isMultiSelectMode) {
                                    viewModel.toggleSelectPath(item.path)
                                } else {
                                    handleItemClick(
                                        item = item,
                                        viewModel = viewModel,
                                        activity = activity,
                                        onFolderLocked = { folderToUnlock = it },
                                        onDecrypt = { fileToDecrypt = it },
                                        onPreview = { fileToPreview = it },
                                        onOpen = { FileUtils.openFileWithExternalApp(context, it) }
                                    )
                                }
                            },
                            onMoreClick = { selectedFileForAction = item }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(displayedFiles, key = { it.path }) { item ->
                        val isSelected = selectedPaths.contains(item.path)
                        FileListItem(
                            fileItem = item,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = isSelected,
                            onToggleSelect = { viewModel.toggleSelectPath(item.path) },
                            onLongClick = {
                                viewModel.enterMultiSelectMode()
                                viewModel.toggleSelectPath(item.path)
                            },
                            onClick = {
                                if (isMultiSelectMode) {
                                    viewModel.toggleSelectPath(item.path)
                                } else {
                                    handleItemClick(
                                        item = item,
                                        viewModel = viewModel,
                                        activity = activity,
                                        onFolderLocked = { folderToUnlock = it },
                                        onDecrypt = { fileToDecrypt = it },
                                        onPreview = { fileToPreview = it },
                                        onOpen = { FileUtils.openFileWithExternalApp(context, it) }
                                    )
                                }
                            },
                            onMoreClick = { selectedFileForAction = item }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
            }
        }
    }

    // File Action Bottom Sheet
    selectedFileForAction?.let { fileItem ->
        FileActionBottomSheet(
            fileItem = fileItem,
            onDismiss = { selectedFileForAction = null },
            onOpenExternal = {
                selectedFileForAction = null
                FileUtils.openFileWithExternalApp(context, fileItem.file)
            },
            onPreview = {
                selectedFileForAction = null
                fileToPreview = fileItem.file
            },
            onEncrypt = {
                selectedFileForAction = null
                fileToEncrypt = fileItem.file
            },
            onDecrypt = {
                selectedFileForAction = null
                fileToDecrypt = fileItem.file
            },
            onLockFolder = {
                selectedFileForAction = null
                folderToLock = fileItem.file
            },
            onMoveToVault = {
                selectedFileForAction = null
                viewModel.moveToSafeVault(fileItem.file)
            },
            onShare = {
                selectedFileForAction = null
                FileUtils.shareFileViaBluetoothOrSystem(context, fileItem.file)
            },
            onCopy = {
                selectedFileForAction = null
                viewModel.copyToClipboard(listOf(fileItem.file))
            },
            onMove = {
                selectedFileForAction = null
                viewModel.cutToClipboard(listOf(fileItem.file))
            },
            onRename = {
                selectedFileForAction = null
                fileToRename = fileItem.file
            },
            onDelete = {
                selectedFileForAction = null
                fileToDeletePermanent = fileItem.file
            },
            onChecksum = {
                selectedFileForAction = null
                fileForChecksum = fileItem.file
            },
            onCompressZip = {
                selectedFileForAction = null
                fileToZip = fileItem.file
            },
            onExtractZip = {
                selectedFileForAction = null
                viewModel.extractZip(fileItem.file)
            },
            onEditImage = {
                val f = fileItem.file
                selectedFileForAction = null
                fileToEditImage = f
            }
        )
    }

    // Encrypt Dialog
    fileToEncrypt?.let { file ->
        EncryptDialog(
            fileName = file.name,
            onDismiss = { fileToEncrypt = null },
            onConfirm = { password, deleteOriginal ->
                fileToEncrypt = null
                viewModel.encryptFileWithAes(file, password, deleteOriginal)
            }
        )
    }

    // Decrypt Dialog
    fileToDecrypt?.let { file ->
        DecryptDialog(
            fileName = file.name,
            onDismiss = { fileToDecrypt = null },
            onConfirm = { password, deleteEncrypted ->
                fileToDecrypt = null
                viewModel.decryptFileWithAes(file, password, deleteEncrypted)
            }
        )
    }

    // Lock Folder Dialog
    folderToLock?.let { folder ->
        LockFolderDialog(
            folderName = folder.name,
            onDismiss = { folderToLock = null },
            onConfirm = { password, allowBiometric ->
                folderToLock = null
                viewModel.lockFolder(folder, password, allowBiometric)
            }
        )
    }

    // Unlock Folder Auth Dialog (Password / PIN / Biometric)
    folderToUnlock?.let { folder ->
        PinAuthDialog(
            title = "Buka Folder Terkunci",
            subtitle = "Folder '${folder.name}' dilindungi keamanan.",
            canUseBiometric = viewModel.securityManager.canUseBiometric(),
            onDismiss = { folderToUnlock = null },
            onPinSubmit = { pin ->
                viewModel.unlockFolderWithPassword(folder.absolutePath, pin) {
                    folderToUnlock = null
                    viewModel.navigateTo(folder)
                }
            },
            onBiometricClick = {
                if (activity != null) {
                    viewModel.unlockWithBiometrics(activity, folder.absolutePath) {
                        folderToUnlock = null
                        viewModel.navigateTo(folder)
                    }
                }
            }
        )
    }

    // In-App File Preview / Editor Dialog (Images, Text, Audio, Video, PDF)
    fileToPreview?.let { file ->
        FilePreviewDialog(
            file = file,
            onDismiss = { fileToPreview = null },
            onSaveContent = { content ->
                viewModel.saveTextFile(file, content) {
                    fileToPreview = null
                }
            },
            onFileUpdated = { updatedFile ->
                viewModel.refreshCurrentDir()
                viewModel.refreshCategoryStats()
                fileToPreview = updatedFile
            }
        )
    }

    // Direct Image Editor Dialog
    fileToEditImage?.let { file ->
        ImageEditorDialog(
            file = file,
            onDismiss = { fileToEditImage = null },
            onSaveSuccess = { savedFile ->
                fileToEditImage = null
                viewModel.refreshCurrentDir()
                viewModel.refreshCategoryStats()
            }
        )
    }

    // Checksum & Metadata Dialog
    fileForChecksum?.let { file ->
        ChecksumDialog(
            file = file,
            onDismiss = { fileForChecksum = null }
        )
    }

    // Destination Picker (for Copy / Move)
    fileToCopyOrMove?.let { (file, isCopy) ->
        DestinationPickerDialog(
            rootDir = viewModel.primaryRootDir,
            onDismiss = { fileToCopyOrMove = null },
            onDestinationSelected = { dest ->
                fileToCopyOrMove = null
                if (isCopy) {
                    viewModel.copyFile(file, dest)
                } else {
                    viewModel.moveFile(file, dest)
                }
            }
        )
    }

    // Create ZIP Dialog
    fileToZip?.let { file ->
        CreateZipDialog(
            defaultZipName = "${file.nameWithoutExtension}.zip",
            onDismiss = { fileToZip = null },
            onConfirm = { zipName ->
                fileToZip = null
                viewModel.compressToZip(listOf(file), zipName)
            }
        )
    }

    // Rename Dialog
    fileToRename?.let { file ->
        RenameDialog(
            currentName = file.name,
            onDismiss = { fileToRename = null },
            onConfirm = { newName ->
                fileToRename = null
                viewModel.renameFile(file, newName)
            }
        )
    }

    // Delete Single File Confirm Dialog
    fileToDeletePermanent?.let { file ->
        ConfirmDeleteDialog(
            itemCount = 1,
            itemName = file.name,
            onDismiss = { fileToDeletePermanent = null },
            onConfirm = {
                fileToDeletePermanent = null
                viewModel.deleteFile(file)
            }
        )
    }

    // Bulk Delete Confirm Dialog
    if (showBulkDeleteConfirm) {
        ConfirmDeleteDialog(
            itemCount = selectedPaths.size,
            onDismiss = { showBulkDeleteConfirm = false },
            onConfirm = {
                showBulkDeleteConfirm = false
                viewModel.deleteSelectedFiles()
            }
        )
    }

    // Duplicate Scanner Dialog
    if (showDuplicateScanner) {
        DuplicateScannerDialog(
            scanResult = duplicateResult,
            currentDir = currentDir,
            onDismiss = { showDuplicateScanner = false },
            onStartScanCurrent = { viewModel.scanDuplicates(listOf(currentDir)) },
            onStartScanAll = { viewModel.scanDuplicates(listOf(activeStorageRoot), scanAllStorage = true) },
            onToggleItem = { viewModel.toggleDuplicateSelection(it) },
            onAutoSelect = { viewModel.autoSelectDuplicates() },
            onDeleteSelected = { viewModel.deleteSelectedDuplicates() }
        )
    }

    // New Item Dialog (Folder or Text Note)
    if (showNewItemDialog) {
        NewItemDialog(
            onDismiss = { showNewItemDialog = false },
            onCreateFolder = { name ->
                showNewItemDialog = false
                viewModel.createFolder(name)
            },
            onCreateTextFile = { name, content ->
                showNewItemDialog = false
                viewModel.createTextFile(name, content)
            }
        )
    }

    // Dashboard Feature Dialogs
    if (showCloudDialog) {
        CloudServicesDialog(onDismiss = { showCloudDialog = false })
    }

    if (showRemoteDialog) {
        RemoteConnectionsDialog(onDismiss = { showRemoteDialog = false })
    }

    if (showNetworkAccessDialog) {
        NetworkAccessDialog(
            onDismiss = { showNetworkAccessDialog = false },
            onOpenFullTransferScreen = {
                // Navigate via bottom bar or toast
            }
        )
    }

    if (showVipDialog) {
        PremiumVipDialog(onDismiss = { showVipDialog = false })
    }

    if (showDrawerSheet) {
        FileManagerDrawerSheet(
            onDismiss = { showDrawerSheet = false },
            onOpenVault = {
                showDrawerSheet = false
                onOpenVault()
            },
            onOpenDuplicates = {
                showDrawerSheet = false
                showDuplicateScanner = true
            },
            onToggleTheme = {
                showDrawerSheet = false
                onThemeToggle()
            },
            themeMode = themeMode,
            overviewStats = categoryOverviewStats
        )
    }
}

private fun handleItemClick(
    item: FileItem,
    viewModel: FileManagerViewModel,
    activity: FragmentActivity?,
    onFolderLocked: (File) -> Unit,
    onDecrypt: (File) -> Unit,
    onPreview: (File) -> Unit,
    onOpen: (File) -> Unit
) {
    if (item.isDirectory) {
        val entered = viewModel.navigateTo(item.file)
        if (!entered) {
            // Folder is locked! Prompt authentication
            onFolderLocked(item.file)
        }
    } else if (item.isEncrypted) {
        // Encrypted AES file - prompt for decryption password
        onDecrypt(item.file)
    } else {
        // Document, media, audio, pdf - open with preview or external app
        val ext = item.extension.lowercase()
        val previewableExtensions = listOf(
            "txt", "md", "json", "xml", "csv", "log", "kt", "java", "conf", "properties",
            "jpg", "jpeg", "png", "webp", "gif", "bmp",
            "mp3", "wav", "m4a", "ogg", "flac", "aac",
            "pdf", "mp4", "mkv", "webm"
        )
        if (ext in previewableExtensions) {
            onPreview(item.file)
        } else {
            onOpen(item.file)
        }
    }
}
