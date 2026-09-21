package com.example.model

import java.io.File

data class DuplicateFileItem(
    val file: File,
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isSelectedForDeletion: Boolean = false
) {
    val formattedSize: String
        get() = StorageStats.formatBytes(size)
}

data class DuplicateGroup(
    val id: String, // Checksum / hash
    val size: Long,
    val files: List<DuplicateFileItem>
) {
    val wastedBytes: Long
        get() = if (files.size > 1) (files.size - 1) * size else 0L

    val formattedSize: String
        get() = StorageStats.formatBytes(size)

    val formattedWasted: String
        get() = StorageStats.formatBytes(wastedBytes)
}

data class DuplicateScanResult(
    val groups: List<DuplicateGroup> = emptyList(),
    val totalDuplicatesFound: Int = 0,
    val totalWastedBytes: Long = 0L,
    val isScanning: Boolean = false,
    val scanProgressText: String = ""
)
