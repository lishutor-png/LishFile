package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.StorageStats
import com.example.model.ViewCategory
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.VaultGold
import com.example.util.StorageVolumeInfo
import java.io.File

@Composable
fun StorageHeader(
    storageStats: StorageStats,
    selectedCategory: ViewCategory,
    onSelectCategory: (ViewCategory) -> Unit,
    currentDir: File,
    rootDir: File,
    onNavigateToDir: (File) -> Unit,
    onLogoClick: (() -> Unit)? = null,
    canNavigateBack: Boolean = false,
    canNavigateForward: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onNavigateForward: () -> Unit = {},
    recentFolders: List<File> = emptyList(),
    availableStorages: List<StorageVolumeInfo> = emptyList(),
    activeStorageRoot: File = rootDir,
    onSwitchStorage: (File) -> Unit = {},
    isLargestFilesActive: Boolean = false,
    onToggleLargestFiles: () -> Unit = {}
) {
    val isRoot = currentDir.absolutePath == activeStorageRoot.absolutePath
    var showStorageMenu by remember { mutableStateOf(false) }
    var showRecentMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Storage Volume Switcher (Internal Storage vs SD Card)
        if (availableStorages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    val activeVolume = availableStorages.find { it.path.absolutePath == activeStorageRoot.absolutePath }
                        ?: availableStorages.firstOrNull()
                    val volumeTitle = activeVolume?.name ?: "Penyimpanan Utama"

                    SuggestionChip(
                        onClick = {
                            if (availableStorages.size > 1) {
                                showStorageMenu = true
                            }
                        },
                        label = {
                            Text(
                                text = volumeTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (activeVolume?.isRemovable == true) Icons.Default.SdCard else Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.testTag("storage_volume_chip")
                    )

                    if (availableStorages.size > 1) {
                        DropdownMenu(
                            expanded = showStorageMenu,
                            onDismissRequest = { showStorageMenu = false }
                        ) {
                            availableStorages.forEach { vol ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(vol.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(
                                                "${StorageStats.formatBytes(vol.totalBytes - vol.freeBytes)} / ${StorageStats.formatBytes(vol.totalBytes)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (vol.isRemovable) Icons.Default.SdCard else Icons.Default.Storage,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showStorageMenu = false
                                        onSwitchStorage(vol.path)
                                    }
                                )
                            }
                        }
                    }
                }

                // Recent Folders Button
                if (recentFolders.isNotEmpty()) {
                    Box {
                        SuggestionChip(
                            onClick = { showRecentMenu = true },
                            label = { Text("Riwayat Folder", fontSize = 11.sp) },
                            icon = {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.testTag("recent_folders_button")
                        )

                        DropdownMenu(
                            expanded = showRecentMenu,
                            onDismissRequest = { showRecentMenu = false }
                        ) {
                            Text(
                                text = "Folder Terakhir",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            recentFolders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showRecentMenu = false
                                        onNavigateToDir(folder)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isRoot) {
            // Elegant Brand Logo Banner Card (Clickable to open locked vault)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .then(
                        if (onLogoClick != null) {
                            Modifier.clickable { onLogoClick() }
                        } else {
                            Modifier
                        }
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "File Manager +",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "File Manager +",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SecureGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OFFLINE AMAN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecureGreen
                                )
                            }
                        }
                        Text(
                            text = "Penyimpanan lokal & vault AES-256",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Storage Meter Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kapasitas Penyimpanan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${(storageStats.usedPercentage * 100).toInt()}% Digunakan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { storageStats.usedPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tersedia: ${storageStats.formattedFree}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${storageStats.totalFiles} File • ${storageStats.totalFolders} Folder",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Navigation Bar: Back / Forward Buttons + Breadcrumbs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onNavigateBack,
                    enabled = canNavigateBack,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("nav_history_back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke riwayat sebelumnya",
                        tint = if (canNavigateBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Forward Button
                IconButton(
                    onClick = onNavigateForward,
                    enabled = canNavigateForward,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("nav_history_forward_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Maju ke riwayat berikutnya",
                        tint = if (canNavigateForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Breadcrumbs Navigation Bar
                val breadcrumbParts = buildBreadcrumbs(activeStorageRoot, currentDir)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigateToDir(activeStorageRoot) }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Beranda",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Root",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    breadcrumbParts.forEach { (dir, name) ->
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNavigateToDir(dir) }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = if (dir.absolutePath == currentDir.absolutePath) FontWeight.Bold else FontWeight.Normal,
                                color = if (dir.absolutePath == currentDir.absolutePath) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Chips Strip + File Terbesar Chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryChip("Semua", Icons.Default.Storage, ViewCategory.ALL, selectedCategory, onSelectCategory)
            CategoryChip("Dokumen", Icons.Default.Description, ViewCategory.DOCUMENTS, selectedCategory, onSelectCategory)
            CategoryChip("Media", Icons.Default.PermMedia, ViewCategory.MEDIA, selectedCategory, onSelectCategory)
            CategoryChip("Aman / Vault", Icons.Default.Lock, ViewCategory.SAFE_VAULT, selectedCategory, onSelectCategory)
            CategoryChip("Terbaru", Icons.Default.Schedule, ViewCategory.RECENT, selectedCategory, onSelectCategory)

            // "File Terbesar" chip (User feature: "Bisa klik kategori -> lihat file terbesar")
            FilterChip(
                selected = isLargestFilesActive,
                onClick = onToggleLargestFiles,
                label = { Text("File Terbesar", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isLargestFilesActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("category_chip_largest_files")
            )
        }
    }
}

@Composable
private fun CategoryChip(
    title: String,
    icon: ImageVector,
    category: ViewCategory,
    selectedCategory: ViewCategory,
    onSelect: (ViewCategory) -> Unit
) {
    val isSelected = category == selectedCategory
    FilterChip(
        selected = isSelected,
        onClick = { onSelect(category) },
        label = { Text(title, fontSize = 12.sp) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            selectedBorderColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("category_chip_${category.name.lowercase()}")
    )
}

private fun buildBreadcrumbs(rootDir: File, currentDir: File): List<Pair<File, String>> {
    val list = mutableListOf<Pair<File, String>>()
    var curr: File? = currentDir
    while (curr != null && curr.absolutePath != rootDir.absolutePath && (rootDir.absolutePath in curr.absolutePath || curr.absolutePath.startsWith(rootDir.absolutePath))) {
        list.add(0, Pair(curr, curr.name))
        curr = curr.parentFile
    }
    return list
}
