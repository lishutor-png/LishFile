package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CategoryOverviewStats
import com.example.model.ViewCategory

enum class DashboardCategoryType {
    PRIMARY_STORAGE,
    SD_CARD,
    DOWNLOADS,
    IMAGES,
    AUDIO,
    VIDEO,
    DOCUMENTS,
    APPS,
    RECENT,
    CLOUD,
    REMOTE,
    NETWORK_ACCESS
}

data class DashboardTileItem(
    val type: DashboardCategoryType,
    val title: String,
    val subtitle: String,
    val testTag: String
)

@Composable
fun CategoryDashboard(
    overviewStats: CategoryOverviewStats,
    onCategoryClick: (DashboardCategoryType) -> Unit,
    modifier: Modifier = Modifier
) {
    val tiles = listOf(
        DashboardTileItem(
            type = DashboardCategoryType.PRIMARY_STORAGE,
            title = overviewStats.primaryStorage.title,
            subtitle = overviewStats.primaryStorage.subtitle,
            testTag = "category_tile_primary_storage"
        ),
        DashboardTileItem(
            type = DashboardCategoryType.SD_CARD,
            title = overviewStats.sdCard.title,
            subtitle = overviewStats.sdCard.subtitle,
            testTag = "category_tile_sd_card"
        ),
        DashboardTileItem(
            type = DashboardCategoryType.DOWNLOADS,
            title = overviewStats.downloads.title,
            subtitle = overviewStats.downloads.subtitle,
            testTag = "category_tile_downloads"
        ),
        DashboardTileItem(
            type = DashboardCategoryType.IMAGES,
            title = overviewStats.images.title,
            subtitle = overviewStats.images.subtitle,
            testTag = "category_tile_images"
        ),
        DashboardTileItem(
            type = DashboardCategoryType.AUDIO,
            title = overviewStats.audio.title,
            subtitle = overviewStats.audio.subtitle,
            testTag = "category_tile_audio"
        ),
        DashboardTileItem(
            type = DashboardCategoryType.VIDEO,
            title = overviewStats.video.title,
            subtitle = overviewStats.video.subtitle,
            testTag = "category_tile_video"
        ),
        DashboardTileItem(
            type = DashboardCategoryType.DOCUMENTS,
            title = overviewStats.documents.title,
            subtitle = overviewStats.documents.subtitle,
            testTag = "category_tile_documents"
        ),
        DashboardTileItem(
            type = DashboardCategoryType.APPS,
            title = overviewStats.apps.title,
            subtitle = overviewStats.apps.subtitle,
            testTag = "category_tile_apps"
        ),
        DashboardTileItem(
            type = DashboardCategoryType.RECENT,
            title = overviewStats.recent.title,
            subtitle = overviewStats.recent.subtitle,
            testTag = "category_tile_recent"
        ),
        DashboardTileItem(
            type = DashboardCategoryType.CLOUD,
            title = overviewStats.cloud.title,
            subtitle = overviewStats.cloud.subtitle,
            testTag = "category_tile_cloud"
        ),
        DashboardTileItem(
            type = DashboardCategoryType.REMOTE,
            title = overviewStats.remote.title,
            subtitle = overviewStats.remote.subtitle,
            testTag = "category_tile_remote"
        ),
        DashboardTileItem(
            type = DashboardCategoryType.NETWORK_ACCESS,
            title = overviewStats.networkAccess.title,
            subtitle = overviewStats.networkAccess.subtitle,
            testTag = "category_tile_network_access"
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(tiles.size) { index ->
            val tile = tiles[index]
            CategoryTile(
                item = tile,
                onClick = { onCategoryClick(tile.type) }
            )
        }
    }
}

@Composable
fun CategoryTile(
    item: DashboardTileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(item.testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Squircle Icon Container
        Surface(
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                ),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CategoryVectorIcon(type = item.type)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = item.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Subtitle
        Text(
            text = item.subtitle,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun CategoryVectorIcon(type: DashboardCategoryType) {
    when (type) {
        DashboardCategoryType.PRIMARY_STORAGE -> PrimaryStorageIcon()
        DashboardCategoryType.SD_CARD -> SdCardIcon()
        DashboardCategoryType.DOWNLOADS -> DownloadsFolderIcon()
        DashboardCategoryType.IMAGES -> GalleryPictureIcon()
        DashboardCategoryType.AUDIO -> AudioNoteIcon()
        DashboardCategoryType.VIDEO -> VideoFilmStripIcon()
        DashboardCategoryType.DOCUMENTS -> DocumentsStripeIcon()
        DashboardCategoryType.APPS -> AndroidBugdroidIcon()
        DashboardCategoryType.RECENT -> RecentClockIcon()
        DashboardCategoryType.CLOUD -> CloudBlueIcon()
        DashboardCategoryType.REMOTE -> RemoteMonitorIcon()
        DashboardCategoryType.NETWORK_ACCESS -> NetworkTransferIcon()
    }
}

/* =====================================================================
   CUSTOM VECTOR ICONS MATCHING FILE MANAGER + GRAPHICS ACCURATELY
   ===================================================================== */

/**
 * 1. Primary Storage (Metallic hard drive with green power indicator)
 */
@Composable
fun PrimaryStorageIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(46.dp)) {
        val w = size.width
        val h = size.height

        // Drive casing base
        val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFCFD8DC), Color(0xFF90A4AE))
            ),
            topLeft = Offset(w * 0.12f, h * 0.14f),
            size = Size(w * 0.76f, h * 0.72f),
            cornerRadius = corner
        )

        // Drive top cover (lighter metallic sheen)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFECEFF1), Color(0xFFB0BEC5))
            ),
            topLeft = Offset(w * 0.14f, h * 0.16f),
            size = Size(w * 0.72f, h * 0.52f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Subtle bottom slot / bevel line
        drawLine(
            color = Color(0xFF78909C),
            start = Offset(w * 0.15f, h * 0.72f),
            end = Offset(w * 0.85f, h * 0.72f),
            strokeWidth = 2.dp.toPx()
        )

        // Glowing green status LED dot on bottom-left
        drawCircle(
            color = Color(0xFF00E676),
            radius = 3.dp.toPx(),
            center = Offset(w * 0.24f, h * 0.78f)
        )
        // LED inner bright center
        drawCircle(
            color = Color(0xFFB9F6CA),
            radius = 1.5.dp.toPx(),
            center = Offset(w * 0.24f, h * 0.78f)
        )
    }
}

/**
 * 2. SD Card (Dark navy/purple SD card with gold contact pads)
 */
@Composable
fun SdCardIcon(modifier: Modifier = Modifier) {
    val notchColor = MaterialTheme.colorScheme.surface
    Canvas(modifier = modifier.size(46.dp)) {
        val w = size.width
        val h = size.height

        // SD Card Body with chamfered top-right corner
        val bodyPath = Path().apply {
            moveTo(w * 0.18f, h * 0.14f)
            lineTo(w * 0.68f, h * 0.14f)
            // Top-right notch
            lineTo(w * 0.82f, h * 0.26f)
            lineTo(w * 0.82f, h * 0.86f)
            // Bottom edge
            lineTo(w * 0.18f, h * 0.86f)
            close()
        }

        drawPath(
            path = bodyPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF3949AB), Color(0xFF1A237E))
            )
        )

        // Left notch (write protection switch recess)
        drawRect(
            color = notchColor,
            topLeft = Offset(w * 0.12f, h * 0.36f),
            size = Size(w * 0.08f, h * 0.14f)
        )

        // Gold contact pins on top
        val pinCount = 5
        val pinWidth = w * 0.07f
        val pinGap = w * 0.035f
        val pinHeight = h * 0.16f
        var startX = w * 0.23f

        for (i in 0 until pinCount) {
            drawRoundRect(
                color = Color(0xFFFFD54F),
                topLeft = Offset(startX, h * 0.17f),
                size = Size(pinWidth, pinHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            startX += pinWidth + pinGap
        }
    }
}

/**
 * 3. Downloads (Golden folder with cyan circle down-arrow badge)
 */
@Composable
fun DownloadsFolderIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(46.dp)) {
        val w = size.width
        val h = size.height

        // Back folder tab
        val backPath = Path().apply {
            moveTo(w * 0.16f, h * 0.24f)
            lineTo(w * 0.45f, h * 0.24f)
            lineTo(w * 0.55f, h * 0.32f)
            lineTo(w * 0.84f, h * 0.32f)
            lineTo(w * 0.84f, h * 0.82f)
            lineTo(w * 0.16f, h * 0.82f)
            close()
        }
        drawPath(backPath, Color(0xFFFFA000))

        // Front folder flap
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFB300), Color(0xFFFF8F00))
            ),
            topLeft = Offset(w * 0.14f, h * 0.35f),
            size = Size(w * 0.72f, h * 0.48f),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )

        // Cyan circle badge
        val badgeCenter = Offset(w * 0.44f, h * 0.56f)
        val badgeRadius = w * 0.22f
        drawCircle(
            color = Color.White,
            radius = badgeRadius + 2.dp.toPx(),
            center = badgeCenter
        )
        drawCircle(
            color = Color(0xFF00ACC1),
            radius = badgeRadius,
            center = badgeCenter
        )

        // White down arrow inside circle
        val arrowPath = Path().apply {
            // Shaft
            moveTo(badgeCenter.x - w * 0.045f, badgeCenter.y - h * 0.12f)
            lineTo(badgeCenter.x + w * 0.045f, badgeCenter.y - h * 0.12f)
            lineTo(badgeCenter.x + w * 0.045f, badgeCenter.y + h * 0.02f)
            // Arrowhead
            lineTo(badgeCenter.x + w * 0.11f, badgeCenter.y + h * 0.02f)
            lineTo(badgeCenter.x, badgeCenter.y + h * 0.13f)
            lineTo(badgeCenter.x - w * 0.11f, badgeCenter.y + h * 0.02f)
            lineTo(badgeCenter.x - w * 0.045f, badgeCenter.y + h * 0.02f)
            close()
        }
        drawPath(arrowPath, Color.White)
    }
}

/**
 * 4. Gambar (Purple picture frame with hanging string & mountain landscape)
 */
@Composable
fun GalleryPictureIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(46.dp)) {
        val w = size.width
        val h = size.height

        // Hanging string / cord triangle at the top
        val cordPath = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.24f, h * 0.26f)
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.76f, h * 0.26f)
        }
        drawPath(
            cordPath,
            Color(0xFF8E24AA),
            style = Stroke(width = 2.dp.toPx())
        )

        // Picture frame
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFAB47BC), Color(0xFF7B1FA2))
            ),
            topLeft = Offset(w * 0.15f, h * 0.26f),
            size = Size(w * 0.7f, h * 0.58f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Inside canvas background
        drawRoundRect(
            color = Color(0xFFF3E5F5),
            topLeft = Offset(w * 0.22f, h * 0.32f),
            size = Size(w * 0.56f, h * 0.46f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )

        // Sun
        drawCircle(
            color = Color(0xFFFFB300),
            radius = 3.dp.toPx(),
            center = Offset(w * 0.34f, h * 0.42f)
        )

        // Mountains
        val mountainPath = Path().apply {
            moveTo(w * 0.22f, h * 0.74f)
            lineTo(w * 0.42f, h * 0.50f)
            lineTo(w * 0.56f, h * 0.65f)
            lineTo(w * 0.66f, h * 0.55f)
            lineTo(w * 0.78f, h * 0.74f)
            close()
        }
        drawPath(mountainPath, Color(0xFF8E24AA))
    }
}

/**
 * 5. Audio (Vibrant teal/turquoise beamed double musical notes)
 */
@Composable
fun AudioNoteIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(46.dp)) {
        val w = size.width
        val h = size.height
        val noteColor = Color(0xFF00897B)

        // Left Note stem & head
        drawOval(
            color = noteColor,
            topLeft = Offset(w * 0.20f, h * 0.62f),
            size = Size(w * 0.24f, h * 0.18f)
        )
        drawRect(
            color = noteColor,
            topLeft = Offset(w * 0.38f, h * 0.22f),
            size = Size(w * 0.07f, h * 0.46f)
        )

        // Right Note stem & head
        drawOval(
            color = noteColor,
            topLeft = Offset(w * 0.54f, h * 0.54f),
            size = Size(w * 0.24f, h * 0.18f)
        )
        drawRect(
            color = noteColor,
            topLeft = Offset(w * 0.72f, h * 0.14f),
            size = Size(w * 0.07f, h * 0.46f)
        )

        // Connecting angled beam
        val beamPath = Path().apply {
            moveTo(w * 0.38f, h * 0.22f)
            lineTo(w * 0.79f, h * 0.14f)
            lineTo(w * 0.79f, h * 0.24f)
            lineTo(w * 0.38f, h * 0.32f)
            close()
        }
        drawPath(beamPath, noteColor)
    }
}

/**
 * 6. Video (Coral red / crimson cinema film strip)
 */
@Composable
fun VideoFilmStripIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(46.dp)) {
        val w = size.width
        val h = size.height
        val filmColor = Color(0xFFE53935)

        // Film strip outer body
        drawRoundRect(
            color = filmColor,
            topLeft = Offset(w * 0.18f, h * 0.14f),
            size = Size(w * 0.64f, h * 0.72f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Center screen / viewport cutouts
        val screenColor = Color.White
        drawRoundRect(
            color = screenColor,
            topLeft = Offset(w * 0.32f, h * 0.22f),
            size = Size(w * 0.36f, h * 0.24f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = screenColor,
            topLeft = Offset(w * 0.32f, h * 0.52f),
            size = Size(w * 0.36f, h * 0.24f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Left sprocket holes
        val sprocketW = w * 0.06f
        val sprocketH = h * 0.08f
        val sprockets = listOf(0.20f, 0.36f, 0.52f, 0.68f)
        sprockets.forEach { yRel ->
            drawRect(
                color = screenColor,
                topLeft = Offset(w * 0.22f, h * yRel),
                size = Size(sprocketW, sprocketH)
            )
            drawRect(
                color = screenColor,
                topLeft = Offset(w * 0.72f, h * yRel),
                size = Size(sprocketW, sprocketH)
            )
        }
    }
}

/**
 * 7. Dokumen (Azure blue document with horizontal stripes)
 */
@Composable
fun DocumentsStripeIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(46.dp)) {
        val w = size.width
        val h = size.height
        val docColor = Color(0xFF1E88E5)

        // Document card base
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF42A5F5), docColor)
            ),
            topLeft = Offset(w * 0.18f, h * 0.12f),
            size = Size(w * 0.64f, h * 0.76f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Horizontal white stripes
        val stripeLeft = w * 0.28f
        val stripeWidth = w * 0.44f
        val stripeHeight = h * 0.045f
        val startY = h * 0.26f
        val gap = h * 0.10f

        for (i in 0 until 5) {
            val curWidth = if (i == 4) stripeWidth * 0.65f else stripeWidth
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(stripeLeft, startY + (i * gap)),
                size = Size(curWidth, stripeHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

/**
 * 8. Aplikasi (Android Bugdroid in bright Android green)
 */
@Composable
fun AndroidBugdroidIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(46.dp)) {
        val w = size.width
        val h = size.height
        val droidColor = Color(0xFF7CB342)

        // Dome Head
        drawArc(
            color = droidColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.25f, h * 0.22f),
            size = Size(w * 0.50f, h * 0.26f)
        )

        // Eyes
        drawCircle(color = Color.White, radius = 1.8.dp.toPx(), center = Offset(w * 0.38f, h * 0.31f))
        drawCircle(color = Color.White, radius = 1.8.dp.toPx(), center = Offset(w * 0.62f, h * 0.31f))

        // Antennae
        drawLine(
            color = droidColor,
            start = Offset(w * 0.34f, h * 0.22f),
            end = Offset(w * 0.26f, h * 0.12f),
            strokeWidth = 2.2.dp.toPx()
        )
        drawLine(
            color = droidColor,
            start = Offset(w * 0.66f, h * 0.22f),
            end = Offset(w * 0.74f, h * 0.12f),
            strokeWidth = 2.2.dp.toPx()
        )

        // Torso Body
        drawRoundRect(
            color = droidColor,
            topLeft = Offset(w * 0.25f, h * 0.38f),
            size = Size(w * 0.50f, h * 0.36f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Left & Right Arms
        drawRoundRect(
            color = droidColor,
            topLeft = Offset(w * 0.14f, h * 0.38f),
            size = Size(w * 0.08f, h * 0.26f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        drawRoundRect(
            color = droidColor,
            topLeft = Offset(w * 0.78f, h * 0.38f),
            size = Size(w * 0.08f, h * 0.26f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )

        // Left & Right Legs
        drawRoundRect(
            color = droidColor,
            topLeft = Offset(w * 0.33f, h * 0.73f),
            size = Size(w * 0.09f, h * 0.15f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        drawRoundRect(
            color = droidColor,
            topLeft = Offset(w * 0.58f, h * 0.73f),
            size = Size(w * 0.09f, h * 0.15f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
    }
}

/**
 * 9. File Baru (Slate blue circular analog clock at 10:10)
 */
@Composable
fun RecentClockIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(46.dp)) {
        val w = size.width
        val h = size.height
        val clockColor = Color(0xFF455A64)
        val center = Offset(w * 0.5f, h * 0.5f)
        val radius = w * 0.36f

        // Outer circular ring
        drawCircle(
            color = clockColor,
            radius = radius,
            center = center,
            style = Stroke(width = 4.5.dp.toPx())
        )

        // Clock center pin
        drawCircle(
            color = clockColor,
            radius = 3.dp.toPx(),
            center = center
        )

        // Minute hand (pointing ~10:10)
        drawLine(
            color = clockColor,
            start = center,
            end = Offset(center.x - radius * 0.45f, center.y - radius * 0.45f),
            strokeWidth = 3.dp.toPx()
        )

        // Hour hand
        drawLine(
            color = clockColor,
            start = center,
            end = Offset(center.x + radius * 0.40f, center.y - radius * 0.20f),
            strokeWidth = 3.dp.toPx()
        )
    }
}

/**
 * 10. Cloud (Sky blue fluffy cloud)
 */
@Composable
fun CloudBlueIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(46.dp)) {
        val w = size.width
        val h = size.height
        val cloudColor = Color(0xFF03A9F4)

        // Cloud puffs
        drawCircle(color = cloudColor, radius = w * 0.18f, center = Offset(w * 0.45f, h * 0.42f))
        drawCircle(color = cloudColor, radius = w * 0.14f, center = Offset(w * 0.66f, h * 0.48f))
        drawCircle(color = cloudColor, radius = w * 0.15f, center = Offset(w * 0.28f, h * 0.54f))

        // Cloud base pill
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(w * 0.16f, h * 0.50f),
            size = Size(w * 0.68f, h * 0.24f),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
    }
}

/**
 * 11. Remote (Warm bronze desktop PC monitor)
 */
@Composable
fun RemoteMonitorIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(46.dp)) {
        val w = size.width
        val h = size.height
        val monitorColor = Color(0xFF8D6E63)

        // Screen bezel
        drawRoundRect(
            color = monitorColor,
            topLeft = Offset(w * 0.15f, h * 0.18f),
            size = Size(w * 0.70f, h * 0.50f),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )

        // Screen display interior
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.22f, h * 0.24f),
            size = Size(w * 0.56f, h * 0.38f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Stand neck
        drawRect(
            color = monitorColor,
            topLeft = Offset(w * 0.45f, h * 0.68f),
            size = Size(w * 0.10f, h * 0.10f)
        )

        // Stand base
        drawRoundRect(
            color = monitorColor,
            topLeft = Offset(w * 0.32f, h * 0.77f),
            size = Size(w * 0.36f, h * 0.07f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
    }
}

/**
 * 12. Akses dari jaringan (PC to phone wireless network sharing in deep teal)
 */
@Composable
fun NetworkTransferIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(46.dp)) {
        val w = size.width
        val h = size.height
        val tealColor = Color(0xFF00796B)

        // Desktop Monitor on left
        drawRoundRect(
            color = tealColor,
            topLeft = Offset(w * 0.10f, h * 0.24f),
            size = Size(w * 0.45f, h * 0.34f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 2.5.dp.toPx())
        )
        // Monitor stand
        drawLine(
            color = tealColor,
            start = Offset(w * 0.32f, h * 0.58f),
            end = Offset(w * 0.32f, h * 0.68f),
            strokeWidth = 2.5.dp.toPx()
        )
        drawLine(
            color = tealColor,
            start = Offset(w * 0.22f, h * 0.68f),
            end = Offset(w * 0.42f, h * 0.68f),
            strokeWidth = 2.5.dp.toPx()
        )

        // Phone on right
        drawRoundRect(
            color = tealColor,
            topLeft = Offset(w * 0.65f, h * 0.36f),
            size = Size(w * 0.24f, h * 0.44f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // Transfer Arrow between PC and Phone
        val arrowPath = Path().apply {
            moveTo(w * 0.38f, h * 0.41f)
            lineTo(w * 0.56f, h * 0.41f)
            // Arrowhead
            lineTo(w * 0.52f, h * 0.35f)
            moveTo(w * 0.56f, h * 0.41f)
            lineTo(w * 0.52f, h * 0.47f)
        }
        drawPath(
            arrowPath,
            color = tealColor,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}
