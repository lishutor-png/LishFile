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
    file: File,
    onDismiss: () -> Unit
) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var isReady by remember { mutableStateOf(false) }

    DisposableEffect(file) {
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
        }

        onDispose {
            try {
                if (player.isPlaying) player.stop()
                player.release()
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
                    Text(
                        text = "Pemutar Audio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
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
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${FileUtils.formatFileSize(file.length())} • ${file.extension.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Seekbar
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { frac ->
                        val targetMs = (frac * duration).toInt()
                        currentPosition = targetMs
                        mediaPlayer?.seekTo(targetMs)
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

                // Controls: -10s, Play/Pause, +10s
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newPos = (currentPosition - 10000).coerceAtLeast(0)
                            currentPosition = newPos
                            mediaPlayer?.seekTo(newPos)
                        },
                        enabled = isReady
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Mundur 10 detik")
                    }

                    FilledIconButton(
                        onClick = {
                            mediaPlayer?.let { player ->
                                if (isPlaying) {
                                    player.pause()
                                    isPlaying = false
                                } else {
                                    player.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        enabled = isReady
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newPos = (currentPosition + 10000).coerceAtMost(duration)
                            currentPosition = newPos
                            mediaPlayer?.seekTo(newPos)
                        },
                        enabled = isReady
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Maju 10 detik")
                    }
                }
            }
        }
    }
}
