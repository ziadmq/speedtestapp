package com.mobix.speedtest.domain.models

data class HeatMapPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val signalStrength: Int, // dBm
    val color: Int // اللون بناءً على القوة (أخضر، أصفر، أحمر)
)