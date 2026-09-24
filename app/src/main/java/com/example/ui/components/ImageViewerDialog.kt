package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ImageViewerDialog(
    file: File,
    onDismiss: () -> Unit,
    onEditImage: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showInfoSheet by remember { mutableStateOf(false) }

    // Zoom & pan states
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                bitmap = BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Interactive Zoomable Image
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.8f, 5f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = file.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            } else {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Top Bar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = FileUtils.formatFileSize(file.length()),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { showInfoSheet = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                }
                IconButton(onClick = { FileUtils.shareFile(context, file) }) {
                    Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = Color.White)
                }
                IconButton(onClick = {
                    onDismiss()
                    onEditImage()
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Gambar", tint = Color.White)
                }
            }

            // Info Dialog
            if (showInfoSheet) {
                AlertDialog(
                    onDismissRequest = { showInfoSheet = false },
                    title = { Text("Detail Gambar") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Nama: ${file.name}", fontWeight = FontWeight.Medium)
                            Text("Ukuran: ${FileUtils.formatFileSize(file.length())}")
                            if (bitmap != null) {
                                Text("Resolusi: ${bitmap!!.width} × ${bitmap!!.height} px")
                            }
                            Text("Format: ${file.extension.uppercase()}")
                            Text("Diubah: ${FileUtils.formatDate(file.lastModified())}")
                            Text("Lokasi: ${file.parent}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showInfoSheet = false }) {
                            Text("Tutup")
                        }
                    }
                )
            }
        }
    }
}
