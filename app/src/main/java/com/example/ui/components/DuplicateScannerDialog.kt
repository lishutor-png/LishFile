package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.util.FileUtils
import java.io.File

@Composable
fun DuplicateScannerDialog(
    currentDir: File,
    storageRoot: File,
    availableStorages: List<StorageVolumeInfo> = emptyList(),
    result: DuplicateScanResult,
    onStartScan: (targetFolders: List<File>, scopeDesc: String) -> Unit,
    onCancelScan: () -> Unit = {},
    onToggleSelect: (checksum: String, path: String) -> Unit,
    onSelectSmartDuplicates: () -> Unit = {},
    onSelectNewestDuplicates: () -> Unit = {},
    onSelectAllDuplicates: (Boolean) -> Unit = {},
    onCleanDuplicates: () -> Unit,
    onResetScan: () -> Unit = {},
    onOpenFile: ((File) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    // Determine primary and external storage volumes
    val primaryStorage = remember(availableStorages, storageRoot) {
        availableStorages.firstOrNull { it.isPrimary }
            ?: availableStorages.firstOrNull()
            ?: StorageVolumeInfo(
                name = "Penyimpanan Internal",
                rootDir = storageRoot,
                totalSpace = storageRoot.totalSpace,
                freeSpace = storageRoot.freeSpace,
                isPrimary = true,
                isRemovable = false
            )
    }

    val externalStorages = remember(availableStorages, primaryStorage) {
        availableStorages.filter { it.rootDir.absolutePath != primaryStorage.rootDir.absolutePath }
    }

    val hasExternalStorage = externalStorages.isNotEmpty()

    var selectedScope by remember {
        mutableStateOf(
            if (hasExternalStorage) DuplicateScopeType.ALL_STORAGES else DuplicateScopeType.ENTIRE_STORAGE
        )
    }
    var selectedExternalVolume by remember { mutableStateOf<StorageVolumeInfo?>(externalStorages.firstOrNull()) }
    var selectedPopularFolder by remember { mutableStateOf<String?>(null) }
    val selectedMultipleFolders = remember { mutableStateListOf<File>() }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    // Search query & category filter inside results
    var resultsSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<FileType?>(null) }

    // Filter duplicate groups by search query & file type category
    val filteredDuplicateGroups = remember(result.duplicateGroups, resultsSearchQuery, selectedCategoryFilter) {
        result.duplicateGroups.filter { group ->
            val matchesCategory = if (selectedCategoryFilter == null) {
                true
            } else {
                group.files.any { it.fileType == selectedCategoryFilter }
            }

            val matchesSearch = if (resultsSearchQuery.isBlank()) {
                true
            } else {
                group.files.any {
                    it.name.contains(resultsSearchQuery, ignoreCase = true) ||
                            it.parentPath.contains(resultsSearchQuery, ignoreCase = true)
                }
            }
            matchesCategory && matchesSearch
        }
    }

    // Selected files count & total freed space
    val selectedFilesCount = remember(result.duplicateGroups) {
        result.duplicateGroups.sumOf { group ->
            group.files.count { it.isSelectedForDelete }
        }
    }

    val selectedBytesFreed = remember(result.duplicateGroups) {
        result.duplicateGroups.sumOf { group ->
            val count = group.files.count { it.isSelectedForDelete }
            count * group.fileSize
        }
    }

    val totalCopiesCount = remember(result.duplicateGroups) {
        result.duplicateGroups.sumOf { (it.files.size - 1).coerceAtLeast(0) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("duplicate_scanner_screen"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                DuplicateScannerTopBar(
                    isScanning = result.isScanning,
                    hasCompletedScan = result.hasCompletedScan,
                    onBack = {
                        if (result.hasCompletedScan) {
                            onResetScan()
                        } else {
                            onDismiss()
                        }
                    },
                    onClose = onDismiss
                )

                // Step Flow Indicator
                StepBreadcrumbHeader(
                    currentStep = when {
                        result.isScanning -> 2
                        result.hasCompletedScan -> 3
                        else -> 1
                    }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 1.dp
                )

                // Content View based on State
                AnimatedContent(
                    targetState = when {
                        result.isScanning -> 2
                        result.hasCompletedScan -> 3
                        else -> 1
                    },
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "duplicate_state_transition",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { step ->
                    when (step) {
                        2 -> {
                            ScanningProgressView(
                                result = result,
                                onCancelScan = onCancelScan
                            )
                        }
                        3 -> {
                            ScanResultsView(
                                result = result,
                                filteredGroups = filteredDuplicateGroups,
                                availableStorages = availableStorages,
                                selectedFilesCount = selectedFilesCount,
                                selectedBytesFreed = selectedBytesFreed,
                                totalCopiesCount = totalCopiesCount,
                                resultsSearchQuery = resultsSearchQuery,
                                onSearchQueryChange = { resultsSearchQuery = it },
                                selectedCategoryFilter = selectedCategoryFilter,
                                onCategoryFilterChange = { selectedCategoryFilter = it },
                                onToggleSelect = onToggleSelect,
                                onSelectSmart = onSelectSmartDuplicates,
                                onSelectNewest = onSelectNewestDuplicates,
                                onSelectAll = onSelectAllDuplicates,
                                onResetScan = onResetScan,
                                onOpenFile = onOpenFile,
                                onRequestClean = { showConfirmDeleteDialog = true }
                            )
                        }
                        else -> {
                            LocationSelectionView(
                                currentDir = currentDir,
                                primaryStorage = primaryStorage,
                                externalStorages = externalStorages,
                                selectedScope = selectedScope,
                                onScopeSelected = { selectedScope = it },
                                selectedExternalVolume = selectedExternalVolume,
                                onExternalVolumeSelected = { selectedExternalVolume = it },
                                selectedPopularFolder = selectedPopularFolder,
                                onPopularFolderSelected = { selectedPopularFolder = it },
                                selectedMultipleFolders = selectedMultipleFolders,
                                onStartScan = { targets, desc ->
                                    onStartScan(targets, desc)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Safety Confirmation Dialog before deletion
    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Konfirmasi Pembersihan Duplikat",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column {
                    Text(
                        text = "Anda akan menghapus $selectedFilesCount file duplikat dengan total ukuran ${FileUtils.formatFileSize(selectedBytesFreed)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• File asli (salinan yang tidak dicentang) tetap aman tersimpan di memori internal maupun kartu SD.\n• Salinan redundant yang Anda centang akan dibersihkan dari penyimpanan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteDialog = false
                        onCleanDuplicates()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_duplicates")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hapus $selectedFilesCount File")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

private data class PopularFolderMeta(
    val name: String,
    val dir: File,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun DuplicateScannerTopBar(
    isScanning: Boolean,
    hasCompletedScan: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_duplicate_back")
            ) {
                Icon(
                    imageVector = if (hasCompletedScan) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                    contentDescription = "Kembali"
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Duplicate Finder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        isScanning -> "Sedang memindai memori & menghitung hash..."
                        hasCompletedScan -> "Hasil temuan & pembersihan file ganda"
                        else -> "Mendukung memori internal & kartu SD eksternal"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_close_duplicate_scanner")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Tutup")
            }
        }
    }
}

@Composable
private fun StepBreadcrumbHeader(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepChip(stepNumber = 1, label = "Pilih Lokasi", isActive = currentStep == 1, isCompleted = currentStep > 1)
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
        StepChip(stepNumber = 2, label = "Pindai File", isActive = currentStep == 2, isCompleted = currentStep > 2)
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
        StepChip(stepNumber = 3, label = "Tinjau & Bersihkan", isActive = currentStep == 3, isCompleted = false)
    }
}

@Composable
private fun StepChip(
    stepNumber: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                isActive -> MaterialTheme.colorScheme.primary
                isCompleted -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Text(
                        text = "$stepNumber",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Step 1: Location Selection with Full Support for External SD Cards
 */
@Composable
private fun LocationSelectionView(
    currentDir: File,
    primaryStorage: StorageVolumeInfo,
    externalStorages: List<StorageVolumeInfo>,
    selectedScope: DuplicateScopeType,
    onScopeSelected: (DuplicateScopeType) -> Unit,
    selectedExternalVolume: StorageVolumeInfo?,
    onExternalVolumeSelected: (StorageVolumeInfo?) -> Unit,
    selectedPopularFolder: String?,
    onPopularFolderSelected: (String?) -> Unit,
    selectedMultipleFolders: MutableList<File>,
    onStartScan: (List<File>, String) -> Unit
) {
    // Top-level folders in primary and external
    val primaryFolders = remember(primaryStorage) {
        primaryStorage.rootDir.listFiles()?.filter {
            it.isDirectory && !FileUtils.isHiddenOrInHiddenFolder(it)
        }?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    val externalFolders = remember(selectedExternalVolume) {
        selectedExternalVolume?.rootDir?.listFiles()?.filter {
            it.isDirectory && !FileUtils.isHiddenOrInHiddenFolder(it)
        }?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    var selectedFolderTab by remember { mutableIntStateOf(0) }

    // Popular candidate folders in primary
    val popularFolderItems = remember(primaryStorage) {
        val candidates = listOf(
            Triple("Download", Icons.Default.Download, Color(0xFF0288D1)),
            Triple("DCIM", Icons.Default.PhotoCamera, Color(0xFF2E7D32)),
            Triple("Pictures", Icons.Default.Image, Color(0xFFEF6C00)),
            Triple("WhatsApp", Icons.Default.Chat, Color(0xFF00897B)),
            Triple("Documents", Icons.Default.Description, Color(0xFF5E35B1)),
            Triple("Music", Icons.Default.Audiotrack, Color(0xFFD81B60)),
            Triple("Movies", Icons.Default.Videocam, Color(0xFFE53935))
        )
        candidates.mapNotNull { (name, icon, color) ->
            val dir = File(primaryStorage.rootDir, name)
            if (dir.exists() && dir.isDirectory) {
                PopularFolderMeta(name, dir, icon, color)
            } else null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("location_selection_view")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Pilih Lokasi yang Ingin Diperiksa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tentukan cakupan pemindaian berkas duplikat. Mendukung memori internal dan kartu SD eksternal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Option 1: ALL STORAGES (Internal + External SD Card)
            if (externalStorages.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onScopeSelected(DuplicateScopeType.ALL_STORAGES)
                                onPopularFolderSelected(null)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedScope == DuplicateScopeType.ALL_STORAGES) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                        border = if (selectedScope == DuplicateScopeType.ALL_STORAGES) {
                            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedScope == DuplicateScopeType.ALL_STORAGES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = if (selectedScope == DuplicateScopeType.ALL_STORAGES) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Semua Penyimpanan (Internal + Kartu SD)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedScope == DuplicateScopeType.ALL_STORAGES) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(
                                            text = "Maksimal",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Pindai internal (${FileUtils.formatFileSize(primaryStorage.totalSpace)}) dan ${externalStorages.size} kartu SD eksternal sekaligus",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedScope == DuplicateScopeType.ALL_STORAGES) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = selectedScope == DuplicateScopeType.ALL_STORAGES,
                                onClick = {
                                    onScopeSelected(DuplicateScopeType.ALL_STORAGES)
                                    onPopularFolderSelected(null)
                                }
                            )
                        }
                    }
                }
            }

            // Option 2: Internal Storage Only
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onScopeSelected(DuplicateScopeType.ENTIRE_STORAGE)
                            onPopularFolderSelected(null)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedScope == DuplicateScopeType.ENTIRE_STORAGE) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    border = if (selectedScope == DuplicateScopeType.ENTIRE_STORAGE) {
                        CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedScope == DuplicateScopeType.ENTIRE_STORAGE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (selectedScope == DuplicateScopeType.ENTIRE_STORAGE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Penyimpanan Internal Saja",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedScope == DuplicateScopeType.ENTIRE_STORAGE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${FileUtils.formatFileSize(primaryStorage.freeSpace)} bebas dari ${FileUtils.formatFileSize(primaryStorage.totalSpace)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedScope == DuplicateScopeType.ENTIRE_STORAGE) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = selectedScope == DuplicateScopeType.ENTIRE_STORAGE,
                            onClick = {
                                onScopeSelected(DuplicateScopeType.ENTIRE_STORAGE)
                                onPopularFolderSelected(null)
                            }
                        )
                    }
                }
            }

            // Option 3: External Storage (SD Card / USB Storage)
            if (externalStorages.isNotEmpty()) {
                externalStorages.forEach { extStorage ->
                    item {
                        val isSelected = selectedScope == DuplicateScopeType.EXTERNAL_STORAGE && selectedExternalVolume?.rootDir?.absolutePath == extStorage.rootDir.absolutePath
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onScopeSelected(DuplicateScopeType.EXTERNAL_STORAGE)
                                    onExternalVolumeSelected(extStorage)
                                    onPopularFolderSelected(null)
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ),
                            border = if (isSelected) {
                                CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
                            } else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.SdCard,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = extStorage.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.padding(2.dp)
                                        ) {
                                            Text(
                                                text = "Eksternal",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${extStorage.rootDir.absolutePath} • ${FileUtils.formatFileSize(extStorage.totalSpace)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onScopeSelected(DuplicateScopeType.EXTERNAL_STORAGE)
                                        onExternalVolumeSelected(extStorage)
                                        onPopularFolderSelected(null)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Option 4: Current Folder Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onScopeSelected(DuplicateScopeType.CURRENT_FOLDER)
                            onPopularFolderSelected(null)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedScope == DuplicateScopeType.CURRENT_FOLDER && selectedPopularFolder == null) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    border = if (selectedScope == DuplicateScopeType.CURRENT_FOLDER && selectedPopularFolder == null) {
                        CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedScope == DuplicateScopeType.CURRENT_FOLDER && selectedPopularFolder == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (selectedScope == DuplicateScopeType.CURRENT_FOLDER && selectedPopularFolder == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Folder Saat Ini (${currentDir.name})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedScope == DuplicateScopeType.CURRENT_FOLDER && selectedPopularFolder == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentDir.absolutePath,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedScope == DuplicateScopeType.CURRENT_FOLDER && selectedPopularFolder == null) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        RadioButton(
                            selected = selectedScope == DuplicateScopeType.CURRENT_FOLDER && selectedPopularFolder == null,
                            onClick = {
                                onScopeSelected(DuplicateScopeType.CURRENT_FOLDER)
                                onPopularFolderSelected(null)
                            }
                        )
                    }
                }
            }

            // Option 5: Quick Popular Folders
            if (popularFolderItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Folder Populer Cepat:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        popularFolderItems.forEach { item ->
                            val isSelected = selectedScope == DuplicateScopeType.CURRENT_FOLDER && selectedPopularFolder == item.name
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = {
                                    onScopeSelected(DuplicateScopeType.CURRENT_FOLDER)
                                    onPopularFolderSelected(item.name)
                                },
                                label = {
                                    Text(
                                        text = item.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else item.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(42.dp)
                            )
                        }
                    }
                }
            }

            // Option 6: Custom Multiple Folders Selection (With tabs for Internal and SD Card)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onScopeSelected(DuplicateScopeType.SELECTED_FOLDERS)
                            onPopularFolderSelected(null)
                            if (selectedMultipleFolders.isEmpty() && primaryFolders.isNotEmpty()) {
                                primaryFolders.take(2).forEach { selectedMultipleFolders.add(it) }
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedScope == DuplicateScopeType.SELECTED_FOLDERS) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    border = if (selectedScope == DuplicateScopeType.SELECTED_FOLDERS) {
                        CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
                    } else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedScope == DuplicateScopeType.SELECTED_FOLDERS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.FolderSpecial,
                                        contentDescription = null,
                                        tint = if (selectedScope == DuplicateScopeType.SELECTED_FOLDERS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pilih Beberapa Folder Spesifik",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedScope == DuplicateScopeType.SELECTED_FOLDERS) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (selectedMultipleFolders.isEmpty()) "Pilih folder dari memori internal maupun kartu SD" else "${selectedMultipleFolders.size} folder terpilih",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedScope == DuplicateScopeType.SELECTED_FOLDERS) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = selectedScope == DuplicateScopeType.SELECTED_FOLDERS,
                                onClick = {
                                    onScopeSelected(DuplicateScopeType.SELECTED_FOLDERS)
                                    onPopularFolderSelected(null)
                                }
                            )
                        }

                        // Checklist inside Custom Folders option with tabs for Internal and SD Card
                        if (selectedScope == DuplicateScopeType.SELECTED_FOLDERS) {
                            Spacer(modifier = Modifier.height(12.dp))

                            if (externalStorages.isNotEmpty()) {
                                TabRow(
                                    selectedTabIndex = selectedFolderTab,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                ) {
                                    Tab(
                                        selected = selectedFolderTab == 0,
                                        onClick = { selectedFolderTab = 0 },
                                        text = { Text("📱 Internal", fontSize = 12.sp) }
                                    )
                                    Tab(
                                        selected = selectedFolderTab == 1,
                                        onClick = { selectedFolderTab = 1 },
                                        text = { Text("💾 Kartu SD", fontSize = 12.sp) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            val activeFolderList = if (selectedFolderTab == 1 && externalFolders.isNotEmpty()) externalFolders else primaryFolders

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (activeFolderList.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Tidak ada folder yang dapat diakses",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.padding(6.dp)) {
                                        itemsIndexed(activeFolderList) { _, folder ->
                                            val isChecked = selectedMultipleFolders.contains(folder)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (isChecked) selectedMultipleFolders.remove(folder)
                                                        else selectedMultipleFolders.add(folder)
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { check ->
                                                        if (check) selectedMultipleFolders.add(folder)
                                                        else selectedMultipleFolders.remove(folder)
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = folder.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Start Scan Action Button
        Button(
            onClick = {
                when (selectedScope) {
                    DuplicateScopeType.ALL_STORAGES -> {
                        val targets = mutableListOf<File>()
                        targets.add(primaryStorage.rootDir)
                        externalStorages.forEach { targets.add(it.rootDir) }
                        onStartScan(targets, "Semua Penyimpanan (Internal + Kartu SD)")
                    }
                    DuplicateScopeType.ENTIRE_STORAGE -> {
                        onStartScan(listOf(primaryStorage.rootDir), "Penyimpanan Internal")
                    }
                    DuplicateScopeType.EXTERNAL_STORAGE -> {
                        val targetVolume = selectedExternalVolume ?: externalStorages.firstOrNull()
                        if (targetVolume != null) {
                            onStartScan(listOf(targetVolume.rootDir), targetVolume.name)
                        } else {
                            onStartScan(listOf(primaryStorage.rootDir), "Penyimpanan Internal")
                        }
                    }
                    DuplicateScopeType.CURRENT_FOLDER -> {
                        if (selectedPopularFolder != null) {
                            val targetDir = File(primaryStorage.rootDir, selectedPopularFolder)
                            onStartScan(listOf(targetDir), "Folder $selectedPopularFolder")
                        } else {
                            onStartScan(listOf(currentDir), "Folder: ${currentDir.name}")
                        }
                    }
                    DuplicateScopeType.SELECTED_FOLDERS -> {
                        val targets = if (selectedMultipleFolders.isNotEmpty()) {
                            selectedMultipleFolders.toList()
                        } else {
                            listOf(primaryStorage.rootDir)
                        }
                        onStartScan(targets, "${targets.size} Folder Terpilih")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("btn_start_duplicate_scan"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Mulai Pindai Duplikat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Step 2: Live Scanning Progress (Flowchart Steps)
 */
@Composable
private fun ScanningProgressView(
    result: DuplicateScanResult,
    onCancelScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("scanning_progress_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(76.dp),
                strokeWidth = 5.dp
            )
            Icon(
                Icons.Default.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sedang Memindai Duplikat...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = result.scopeDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Alur Analisis Berkas:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                ProgressFlowRow(
                    stepNumber = "1",
                    title = "Scan semua file yang dapat diakses",
                    subtitle = "${result.progress.filesScannedCount} file berhasil dibaca",
                    status = when {
                        result.progress.candidateFilesCount > 0 -> StepStatus.COMPLETED
                        else -> StepStatus.IN_PROGRESS
                    }
                )

                ProgressFlowRow(
                    stepNumber = "2",
                    title = "Kelompokkan berdasarkan ukuran sama",
                    subtitle = if (result.progress.candidateFilesCount > 0) {
                        "${result.progress.candidateFilesCount} berkas kandidat ukuran sama (ukuran unik dilewati)"
                    } else "Memeriksa ukuran berkas...",
                    status = when {
                        result.progress.processedCandidatesCount > 0 -> StepStatus.COMPLETED
                        result.progress.candidateFilesCount > 0 -> StepStatus.IN_PROGRESS
                        else -> StepStatus.PENDING
                    }
                )

                ProgressFlowRow(
                    stepNumber = "3",
                    title = "Hitung sidik jari SHA-256 kandidat",
                    subtitle = if (result.progress.totalCandidatesCount > 0) {
                        "${result.progress.processedCandidatesCount} / ${result.progress.totalCandidatesCount} berkas dihitung"
                    } else "Menunggu kandidat ukuran sama...",
                    status = when {
                        result.progress.processedCandidatesCount > 0 && result.progress.processedCandidatesCount >= result.progress.totalCandidatesCount -> StepStatus.COMPLETED
                        result.progress.processedCandidatesCount > 0 -> StepStatus.IN_PROGRESS
                        else -> StepStatus.PENDING
                    }
                )

                ProgressFlowRow(
                    stepNumber = "4",
                    title = "Kelompokkan berdasarkan hash yang identik",
                    subtitle = "Menyiapkan daftar salinan duplikat...",
                    status = StepStatus.PENDING
                )

                if (result.progress.totalCandidatesCount > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    val progressFraction = (result.progress.processedCandidatesCount.toFloat() / result.progress.totalCandidatesCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Proses SHA-256",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedButton(
            onClick = onCancelScan,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(46.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Batal Pemindaian")
        }
    }
}

private enum class StepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}

@Composable
private fun ProgressFlowRow(
    stepNumber: String,
    title: String,
    subtitle: String,
    status: StepStatus
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = when (status) {
                StepStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                StepStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
                StepStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (status) {
                    StepStatus.COMPLETED -> {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    StepStatus.IN_PROGRESS -> {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    StepStatus.PENDING -> {
                        Text(
                            text = stepNumber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (status != StepStatus.PENDING) FontWeight.SemiBold else FontWeight.Normal,
                color = if (status == StepStatus.IN_PROGRESS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Step 3: Scan Results & Cleanup with Storage Badges (Internal / SD Card)
 */
@Composable
private fun ScanResultsView(
    result: DuplicateScanResult,
    filteredGroups: List<DuplicateGroup>,
    availableStorages: List<StorageVolumeInfo>,
    selectedFilesCount: Int,
    selectedBytesFreed: Long,
    totalCopiesCount: Int,
    resultsSearchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategoryFilter: FileType?,
    onCategoryFilterChange: (FileType?) -> Unit,
    onToggleSelect: (checksum: String, path: String) -> Unit,
    onSelectSmart: () -> Unit,
    onSelectNewest: () -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onResetScan: () -> Unit,
    onOpenFile: ((File) -> Unit)?,
    onRequestClean: () -> Unit
) {
    if (result.duplicateGroups.isEmpty()) {
        // Clean State
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("no_duplicates_view"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(84.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (result.freedBytesLastAction > 0) "Penyimpanan Bersih!" else "Tidak Ditemukan File Duplikat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (result.freedBytesLastAction > 0) {
                    "Berhasil membebaskan ${FileUtils.formatFileSize(result.freedBytesLastAction)} dari ${result.deletedCountLastAction} berkas salinan redundant."
                } else {
                    "Semua berkas yang dipindai di lokasi '${result.scopeDescription}' memiliki konten dan ukuran yang unik."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onResetScan,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pindai Lokasi Lain")
            }
        }
    } else {
        // Duplicates Found Screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("duplicate_results_view")
        ) {
            // Hero Savings Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Potensi Ruang Bebas",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = FileUtils.formatFileSize(result.totalWastedBytes),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "${result.duplicateGroups.size} Kelompok • $totalCopiesCount Salinan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Terpilih untuk dihapus: $selectedFilesCount berkas (${FileUtils.formatFileSize(selectedBytesFreed)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Quick Category Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { onCategoryFilterChange(null) },
                    label = { Text("Semua (${result.duplicateGroups.size})", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedCategoryFilter == FileType.IMAGE,
                    onClick = { onCategoryFilterChange(if (selectedCategoryFilter == FileType.IMAGE) null else FileType.IMAGE) },
                    label = { Text("Gambar", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = selectedCategoryFilter == FileType.VIDEO,
                    onClick = { onCategoryFilterChange(if (selectedCategoryFilter == FileType.VIDEO) null else FileType.VIDEO) },
                    label = { Text("Video", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = selectedCategoryFilter == FileType.AUDIO,
                    onClick = { onCategoryFilterChange(if (selectedCategoryFilter == FileType.AUDIO) null else FileType.AUDIO) },
                    label = { Text("Audio", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = selectedCategoryFilter == FileType.DOCUMENT,
                    onClick = { onCategoryFilterChange(if (selectedCategoryFilter == FileType.DOCUMENT) null else FileType.DOCUMENT) },
                    label = { Text("Dokumen", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = selectedCategoryFilter == FileType.ARCHIVE,
                    onClick = { onCategoryFilterChange(if (selectedCategoryFilter == FileType.ARCHIVE) null else FileType.ARCHIVE) },
                    label = { Text("Arsip", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Smart Selection Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onSelectSmart,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pertahankan Terlama (Asli)", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = onSelectNewest,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pertahankan Terbaru", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onSelectAll(true) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Pilih Semua", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onSelectAll(false) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Batal Pilih", fontSize = 11.sp)
                }
            }

            // Duplicate Groups List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(filteredGroups) { groupIndex, group ->
                    DuplicateGroupCard(
                        groupIndex = groupIndex + 1,
                        group = group,
                        availableStorages = availableStorages,
                        onToggleSelect = onToggleSelect,
                        onOpenFile = onOpenFile
                    )
                }
            }

            // Bottom Sticky Clean Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedFilesCount > 0) {
                                "$selectedFilesCount berkas terpilih"
                            } else {
                                "Pilih berkas untuk dihapus"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedFilesCount > 0) {
                                "Bebaskan ${FileUtils.formatFileSize(selectedBytesFreed)}"
                            } else {
                                "Gunakan tombol pilihan cerdas di atas"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedFilesCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onRequestClean,
                        enabled = selectedFilesCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("btn_clean_selected_duplicates"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hapus Duplikat",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Group Card for identical files with same SHA-256
 */
@Composable
private fun DuplicateGroupCard(
    groupIndex: Int,
    group: DuplicateGroup,
    availableStorages: List<StorageVolumeInfo>,
    onToggleSelect: (checksum: String, path: String) -> Unit,
    onOpenFile: ((File) -> Unit)?
) {
    val wastedSize = (group.files.size - 1) * group.fileSize

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Group Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Kelompok #$groupIndex",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${group.files.size} salinan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Ukuran: ${FileUtils.formatFileSize(group.fileSize)} • Boros: ${FileUtils.formatFileSize(wastedSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = "SHA: ${group.checksum.take(10)}...",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Items in group
            group.files.forEachIndexed { index, item ->
                DuplicateFileItemRow(
                    index = index,
                    item = item,
                    checksum = group.checksum,
                    availableStorages = availableStorages,
                    onToggleSelect = onToggleSelect,
                    onOpenFile = onOpenFile
                )
            }
        }
    }
}

@Composable
private fun DuplicateFileItemRow(
    index: Int,
    item: DuplicateFileItem,
    checksum: String,
    availableStorages: List<StorageVolumeInfo>,
    onToggleSelect: (checksum: String, path: String) -> Unit,
    onOpenFile: ((File) -> Unit)?
) {
    val (icon, iconColor) = getFileTypeIconAndColor(
        FileItem(
            file = item.file,
            fileType = item.fileType
        )
    )

    // Detect if file is on external SD card or internal
    val storageTag = remember(item.file.absolutePath, availableStorages) {
        val extVolume = availableStorages.find { !it.isPrimary && item.file.absolutePath.startsWith(it.rootDir.absolutePath) }
        if (extVolume != null) {
            "💾 Kartu SD"
        } else {
            "📱 Internal"
        }
    }

    // Extract folder name nicely
    val folderName = remember(item.parentPath) {
        val parent = File(item.parentPath)
        val grandParent = parent.parentFile?.name
        if (!grandParent.isNullOrEmpty() && grandParent != "0" && grandParent != "emulated") {
            "$grandParent/${parent.name}"
        } else {
            parent.name
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggleSelect(checksum, item.file.absolutePath) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isSelectedForDelete,
            onCheckedChange = { onToggleSelect(checksum, item.file.absolutePath) },
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = iconColor.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (index == 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = "Asli (Terlama)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (item.isSelectedForDelete) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = "Akan Dihapus",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Storage tag (Internal vs Kartu SD)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (storageTag.contains("Kartu SD")) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = storageTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (storageTag.contains("Kartu SD")) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = FileUtils.formatDate(item.lastModified),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }

        // Preview button to inspect/open duplicate file
        if (onOpenFile != null) {
            IconButton(
                onClick = { onOpenFile(item.file) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Pratinjau",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
