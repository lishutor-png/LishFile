package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SizeFilter
import com.example.model.SortBy
import com.example.model.SortOrder
import com.example.ui.theme.AppThemeMode
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LishFileTopBar(
    currentDir: File,
    isRoot: Boolean,
    onNavigateUp: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showHiddenFiles: Boolean,
    onToggleHiddenFiles: () -> Unit,
    isGridView: Boolean,
    onToggleGridView: () -> Unit,
    currentSortBy: SortBy,
    currentSortOrder: SortOrder,
    onSortChange: (SortBy, SortOrder) -> Unit,
    themeMode: AppThemeMode,
    onThemeToggle: () -> Unit,
    onLogoClick: (() -> Unit)? = null,
    // Multi-Select
    isMultiSelectMode: Boolean = false,
    selectedCount: Int = 0,
    onCloseMultiSelect: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onCopySelected: () -> Unit = {},
    onCutSelected: () -> Unit = {},
    onShareSelected: () -> Unit = {},
    onZipSelected: () -> Unit = {},
    onStartMultiSelect: () -> Unit = {},
    // Search Advanced Filters
    searchExtension: String = "",
    onExtensionFilterChange: (String) -> Unit = {},
    searchSizeFilter: SizeFilter = SizeFilter.ALL,
    onSizeFilterChange: (SizeFilter) -> Unit = {},
    searchEntireStorage: Boolean = false,
    onToggleSearchEntireStorage: () -> Unit = {},
    // Duplicate Scanner
    onOpenDuplicateScanner: () -> Unit = {},
    // Dashboard & Home
    isHomeDashboard: Boolean = false,
    onNavigateToDashboard: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onOpenVip: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var isMoreMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isMultiSelectMode) {
                // Multi-Select Contextual Top Bar
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    navigationIcon = {
                        IconButton(onClick = onCloseMultiSelect, modifier = Modifier.testTag("close_multiselect_button")) {
                            Icon(Icons.Default.Close, contentDescription = "Batalkan pemilihan")
                        }
                    },
                    title = {
                        Text(
                            text = "$selectedCount dipilih",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = onSelectAll, modifier = Modifier.testTag("select_all_button")) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Pilih Semua")
                        }
                        if (selectedCount > 0) {
                            IconButton(onClick = onCopySelected, modifier = Modifier.testTag("copy_selected_button")) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Salin")
                            }
                            IconButton(onClick = onCutSelected, modifier = Modifier.testTag("cut_selected_button")) {
                                Icon(Icons.Default.ContentCut, contentDescription = "Potong")
                            }
                            IconButton(onClick = onZipSelected, modifier = Modifier.testTag("zip_selected_button")) {
                                Icon(Icons.Default.Archive, contentDescription = "Kompres ke ZIP")
                            }
                            IconButton(onClick = onShareSelected, modifier = Modifier.testTag("share_selected_button")) {
                                Icon(Icons.Default.Share, contentDescription = "Bagikan")
                            }
                            IconButton(onClick = onDeleteSelected, modifier = Modifier.testTag("delete_selected_button")) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus")
                            }
                        }
                    }
                )
            } else if (isHomeDashboard) {
                // Home Category Dashboard Top Bar (Matching reference image)
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("drawer_menu_button")) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Navigasi")
                        }
                    },
                    title = {
                        Text(
                            text = "File Manager +",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        // VIP Crown Icon (Golden)
                        IconButton(onClick = onOpenVip, modifier = Modifier.testTag("vip_crown_button")) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Fitur Premium",
                                tint = Color(0xFFF59E0B)
                            )
                        }

                        // Overflow Menu
                        Box {
                            IconButton(
                                onClick = { isMoreMenuExpanded = true },
                                modifier = Modifier.testTag("more_options_button")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu lainnya")
                            }

                            DropdownMenu(
                                expanded = isMoreMenuExpanded,
                                onDismissRequest = { isMoreMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Segarkan") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    onClick = {
                                        isMoreMenuExpanded = false
                                        onRefresh()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pindai File Duplikat") },
                                    leadingIcon = { Icon(Icons.Default.Difference, contentDescription = null) },
                                    onClick = {
                                        isMoreMenuExpanded = false
                                        onOpenDuplicateScanner()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (themeMode == AppThemeMode.DARK) "Tema Terang" else "Tema Gelap") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        isMoreMenuExpanded = false
                                        onThemeToggle()
                                    }
                                )
                                if (onLogoClick != null) {
                                    DropdownMenuItem(
                                        text = { Text("Brankas Pribadi (Terkunci)") },
                                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            isMoreMenuExpanded = false
                                            onLogoClick()
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            } else {
                // Standard Directory & Search Top Bar
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        if (isSearchExpanded) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = { Text("Cari nama file atau ekstensi...", fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("search_text_input"),
                                shape = RoundedCornerShape(26.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Hapus teks")
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (onLogoClick != null) {
                                            Modifier
                                                .clickable { onLogoClick() }
                                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                        } else {
                                            Modifier.padding(vertical = 4.dp)
                                        }
                                    )
                                    .testTag("file_manager_logo_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "File Manager Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isRoot) "File Manager +" else currentDir.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isRoot) "Penyimpanan Utama" else currentDir.parentFile?.name ?: "Folder",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        if (isSearchExpanded) {
                            IconButton(
                                onClick = {
                                    isSearchExpanded = false
                                    onSearchQueryChange("")
                                    onExtensionFilterChange("")
                                    onSizeFilterChange(SizeFilter.ALL)
                                },
                                modifier = Modifier.testTag("close_search_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tutup pencarian")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (!isRoot) onNavigateUp() else onNavigateToDashboard()
                                },
                                modifier = Modifier.testTag("nav_up_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                            }
                        }
                    },
                    actions = {
                        if (!isSearchExpanded) {
                            // Home Dashboard Button
                            IconButton(
                                onClick = onNavigateToDashboard,
                                modifier = Modifier.testTag("home_dashboard_button")
                            ) {
                                Icon(Icons.Default.Home, contentDescription = "Beranda Kategori")
                            }

                            // Search Button
                            IconButton(
                                onClick = { isSearchExpanded = true },
                                modifier = Modifier.testTag("open_search_button")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Cari file")
                            }

                            // Duplicate Scanner Action Icon
                            IconButton(
                                onClick = onOpenDuplicateScanner,
                                modifier = Modifier.testTag("duplicate_scanner_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Difference,
                                    contentDescription = "Pindai Berkas Duplikat",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Grid / List Toggle
                            IconButton(
                                onClick = onToggleGridView,
                                modifier = Modifier.testTag("toggle_grid_view_button")
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = if (isGridView) "Beralih ke tampilan daftar" else "Beralih ke tampilan kisi"
                                )
                            }

                            // Sort Menu
                            Box {
                                IconButton(
                                    onClick = { isSortMenuExpanded = true },
                                    modifier = Modifier.testTag("sort_menu_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Urutkan file")
                                }

                                DropdownMenu(
                                    expanded = isSortMenuExpanded,
                                    onDismissRequest = { isSortMenuExpanded = false }
                                ) {
                                    Text(
                                        text = "Urutkan Berdasarkan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                    SortBy.entries.forEach { sortBy ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(sortBy.label, fontSize = 14.sp)
                                                    if (currentSortBy == sortBy) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                isSortMenuExpanded = false
                                                onSortChange(sortBy, currentSortOrder)
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Urutan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                    SortOrder.entries.forEach { sortOrder ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(sortOrder.label, fontSize = 14.sp)
                                                    if (currentSortOrder == sortOrder) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                isSortMenuExpanded = false
                                                onSortChange(currentSortBy, sortOrder)
                                            }
                                        )
                                    }
                                }
                            }

                            // Overflow Menu
                            Box {
                                IconButton(
                                    onClick = { isMoreMenuExpanded = true },
                                    modifier = Modifier.testTag("more_options_button")
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu lainnya")
                                }

                                DropdownMenu(
                                    expanded = isMoreMenuExpanded,
                                    onDismissRequest = { isMoreMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Pilih Banyak Berkas") },
                                        leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                                        onClick = {
                                            isMoreMenuExpanded = false
                                            onStartMultiSelect()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (showHiddenFiles) "Sembunyikan Berkas Tersembunyi" else "Tampilkan Berkas Tersembunyi") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (showHiddenFiles) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            isMoreMenuExpanded = false
                                            onToggleHiddenFiles()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Ganti Tema (Gelap/Terang)") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            isMoreMenuExpanded = false
                                            onThemeToggle()
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // Search Advanced Filter Bar when Search is active
            AnimatedVisibility(visible = isSearchExpanded && !isMultiSelectMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Extension filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ekstensi:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val extensions = listOf("" to "Semua", "pdf" to "PDF", "jpg" to "JPG", "png" to "PNG", "mp4" to "MP4", "mp3" to "MP3", "zip" to "ZIP", "txt" to "TXT")
                        extensions.forEach { (ext, label) ->
                            FilterChip(
                                selected = searchExtension == ext,
                                onClick = { onExtensionFilterChange(ext) },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("filter_ext_$label")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Size filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ukuran:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SizeFilter.entries.forEach { sf ->
                            FilterChip(
                                selected = searchSizeFilter == sf,
                                onClick = { onSizeFilterChange(sf) },
                                label = { Text(sf.label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("filter_size_${sf.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Deep Search across Entire Storage toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cari di Seluruh Penyimpanan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = searchEntireStorage,
                            onCheckedChange = { onToggleSearchEntireStorage() },
                            modifier = Modifier.testTag("search_entire_storage_switch")
                        )
                    }
                }
            }
        }
    }
}
