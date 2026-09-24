package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DuplicateScanResult
import com.example.model.DuplicateScopeType
import com.example.util.FileUtils
import java.io.File

@Composable
fun DuplicateScannerDialog(
    currentDir: File,
    storageRoot: File,
    result: DuplicateScanResult,
    onStartScan: (targetFolders: List<File>, scopeDesc: String) -> Unit,
    onToggleSelect: (checksum: String, path: String) -> Unit,
    onCleanDuplicates: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedScope by remember { mutableStateOf(DuplicateScopeType.CURRENT_FOLDER) }

    // List of common top folders in storage for multi-selection
    val commonFolders = remember(storageRoot) {
        val candidates = storageRoot.listFiles()?.filter {
            it.isDirectory && !com.example.util.FileUtils.isHiddenOrInHiddenFolder(it)
        }?.sortedBy { it.name.lowercase() } ?: emptyList()
        candidates
    }

    val selectedMultipleFolders = remember { mutableStateListOf<File>() }

    // Initialize with currentDir and common folders if empty
    LaunchedEffect(Unit) {
        if (selectedMultipleFolders.isEmpty() && commonFolders.isNotEmpty()) {
            commonFolders.take(3).forEach { selectedMultipleFolders.add(it) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Pencarian File Duplikat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Cari file ganda di folder tertentu / seluruhnya", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            ) {
                // Scope Selection Tabs/Chips
                Text(
                    text = "Pilih Cakupan Pindai:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedScope == DuplicateScopeType.CURRENT_FOLDER,
                        onClick = { selectedScope = DuplicateScopeType.CURRENT_FOLDER },
                        label = { Text("Folder Ini", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedScope == DuplicateScopeType.SELECTED_FOLDERS,
                        onClick = { selectedScope = DuplicateScopeType.SELECTED_FOLDERS },
                        label = { Text("Pilih Folder", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedScope == DuplicateScopeType.ENTIRE_STORAGE,
                        onClick = { selectedScope = DuplicateScopeType.ENTIRE_STORAGE },
                        label = { Text("Semua", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Detail of chosen scope if "SELECTED_FOLDERS" is active
                if (selectedScope == DuplicateScopeType.SELECTED_FOLDERS && !result.isScanning && result.duplicateGroups.isEmpty()) {
                    Text(
                        text = "Centang beberapa folder yang ingin dipindai:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        LazyColumn(modifier = Modifier.padding(6.dp)) {
                            items(commonFolders) { folder ->
                                val isChecked = selectedMultipleFolders.contains(folder)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) selectedMultipleFolders.remove(folder)
                                            else selectedMultipleFolders.add(folder)
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { check ->
                                            if (check) selectedMultipleFolders.add(folder)
                                            else selectedMultipleFolders.remove(folder)
                                        }
                                    )
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Main Scan / Result Area
                if (result.isScanning) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Memindai file duplikat...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = result.scopeDescription,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (result.duplicateGroups.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Siap Memindai Duplikat",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (selectedScope) {
                                DuplicateScopeType.CURRENT_FOLDER -> "Akan memindai isi folder: ${currentDir.name}"
                                DuplicateScopeType.SELECTED_FOLDERS -> "${selectedMultipleFolders.size} folder terpilih untuk dipindai"
                                DuplicateScopeType.ENTIRE_STORAGE -> "Akan memindai seluruh direktori penyimpanan"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                when (selectedScope) {
                                    DuplicateScopeType.CURRENT_FOLDER -> onStartScan(listOf(currentDir), "Folder: ${currentDir.name}")
                                    DuplicateScopeType.SELECTED_FOLDERS -> {
                                        val targets = if (selectedMultipleFolders.isNotEmpty()) selectedMultipleFolders.toList() else listOf(currentDir)
                                        onStartScan(targets, "${targets.size} Folder Terpilih")
                                    }
                                    DuplicateScopeType.ENTIRE_STORAGE -> onStartScan(listOf(storageRoot), "Seluruh Penyimpanan")
                                }
                            }
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mulai Pindai Duplikat")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${result.duplicateGroups.size} grup (${FileUtils.formatFileSize(result.totalWastedBytes)} boros)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = result.scopeDescription,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(result.duplicateGroups) { group ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "Ukuran: ${FileUtils.formatFileSize(group.fileSize)} • ${group.files.size} File Kembar",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    group.files.forEachIndexed { idx, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = item.isSelectedForDelete,
                                                onCheckedChange = {
                                                    onToggleSelect(group.checksum, item.file.absolutePath)
                                                }
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.file.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = if (idx == 0) FontWeight.SemiBold else FontWeight.Normal,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (idx == 0) {
                                                        Text(
                                                            text = "(Asli)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = item.file.parent ?: "",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        },
        confirmButton = {
            if (result.duplicateGroups.isNotEmpty() && !result.isScanning) {
                Button(
                    onClick = onCleanDuplicates,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bersihkan Duplikat")
                }
            } else if (!result.isScanning) {
                TextButton(
                    onClick = {
                        when (selectedScope) {
                            DuplicateScopeType.CURRENT_FOLDER -> onStartScan(listOf(currentDir), "Folder: ${currentDir.name}")
                            DuplicateScopeType.SELECTED_FOLDERS -> {
                                val targets = if (selectedMultipleFolders.isNotEmpty()) selectedMultipleFolders.toList() else listOf(currentDir)
                                onStartScan(targets, "${targets.size} Folder Terpilih")
                            }
                            DuplicateScopeType.ENTIRE_STORAGE -> onStartScan(listOf(storageRoot), "Seluruh Penyimpanan")
                        }
                    }
                ) {
                    Text("Pindai")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
