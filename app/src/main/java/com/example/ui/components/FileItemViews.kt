package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.model.FileItem
import com.example.model.FileType
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.VaultGold

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    fileItem: FileItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val containerBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerBg)
            .combinedClickable(
                onClick = {
                    if (isMultiSelectMode) onToggleSelect() else onClick()
                },
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("file_item_${fileItem.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag("file_checkbox_${fileItem.name}")
            )
        }

        FileIconBadge(fileItem = fileItem, size = 44)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fileItem.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (fileItem.isHidden) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (fileItem.isLocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VaultGold.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Terkunci",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultGold
                        )
                    }
                }

                if (fileItem.isEncrypted) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SecureGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AES-256",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecureGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${fileItem.formattedSize} • ${fileItem.formattedDate}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!fileItem.isDirectory && fileItem.extension.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = ".${fileItem.extension.uppercase()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (!isMultiSelectMode) {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.testTag("file_action_button_${fileItem.name}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opsi file",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    fileItem: FileItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isMultiSelectMode) onToggleSelect() else onClick()
                },
                onLongClick = onLongClick
            )
            .testTag("file_grid_item_${fileItem.name}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                if (isMultiSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .testTag("file_checkbox_${fileItem.name}")
                    )
                } else {
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opsi file",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FileIconBadge(fileItem = fileItem, size = 52)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = fileItem.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (fileItem.isHidden) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = fileItem.formattedSize,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (fileItem.isLocked || fileItem.isEncrypted) {
                Spacer(modifier = Modifier.height(4.dp))
                val badgeText = if (fileItem.isEncrypted) "AES-256" else "Terkunci"
                val badgeColor = if (fileItem.isEncrypted) SecureGreen else VaultGold
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }
        }
    }
}

@Composable
fun FileIconBadge(fileItem: FileItem, size: Int = 44) {
    val (icon, bgColor, iconTint) = when {
        fileItem.isLocked -> Triple(Icons.Default.Lock, VaultGold.copy(alpha = 0.15f), VaultGold)
        fileItem.isEncrypted -> Triple(Icons.Default.Shield, SecureGreen.copy(alpha = 0.15f), SecureGreen)
        fileItem.isDirectory -> Triple(Icons.Default.Folder, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
        fileItem.fileType == FileType.DOCUMENT -> Triple(Icons.Default.Description, Color(0xFFE0F2FE), Color(0xFF0284C7))
        fileItem.fileType == FileType.IMAGE -> Triple(Icons.Default.Image, Color(0xFFEDE9FE), Color(0xFF7C3AED))
        fileItem.fileType == FileType.VIDEO -> Triple(Icons.Default.VideoFile, Color(0xFFFFEDD5), Color(0xFFEA580C))
        fileItem.fileType == FileType.AUDIO -> Triple(Icons.Default.AudioFile, Color(0xFFFCE7F3), Color(0xFFDB2777))
        fileItem.fileType == FileType.ARCHIVE -> Triple(Icons.Default.Archive, Color(0xFFFEF3C7), Color(0xFFD97706))
        fileItem.fileType == FileType.APK -> Triple(Icons.Default.Android, Color(0xFFE8F5E9), Color(0xFF43A047))
        fileItem.fileType == FileType.CODE -> Triple(Icons.Default.Code, Color(0xFFDCFCE7), Color(0xFF16A34A))
        else -> Triple(Icons.Default.InsertDriveFile, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 4).dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size((size * 0.58).dp)
        )
    }
}
