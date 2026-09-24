package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.AesCryptoEngine
import com.example.model.FileItem
import com.example.util.FileUtils

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Folder Baru") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Nama Folder") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_folder_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onConfirm(folderName.trim())
                    }
                },
                enabled = folderName.isNotBlank(),
                modifier = Modifier.testTag("create_folder_confirm")
            ) {
                Text("Buat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun RenameFileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Nama") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nama Baru") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank() && newName != currentName) {
                        onConfirm(newName.trim())
                    }
                },
                enabled = newName.isNotBlank() && newName != currentName,
                modifier = Modifier.testTag("rename_confirm")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun CompressZipDialog(
    defaultZipName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var zipName by remember { mutableStateOf(defaultZipName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kompres ke ZIP") },
        text = {
            OutlinedTextField(
                value = zipName,
                onValueChange = { zipName = it },
                label = { Text("Nama File ZIP") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (zipName.isNotBlank()) onConfirm(zipName.trim())
                },
                enabled = zipName.isNotBlank()
            ) {
                Text("Kompres")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun FileDetailsDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    var md5Checksum by remember { mutableStateOf<String?>("Menghitung...") }
    var sha256Checksum by remember { mutableStateOf<String?>("Menghitung...") }

    LaunchedEffect(item) {
        if (!item.isDirectory) {
            md5Checksum = AesCryptoEngine.calculateChecksum(item.file, "MD5")
            sha256Checksum = AesCryptoEngine.calculateChecksum(item.file, "SHA-256")
        } else {
            md5Checksum = "-"
            sha256Checksum = "-"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detail File") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow(label = "Nama", value = item.name)
                DetailRow(label = "Lokasi", value = item.path)
                DetailRow(label = "Tipe", value = if (item.isDirectory) "Folder" else item.fileType.name)
                DetailRow(label = "Ukuran", value = FileUtils.formatFileSize(item.size))
                DetailRow(label = "Dimodifikasi", value = FileUtils.formatDate(item.lastModified))
                DetailRow(label = "Dapat Dibaca", value = if (item.file.canRead()) "Ya" else "Tidak")
                DetailRow(label = "Dapat Ditulis", value = if (item.file.canWrite()) "Ya" else "Tidak")

                if (!item.isDirectory) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Checksum Keamanan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    DetailRow(label = "MD5", value = md5Checksum ?: "-")
                    DetailRow(label = "SHA-256", value = sha256Checksum ?: "-")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (label.contains("MD5") || label.contains("SHA")) FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
fun TextEditorDialog(
    fileName: String,
    initialContent: String,
    isNewFile: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, content: String) -> Unit
) {
    var name by remember { mutableStateOf(fileName) }
    var content by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isNewFile) "Buat File Teks" else "Edit File: $fileName")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isNewFile) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama File (.txt)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Konten") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    maxLines = 20
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name.trim(), content)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun PinInputDialog(
    title: String,
    subtitle: String = "Masukkan 4–6 digit PIN untuk melanjutkan",
    confirmText: String = "Lanjutkan",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            pin = it
                            errorMsg = null
                        }
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_pin_input"),
                    isError = errorMsg != null
                )
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length in 4..6) {
                        onConfirm(pin)
                    } else {
                        errorMsg = "PIN harus 4 sampai 6 angka"
                    }
                },
                enabled = pin.length >= 4,
                modifier = Modifier.testTag("vault_pin_confirm")
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
