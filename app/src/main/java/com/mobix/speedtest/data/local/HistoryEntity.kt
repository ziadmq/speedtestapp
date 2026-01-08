package com.mobix.speedtest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val download: Double,
    val upload: Double,
    val ping: Int,
    val jitter: Int,
    val packetLoss: Double,
    val serverName: String,
    val networkType: String,
    val timestamp: Long
)