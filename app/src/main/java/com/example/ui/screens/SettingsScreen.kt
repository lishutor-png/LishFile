package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.VaultGold
import com.example.viewmodel.FileManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FileManagerViewModel,
    themeMode: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit
) {
    val context = LocalContext.current

    val showHiddenFiles by viewModel.preferences.showHiddenFiles.collectAsState()
    val showSystemFiles by viewModel.preferences.showSystemFiles.collectAsState()
    val isBiometricEnabled by viewModel.preferences.isBiometricEnabled.collectAsState()
    val hasMasterPin by viewModel.preferences.hasMasterPin.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showDeviceNameDialog by remember { mutableStateOf(false) }
    var showAesInfoDialog by remember { mutableStateOf(false) }
    var showSystemWarningDialog by remember { mutableStateOf(false) }

    var currentDeviceName by remember { mutableStateOf(viewModel.preferences.getDeviceName()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pengaturan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Tampilan (Appearance)
            item {
                SectionHeader("Tampilan & Antarmuka")
            }

            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = "Tema Aplikasi",
                        subtitle = when (themeMode) {
                            AppThemeMode.SYSTEM -> "Ikuti Sistem"
                            AppThemeMode.LIGHT -> "Mode Terang (Light)"
                            AppThemeMode.DARK -> "Mode Gelap (Dark)"
                        },
                        tag = "settings_theme_row",
                        onClick = { showThemeDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Tampilkan File Tersembunyi", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("File dan folder dengan awalan titik (.)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = showHiddenFiles,
                            onCheckedChange = { viewModel.toggleHiddenFiles() },
                            modifier = Modifier.testTag("show_hidden_files_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Tampilkan File Sistem", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("File dan direktori internal sistem (berisiko)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = showSystemFiles,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    showSystemWarningDialog = true
                                } else {
                                    viewModel.setShowSystemFiles(false)
                                }
                            },
                            modifier = Modifier.testTag("show_system_files_switch")
                        )
                    }
                }
            }

            // Keamanan (Security)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Keamanan & Proteksi")
            }

            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Key,
                        title = if (hasMasterPin) "Ubah PIN Master Vault" else "Atur PIN Master Vault",
                        subtitle = if (hasMasterPin) "PIN aktif untuk membuka folder terkunci" else "Belum diatur",
                        tag = "settings_pin_row",
                        onClick = { showSetPinDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Buka dengan Sidik Jari", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("Gunakan sensor biometrik perangkat", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { viewModel.preferences.setBiometricEnabled(it) },
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    SettingsRow(
                        icon = Icons.Default.Shield,
                        title = "Tentang Enkripsi AES-256",
                        subtitle = "Kunci 256-bit PBKDF2 HMAC-SHA256 • Streaming 64KB",
                        tag = "settings_aes_info_row",
                        onClick = { showAesInfoDialog = true }
                    )
                }
            }

            // Transfer & Perangkat (Device & Network)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Jaringan & Transfer Lokal")
            }

            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Nama Perangkat",
                        subtitle = currentDeviceName,
                        tag = "settings_device_name_row",
                        onClick = { showDeviceNameDialog = true }
                    )
                }
            }

            // Branding & Info (About)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Tentang File Manager +")
            }

            item {
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("File Manager + v3.4.1", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("File Manager Offline & Aman", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Didesain dengan mengutamakan privasi 100% tanpa server eksternal, enkripsi AES-256 terstandarisasi, dan transfer P2P lokal.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Theme Mode Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Pilih Tema Aplikasi") },
            text = {
                Column {
                    ThemeOptionRow("Ikuti Tema Sistem", themeMode == AppThemeMode.SYSTEM) {
                        onThemeChange(AppThemeMode.SYSTEM)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Mode Terang (Light)", themeMode == AppThemeMode.LIGHT) {
                        onThemeChange(AppThemeMode.LIGHT)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Mode Gelap (Dark)", themeMode == AppThemeMode.DARK) {
                        onThemeChange(AppThemeMode.DARK)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Set Master PIN Dialog
    if (showSetPinDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text("Atur PIN Master Vault") },
            text = {
                Column {
                    Text("PIN ini digunakan untuk membuka folder terkunci jika sidik jari tidak tersedia.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it; pinError = null },
                        label = { Text("PIN Baru (4-6 Angka)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("new_master_pin_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it; pinError = null },
                        label = { Text("Konfirmasi PIN Baru") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_master_pin_input")
                    )

                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(pinError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length < 4) {
                            pinError = "PIN minimal 4 angka"
                        } else if (newPin != confirmPin) {
                            pinError = "Konfirmasi PIN tidak cocok"
                        } else {
                            viewModel.preferences.setMasterPin(newPin)
                            Toast.makeText(context, "PIN Master Vault berhasil disimpan", Toast.LENGTH_SHORT).show()
                            showSetPinDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_master_pin_button")
                ) {
                    Text("Simpan PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Device Name Dialog
    if (showDeviceNameDialog) {
        var tempName by remember { mutableStateOf(currentDeviceName) }

        AlertDialog(
            onDismissRequest = { showDeviceNameDialog = false },
            title = { Text("Nama Perangkat") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Nama Perangkat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            viewModel.preferences.setDeviceName(tempName)
                            currentDeviceName = tempName
                        }
                        showDeviceNameDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceNameDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // AES Info Dialog
    if (showAesInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAesInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = SecureGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Spesifikasi Keamanan AES-256", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("• Standar Kriptografi: Advanced Encryption Standard (AES) 256-bit.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Key Derivation: PBKDF2WithHmacSHA256 dengan 10.000 iterasi dan random salt 16-byte cryptographically secure.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Mode Operasi: CBC dengan PKCS5Padding dan Inisialisasi Vektor (IV) 16-byte unik per file.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Optimasi Streaming: Buffer chunk 64KB I/O, mampu memproses file besar tanpa membebani memori RAM perangkat.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Privasi: Seluruh kunci dan data tetap berada di penyimpanan internal perangkat Anda.", fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showAesInfoDialog = false }) {
                    Text("Mengerti")
                }
            }
        )
    }

    // System Files Warning Dialog
    if (showSystemWarningDialog) {
        AlertDialog(
            onDismissRequest = { showSystemWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Peringatan File Sistem",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Menampilkan file sistem dapat memaparkan direktori internal perangkat. Menghapus, mengubah, atau memindahkan berkas di folder sistem berisiko mengganggu kestabilan sistem atau aplikasi.\n\nApakah Anda yakin ingin mengaktifkannya?",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSystemWarningDialog = false
                        viewModel.setShowSystemFiles(true)
                    }
                ) {
                    Text("Ya, Tampilkan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSystemWarningDialog = false
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 14.sp)
    }
}
