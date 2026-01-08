package com.mobix.speedtest.domain.repository

import com.mobix.speedtest.domain.models.SpeedResult
import kotlinx.coroutines.flow.Flow

interface SpeedTestRepository {
    // تشغيل الاختبار (سيتم تنفيذ المنطق المعقد في الـ Data Layer)
    fun startSpeedTest(): Flow<SpeedResult>

    // العمليات على السجل (History)
    suspend fun saveResult(result: SpeedResult)
    fun getHistory(): Flow<List<SpeedResult>>
    suspend fun deleteResult(result: SpeedResult)
}