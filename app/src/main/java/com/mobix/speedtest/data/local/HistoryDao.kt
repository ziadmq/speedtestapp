package com.mobix.speedtest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: HistoryEntity)

    @Query("SELECT * FROM speed_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Delete
    suspend fun deleteResult(result: HistoryEntity)
}