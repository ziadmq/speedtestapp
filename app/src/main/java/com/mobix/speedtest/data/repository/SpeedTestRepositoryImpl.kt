package com.mobix.speedtest.data.repository

import com.mobix.speedtest.data.local.HistoryDao
import com.mobix.speedtest.data.local.HistoryEntity
import com.mobix.speedtest.domain.models.SpeedResult
import com.mobix.speedtest.domain.repository.SpeedTestRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Date

class SpeedTestRepositoryImpl(
    private val historyDao: HistoryDao
) : SpeedTestRepository {

    // محاكاة لعملية قياس السرعة (Simulation) - هنا تضع كود السوكيت لاحقاً
    override fun startSpeedTest(): Flow<SpeedResult> = flow {
        for (i in 1..100) {
            delay(100) // محاكاة وقت المعالجة
            emit(SpeedResult(
                downloadSpeed = (i * 1.5), // قيمة تصاعدية للتجربة
                uploadSpeed = (i * 0.8),
                ping = 20,
                jitter = 2,
                packetLoss = 0.0,
                serverName = "Mobix Cairo Server",
                networkType = "Wi-Fi"
            ))
        }
    }

    override suspend fun saveResult(result: SpeedResult) {
        historyDao.insertResult(result.toEntity())
    }

    override fun getHistory(): Flow<List<SpeedResult>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteResult(result: SpeedResult) {
        historyDao.deleteResult(result.toEntity())
    }
}

// Extension functions للتحويل بين Entity و Domain Model
fun SpeedResult.toEntity() = HistoryEntity(
    download = downloadSpeed, upload = uploadSpeed, ping = ping,
    jitter = jitter, packetLoss = packetLoss, serverName = serverName,
    networkType = networkType, timestamp = timestamp.time
)

fun HistoryEntity.toDomain() = SpeedResult(
    id = id, downloadSpeed = download, uploadSpeed = upload, ping = ping,
    jitter = jitter, packetLoss = packetLoss, serverName = serverName,
    networkType = networkType, timestamp = Date(timestamp)
)