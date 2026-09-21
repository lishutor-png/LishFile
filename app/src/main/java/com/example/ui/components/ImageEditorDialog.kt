package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ImageEditorTab(val title: String) {
    TRANSFORM("Putar & Balik"),
    FILTERS("Filter"),
    ADJUST("Penyesuaian"),
    CROP("Pangkas"),
    DRAW("Coret")
}

enum class ColorFilterPreset(val label: String) {
    NONE("Asli"),
    GRAYSCALE("Hitam Putih"),
    SEPIA("Sepia Retro"),
    VIVID("Kontras Tinggi"),
    BRIGHT("Cerah"),
    WARM("Hangat"),
    COOL("Dingin"),
    INVERT("Negatif")
}

enum class CropPreset(val label: String, val ratioX: Float, val ratioY: Float) {
    ORIGINAL("Asli", 0f, 0f),
    SQUARE_1_1("1:1 Persegi", 1f, 1f),
    PHOTO_4_3("4:3 Foto", 4f, 3f),
    WIDE_16_9("16:9 Lanskap", 16f, 9f),
    PORTRAIT_9_16("9:16 Vertikal", 9f, 16f)
}

data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun ImageEditorDialog(
    file: File,
    onDismiss: () -> Unit,
    onSaveSuccess: (File) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Transformation States
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }

    // Filter & Adjust States
    var selectedFilter by remember { mutableStateOf(ColorFilterPreset.NONE) }
    var brightness by remember { mutableFloatStateOf(0f) }      // -100 to +100
    var contrast by remember { mutableFloatStateOf(1f) }        // 0.5 to 2.0
    var saturation by remember { mutableFloatStateOf(1f) }      // 0.0 to 2.0

    // Crop State
    var selectedCrop by remember { mutableStateOf(CropPreset.ORIGINAL) }

    // Drawing State
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var currentStrokePoints = remember { mutableStateListOf<Offset>() }
    var brushColor by remember { mutableStateOf(Color(0xFFEF4444)) } // Red default
    var brushWidth by remember { mutableFloatStateOf(6f) }

    var activeTab by remember { mutableStateOf(ImageEditorTab.TRANSFORM) }
    var showSaveOptions by remember { mutableStateOf(false) }

    // Load original bitmap safely
    LaunchedEffect(file) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)

                // Subsample if image is overly large
                val maxDim = 1920
                var sampleSize = 1
                while ((options.outWidth / sampleSize) > maxDim || (options.outHeight / sampleSize) > maxDim) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inMutable = true
                }
                val loaded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                withContext(Dispatchers.Main) {
                    originalBitmap = loaded
                    previewBitmap = loaded
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(context, "Gagal memuat gambar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Function to re-render preview bitmap whenever filter, adjust, rotation, or flip changes
    fun renderEditedBitmap(source: Bitmap?): Bitmap? {
        if (source == null) return null
        return try {
            val matrix = Matrix()
            if (rotationDegrees != 0) {
                matrix.postRotate(rotationDegrees.toFloat())
            }
            val scaleX = if (flipHorizontal) -1f else 1f
            val scaleY = if (flipVertical) -1f else 1f
            if (flipHorizontal || flipVertical) {
                matrix.postScale(scaleX, scaleY)
            }

            var transformed = Bitmap.createBitmap(
                source, 0, 0, source.width, source.height, matrix, true
            )

            // Apply Crop if selected and not original
            if (selectedCrop != CropPreset.ORIGINAL && selectedCrop.ratioX > 0f) {
                val targetAspect = selectedCrop.ratioX / selectedCrop.ratioY
                val currentAspect = transformed.width.toFloat() / transformed.height.toFloat()

                val cropWidth: Int
                val cropHeight: Int
                if (currentAspect > targetAspect) {
                    // Current is wider than target: trim width
                    cropHeight = transformed.height
                    cropWidth = (cropHeight * targetAspect).toInt().coerceAtMost(transformed.width)
                } else {
                    // Current is taller than target: trim height
                    cropWidth = transformed.width
                    cropHeight = (cropWidth / targetAspect).toInt().coerceAtMost(transformed.height)
                }
                val cropX = ((transformed.width - cropWidth) / 2).coerceAtLeast(0)
                val cropY = ((transformed.height - cropHeight) / 2).coerceAtLeast(0)

                val cropped = Bitmap.createBitmap(transformed, cropX, cropY, cropWidth, cropHeight)
                if (cropped != transformed) {
                    transformed = cropped
                }
            }

            // Apply ColorMatrix (Filter + Adjustments)
            val cm = ColorMatrix()
            // 1. Preset filter
            when (selectedFilter) {
                ColorFilterPreset.NONE -> {}
                ColorFilterPreset.GRAYSCALE -> {
                    val gray = ColorMatrix()
                    gray.setSaturation(0f)
                    cm.postConcat(gray)
                }
                ColorFilterPreset.SEPIA -> {
                    val sepiaMatrix = ColorMatrix(
                        floatArrayOf(
                            0.393f, 0.769f, 0.189f, 0f, 0f,
                            0.349f, 0.686f, 0.168f, 0f, 0f,
                            0.272f, 0.534f, 0.131f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    cm.postConcat(sepiaMatrix)
                }
                ColorFilterPreset.VIVID -> {
                    val vivid = ColorMatrix()
                    vivid.setSaturation(1.6f)
                    cm.postConcat(vivid)
                }
                ColorFilterPreset.BRIGHT -> {
                    val bright = ColorMatrix(
                        floatArrayOf(
                            1f, 0f, 0f, 0f, 40f,
                            0f, 1f, 0f, 0f, 40f,
                            0f, 0f, 1f, 0f, 40f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    cm.postConcat(bright)
                }
                ColorFilterPreset.WARM -> {
                    val warm = ColorMatrix(
                        floatArrayOf(
                            1.2f, 0f, 0f, 0f, 20f,
                            0f, 1.0f, 0f, 0f, 0f,
                            0f, 0f, 0.85f, 0f, -15f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    cm.postConcat(warm)
                }
                ColorFilterPreset.COOL -> {
                    val cool = ColorMatrix(
                        floatArrayOf(
                            0.85f, 0f, 0f, 0f, -15f,
                            0f, 1.0f, 0f, 0f, 0f,
                            0f, 0f, 1.25f, 0f, 25f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    cm.postConcat(cool)
                }
                ColorFilterPreset.INVERT -> {
                    val invert = ColorMatrix(
                        floatArrayOf(
                            -1f, 0f, 0f, 0f, 255f,
                            0f, -1f, 0f, 0f, 255f,
                            0f, 0f, -1f, 0f, 255f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    cm.postConcat(invert)
                }
            }

            // 2. Custom brightness/contrast/saturation adjustments
            if (saturation != 1f) {
                val satMatrix = ColorMatrix()
                satMatrix.setSaturation(saturation)
                cm.postConcat(satMatrix)
            }
            if (brightness != 0f || contrast != 1f) {
                val scale = contrast
                val translate = (-0.5f * scale + 0.5f) * 255f + brightness
                val adjustMatrix = ColorMatrix(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(adjustMatrix)
            }

            val resultBitmap = Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(resultBitmap)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(transformed, 0f, 0f, paint)

            resultBitmap
        } catch (e: Exception) {
            source
        }
    }

    // Save final bitmap to file
    fun saveImage(overwrite: Boolean) {
        if (originalBitmap == null) return
        isSaving = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val finalBitmap = renderEditedBitmap(originalBitmap)
                if (finalBitmap == null) {
                    withContext(Dispatchers.Main) {
                        isSaving = false
                        Toast.makeText(context, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Burn doodle strokes onto final bitmap if any exist
                if (strokes.isNotEmpty()) {
                    val canvas = android.graphics.Canvas(finalBitmap)
                    strokes.forEach { stroke ->
                        if (stroke.points.size > 1) {
                            val paint = Paint().apply {
                                color = stroke.color.toArgb()
                                strokeWidth = stroke.strokeWidth * (finalBitmap.width / 400f).coerceAtLeast(1f)
                                style = Paint.Style.STROKE
                                strokeCap = Paint.Cap.ROUND
                                strokeJoin = Paint.Join.ROUND
                                isAntiAlias = true
                            }
                            val path = android.graphics.Path()
                            val first = stroke.points.first()
                            path.moveTo(first.x * finalBitmap.width, first.y * finalBitmap.height)
                            for (i in 1 until stroke.points.size) {
                                val pt = stroke.points[i]
                                path.lineTo(pt.x * finalBitmap.width, pt.y * finalBitmap.height)
                            }
                            canvas.drawPath(path, paint)
                        }
                    }
                }

                val destFile = if (overwrite) {
                    file
                } else {
                    val nameWithoutExt = file.nameWithoutExtension
                    val ext = file.extension.ifEmpty { "jpg" }
                    var candidate = File(file.parentFile, "${nameWithoutExt}_edit.$ext")
                    var count = 1
                    while (candidate.exists()) {
                        candidate = File(file.parentFile, "${nameWithoutExt}_edit_$count.$ext")
                        count++
                    }
                    candidate
                }

                val format = if (destFile.extension.equals("png", ignoreCase = true)) {
                    Bitmap.CompressFormat.PNG
                } else if (destFile.extension.equals("webp", ignoreCase = true)) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                } else {
                    Bitmap.CompressFormat.JPEG
                }

                FileOutputStream(destFile).use { out ->
                    finalBitmap.compress(format, 93, out)
                }

                withContext(Dispatchers.Main) {
                    isSaving = false
                    Toast.makeText(
                        context,
                        if (overwrite) "Gambar berhasil ditimpa!" else "Salinan baru disimpan: ${destFile.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onSaveSuccess(destFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSaving = false
                    Toast.makeText(context, "Gagal menyimpan gambar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Update preview when adjustments change
    LaunchedEffect(rotationDegrees, flipHorizontal, flipVertical, selectedFilter, brightness, contrast, saturation, selectedCrop, originalBitmap) {
        if (originalBitmap != null) {
            withContext(Dispatchers.Default) {
                val updated = renderEditedBitmap(originalBitmap)
                withContext(Dispatchers.Main) {
                    previewBitmap = updated
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101418)),
            color = Color(0xFF101418)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar in Image Editor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF181F26))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_image_editor_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = file.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        originalBitmap?.let { bmp ->
                            Text(
                                text = "${bmp.width} × ${bmp.height} • ${com.example.model.StorageStats.formatBytes(file.length())}",
                                fontSize = 11.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }

                    // Save Button
                    Button(
                        onClick = { showSaveOptions = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("save_image_button"),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Middle: Interactive Image Canvas & Drawing Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0B0F14)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else if (previewBitmap != null) {
                        var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "Pratinjau Gambar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )

                            // Drawing Overlay Canvas (active in DRAW mode)
                            if (activeTab == ImageEditorTab.DRAW) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .pointerInput(brushColor, brushWidth) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    if (canvasSize.width > 0 && canvasSize.height > 0) {
                                                        currentStrokePoints.clear()
                                                        currentStrokePoints.add(
                                                            Offset(offset.x / canvasSize.width, offset.y / canvasSize.height)
                                                        )
                                                    }
                                                },
                                                onDrag = { change, _ ->
                                                    change.consume()
                                                    if (canvasSize.width > 0 && canvasSize.height > 0) {
                                                        currentStrokePoints.add(
                                                            Offset(change.position.x / canvasSize.width, change.position.y / canvasSize.height)
                                                        )
                                                    }
                                                },
                                                onDragEnd = {
                                                    if (currentStrokePoints.size > 1) {
                                                        strokes.add(
                                                            DrawStroke(
                                                                points = currentStrokePoints.toList(),
                                                                color = brushColor,
                                                                strokeWidth = brushWidth
                                                            )
                                                        )
                                                    }
                                                    currentStrokePoints.clear()
                                                }
                                            )
                                        }
                                ) {
                                    canvasSize = size

                                    // Draw completed strokes
                                    strokes.forEach { stroke ->
                                        if (stroke.points.size > 1) {
                                            for (i in 0 until stroke.points.size - 1) {
                                                val start = Offset(stroke.points[i].x * size.width, stroke.points[i].y * size.height)
                                                val end = Offset(stroke.points[i + 1].x * size.width, stroke.points[i + 1].y * size.height)
                                                drawLine(
                                                    color = stroke.color,
                                                    start = start,
                                                    end = end,
                                                    strokeWidth = stroke.strokeWidth,
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    }

                                    // Draw current active stroke
                                    if (currentStrokePoints.size > 1) {
                                        for (i in 0 until currentStrokePoints.size - 1) {
                                            val start = Offset(currentStrokePoints[i].x * size.width, currentStrokePoints[i].y * size.height)
                                            val end = Offset(currentStrokePoints[i + 1].x * size.width, currentStrokePoints[i + 1].y * size.height)
                                            drawLine(
                                                color = brushColor,
                                                start = start,
                                                end = end,
                                                strokeWidth = brushWidth,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Panel: Control Deck with Tabs & Specific Controls
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF181F26),
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        // Dynamic Control Panel depending on active tab
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(105.dp)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (activeTab) {
                                ImageEditorTab.TRANSFORM -> {
                                    TransformControls(
                                        onRotateLeft = { rotationDegrees = (rotationDegrees - 90 + 360) % 360 },
                                        onRotateRight = { rotationDegrees = (rotationDegrees + 90) % 360 },
                                        onFlipHorizontal = { flipHorizontal = !flipHorizontal },
                                        onFlipVertical = { flipVertical = !flipVertical },
                                        onReset = {
                                            rotationDegrees = 0
                                            flipHorizontal = false
                                            flipVertical = false
                                        }
                                    )
                                }
                                ImageEditorTab.FILTERS -> {
                                    FilterControls(
                                        selectedFilter = selectedFilter,
                                        onSelectFilter = { selectedFilter = it }
                                    )
                                }
                                ImageEditorTab.ADJUST -> {
                                    AdjustControls(
                                        brightness = brightness,
                                        onBrightnessChange = { brightness = it },
                                        contrast = contrast,
                                        onContrastChange = { contrast = it },
                                        saturation = saturation,
                                        onSaturationChange = { saturation = it },
                                        onReset = {
                                            brightness = 0f
                                            contrast = 1f
                                            saturation = 1f
                                        }
                                    )
                                }
                                ImageEditorTab.CROP -> {
                                    CropControls(
                                        selectedCrop = selectedCrop,
                                        onSelectCrop = { selectedCrop = it }
                                    )
                                }
                                ImageEditorTab.DRAW -> {
                                    DrawControls(
                                        selectedColor = brushColor,
                                        onSelectColor = { brushColor = it },
                                        strokeWidth = brushWidth,
                                        onStrokeWidthChange = { brushWidth = it },
                                        onUndo = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.size - 1) },
                                        onClear = { strokes.clear() }
                                    )
                                }
                            }
                        }

                        // Tab Selection Row
                        TabRow(
                            selectedTabIndex = activeTab.ordinal,
                            containerColor = Color(0xFF13181E),
                            contentColor = Color.White
                        ) {
                            ImageEditorTab.entries.forEach { tab ->
                                Tab(
                                    selected = activeTab == tab,
                                    onClick = { activeTab = tab },
                                    text = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                            color = if (activeTab == tab) MaterialTheme.colorScheme.primary else Color(0xFFB0BEC5)
                                        )
                                    },
                                    modifier = Modifier.testTag("editor_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Save Confirmation Options BottomSheet / Dialog
            if (showSaveOptions) {
                SaveOptionsDialog(
                    fileName = file.name,
                    onDismiss = { showSaveOptions = false },
                    onSaveNewCopy = {
                        showSaveOptions = false
                        saveImage(overwrite = false)
                    },
                    onOverwriteOriginal = {
                        showSaveOptions = false
                        saveImage(overwrite = true)
                    }
                )
            }
        }
    }
}

@Composable
private fun TransformControls(
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(icon = Icons.Default.RotateLeft, label = "Putar Kiri", onClick = onRotateLeft)
        ActionButton(icon = Icons.Default.RotateRight, label = "Putar Kanan", onClick = onRotateRight)
        ActionButton(icon = Icons.Default.Flip, label = "Balik H", onClick = onFlipHorizontal)
        ActionButton(icon = Icons.Default.Rotate90DegreesCw, label = "Balik V", onClick = onFlipVertical)
        ActionButton(icon = Icons.Default.RestartAlt, label = "Reset", onClick = onReset)
    }
}

@Composable
private fun FilterControls(
    selectedFilter: ColorFilterPreset,
    onSelectFilter: (ColorFilterPreset) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(ColorFilterPreset.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onSelectFilter(filter) },
                label = { Text(filter.label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF222B35),
                    labelColor = Color(0xFFECEFF1)
                )
            )
        }
    }
}

@Composable
private fun AdjustControls(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    var adjustMode by remember { mutableIntStateOf(0) } // 0: Kecerahan, 1: Kontras, 2: Saturasi

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Kecerahan", "Kontras", "Saturasi").forEachIndexed { index, name ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (adjustMode == index) MaterialTheme.colorScheme.primary else Color(0xFF242E38),
                        modifier = Modifier.clickable { adjustMode = index }
                    ) {
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            TextButton(
                onClick = onReset,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("Reset", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (adjustMode) {
            0 -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Nilai: ${brightness.toInt()}", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(60.dp))
                    Slider(
                        value = brightness,
                        onValueChange = onBrightnessChange,
                        valueRange = -80f..80f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
            1 -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Nilai: ${String.format("%.1fx", contrast)}", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(60.dp))
                    Slider(
                        value = contrast,
                        onValueChange = onContrastChange,
                        valueRange = 0.5f..1.8f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
            2 -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Nilai: ${String.format("%.1fx", saturation)}", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(60.dp))
                    Slider(
                        value = saturation,
                        onValueChange = onSaturationChange,
                        valueRange = 0.0f..2.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun CropControls(
    selectedCrop: CropPreset,
    onSelectCrop: (CropPreset) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(CropPreset.entries) { preset ->
            FilterChip(
                selected = selectedCrop == preset,
                onClick = { onSelectCrop(preset) },
                label = { Text(preset.label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF222B35),
                    labelColor = Color(0xFFECEFF1)
                )
            )
        }
    }
}

@Composable
private fun DrawControls(
    selectedColor: Color,
    onSelectColor: (Color) -> Unit,
    strokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit
) {
    val palette = listOf(
        Color(0xFFEF4444), // Merah
        Color(0xFFF59E0B), // Kuning
        Color(0xFF10B981), // Hijau
        Color(0xFF3B82F6), // Biru
        Color(0xFF8B5CF6), // Ungu
        Color(0xFFFFFFFF), // Putih
        Color(0xFF000000)  // Hitam
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (color == Color.White || color == Color.Black) {
                                    Modifier.border(1.dp, Color.Gray, CircleShape)
                                } else Modifier
                            )
                            .then(
                                if (selectedColor == color) {
                                    Modifier.border(2.dp, Color.Cyan, CircleShape)
                                } else Modifier
                            )
                            .clickable { onSelectColor(color) }
                    )
                }
            }

            Row {
                IconButton(onClick = onUndo, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tebal Kuas: ${strokeWidth.toInt()}px", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(100.dp))
            Slider(
                value = strokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = 3f..24f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFCFD8DC),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SaveOptionsDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onSaveNewCopy: () -> Unit,
    onOverwriteOriginal: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Simpan Hasil Edit", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column {
                Text(
                    text = "Pilih bagaimana Anda ingin menyimpan hasil editan gambar '$fileName':",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSaveNewCopy),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Simpan Sebagai Salinan Baru", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Membuat file gambar baru tanpa mengubah file asli", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOverwriteOriginal),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Timpa File Asli", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Mengganti file gambar yang ada dengan versi editan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
