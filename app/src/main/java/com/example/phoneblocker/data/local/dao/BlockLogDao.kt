package com.example.phoneblocker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.phoneblocker.data.local.entity.BlockLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockLogDao {
    @Query("SELECT * FROM block_log ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<BlockLog>>

    @Insert
    suspend fun insertLog(log: BlockLog)
    
    @Query("DELETE FROM block_log")
    suspend fun clearLogs()
}
