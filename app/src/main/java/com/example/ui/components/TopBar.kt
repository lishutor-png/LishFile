package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SizeFilter
import com.example.model.SortBy
import com.example.model.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LishFileTopBar(
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onToggleSearch: () -> Unit,
    isGridView: Boolean,
    onToggleGridView: () -> Unit,
    showHiddenFiles: Boolean,
    onToggleHiddenFiles: () -> Unit,
    onSortChange: (SortBy, SortOrder) -> Unit,
    isMultiSelect: Boolean,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCopySelected: () -> Unit,
    onCutSelected: () -> Unit,
    onMoveSelected: () -> Unit,
    onShareSelected: () -> Unit,
    onZipSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    searchEntireStorage: Boolean,
    onToggleSearchEntireStorage: () -> Unit,
    selectedExtension: String?,
    onSelectExtension: (String?) -> Unit,
    selectedSizeFilter: SizeFilter,
    onSelectSizeFilter: (SizeFilter) -> Unit,
    onLogoClick: () -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column {
        if (isMultiSelect) {
            TopAppBar(
                title = { Text("$selectedCount Dipilih", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClearSelection, modifier = Modifier.testTag("clear_selection_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup Pilihan")
                    }
                },
                actions = {
                    IconButton(onClick = onSelectAll, modifier = Modifier.testTag("select_all_button")) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Pilih Semua")
                    }
                    IconButton(onClick = onMoveSelected) {
                        Icon(Icons.Default.DriveFileMove, contentDescription = "Pindahkan")
                    }
                    IconButton(onClick = onShareSelected) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan")
                    }
                    IconButton(onClick = onZipSelected) {
                        Icon(Icons.Default.Archive, contentDescription = "Kompres ZIP")
                    }
                    IconButton(onClick = onCopySelected, modifier = Modifier.testTag("copy_selected_button")) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin")
                    }
                    IconButton(onClick = onCutSelected, modifier = Modifier.testTag("cut_selected_button")) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Potong")
                    }
                    IconButton(onClick = onDeleteSelected, modifier = Modifier.testTag("delete_selected_button")) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        } else {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text(if (searchEntireStorage) "Cari di seluruh penyimpanan…" else "Cari di folder ini…", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 4.dp)
                                .testTag("search_text_field"),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Bersihkan")
                                    }
                                }
                            }
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onLogoClick() }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Logo LishFile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleSearch, modifier = Modifier.testTag("toggle_search_button")) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Cari"
                        )
                    }
                    IconButton(onClick = onToggleGridView, modifier = Modifier.testTag("toggle_grid_button")) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Ganti Tampilan"
                        )
                    }
                    IconButton(onClick = onToggleHiddenFiles, modifier = Modifier.testTag("toggle_hidden_button")) {
                        Icon(
                            imageVector = if (showHiddenFiles) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showHiddenFiles) "Sembunyikan Berkas Tersembunyi" else "Tampilkan Berkas Tersembunyi",
                            tint = if (showHiddenFiles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSortMenu = true }, modifier = Modifier.testTag("sort_menu_button")) {
                        Icon(Icons.Default.Sort, contentDescription = "Urutkan")
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Nama (A - Z)") },
                            onClick = {
                                onSortChange(SortBy.NAME, SortOrder.ASCENDING)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Nama (Z - A)") },
                            onClick = {
                                onSortChange(SortBy.NAME, SortOrder.DESCENDING)
                                showSortMenu = false
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Ukuran (Terbesar)") },
                            onClick = {
                                onSortChange(SortBy.SIZE, SortOrder.DESCENDING)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ukuran (Terkecil)") },
                            onClick = {
                                onSortChange(SortBy.SIZE, SortOrder.ASCENDING)
                                showSortMenu = false
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Tanggal (Terbaru)") },
                            onClick = {
                                onSortChange(SortBy.DATE, SortOrder.DESCENDING)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Tanggal (Terlama)") },
                            onClick = {
                                onSortChange(SortBy.DATE, SortOrder.ASCENDING)
                                showSortMenu = false
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Tipe File (Ekstensi)") },
                            onClick = {
                                onSortChange(SortBy.TYPE, SortOrder.ASCENDING)
                                showSortMenu = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        // Search Filter Bar (Scope, Extension, Size) when Search is active
        AnimatedVisibility(visible = isSearchActive && !isMultiSelect) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // Scope row & Extension chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = searchEntireStorage,
                        onClick = onToggleSearchEntireStorage,
                        leadingIcon = {
                            Icon(
                                if (searchEntireStorage) Icons.Default.Public else Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(if (searchEntireStorage) "Seluruh Penyimpanan" else "Folder Saat Ini", fontSize = 11.sp) }
                    )

                    // Extension filter chips
                    val extensions = listOf("Semua", "pdf", "jpg", "png", "mp4", "mp3", "zip", "apk", "docx")
                    extensions.forEach { ext ->
                        val isAll = ext == "Semua"
                        val isSelected = if (isAll) selectedExtension == null else selectedExtension.equals(ext, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectExtension(if (isAll) null else ext) },
                            label = { Text(if (isAll) "Semua Ekstensi" else ".$ext", fontSize = 11.sp) }
                        )
                    }
                }

                // Size Filter row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ukuran:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SizeFilter.values().forEach { filter ->
                        val label = when (filter) {
                            SizeFilter.ANY -> "Semua Ukuran"
                            SizeFilter.SMALL -> "< 1 MB"
                            SizeFilter.MEDIUM -> "1 - 50 MB"
                            SizeFilter.LARGE -> "50 - 500 MB"
                            SizeFilter.HUGE -> "> 500 MB"
                            else -> "Semua Ukuran"
                        }
                        FilterChip(
                            selected = selectedSizeFilter == filter,
                            onClick = { onSelectSizeFilter(filter) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}
