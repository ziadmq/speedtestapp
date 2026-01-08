package com.mobix.speedtest.domain.models

import java.util.Date

data class SpeedResult(
    val id: Long = 0,
    val downloadSpeed: Double, // Mbps
    val uploadSpeed: Double,   // Mbps
    val ping: Int,             // ms
    val jitter: Int,           // ms
    val packetLoss: Double,    // %
    val serverName: String,
    val networkType: String,   // Wi-Fi, 5G, etc.
    val timestamp: Date = Date()
)