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

    override fun startSpeedTest(): Flow<SpeedResult> = flow {
        // مرحلة 1: زمن الاستجابة (Ping/Jitter)
        emit(SpeedResult(serverName = "جاري اختيار أفضل سيرفر..."))
        delay(1500)

        val baseResult = SpeedResult(
            ping = 18, jitter = 2, packetLoss = 0.0,
            serverName = "Mobix Premium Server",
            serverLocation = "Cairo, Egypt",
            networkType = "Wi-Fi 6"
        )

        // مرحلة 2: اختبار التحميل (Download)
        var currentMaxDown = 0.0
        var totalDown = 0.0
        for (i in 1..20) {
            delay(150)
            val current = (40..120).random().toDouble()
            totalDown += current
            if (current > currentMaxDown) currentMaxDown = current
            emit(baseResult.copy(
                downloadSpeed = current,
                maxDownloadSpeed = currentMaxDown,
                avgDownloadSpeed = totalDown / i
            ))
        }

        val finalDown = baseResult.copy(
            downloadSpeed = currentMaxDown,
            maxDownloadSpeed = currentMaxDown,
            avgDownloadSpeed = totalDown / 20
        )

        // مرحلة 3: اختبار الرفع (Upload)
        var currentMaxUp = 0.0
        var totalUp = 0.0
        for (i in 1..20) {
            delay(150)
            val current = (15..45).random().toDouble()
            totalUp += current
            if (current > currentMaxUp) currentMaxUp = current
            emit(finalDown.copy(
                uploadSpeed = current,
                maxUploadSpeed = currentMaxUp,
                avgUploadSpeed = totalUp / i
            ))
        }
    }

    override suspend fun saveResult(result: SpeedResult) {
        historyDao.insertResult(result.toEntity())
    }

    override fun getHistory(): Flow<List<SpeedResult>> =
        historyDao.getAllHistory().map { list -> list.map { it.toDomain() } }

    override suspend fun deleteResult(result: SpeedResult) {
        historyDao.deleteResult(result.toEntity())
    }
}

// دالات التحويل المصححة
fun SpeedResult.toEntity() = HistoryEntity(
    id = id, download = downloadSpeed, maxDownload = maxDownloadSpeed, avgDownload = avgDownloadSpeed,
    upload = uploadSpeed, maxUpload = maxUploadSpeed, avgUpload = avgUploadSpeed,
    ping = ping, jitter = jitter, packetLoss = packetLoss, bufferbloat = bufferbloat,
    serverName = serverName, serverLocation = serverLocation, networkType = networkType,
    ssid = ssid, isp = isp, ipAddress = ipAddress, signalStrength = signalStrength,
    timestamp = timestamp.time
)

fun HistoryEntity.toDomain() = SpeedResult(
    id = id, downloadSpeed = download, maxDownloadSpeed = maxDownload, avgDownloadSpeed = avgDownload,
    uploadSpeed = upload, maxUploadSpeed = maxUpload, avgUploadSpeed = avgUpload, // تم إصلاح maxUp هنا
    ping = ping, jitter = jitter, packetLoss = packetLoss, bufferbloat = bufferbloat,
    serverName = serverName, serverLocation = serverLocation, networkType = networkType,
    ssid = ssid, isp = isp, ipAddress = ipAddress, signalStrength = signalStrength,
    timestamp = Date(timestamp)
)