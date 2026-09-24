package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.FileItem
import com.example.ui.components.PinInputDialog
import com.example.ui.theme.ColorVault
import com.example.util.FileUtils
import com.example.viewmodel.FileManagerViewModel
import java.io.File

@Composable
fun SafeVaultScreen(
    viewModel: FileManagerViewModel,
    onTriggerBiometrics: () -> Unit,
    onExitVault: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val hasMasterPin by viewModel.preferences.hasMasterPin.collectAsState()
    val vaultFiles by viewModel.vaultFiles.collectAsState()

    var inputPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var restoreTargetFile by remember { mutableStateOf<File?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExitVault) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali ke Beranda"
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Brankas Rahasia (AES-256)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (!isVaultUnlocked) {
            // Locked screen: PIN / Biometric unlock
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ColorVault.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Brankas",
                        tint = ColorVault,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (hasMasterPin) "Buka Brankas Aman" else "Atur PIN Brankas Aman",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "File dienkripsi menggunakan standar militer AES-256",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = inputPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            inputPin = it
                            pinError = null
                        }
                    },
                    label = { Text("Masukkan 4–6 Digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = pinError != null,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .testTag("vault_screen_pin_input")
                )

                if (pinError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pinError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (inputPin.length < 4) {
                                pinError = "PIN minimal 4 digit"
                            } else {
                                if (hasMasterPin) {
                                    val ok = viewModel.unlockVaultWithPin(inputPin)
                                    if (!ok) pinError = "PIN salah!"
                                } else {
                                    viewModel.setupVaultPin(inputPin)
                                }
                            }
                        },
                        modifier = Modifier.testTag("vault_unlock_button")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (hasMasterPin) "Buka Brankas" else "Simpan PIN")
                    }

                    if (hasMasterPin) {
                        OutlinedButton(
                            onClick = onTriggerBiometrics,
                            modifier = Modifier.testTag("vault_biometric_button")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Biometrik")
                        }
                    }
                }
            }
        } else {
            // Unlocked screen: File list inside vault
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Brankas Aman",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${vaultFiles.size} file terenkripsi AES-256",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { viewModel.lockVault() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kunci")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (vaultFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Brankas masih kosong",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Di File Manager, tekan opsi file lalu pilih 'Kunci ke Brankas'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vaultFiles) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name.removeSuffix(".lish"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${FileUtils.formatFileSize(item.size)} • Terenkripsi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { restoreTargetFile = item.file }
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Pulihkan File", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteFile(item.file); viewModel.loadVaultFiles() }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    restoreTargetFile?.let { file ->
        PinInputDialog(
            title = "Pulihkan File: ${file.name.removeSuffix(".lish")}",
            subtitle = "Masukkan PIN untuk mendekripsi dan mengembalikan file ini ke folder aktif",
            confirmText = "Dekripsi & Pulihkan",
            onDismiss = { restoreTargetFile = null },
            onConfirm = { pin ->
                viewModel.restoreFromSafeVault(file, pin)
                restoreTargetFile = null
            }
        )
    }
}
