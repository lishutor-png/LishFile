package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.components.CompressZipDialog
import com.example.ui.components.LishFileTopBar
import com.example.ui.components.MoveDestinationDialog
import com.example.util.FileUtils
import com.example.viewmodel.FileManagerViewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File

enum class MainNavTab {
    EXPLORER,
    VAULT,
    TRANSFER,
    SETTINGS
}

@Composable
fun MainAppScreen(
    viewModel: FileManagerViewModel,
    onTriggerBiometrics: () -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainNavTab.EXPLORER) }
    val snackbarHostState = remember { SnackbarHostState() }

    var logoClickCount by remember { mutableStateOf(0) }
    var lastLogoClickTime by remember { mutableStateOf(0L) }

    val handleLogoClick: () -> Unit = {
        val now = System.currentTimeMillis()
        if (now - lastLogoClickTime < 1500) {
            logoClickCount++
        } else {
            logoClickCount = 1
        }
        lastLogoClickTime = now

        if (logoClickCount >= 3) {
            logoClickCount = 0
            currentTab = MainNavTab.VAULT
            viewModel.notifySnackbar("Membuka Brankas Aman...")
        }
    }

    BackHandler(enabled = currentTab == MainNavTab.VAULT) {
        currentTab = MainNavTab.EXPLORER
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    val isGridView by viewModel.preferences.isGridView.collectAsState()
    val showHiddenFiles by viewModel.preferences.showHiddenFiles.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val allFiles by viewModel.allFilesInCurrentDir.collectAsState()
    val searchEntireStorage by viewModel.searchEntireStorage.collectAsState()
    val extensionFilter by viewModel.extensionFilter.collectAsState()
    val sizeFilter by viewModel.sizeFilter.collectAsState()
    val availableStorages by viewModel.availableStorages.collectAsState()
    val currentDir by viewModel.currentDir.collectAsState()

    var showMoveSelectedDialog by remember { mutableStateOf(false) }
    var showZipSelectedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentTab == MainNavTab.EXPLORER) {
                LishFileTopBar(
                    title = "LishFile",
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    isSearchActive = isSearchActive,
                    onToggleSearch = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) {
                            viewModel.setSearchQuery("")
                            viewModel.setExtensionFilter(null)
                        }
                    },
                    isGridView = isGridView,
                    onToggleGridView = { viewModel.preferences.setGridView(!isGridView) },
                    showHiddenFiles = showHiddenFiles,
                    onToggleHiddenFiles = {
                        val next = !showHiddenFiles
                        viewModel.preferences.setShowHiddenFiles(next)
                        viewModel.notifySnackbar(
                            if (next) "Berkas tersembunyi ditampilkan" else "Berkas tersembunyi disembunyikan"
                        )
                    },
                    onSortChange = { by, order -> viewModel.setSorting(by, order) },
                    isMultiSelect = isMultiSelectMode,
                    selectedCount = selectedPaths.size,
                    onSelectAll = { viewModel.selectAll(allFiles) },
                    onClearSelection = { viewModel.clearSelection() },
                    onCopySelected = { viewModel.copySelected() },
                    onCutSelected = { viewModel.cutSelected() },
                    onMoveSelected = { showMoveSelectedDialog = true },
                    onShareSelected = {
                        val filesToShare = selectedPaths.map { File(it) }
                        FileUtils.shareMultipleFiles(context, filesToShare)
                    },
                    onZipSelected = { showZipSelectedDialog = true },
                    onDeleteSelected = { viewModel.deleteSelected() },
                    searchEntireStorage = searchEntireStorage,
                    onToggleSearchEntireStorage = { viewModel.toggleSearchEntireStorage() },
                    selectedExtension = extensionFilter,
                    onSelectExtension = { viewModel.setExtensionFilter(it) },
                    selectedSizeFilter = sizeFilter,
                    onSelectSizeFilter = { viewModel.setSizeFilter(it) },
                    onLogoClick = handleLogoClick
                )
            }
        },
        bottomBar = {
            if (currentTab != MainNavTab.VAULT) {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    NavigationBarItem(
                        selected = currentTab == MainNavTab.EXPLORER,
                        onClick = { currentTab = MainNavTab.EXPLORER },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Berkas") },
                        label = { Text("Berkas") },
                        modifier = Modifier.testTag("nav_tab_explorer")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainNavTab.TRANSFER,
                        onClick = { currentTab = MainNavTab.TRANSFER },
                        icon = { Icon(Icons.Default.Wifi, contentDescription = "Transfer P2P") },
                        label = { Text("Transfer") },
                        modifier = Modifier.testTag("nav_tab_transfer")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainNavTab.SETTINGS,
                        onClick = { currentTab = MainNavTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                        label = { Text("Pengaturan") },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val mod = Modifier.padding(paddingValues)
        when (currentTab) {
            MainNavTab.EXPLORER -> FileManagerScreen(
                viewModel = viewModel,
                onOpenVault = { currentTab = MainNavTab.VAULT },
                modifier = mod
            )
            MainNavTab.VAULT -> SafeVaultScreen(
                viewModel = viewModel,
                onTriggerBiometrics = onTriggerBiometrics,
                onExitVault = { currentTab = MainNavTab.EXPLORER },
                modifier = mod
            )
            MainNavTab.TRANSFER -> LocalTransferScreen(
                viewModel = viewModel,
                modifier = mod
            )
            MainNavTab.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                modifier = mod
            )
        }
    }

    if (showMoveSelectedDialog) {
        val rootDir = availableStorages.firstOrNull()?.rootDir ?: currentDir
        MoveDestinationDialog(
            initialDirectory = currentDir,
            rootStorage = rootDir,
            title = "Pindahkan ${selectedPaths.size} File Dipilih",
            onDismiss = { showMoveSelectedDialog = false },
            onSelectDestination = { destFolder ->
                viewModel.moveSelectedTo(destFolder)
                showMoveSelectedDialog = false
            }
        )
    }

    if (showZipSelectedDialog) {
        val files = selectedPaths.map { File(it) }
        CompressZipDialog(
            defaultZipName = "Arsip_${System.currentTimeMillis()}.zip",
            onDismiss = { showZipSelectedDialog = false },
            onConfirm = { zipName ->
                viewModel.compressToZip(files, zipName)
                showZipSelectedDialog = false
                viewModel.clearSelection()
            }
        )
    }
}
