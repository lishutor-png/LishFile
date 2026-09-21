package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.ui.components.FileListItem
import com.example.ui.components.FilePreviewDialog
import com.example.ui.components.PinAuthDialog
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.VaultGold
import com.example.util.FileUtils
import com.example.viewmodel.FileManagerViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeVaultScreen(
    viewModel: FileManagerViewModel,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val isVaultUnlocked by viewModel.securityManager.isVaultUnlocked.collectAsState()
    val hasMasterPin by viewModel.preferences.hasMasterPin.collectAsState()
    val lockedFoldersList by viewModel.lockedFolders.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var fileToPreview by remember { mutableStateOf<File?>(null) }

    // Vault directory files
    val vaultFiles = remember(isVaultUnlocked) {
        val dir = viewModel.securityManager.vaultDirectory
        if (dir.exists()) {
            dir.listFiles()?.map { FileItem.fromFile(it, isLocked = false) } ?: emptyList()
        } else emptyList()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFileFromUri(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("vault_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke File Manager"
                            )
                        }
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isVaultUnlocked) SecureGreen else VaultGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Folder Terkunci (Safe Vault)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (isVaultUnlocked) {
                        OutlinedButton(
                            onClick = { viewModel.securityManager.lockVault() },
                            modifier = Modifier.testTag("lock_vault_now_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kunci Kembali", fontSize = 12.sp)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isVaultUnlocked) {
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    containerColor = VaultGold,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("vault_import_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah file ke vault")
                }
            }
        }
    ) { innerPadding ->
        if (!isVaultUnlocked) {
            // Locked Vault state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(VaultGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield Lock",
                                tint = VaultGold,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Area Penyimpanan Terproteksi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Dokumen sensitif di sini dilindungi oleh enkripsi AES-256 tingkat militer. Buka menggunakan sidik jari atau PIN Master.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        if (viewModel.securityManager.canUseBiometric()) {
                            Button(
                                onClick = {
                                    if (activity != null) {
                                        viewModel.unlockWithBiometrics(activity, null) {}
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("vault_biometric_unlock_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buka dengan Sidik Jari")
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                if (!hasMasterPin) {
                                    showSetPinDialog = true
                                } else {
                                    showPinDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("vault_pin_unlock_button")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (hasMasterPin) "Gunakan PIN Master" else "Atur PIN Master Baru")
                        }
                    }
                }
            }
        } else {
            // Unlocked Vault state
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    // Security status banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SecureGreen.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = SecureGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Safe Vault Terbuka",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SecureGreen
                                )
                                Text(
                                    text = "File dienkripsi saat disimpan ke media penyimpanan",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Section 1: Locked Folders registered in app
                item {
                    Text(
                        text = "Folder Terkunci Aktif (${lockedFoldersList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (lockedFoldersList.isEmpty()) {
                    item {
                        Text(
                            text = "Belum ada folder yang dikunci. Klik opsi (⋮) pada folder manapun di File Manager untuk menguncinya.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                } else {
                    items(lockedFoldersList) { lockedFolder ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = VaultGold, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(lockedFolder.folderName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(lockedFolder.folderPath, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                IconButton(onClick = { viewModel.securityManager.unlockFolder(lockedFolder.folderPath) }) {
                                    Icon(Icons.Default.LockOpen, contentDescription = "Buka Kunci", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // Section 2: Isolated Vault Files
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "File di Safe Vault (${vaultFiles.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (vaultFiles.isEmpty()) {
                    item {
                        Text(
                            text = "Belum ada file di dalam Vault. Gunakan tombol '+' di bawah atau pilih 'Pindahkan ke Folder Terkunci' pada file apa saja.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(vaultFiles) { fileItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { fileToPreview = fileItem.file }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(fileItem.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("${fileItem.formattedSize} • ${fileItem.formattedDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { viewModel.restoreFromVault(fileItem.file) },
                                    modifier = Modifier.testTag("restore_vault_file_${fileItem.name}")
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = "Kembalikan ke File Manager", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Pin Authentication Dialog
    if (showPinDialog) {
        PinAuthDialog(
            title = "Buka Safe Vault",
            subtitle = "Masukkan PIN Master Anda",
            canUseBiometric = viewModel.securityManager.canUseBiometric(),
            onDismiss = { showPinDialog = false },
            onPinSubmit = { pin ->
                viewModel.unlockVaultWithMasterPin(pin) {
                    showPinDialog = false
                }
            },
            onBiometricClick = {
                if (activity != null) {
                    viewModel.unlockWithBiometrics(activity, null) {
                        showPinDialog = false
                    }
                }
            }
        )
    }

    // Set Master PIN Dialog (if user doesn't have one)
    if (showSetPinDialog) {
        PinAuthDialog(
            title = "Buat PIN Master",
            subtitle = "Buat PIN 4-6 digit untuk melindungi Safe Vault Anda",
            canUseBiometric = false,
            onDismiss = { showSetPinDialog = false },
            onPinSubmit = { pin ->
                viewModel.preferences.setMasterPin(pin)
                viewModel.securityManager.unlockVault()
                showSetPinDialog = false
            },
            onBiometricClick = {}
        )
    }

    // File Preview
    fileToPreview?.let { file ->
        FilePreviewDialog(
            file = file,
            onDismiss = { fileToPreview = null },
            onSaveContent = { content ->
                viewModel.saveTextFile(file, content) {
                    fileToPreview = null
                }
            }
        )
    }
}
