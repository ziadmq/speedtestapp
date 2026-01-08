package com.mobix.speedtest.domain.models

import java.util.Date

data class SpeedResult(
    val id: Long = 0,
    val downloadSpeed: Double = 0.0,    // السرعة اللحظية
    val maxDownloadSpeed: Double = 0.0, // السرعة القصوى
    val avgDownloadSpeed: Double = 0.0, // متوسط السرعة
    val uploadSpeed: Double = 0.0,
    val maxUploadSpeed: Double = 0.0,
    val avgUploadSpeed: Double = 0.0,
    val ping: Int = 0,
    val jitter: Int = 0,
    val packetLoss: Double = 0.0,
    val bufferbloat: Int = 0,
    val serverName: String = "",
    val serverLocation: String = "",
    val networkType: String = "",
    val ssid: String? = null,
    val isp: String? = null,
    val ipAddress: String? = null,
    val signalStrength: Int = 0,
    val timestamp: Date = Date()
)