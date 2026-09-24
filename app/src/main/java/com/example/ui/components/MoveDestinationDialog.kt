package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File

@Composable
fun MoveDestinationDialog(
    initialDir: File = File("/"),
    initialDirectory: File = initialDir,
    rootStorage: File? = null,
    title: String = "Pilih Folder Tujuan",
    onDismiss: () -> Unit,
    onConfirmMove: ((destinationDir: File) -> Unit)? = null,
    onSelectDestination: ((destinationDir: File) -> Unit)? = null
) {
    val callback = onConfirmMove ?: onSelectDestination ?: {}
    var currentFolder by remember { mutableStateOf(if (initialDirectory != File("/")) initialDirectory else initialDir) }
    var subFolders by remember { mutableStateOf<List<File>>(emptyList()) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    fun refreshFolders(folder: File) {
        val dirs = folder.listFiles()?.filter { it.isDirectory && !com.example.util.FileUtils.isHiddenOrInHiddenFolder(it) }?.sortedBy { it.name.lowercase() } ?: emptyList()
        subFolders = dirs
    }

    LaunchedEffect(currentFolder) {
        refreshFolders(currentFolder)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Title and Up Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val parent = currentFolder.parentFile
                    if (parent != null) {
                        IconButton(onClick = { currentFolder = parent }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Folder Induk")
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentFolder.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Folder Baru")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                // List of subdirectories
                Box(modifier = Modifier.weight(1f)) {
                    if (subFolders.isEmpty()) {
                        Text(
                            text = "Tidak ada sub-folder di sini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subFolders) { folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currentFolder = folder }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Confirm / Cancel buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            callback(currentFolder)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pindah ke Sini")
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                val newDir = File(currentFolder, name)
                if (!newDir.exists()) newDir.mkdirs()
                refreshFolders(currentFolder)
                showCreateFolderDialog = false
            }
        )
    }
}
