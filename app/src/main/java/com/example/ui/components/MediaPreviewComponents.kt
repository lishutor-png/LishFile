package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.FileItem
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun AudioPlayerView(file: File) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }

    DisposableEffect(file) {
        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                duration = this.duration
                setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                }
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memuat audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    currentPosition = player.currentPosition
                }
            }
            delay(500)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = file.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                onValueChange = { fraction ->
                    val newPos = (fraction * duration).toInt()
                    mediaPlayer?.seekTo(newPos)
                    currentPosition = newPos
                },
                modifier = Modifier.fillMaxWidth().testTag("audio_seek_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(currentPosition), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDuration(duration), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newPos = (currentPosition - 5000).coerceAtLeast(0)
                        mediaPlayer?.seekTo(newPos)
                        currentPosition = newPos
                    }
                ) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Mundur 5 detik")
                }

                IconButton(
                    onClick = {
                        mediaPlayer?.let { player ->
                            if (player.isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("audio_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Jeda" else "Putar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val newPos = (currentPosition + 5000).coerceAtMost(duration)
                        mediaPlayer?.seekTo(newPos)
                        currentPosition = newPos
                    }
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = "Maju 5 detik")
                }
            }
        }
    }
}

@Composable
fun PdfViewer(file: File) {
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(file, currentPageIndex) {
        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        var currentPage: PdfRenderer.Page? = null

        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)
            totalPages = pdfRenderer.pageCount

            if (totalPages > 0 && currentPageIndex < totalPages) {
                currentPage = pdfRenderer.openPage(currentPageIndex)
                val bitmap = Bitmap.createBitmap(
                    currentPage.width,
                    currentPage.height,
                    Bitmap.Config.ARGB_8888
                )
                currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pageBitmap = bitmap
            }
        } catch (e: Exception) {
            errorMessage = "Gagal merender PDF: ${e.message}"
        }

        onDispose {
            currentPage?.close()
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (errorMessage != null) {
            Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        } else if (pageBitmap != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Image(
                    bitmap = pageBitmap!!.asImageBitmap(),
                    contentDescription = "Halaman PDF",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                    enabled = currentPageIndex > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Halaman Sebelumnya")
                }

                Text(
                    text = "Halaman ${currentPageIndex + 1} dari $totalPages",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                    enabled = currentPageIndex < totalPages - 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Halaman Selanjutnya")
                }
            }
        }
    }
}

@Composable
fun VideoLaunchView(file: File) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.VideoFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = file.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    try {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Tidak ada pemutar video ditemukan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("launch_video_player_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Putar Video")
            }
        }
    }
}

private fun formatDuration(millis: Int): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
