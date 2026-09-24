package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ImageFilterPreset {
    ORIGINAL,
    GRAYSCALE,
    SEPIA,
    INVERT,
    WARM,
    COOL,
    BRIGHTEN
}

@Composable
fun ImageEditorDialog(
    imageFile: File,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotationDegrees by remember { mutableStateOf(0f) }
    var selectedFilter by remember { mutableStateOf(ImageFilterPreset.ORIGINAL) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(imageFile) {
        withContext(Dispatchers.IO) {
            val bmp = BitmapFactory.decodeFile(imageFile.absolutePath)
            originalBitmap = bmp
            displayedBitmap = bmp
        }
    }

    fun applyModifications(rot: Float, filter: ImageFilterPreset) {
        val orig = originalBitmap ?: return
        val matrix = Matrix().apply { postRotate(rot) }
        val rotated = Bitmap.createBitmap(orig, 0, 0, orig.width, orig.height, matrix, true)

        val colorMatrix = when (filter) {
            ImageFilterPreset.ORIGINAL -> ColorMatrix()
            ImageFilterPreset.GRAYSCALE -> ColorMatrix().apply { setSaturation(0f) }
            ImageFilterPreset.SEPIA -> ColorMatrix().apply {
                setScale(1f, 0.95f, 0.82f, 1.0f)
            }
            ImageFilterPreset.INVERT -> ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ImageFilterPreset.WARM -> ColorMatrix().apply {
                setScale(1.2f, 1.0f, 0.8f, 1.0f)
            }
            ImageFilterPreset.COOL -> ColorMatrix().apply {
                setScale(0.8f, 1.0f, 1.2f, 1.0f)
            }
            ImageFilterPreset.BRIGHTEN -> ColorMatrix().apply {
                setScale(1.25f, 1.25f, 1.25f, 1.0f)
            }
        }

        val resultBitmap = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(rotated, 0f, 0f, paint)
        displayedBitmap = resultBitmap
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editor Gambar: ${imageFile.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Image preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    displayedBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Preview Gambar",
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action tools: Rotation & Crop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            rotationDegrees = (rotationDegrees + 90f) % 360f
                            applyModifications(rotationDegrees, selectedFilter)
                        }
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Putar 90°")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Color filter presets row
                Text(
                    text = "Preset Filter Warna",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ImageFilterPreset.values()) { preset ->
                        val isSel = selectedFilter == preset
                        Card(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedFilter = preset
                                    applyModifications(rotationDegrees, preset)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = preset.name.lowercase().capitalize(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalBmp = displayedBitmap ?: return@Button
                    isSaving = true
                    try {
                        FileOutputStream(imageFile).use { fos ->
                            finalBmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                        }
                        onSaved()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isSaving = false
                    }
                },
                enabled = !isSaving
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Simpan Perubahan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
