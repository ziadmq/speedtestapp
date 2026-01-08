package com.mobix.speedtest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val download: Double,
    val maxDownload: Double,
    val avgDownload: Double,
    val upload: Double,
    val maxUpload: Double,
    val avgUpload: Double,
    val ping: Int,
    val jitter: Int,
    val packetLoss: Double,
    val bufferbloat: Int,
    val serverName: String,
    val serverLocation: String,
    val networkType: String,
    val ssid: String?,
    val isp: String?,
    val ipAddress: String?,
    val signalStrength: Int,
    val timestamp: Long
)