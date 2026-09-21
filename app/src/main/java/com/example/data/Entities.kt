package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locked_folders")
data class LockedFolderEntity(
    @PrimaryKey
    val folderPath: String,
    val folderName: String,
    val passwordHash: String? = null,
    val passwordSalt: String? = null,
    val isBiometricAllowed: Boolean = true,
    val lockedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transfer_history")
data class TransferHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileSize: Long,
    val direction: String, // "SENT" or "RECEIVED"
    val peerAddress: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = true,
    val isSuccess: Boolean = true
)
