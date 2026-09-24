package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class CropAspectRatio(val label: String, val ratioWidth: Float?, val ratioHeight: Float?) {
    ORIGINAL("Asli", null, null),
    SQUARE("1:1 (Persegi)", 1f, 1f),
    PHOTO_4_3("4:3 (Foto)", 4f, 3f),
    WIDE_16_9("16:9 (Layar Lebar)", 16f, 9f),
    STORY_9_16("9:16 (Story)", 9f, 16f),
    PORTRAIT_3_4("3:4 (Potret)", 3f, 4f)
}

enum class CropPosition(val label: String) {
    CENTER("Tengah"),
    START("Kiri / Atas"),
    END("Kanan / Bawah")
}

@Composable
fun ImageEditorDialog(
    imageFile: File,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Active edit tab: "PUTAR" or "POTONG"
    var activeTab by remember { mutableStateOf(0) } // 0: Putar, 1: Potong

    // Crop settings
    var selectedCropRatio by remember { mutableStateOf(CropAspectRatio.ORIGINAL) }
    var selectedCropPos by remember { mutableStateOf(CropPosition.CENTER) }

    LaunchedEffect(imageFile) {
        withContext(Dispatchers.IO) {
            val bmp = BitmapFactory.decodeFile(imageFile.absolutePath)
            originalBitmap = bmp
            currentBitmap = bmp
        }
    }

    fun rotateImage(degrees: Float) {
        val src = currentBitmap ?: return
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        currentBitmap = rotated
    }

    fun flipHorizontal() {
        val src = currentBitmap ?: return
        val matrix = Matrix().apply { postScale(-1f, 1f, src.width / 2f, src.height / 2f) }
        val flipped = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        currentBitmap = flipped
    }

    fun applyCrop(ratio: CropAspectRatio, pos: CropPosition) {
        val src = currentBitmap ?: return
        if (ratio.ratioWidth == null || ratio.ratioHeight == null) return

        val targetRatio = ratio.ratioWidth / ratio.ratioHeight
        val currentRatio = src.width.toFloat() / src.height.toFloat()

        var cropWidth = src.width
        var cropHeight = src.height
        var cropX = 0
        var cropY = 0

        if (currentRatio > targetRatio) {
            // Gambar lebih lebar dari rasio target -> potong lebar
            cropWidth = (src.height * targetRatio).toInt().coerceAtMost(src.width)
            val diffX = src.width - cropWidth
            cropX = when (pos) {
                CropPosition.START -> 0
                CropPosition.CENTER -> diffX / 2
                CropPosition.END -> diffX
            }
        } else {
            // Gambar lebih tinggi dari rasio target -> potong tinggi
            cropHeight = (src.width / targetRatio).toInt().coerceAtMost(src.height)
            val diffY = src.height - cropHeight
            cropY = when (pos) {
                CropPosition.START -> 0
                CropPosition.CENTER -> diffY / 2
                CropPosition.END -> diffY
            }
        }

        if (cropWidth > 0 && cropHeight > 0) {
            val cropped = Bitmap.createBitmap(src, cropX, cropY, cropWidth, cropHeight)
            currentBitmap = cropped
        }
    }

    fun resetToOriginal() {
        currentBitmap = originalBitmap
        selectedCropRatio = CropAspectRatio.ORIGINAL
        selectedCropPos = CropPosition.CENTER
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edit Gambar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = imageFile.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    // Reset button
                    IconButton(
                        onClick = { resetToOriginal() },
                        enabled = currentBitmap != originalBitmap
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Reset Semula",
                            tint = if (currentBitmap != originalBitmap) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Image Preview Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    currentBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Preview Gambar",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    } ?: CircularProgressIndicator()

                    // Resolution badge at bottom
                    currentBitmap?.let { bmp ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = "${bmp.width} × ${bmp.height} px",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector: Putar vs Potong
                TabRow(
                    selectedTabIndex = activeTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Putar Gambar", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Potong Gambar", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                if (activeTab == 0) {
                    // Controls for ROTATE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Pilih arah putar atau balik:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(
                                onClick = { rotateImage(-90f) }
                            ) {
                                Icon(Icons.Default.RotateLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("-90° (Kiri)")
                            }

                            FilledTonalButton(
                                onClick = { rotateImage(90f) }
                            ) {
                                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+90° (Kanan)")
                            }

                            OutlinedButton(
                                onClick = { flipHorizontal() }
                            ) {
                                Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Balik")
                            }
                        }
                    }
                } else {
                    // Controls for CROP
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "1. Pilih Rasio Potong:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(CropAspectRatio.entries) { ratio ->
                                val isSelected = selectedCropRatio == ratio
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCropRatio = ratio
                                        if (ratio != CropAspectRatio.ORIGINAL) {
                                            applyCrop(ratio, selectedCropPos)
                                        }
                                    },
                                    label = { Text(ratio.label, fontSize = 12.sp) }
                                )
                            }
                        }

                        if (selectedCropRatio != CropAspectRatio.ORIGINAL) {
                            Text(
                                text = "2. Posisi Pemotongan:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CropPosition.entries.forEach { pos ->
                                    val isSelected = selectedCropPos == pos
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedCropPos = pos
                                            applyCrop(selectedCropRatio, pos)
                                        },
                                        label = { Text(pos.label, fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Actions: Batal & Simpan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val finalBmp = currentBitmap ?: return@Button
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
                        enabled = !isSaving && currentBitmap != null
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Simpan Perubahan")
                    }
                }
            }
        }
    }
}
