package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedFolderDao {
    @Query("SELECT * FROM locked_folders ORDER BY lockedAt DESC")
    fun getAllLockedFolders(): Flow<List<LockedFolderEntity>>

    @Query("SELECT * FROM locked_folders WHERE folderPath = :path LIMIT 1")
    suspend fun getLockedFolderByPath(path: String): LockedFolderEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM locked_folders WHERE folderPath = :path)")
    suspend fun isFolderLocked(path: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLockedFolder(folder: LockedFolderEntity)

    @Update
    suspend fun updateLockedFolder(folder: LockedFolderEntity)

    @Query("DELETE FROM locked_folders WHERE folderPath = :path")
    suspend fun deleteLockedFolder(path: String)
}

@Dao
interface TransferHistoryDao {
    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC LIMIT 100")
    fun getAllTransferHistory(): Flow<List<TransferHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferHistoryEntity): Long

    @Query("DELETE FROM transfer_history")
    suspend fun clearHistory()
}
