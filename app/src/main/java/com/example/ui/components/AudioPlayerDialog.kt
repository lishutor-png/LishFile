package com.example.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.util.FileUtils
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun AudioPlayerDialog(
    initialFile: File,
    audioList: List<File> = listOf(initialFile),
    onDismiss: () -> Unit
) {
    val actualList = remember(audioList, initialFile) {
        if (audioList.isNotEmpty() && audioList.contains(initialFile)) audioList else listOf(initialFile)
    }

    var currentIndex by remember {
        mutableStateOf(actualList.indexOf(initialFile).coerceAtLeast(0))
    }

    val currentFile = actualList.getOrElse(currentIndex) { initialFile }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var isReady by remember { mutableStateOf(false) }

    fun playTrack(file: File) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val player = MediaPlayer()
        try {
            player.setDataSource(file.absolutePath)
            player.prepare()
            duration = player.duration
            isReady = true
            player.start()
            isPlaying = true
            mediaPlayer = player
        } catch (e: Exception) {
            e.printStackTrace()
        }

        player.setOnCompletionListener {
            isPlaying = false
            currentPosition = duration
            // Auto play next track if available
            if (currentIndex < actualList.size - 1) {
                currentIndex++
            }
        }
    }

    DisposableEffect(currentFile) {
        playTrack(currentFile)

        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Polling progress
    LaunchedEffect(isPlaying) {
        while (isPlaying && mediaPlayer != null) {
            currentPosition = mediaPlayer?.currentPosition ?: 0
            delay(300)
        }
    }

    fun formatDuration(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pemutar Musik",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (actualList.size > 1) {
                            Text(
                                text = "Lagu ${currentIndex + 1} dari ${actualList.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vinyl / Equalizer graphic icon
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentFile.nameWithoutExtension,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${currentFile.extension.uppercase()} • ${FileUtils.formatFileSize(currentFile.length())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar Slider
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                    onValueChange = { frac ->
                        val target = (frac * duration).toInt()
                        mediaPlayer?.seekTo(target)
                        currentPosition = target
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback Controls with Next and Previous Song buttons!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Track (⏮)
                    IconButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                            }
                        },
                        enabled = currentIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Lagu Sebelumnya",
                            modifier = Modifier.size(32.dp),
                            tint = if (currentIndex > 0) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val newPos = (currentPosition - 10000).coerceAtLeast(0)
                            mediaPlayer?.seekTo(newPos)
                            currentPosition = newPos
                        }
                    ) {
                        Icon(
                            Icons.Default.FastRewind,
                            contentDescription = "Mundur 10 dtk",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Play / Pause FAB
                    FloatingActionButton(
                        onClick = {
                            mediaPlayer?.let { p ->
                                if (isPlaying) {
                                    p.pause()
                                    isPlaying = false
                                } else {
                                    p.start()
                                    isPlaying = true
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Fast Forward 10s
                    IconButton(
                        onClick = {
                            val newPos = (currentPosition + 10000).coerceAtMost(duration)
                            mediaPlayer?.seekTo(newPos)
                            currentPosition = newPos
                        }
                    ) {
                        Icon(
                            Icons.Default.FastForward,
                            contentDescription = "Maju 10 dtk",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Next Track (⏭)
                    IconButton(
                        onClick = {
                            if (currentIndex < actualList.size - 1) {
                                currentIndex++
                            }
                        },
                        enabled = currentIndex < actualList.size - 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Lagu Berikutnya",
                            modifier = Modifier.size(32.dp),
                            tint = if (currentIndex < actualList.size - 1) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    }
}
