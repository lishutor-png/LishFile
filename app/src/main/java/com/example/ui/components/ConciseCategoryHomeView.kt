package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SecurityUpdateWarning
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CategoryOverviewStats
import com.example.model.StorageVolumeInfo
import com.example.model.ViewCategory
import com.example.ui.theme.ColorApks
import com.example.ui.theme.ColorApksContainer
import com.example.ui.theme.ColorArchives
import com.example.ui.theme.ColorArchivesContainer
import com.example.ui.theme.ColorAudio
import com.example.ui.theme.ColorAudioContainer
import com.example.ui.theme.ColorDocs
import com.example.ui.theme.ColorDocsContainer
import com.example.ui.theme.ColorDownloads
import com.example.ui.theme.ColorDownloadsContainer
import com.example.ui.theme.ColorDuplicates
import com.example.ui.theme.ColorDuplicatesContainer
import com.example.ui.theme.ColorImages
import com.example.ui.theme.ColorImagesContainer
import com.example.ui.theme.ColorVideos
import com.example.ui.theme.ColorVideosContainer
import com.example.util.FileUtils
import java.io.File

private data class HomeCategoryItem(
    val category: ViewCategory?,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val containerColor: Color,
    val isDuplicatesAction: Boolean = false
)

@Composable
fun ConciseCategoryHomeView(
    hasStoragePermission: Boolean,
    onRequestPermission: () -> Unit,
    storages: List<StorageVolumeInfo>,
    categoryStats: CategoryOverviewStats,
    recentFolders: List<String>,
    onOpenStorage: (StorageVolumeInfo) -> Unit,
    onOpenCategory: (ViewCategory) -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenRecentFolder: (File) -> Unit,
    onShowAllRecentFolders: () -> Unit,
    onSdCardNotAvailable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryStorage = storages.firstOrNull { it.isPrimary } ?: storages.firstOrNull()
    val secondaryStorage = storages.firstOrNull { it.isRemovable }

    val categories = listOf(
        HomeCategoryItem(
            category = ViewCategory.IMAGES,
            title = "Gambar",
            subtitle = "${categoryStats.imagesCount} file • ${FileUtils.formatFileSize(categoryStats.imagesSize)}",
            icon = Icons.Default.Image,
            color = ColorImages,
            containerColor = ColorImagesContainer
        ),
        HomeCategoryItem(
            category = ViewCategory.VIDEOS,
            title = "Video",
            subtitle = "${categoryStats.videosCount} file • ${FileUtils.formatFileSize(categoryStats.videosSize)}",
            icon = Icons.Default.Videocam,
            color = ColorVideos,
            containerColor = ColorVideosContainer
        ),
        HomeCategoryItem(
            category = ViewCategory.AUDIO,
            title = "Audio",
            subtitle = "${categoryStats.audioCount} file • ${FileUtils.formatFileSize(categoryStats.audioSize)}",
            icon = Icons.Default.Audiotrack,
            color = ColorAudio,
            containerColor = ColorAudioContainer
        ),
        HomeCategoryItem(
            category = ViewCategory.DOCUMENTS,
            title = "Dokumen",
            subtitle = "${categoryStats.docsCount} file • ${FileUtils.formatFileSize(categoryStats.docsSize)}",
            icon = Icons.Default.Description,
            color = ColorDocs,
            containerColor = ColorDocsContainer
        ),
        HomeCategoryItem(
            category = ViewCategory.DOWNLOADS,
            title = "Unduhan",
            subtitle = "${categoryStats.downloadsCount} file • ${FileUtils.formatFileSize(categoryStats.downloadsSize)}",
            icon = Icons.Default.Download,
            color = ColorDownloads,
            containerColor = ColorDownloadsContainer
        ),
        HomeCategoryItem(
            category = ViewCategory.ARCHIVES,
            title = "Arsip ZIP",
            subtitle = "${categoryStats.archivesCount} file • ${FileUtils.formatFileSize(categoryStats.archivesSize)}",
            icon = Icons.Default.FolderZip,
            color = ColorArchives,
            containerColor = ColorArchivesContainer
        ),
        HomeCategoryItem(
            category = ViewCategory.APKS,
            title = "APK Aplikasi",
            subtitle = "${categoryStats.apksCount} file • ${FileUtils.formatFileSize(categoryStats.apksSize)}",
            icon = Icons.Default.Android,
            color = ColorApks,
            containerColor = ColorApksContainer
        ),
        HomeCategoryItem(
            category = null,
            title = "Pembersih Duplikat",
            subtitle = "Pindai & bersihkan file ganda",
            icon = Icons.Default.CleaningServices,
            color = ColorDuplicates,
            containerColor = ColorDuplicatesContainer,
            isDuplicatesAction = true
        )
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
    ) {
        // 1. Storage Permission Card (if not granted)
        if (!hasStoragePermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SecurityUpdateWarning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Izin Akses Penyimpanan Dibutuhkan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Beri izin agar semua file di HP terhubung ke aplikasi.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRequestPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Beri Izin Sekarang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Storage Overview: Internal & External Memory
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Penyimpanan Perangkat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Internal Memory Card
                if (primaryStorage != null) {
                    val used = (primaryStorage.totalSpace - primaryStorage.freeSpace).coerceAtLeast(0L)
                    val percent = if (primaryStorage.totalSpace > 0) (used.toFloat() / primaryStorage.totalSpace) else 0f
                    val percentInt = (percent * 100).toInt()

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenStorage(primaryStorage) }
                            .testTag("internal_storage_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = "Memori Internal",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "$percentInt%",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Memori Internal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${FileUtils.formatFileSize(primaryStorage.freeSpace)} Bebas",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { percent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (percent > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${FileUtils.formatFileSize(used)} / ${FileUtils.formatFileSize(primaryStorage.totalSpace)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // External Memory / SD Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (secondaryStorage != null) {
                                onOpenStorage(secondaryStorage)
                            } else {
                                onSdCardNotAvailable()
                            }
                        }
                        .testTag("external_storage_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (secondaryStorage != null) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (secondaryStorage != null) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SdStorage,
                                    contentDescription = "Memori Eksternal",
                                    tint = if (secondaryStorage != null) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (secondaryStorage != null) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (secondaryStorage != null) {
                                        val usedSec = (secondaryStorage.totalSpace - secondaryStorage.freeSpace).coerceAtLeast(0L)
                                        val p = if (secondaryStorage.totalSpace > 0) (usedSec.toFloat() / secondaryStorage.totalSpace * 100).toInt() else 0
                                        "$p%"
                                    } else "Off",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (secondaryStorage != null) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (secondaryStorage != null) secondaryStorage.name else "Memori Eksternal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (secondaryStorage != null) "${FileUtils.formatFileSize(secondaryStorage.freeSpace)} Bebas" else "Tidak Terpasang",
                            fontSize = 12.sp,
                            color = if (secondaryStorage != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (secondaryStorage != null) {
                            val usedSec = (secondaryStorage.totalSpace - secondaryStorage.freeSpace).coerceAtLeast(0L)
                            val percentSec = if (secondaryStorage.totalSpace > 0) (usedSec.toFloat() / secondaryStorage.totalSpace) else 0f
                            LinearProgressIndicator(
                                progress = { percentSec },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${FileUtils.formatFileSize(usedSec)} / ${FileUtils.formatFileSize(secondaryStorage.totalSpace)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Slot SD / USB OTG",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Category File Grid (2 columns per row)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Kategori Berkas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Pair items into rows
        val chunkedCategories = categories.chunked(2)
        chunkedCategories.forEach { rowItems ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        val badgeBg = if (isDark) item.color.copy(alpha = 0.18f) else item.containerColor

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (item.isDuplicatesAction) {
                                        onOpenDuplicates()
                                    } else if (item.category != null) {
                                        onOpenCategory(item.category)
                                    }
                                }
                                .testTag("home_category_${item.title.lowercase()}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(badgeBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = item.color,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 4. Quick Recent Folders
        if (recentFolders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Folder Terakhir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onShowAllRecentFolders) {
                        Text(
                            text = "Lihat Semua",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentFolders.take(6).forEach { folderPath ->
                        val f = File(folderPath)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            modifier = Modifier.clickable { onOpenRecentFolder(f) },
                            shadowElevation = 0.5.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = f.name.ifEmpty { "Penyimpanan" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
