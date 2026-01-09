package com.mobix.speedtest.domain.repository

import com.mobix.speedtest.domain.models.SpeedResult
import kotlinx.coroutines.flow.Flow

interface SpeedTestRepository {
    fun startSpeedTest(): Flow<SpeedResult>
}