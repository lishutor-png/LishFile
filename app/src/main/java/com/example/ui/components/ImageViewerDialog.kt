package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import kotlin.math.abs

@Composable
fun ImageViewerDialog(
    initialFile: File,
    imageList: List<File> = listOf(initialFile),
    onDismiss: () -> Unit,
    onEditImage: (File) -> Unit
) {
    val context = LocalContext.current
    val actualList = remember(imageList, initialFile) {
        if (imageList.isNotEmpty() && imageList.contains(initialFile)) imageList else listOf(initialFile)
    }

    var currentIndex by remember {
        mutableStateOf(actualList.indexOf(initialFile).coerceAtLeast(0))
    }

    val currentFile = actualList.getOrElse(currentIndex) { initialFile }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showInfoSheet by remember { mutableStateOf(false) }

    // Zoom & pan states
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Load bitmap whenever currentFile changes
    LaunchedEffect(currentFile) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        withContext(Dispatchers.IO) {
            try {
                bitmap = BitmapFactory.decodeFile(currentFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun goToNext() {
        if (currentIndex < actualList.size - 1) {
            currentIndex++
        }
    }

    fun goToPrevious() {
        if (currentIndex > 0) {
            currentIndex--
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
            // Drag gesture detector for swiping when scale == 1f, or zoom gesture when zooming
            var totalDragX by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentFile, scale) {
                        if (scale > 1.05f) {
                            // Zoom / Pan mode
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        } else {
                            // Swipe detection mode
                            detectDragGestures(
                                onDragEnd = {
                                    if (totalDragX < -80f) {
                                        goToNext()
                                    } else if (totalDragX > 80f) {
                                        goToPrevious()
                                    }
                                    totalDragX = 0f
                                },
                                onDragCancel = { totalDragX = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount.x
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentFile,
                    transitionSpec = {
                        val isForward = actualList.indexOf(targetState) >= actualList.indexOf(initialState)
                        if (isForward) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "ImageTransition"
                ) { targetImg ->
                    var itemBitmap by remember(targetImg) { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(targetImg) {
                        withContext(Dispatchers.IO) {
                            try {
                                itemBitmap = BitmapFactory.decodeFile(targetImg.absolutePath)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (itemBitmap != null) {
                            Image(
                                bitmap = itemBitmap!!.asImageBitmap(),
                                contentDescription = targetImg.name,
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
                        } else {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
            }

            // Floating Navigation Arrows (Left & Right)
            if (actualList.size > 1) {
                // Previous Button
                if (currentIndex > 0) {
                    IconButton(
                        onClick = { goToPrevious() },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Gambar Sebelumnya",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Next Button
                if (currentIndex < actualList.size - 1) {
                    IconButton(
                        onClick = { goToNext() },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Gambar Berikutnya",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Top Bar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.6f))
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentFile.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (actualList.size > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = "${currentIndex + 1} / ${actualList.size}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${FileUtils.formatFileSize(currentFile.length())} • Geser untuk pindah foto",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = { showInfoSheet = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                }
                IconButton(onClick = { FileUtils.shareFile(context, currentFile) }) {
                    Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = Color.White)
                }
                IconButton(onClick = {
                    onDismiss()
                    onEditImage(currentFile)
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
                            Text("Nama: ${currentFile.name}", fontWeight = FontWeight.Medium)
                            Text("Posisi: ${currentIndex + 1} dari ${actualList.size} foto")
                            Text("Ukuran: ${FileUtils.formatFileSize(currentFile.length())}")
                            if (bitmap != null) {
                                Text("Resolusi: ${bitmap!!.width} × ${bitmap!!.height} px")
                            }
                            Text("Format: ${currentFile.extension.uppercase()}")
                            Text("Diubah: ${FileUtils.formatDate(currentFile.lastModified())}")
                            Text(
                                "Lokasi: ${currentFile.parent}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
