package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.model.FileItem
import com.example.model.FileType
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRose
import com.example.ui.theme.ColorApks
import com.example.ui.theme.ColorArchives
import com.example.ui.theme.ColorAudio
import com.example.ui.theme.ColorDocs
import com.example.ui.theme.ColorImages
import com.example.ui.theme.ColorVault
import com.example.ui.theme.ColorVideos
import com.example.ui.theme.PrimaryBlue
import com.example.util.FileUtils

fun getFileTypeIconAndColor(item: FileItem): Pair<ImageVector, Color> {
    if (item.isLocked) return Pair(Icons.Default.Lock, ColorVault)
    if (item.isDirectory) return Pair(Icons.Default.Folder, PrimaryBlue)
    return when (item.fileType) {
        FileType.IMAGE -> Pair(Icons.Default.Image, ColorImages)
        FileType.VIDEO -> Pair(Icons.Default.Videocam, ColorVideos)
        FileType.AUDIO -> Pair(Icons.Default.Audiotrack, ColorAudio)
        FileType.DOCUMENT -> Pair(Icons.Default.Description, ColorDocs)
        FileType.ARCHIVE -> Pair(Icons.Default.FolderZip, ColorArchives)
        FileType.APK -> Pair(Icons.Default.Android, ColorApks)
        FileType.CODE -> Pair(Icons.Default.Code, AccentPurple)
        FileType.ENCRYPTED -> Pair(Icons.Default.Lock, ColorVault)
        FileType.OTHER -> Pair(Icons.Default.InsertDriveFile, Color(0xFF64748B))
        FileType.FOLDER -> Pair(Icons.Default.Folder, PrimaryBlue)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    item: FileItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDetails: () -> Unit,
    onZip: () -> Unit,
    onExtractZip: () -> Unit,
    onMoveToVault: () -> Unit,
    onEditImage: () -> Unit,
    onShare: () -> Unit = {},
    onMove: () -> Unit = {},
    onPreviewZip: () -> Unit = {},
    onOpenFileLocation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val (icon, iconColor) = getFileTypeIconAndColor(item)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = {
                    if (isMultiSelectMode) onToggleSelect() else onClick()
                },
                onLongClick = onLongClick
            )
            .testTag("file_list_item_${item.name}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.name,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val metaText = if (item.isDirectory) {
                        FileUtils.formatDate(item.lastModified)
                    } else {
                        "${FileUtils.formatFileSize(item.size)} • ${FileUtils.formatDate(item.lastModified)}"
                    }
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val parentName = item.file.parentFile?.name
                    if (!item.isDirectory && !parentName.isNullOrEmpty() && parentName != "0" && parentName != "emulated") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "📁 $parentName",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            if (!isMultiSelectMode) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("file_menu_button_${item.name}")
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opsi",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FileActionDropdown(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        item = item,
                        onRename = onRename,
                        onDelete = onDelete,
                        onCopy = onCopy,
                        onCut = onCut,
                        onDetails = onDetails,
                        onZip = onZip,
                        onExtractZip = onExtractZip,
                        onMoveToVault = onMoveToVault,
                        onEditImage = onEditImage,
                        onShare = onShare,
                        onMove = onMove,
                        onPreviewZip = onPreviewZip,
                        onOpenFileLocation = onOpenFileLocation
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    item: FileItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDetails: () -> Unit,
    onZip: () -> Unit,
    onExtractZip: () -> Unit,
    onMoveToVault: () -> Unit,
    onEditImage: () -> Unit,
    onShare: () -> Unit = {},
    onMove: () -> Unit = {},
    onPreviewZip: () -> Unit = {},
    onOpenFileLocation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val (icon, iconColor) = getFileTypeIconAndColor(item)

    Card(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = {
                    if (isMultiSelectMode) onToggleSelect() else onClick()
                },
                onLongClick = onLongClick
            )
            .testTag("file_grid_item_${item.name}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                if (isMultiSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() }
                    )
                } else {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opsi",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FileActionDropdown(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        item = item,
                        onRename = onRename,
                        onDelete = onDelete,
                        onCopy = onCopy,
                        onCut = onCut,
                        onDetails = onDetails,
                        onZip = onZip,
                        onExtractZip = onExtractZip,
                        onMoveToVault = onMoveToVault,
                        onEditImage = onEditImage,
                        onShare = onShare,
                        onMove = onMove,
                        onPreviewZip = onPreviewZip,
                        onOpenFileLocation = onOpenFileLocation
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.name,
                    tint = iconColor,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val sizeStr = if (item.isDirectory) "Folder" else FileUtils.formatFileSize(item.size)
            Text(
                text = sizeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FileActionDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    item: FileItem,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDetails: () -> Unit,
    onZip: () -> Unit,
    onExtractZip: () -> Unit,
    onMoveToVault: () -> Unit,
    onEditImage: () -> Unit,
    onShare: () -> Unit = {},
    onMove: () -> Unit = {},
    onPreviewZip: () -> Unit = {},
    onOpenFileLocation: (() -> Unit)? = null
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        if (onOpenFileLocation != null) {
            DropdownMenuItem(
                text = { Text("Buka Lokasi Folder") },
                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                onClick = { onDismiss(); onOpenFileLocation() }
            )
        }
        if (item.fileType == FileType.IMAGE) {
            DropdownMenuItem(
                text = { Text("Edit Gambar") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = { onDismiss(); onEditImage() }
            )
        }
        if (!item.isDirectory) {
            DropdownMenuItem(
                text = { Text("Bagikan") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = { onDismiss(); onShare() }
            )
        }
        DropdownMenuItem(
            text = { Text("Pindahkan ke Folder...") },
            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
            onClick = { onDismiss(); onMove() }
        )
        DropdownMenuItem(
            text = { Text("Salin") },
            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
            onClick = { onDismiss(); onCopy() }
        )
        DropdownMenuItem(
            text = { Text("Potong") },
            leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
            onClick = { onDismiss(); onCut() }
        )
        DropdownMenuItem(
            text = { Text("Ubah Nama") },
            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
            onClick = { onDismiss(); onRename() }
        )
        if (item.fileType == FileType.ARCHIVE) {
            DropdownMenuItem(
                text = { Text("Lihat Isi Arsip (Preview)") },
                leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                onClick = { onDismiss(); onPreviewZip() }
            )
            DropdownMenuItem(
                text = { Text("Ekstrak ZIP") },
                leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null) },
                onClick = { onDismiss(); onExtractZip() }
            )
        } else {
            DropdownMenuItem(
                text = { Text("Kompres ke ZIP") },
                leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null) },
                onClick = { onDismiss(); onZip() }
            )
        }
        if (!item.isDirectory) {
            DropdownMenuItem(
                text = { Text("Kunci ke Brankas") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                onClick = { onDismiss(); onMoveToVault() }
            )
        }
        DropdownMenuItem(
            text = { Text("Detail & Info") },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
            onClick = { onDismiss(); onDetails() }
        )
        DropdownMenuItem(
            text = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); onDelete() }
        )
    }
}
