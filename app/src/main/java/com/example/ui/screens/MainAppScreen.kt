package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.PinAuthDialog
import com.example.ui.theme.AppThemeMode
import com.example.viewmodel.FileManagerViewModel
import kotlinx.coroutines.flow.collectLatest

enum class NavigationTab {
    EXPLORER,
    TRANSFER,
    SETTINGS
}

@Composable
fun MainAppScreen(
    viewModel: FileManagerViewModel,
    themeMode: AppThemeMode,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableStateOf(NavigationTab.EXPLORER) }

    // Vault overlay/screen state
    var isVaultOpen by remember { mutableStateOf(false) }
    var showVaultAuthDialog by remember { mutableStateOf(false) }
    var showVaultSetPinDialog by remember { mutableStateOf(false) }

    val isVaultUnlocked by viewModel.securityManager.isVaultUnlocked.collectAsState()
    val hasMasterPin by viewModel.preferences.hasMasterPin.collectAsState()

    // Function triggered when user clicks the LishFile logo
    val onLogoClick: () -> Unit = {
        if (isVaultUnlocked) {
            isVaultOpen = true
        } else if (!hasMasterPin) {
            // First time clicking logo: prompt user to set Master Password/PIN
            showVaultSetPinDialog = true
        } else {
            // Prompt to authenticate with PIN or Biometrics
            showVaultAuthDialog = true
        }
    }

    // Listen to ViewModel snackbar events
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Intercept back button when in vault or subfolder
    val currentDir by viewModel.currentDir.collectAsState()
    val isRoot = currentDir.absolutePath == viewModel.primaryRootDir.absolutePath

    BackHandler(enabled = isVaultOpen) {
        isVaultOpen = false
    }

    BackHandler(enabled = !isVaultOpen && currentTab == NavigationTab.EXPLORER && !isRoot) {
        viewModel.navigateUp()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isVaultOpen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.EXPLORER,
                        onClick = { currentTab = NavigationTab.EXPLORER },
                        icon = {
                            Icon(
                                if (currentTab == NavigationTab.EXPLORER) Icons.Filled.Folder else Icons.Outlined.Folder,
                                contentDescription = "File Manager"
                            )
                        },
                        label = { Text("Berkas", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_item_explorer"),
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == NavigationTab.TRANSFER,
                        onClick = { currentTab = NavigationTab.TRANSFER },
                        icon = {
                            Icon(
                                if (currentTab == NavigationTab.TRANSFER) Icons.Filled.WifiTethering else Icons.Outlined.WifiTethering,
                                contentDescription = "Transfer Lokal"
                            )
                        },
                        label = { Text("Transfer P2P", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_item_transfer"),
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == NavigationTab.SETTINGS,
                        onClick = { currentTab = NavigationTab.SETTINGS },
                        icon = {
                            Icon(
                                if (currentTab == NavigationTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Pengaturan"
                            )
                        },
                        label = { Text("Pengaturan", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_item_settings"),
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isVaultOpen) {
                SafeVaultScreen(
                    viewModel = viewModel,
                    onBackClick = { isVaultOpen = false }
                )
            } else {
                when (currentTab) {
                    NavigationTab.EXPLORER -> FileManagerScreen(
                        viewModel = viewModel,
                        themeMode = themeMode,
                        onThemeToggle = onThemeToggle,
                        onOpenVault = onLogoClick
                    )
                    NavigationTab.TRANSFER -> LocalTransferScreen(
                        viewModel = viewModel
                    )
                    NavigationTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        themeMode = themeMode,
                        onThemeChange = { viewModel.setThemeMode(it) }
                    )
                }
            }
        }
    }

    // Dialog for setting master PIN for the first time when clicking LishFile logo
    if (showVaultSetPinDialog) {
        PinAuthDialog(
            title = "Atur Kata Sandi / PIN Pertama Kali",
            subtitle = "Anda baru pertama kali mengakses folder terkunci. Buat PIN atau kata sandi untuk melindungi Safe Vault Anda (dapat diubah nanti di Pengaturan).",
            canUseBiometric = false,
            onDismiss = { showVaultSetPinDialog = false },
            onPinSubmit = { pin ->
                viewModel.preferences.setMasterPin(pin)
                viewModel.securityManager.unlockVault()
                showVaultSetPinDialog = false
                isVaultOpen = true
            },
            onBiometricClick = {}
        )
    }

    // Dialog for authenticating to access Safe Vault via logo
    if (showVaultAuthDialog) {
        PinAuthDialog(
            title = "Buka Berkas Terkunci",
            subtitle = "Masukkan PIN atau gunakan Sidik Jari untuk membuka Safe Vault",
            canUseBiometric = viewModel.securityManager.canUseBiometric(),
            onDismiss = { showVaultAuthDialog = false },
            onPinSubmit = { pin ->
                viewModel.unlockVaultWithMasterPin(pin) {
                    showVaultAuthDialog = false
                    isVaultOpen = true
                }
            },
            onBiometricClick = {
                if (activity != null) {
                    viewModel.unlockWithBiometrics(activity, null) {
                        showVaultAuthDialog = false
                        isVaultOpen = true
                    }
                }
            }
        )
    }
}

