package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.crypto.AesCryptoEngine
import com.example.model.FileItem
import com.example.model.FileType
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.VaultGold
import com.example.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionBottomSheet(
    fileItem: FileItem,
    onDismiss: () -> Unit,
    onOpenExternal: () -> Unit,
    onPreview: () -> Unit,
    onEncrypt: () -> Unit,
    onDecrypt: () -> Unit,
    onLockFolder: () -> Unit,
    onMoveToVault: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onChecksum: () -> Unit,
    onCompressZip: () -> Unit = {},
    onExtractZip: () -> Unit = {},
    onEditImage: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                FileIconBadge(fileItem = fileItem, size = 42)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = fileItem.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${fileItem.formattedSize} • ${fileItem.formattedDate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Items
            if (!fileItem.isDirectory) {
                val isImage = fileItem.fileType == FileType.IMAGE ||
                    fileItem.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

                if (isImage) {
                    ActionRow(Icons.Default.Edit, "Edit Gambar (Putar, Pangkas, Filter)", "edit_image_action", onEditImage, tint = MaterialTheme.colorScheme.primary)
                }
                ActionRow(Icons.Default.OpenInNew, "Buka dengan Aplikasi Lain", "open_external_action", onOpenExternal)
                ActionRow(Icons.Default.Visibility, if (isImage) "Lihat Gambar Penuh" else "Pratinjau / Editor Teks", "preview_action", onPreview)

                if (fileItem.isEncrypted) {
                    ActionRow(Icons.Default.LockOpen, "Dekripsi File (AES-256)", "decrypt_action", onDecrypt, tint = SecureGreen)
                } else {
                    ActionRow(Icons.Default.Shield, "Enkripsi dengan AES-256", "encrypt_action", onEncrypt, tint = MaterialTheme.colorScheme.primary)
                }

                ActionRow(Icons.Default.Lock, "Pindahkan ke Folder Terkunci (Vault)", "move_to_vault_action", onMoveToVault, tint = VaultGold)
            } else {
                ActionRow(Icons.Default.Lock, "Kunci Folder Ini dengan Kata Sandi", "lock_folder_action", onLockFolder, tint = VaultGold)
            }

            // Archive actions (ZIP create / extract)
            if (fileItem.name.endsWith(".zip", ignoreCase = true) || fileItem.fileType == FileType.ARCHIVE) {
                ActionRow(Icons.Default.Archive, "Ekstrak Arsip ZIP", "extract_zip_action", onExtractZip)
            } else {
                ActionRow(Icons.Default.Archive, "Kompres ke Arsip ZIP", "compress_zip_action", onCompressZip)
            }

            ActionRow(Icons.Default.Share, "Kirim via Bluetooth / Jaringan", "share_action", onShare)
            ActionRow(Icons.Default.ContentCopy, "Salin File", "copy_action", onCopy)
            ActionRow(Icons.Default.DriveFileMove, "Pindahkan File", "move_action", onMove)
            ActionRow(Icons.Default.DriveFileRenameOutline, "Ubah Nama", "rename_action", onRename)
            ActionRow(Icons.Default.Info, "Periksa Checksum SHA-256 / Info", "checksum_action", onChecksum)

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

            ActionRow(Icons.Default.Delete, "Hapus", "delete_action", onDelete, tint = DangerRed)
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    tag: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = tint)
    }
}

@Composable
fun EncryptDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: (password: String, deleteOriginal: Boolean) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var deleteOriginal by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enkripsi AES-256", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Enkripsi '$fileName' dengan algoritma AES-256 terstandarisasi keamanan tinggi.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorText = null },
                    label = { Text("Kata Sandi Enkripsi") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("encrypt_password_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorText = null },
                    label = { Text("Konfirmasi Kata Sandi") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("encrypt_confirm_password_input")
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorText!!, color = DangerRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { deleteOriginal = !deleteOriginal }
                ) {
                    Checkbox(
                        checked = deleteOriginal,
                        onCheckedChange = { deleteOriginal = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hapus file asli setelah enkripsi selesai", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.length < 4) {
                        errorText = "Kata sandi minimal 4 karakter"
                    } else if (password != confirmPassword) {
                        errorText = "Konfirmasi kata sandi tidak cocok!"
                    } else {
                        onConfirm(password, deleteOriginal)
                    }
                },
                modifier = Modifier.testTag("confirm_encrypt_button")
            ) {
                Text("Enkripsi Sekarang")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun DecryptDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: (password: String, deleteEncrypted: Boolean) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var deleteEncrypted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockOpen, contentDescription = null, tint = SecureGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dekripsi File", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Masukkan kata sandi yang digunakan saat mengenkripsi '$fileName'.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Kata Sandi") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("decrypt_password_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { deleteEncrypted = !deleteEncrypted }
                ) {
                    Checkbox(
                        checked = deleteEncrypted,
                        onCheckedChange = { deleteEncrypted = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hapus file .lish setelah didekripsi", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password, deleteEncrypted) },
                enabled = password.isNotBlank(),
                modifier = Modifier.testTag("confirm_decrypt_button")
            ) {
                Text("Dekripsi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun LockFolderDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onConfirm: (password: String?, allowBiometric: Boolean) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var allowBiometric by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = VaultGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kunci Folder", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Lindungi folder '$folderName' dari akses tidak sah. Folder hanya dapat dibuka dengan PIN / Sidik Jari.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Kata Sandi Khusus (Opsional)") },
                    placeholder = { Text("Kosongkan untuk pakai PIN Master") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("folder_lock_password_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { allowBiometric = !allowBiometric }
                ) {
                    Checkbox(
                        checked = allowBiometric,
                        onCheckedChange = { allowBiometric = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Izinkan pembukaan dengan Sidik Jari (Biometrik)", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(if (password.isBlank()) null else password, allowBiometric) },
                modifier = Modifier.testTag("confirm_lock_folder_button")
            ) {
                Text("Kunci Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun PinAuthDialog(
    title: String = "Autentikasi Keamanan",
    subtitle: String = "Masukkan PIN atau gunakan Sidik Jari untuk membuka",
    canUseBiometric: Boolean = true,
    onDismiss: () -> Unit,
    onPinSubmit: (String) -> Unit,
    onBiometricClick: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 12) pin = it },
                    label = { Text("PIN / Kata Sandi") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("auth_pin_input")
                )

                if (canUseBiometric) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onBiometricClick,
                        modifier = Modifier.fillMaxWidth().testTag("biometric_auth_button")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gunakan Sidik Jari")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPinSubmit(pin) },
                enabled = pin.isNotBlank(),
                modifier = Modifier.testTag("confirm_pin_button")
            ) {
                Text("Buka")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun NewItemDialog(
    onDismiss: () -> Unit,
    onCreateFolder: (name: String) -> Unit,
    onCreateTextFile: (name: String, content: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var folderName by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Buat Baru", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Folder") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Dokumen Teks") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Nama Folder") },
                        placeholder = { Text("contoh: Laporan_2026") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_folder_name_input")
                    )
                } else {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("Nama File (.txt)") },
                        placeholder = { Text("contoh: Catatan_Aman") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_file_name_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = fileContent,
                        onValueChange = { fileContent = it },
                        label = { Text("Isi Dokumen (Opsional)") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag("new_file_content_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0 && folderName.isNotBlank()) {
                        onCreateFolder(folderName)
                    } else if (selectedTab == 1 && fileName.isNotBlank()) {
                        onCreateTextFile(fileName, fileContent)
                    }
                },
                enabled = (selectedTab == 0 && folderName.isNotBlank()) || (selectedTab == 1 && fileName.isNotBlank()),
                modifier = Modifier.testTag("confirm_create_new_item_button")
            ) {
                Text("Buat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun FilePreviewDialog(
    file: File,
    onDismiss: () -> Unit,
    onSaveContent: (String) -> Unit,
    onFileUpdated: ((File) -> Unit)? = null
) {
    var activeFile by remember(file) { mutableStateOf(file) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var showImageEditor by remember { mutableStateOf(false) }

    val extension = activeFile.extension.lowercase()
    val isImage = extension in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    val isText = extension in listOf("txt", "md", "json", "xml", "kt", "java", "csv", "log", "html", "css", "js", "conf", "properties")
    val isAudio = extension in listOf("mp3", "wav", "m4a", "ogg", "flac", "aac")
    val isVideo = extension in listOf("mp4", "mkv", "webm", "avi", "3gp")
    val isPdf = extension == "pdf"

    var textContent by remember { mutableStateOf("") }
    var isLoadingText by remember { mutableStateOf(isText) }
    var isEditing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(activeFile) {
        if (isText) {
            textContent = FileUtils.readTextFile(activeFile)
            isLoadingText = false
        }
    }

    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeFile.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (isImage) {
                        Button(
                            onClick = { showImageEditor = true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("open_image_editor_top_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isText) {
                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Visibility else Icons.Default.Edit,
                                contentDescription = if (isEditing) "Mode Pratinjau" else "Mode Edit"
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${com.example.model.StorageStats.formatBytes(activeFile.length())} • ${dateFormat.format(java.util.Date(activeFile.lastModified()))} • .${extension.uppercase()}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isImage -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F141C)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(activeFile)
                                        .setParameter("key", refreshKey, memoryCacheKey = null)
                                        .build(),
                                    contentDescription = activeFile.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showImageEditor = true },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit Gambar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        FileUtils.openFileWithExternalApp(context, activeFile)
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = "Buka dengan aplikasi lain", modifier = Modifier.size(16.dp))
                                }
                                OutlinedButton(
                                    onClick = {
                                        FileUtils.shareMultipleFiles(context, listOf(activeFile))
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Bagikan", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    isAudio -> {
                        AudioPlayerView(file = activeFile)
                    }
                    isPdf -> {
                        PdfViewer(file = activeFile)
                    }
                    isVideo -> {
                        VideoLaunchView(file = activeFile)
                    }
                    isText -> {
                        if (isLoadingText) {
                            CircularProgressIndicator()
                        } else if (isEditing) {
                            OutlinedTextField(
                                value = textContent,
                                onValueChange = { textContent = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("text_editor_input"),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = textContent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Pratinjau langsung tidak tersedia untuk format ini.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Ukuran: ${com.example.model.StorageStats.formatBytes(activeFile.length())}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isText && isEditing) {
                Button(
                    onClick = {
                        onSaveContent(textContent)
                        isEditing = false
                    },
                    modifier = Modifier.testTag("save_text_content_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Tutup")
                }
            }
        },
        dismissButton = {
            if (isText && isEditing) {
                TextButton(onClick = { isEditing = false }) {
                    Text("Batal")
                }
            }
        }
    )

    if (showImageEditor) {
        ImageEditorDialog(
            file = activeFile,
            onDismiss = { showImageEditor = false },
            onSaveSuccess = { savedFile ->
                showImageEditor = false
                activeFile = savedFile
                refreshKey++
                onFileUpdated?.invoke(savedFile)
            }
        )
    }
}

@Composable
fun CreateZipDialog(
    defaultZipName: String,
    onDismiss: () -> Unit,
    onConfirm: (zipName: String) -> Unit
) {
    var zipName by remember { mutableStateOf(defaultZipName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buat Arsip ZIP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text("Masukkan nama untuk file arsip ZIP baru:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = zipName,
                    onValueChange = { zipName = it },
                    label = { Text("Nama Arsip (.zip)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("zip_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(zipName) },
                enabled = zipName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_zip_button")
            ) {
                Text("Kompres")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ubah Nama", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text("Masukkan nama baru untuk berkas/folder:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nama Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank() && newName != currentName,
                modifier = Modifier.testTag("confirm_rename_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    itemCount: Int,
    itemName: String = "",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Konfirmasi Hapus", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            val message = if (itemCount == 1 && itemName.isNotEmpty()) {
                "Apakah Anda yakin ingin menghapus '$itemName' secara permanen?"
            } else {
                "Apakah Anda yakin ingin menghapus $itemCount item terpilih secara permanen?"
            }
            Text(message, fontSize = 14.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                modifier = Modifier.testTag("confirm_delete_dialog_button")
            ) {
                Text("Hapus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun ConfirmSystemFilesWarningDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = DangerRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Peringatan Berkas Sistem", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                text = "PERINGATAN: Berkas sistem sangat penting untuk kestabilan aplikasi dan sistem operasi Android. Mengubah, memindahkan, atau menghapus berkas sistem dapat menyebabkan sistem tidak stabil atau kehilangan data penting. Lanjutkan hanya jika Anda memahami risikonya.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                modifier = Modifier.testTag("confirm_system_files_warning_button")
            ) {
                Text("Saya Mengerti, Tampilkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun ChecksumDialog(
    file: File,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var sha256 by remember { mutableStateOf("Menghitung...") }
    var md5 by remember { mutableStateOf("Menghitung...") }

    LaunchedEffect(file) {
        sha256 = AesCryptoEngine.calculateChecksum(file, "SHA-256")
        md5 = AesCryptoEngine.calculateMd5(file)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Informasi & Checksum", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                InfoItem("Nama File", file.name)
                InfoItem("Lokasi", file.absolutePath)
                InfoItem("Ukuran", com.example.model.StorageStats.formatBytes(file.length()))
                InfoItem("MIME Type", FileUtils.getMimeType(file))
                InfoItem("Terenkripsi AES", if (file.name.endsWith(".lish")) "Ya (AES-256-GCM/CBC)" else "Tidak")

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text("SHA-256 Checksum:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(sha256, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            val clip = ClipData.newPlainText("SHA256", sha256)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, "SHA-256 disalin", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin SHA-256", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("MD5 Checksum:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(md5, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            val clip = ClipData.newPlainText("MD5", md5)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, "MD5 disalin", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin MD5", modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DestinationPickerDialog(
    rootDir: File,
    onDismiss: () -> Unit,
    onDestinationSelected: (File) -> Unit
) {
    var currentBrowseDir by remember { mutableStateOf(rootDir) }
    val subfolders = remember(currentBrowseDir) {
        currentBrowseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pilih Folder Tujuan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                Text("Lokasi saat ini: ${currentBrowseDir.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(10.dp))

                if (currentBrowseDir != rootDir) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentBrowseDir = currentBrowseDir.parentFile ?: rootDir }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(".. (Kembali ke folder atas)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider()
                }

                if (subfolders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada subfolder. Simpan di folder ini?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(subfolders) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentBrowseDir = folder }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(folder.name, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDestinationSelected(currentBrowseDir) },
                modifier = Modifier.testTag("confirm_destination_button")
            ) {
                Text("Pilih '${currentBrowseDir.name}'")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
