package com.example.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.FileUtils
import java.io.File

@Composable
fun VideoPlayerDialog(
    initialFile: File,
    videoList: List<File> = listOf(initialFile),
    onDismiss: () -> Unit
) {
    val actualList = remember(videoList, initialFile) {
        if (videoList.isNotEmpty() && videoList.contains(initialFile)) videoList else listOf(initialFile)
    }

    var currentIndex by remember {
        mutableStateOf(actualList.indexOf(initialFile).coerceAtLeast(0))
    }

    val currentFile = actualList.getOrElse(currentIndex) { initialFile }
    var currentVideoView by remember { mutableStateOf<VideoView?>(null) }

    fun playNext() {
        if (currentIndex < actualList.size - 1) {
            currentIndex++
        }
    }

    fun playPrevious() {
        if (currentIndex > 0) {
            currentIndex--
        }
    }

    // Update video source when currentFile changes
    LaunchedEffect(currentFile) {
        currentVideoView?.apply {
            setVideoURI(Uri.fromFile(currentFile))
            start()
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
            // Video View Container
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        val mediaController = MediaController(context)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                        setVideoURI(Uri.fromFile(currentFile))
                        setOnPreparedListener { mp ->
                            mp.isLooping = false
                            start()
                        }
                        setOnCompletionListener {
                            // Auto-play next video if available
                            if (currentIndex < actualList.size - 1) {
                                playNext()
                            }
                        }
                        currentVideoView = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )

            // Top Bar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.65f))
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
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
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
                        text = FileUtils.formatFileSize(currentFile.length()),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            // Bottom Quick Playlist Switch Bar (if more than 1 video)
            if (actualList.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = { playPrevious() },
                            enabled = currentIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Video Sebelumnya",
                                tint = if (currentIndex > 0) Color.White else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "Video ${currentIndex + 1} dari ${actualList.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        IconButton(
                            onClick = { playNext() },
                            enabled = currentIndex < actualList.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Video Berikutnya",
                                tint = if (currentIndex < actualList.size - 1) Color.White else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
