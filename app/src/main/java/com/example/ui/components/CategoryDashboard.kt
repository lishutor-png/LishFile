package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CategoryOverviewStats
import com.example.model.DashboardCategoryType
import com.example.model.ViewCategory
import com.example.ui.theme.ColorApks
import com.example.ui.theme.ColorArchives
import com.example.ui.theme.ColorAudio
import com.example.ui.theme.ColorDocs
import com.example.ui.theme.ColorDownloads
import com.example.ui.theme.ColorDuplicates
import com.example.ui.theme.ColorImages
import com.example.ui.theme.ColorVault
import com.example.ui.theme.ColorVideos
import com.example.util.FileUtils

private data class DashboardItem(
    val type: DashboardCategoryType,
    val title: String,
    val count: Int,
    val sizeText: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun CategoryDashboard(
    stats: CategoryOverviewStats,
    onCategoryClick: (ViewCategory) -> Unit,
    onCategoryShowLargest: (ViewCategory) -> Unit = {},
    onOpenVault: () -> Unit,
    onOpenDuplicates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        DashboardItem(
            type = DashboardCategoryType.IMAGES,
            title = "Gambar",
            count = stats.imagesCount,
            sizeText = FileUtils.formatFileSize(stats.imagesSize),
            icon = Icons.Default.Image,
            color = ColorImages
        ),
        DashboardItem(
            type = DashboardCategoryType.VIDEOS,
            title = "Video",
            count = stats.videosCount,
            sizeText = FileUtils.formatFileSize(stats.videosSize),
            icon = Icons.Default.Videocam,
            color = ColorVideos
        ),
        DashboardItem(
            type = DashboardCategoryType.AUDIO,
            title = "Audio",
            count = stats.audioCount,
            sizeText = FileUtils.formatFileSize(stats.audioSize),
            icon = Icons.Default.Audiotrack,
            color = ColorAudio
        ),
        DashboardItem(
            type = DashboardCategoryType.DOCUMENTS,
            title = "Dokumen",
            count = stats.docsCount,
            sizeText = FileUtils.formatFileSize(stats.docsSize),
            icon = Icons.Default.Description,
            color = ColorDocs
        ),
        DashboardItem(
            type = DashboardCategoryType.DOWNLOADS,
            title = "Unduhan",
            count = stats.downloadsCount,
            sizeText = FileUtils.formatFileSize(stats.downloadsSize),
            icon = Icons.Default.Download,
            color = ColorDownloads
        ),
        DashboardItem(
            type = DashboardCategoryType.APKS,
            title = "APK",
            count = stats.apksCount,
            sizeText = FileUtils.formatFileSize(stats.apksSize),
            icon = Icons.Default.Android,
            color = ColorApks
        ),
        DashboardItem(
            type = DashboardCategoryType.ARCHIVES,
            title = "Arsip",
            count = stats.archivesCount,
            sizeText = FileUtils.formatFileSize(stats.archivesSize),
            icon = Icons.Default.FolderZip,
            color = ColorArchives
        ),
        DashboardItem(
            type = DashboardCategoryType.DUPLICATE_SCANNER,
            title = "Pembersih",
            count = 0,
            sizeText = "Scan Duplikat",
            icon = Icons.Default.CleaningServices,
            color = ColorDuplicates
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kategori File",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ketuk kategori untuk urutkan ukuran",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 3-column grid for categories
        val chunked = items.chunked(3)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                when (item.type) {
                                    DashboardCategoryType.IMAGES -> onCategoryShowLargest(ViewCategory.IMAGES)
                                    DashboardCategoryType.VIDEOS -> onCategoryShowLargest(ViewCategory.VIDEOS)
                                    DashboardCategoryType.AUDIO -> onCategoryShowLargest(ViewCategory.AUDIO)
                                    DashboardCategoryType.DOCUMENTS -> onCategoryShowLargest(ViewCategory.DOCUMENTS)
                                    DashboardCategoryType.DOWNLOADS -> onCategoryShowLargest(ViewCategory.DOWNLOADS)
                                    DashboardCategoryType.APKS -> onCategoryShowLargest(ViewCategory.APKS)
                                    DashboardCategoryType.ARCHIVES -> onCategoryShowLargest(ViewCategory.ARCHIVES)
                                    DashboardCategoryType.DUPLICATE_SCANNER -> onOpenDuplicates()
                                    else -> {}
                                }
                            }
                            .testTag("category_tile_${item.title.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(item.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = item.color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (item.type != DashboardCategoryType.DUPLICATE_SCANNER) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Urutkan Terbesar",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1
                            )
                            Text(
                                text = item.sizeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
