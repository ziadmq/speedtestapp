package com.mobix.speedtest.data.repository

import android.os.SystemClock
import com.mobix.speedtest.data.local.HistoryDao
import com.mobix.speedtest.data.local.HistoryEntity
import com.mobix.speedtest.domain.models.SpeedResult
import com.mobix.speedtest.domain.repository.SpeedTestRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.json.JSONObject // تأكد من استيراد هذه المكتبة لمعالجة الـ JSON
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class SpeedTestRepositoryImpl(
    private val historyDao: HistoryDao
) : SpeedTestRepository {

    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val downloadUrl = "https://speed.cloudflare.com/__down?bytes=50000000"
    private val pingUrl = "https://speed.cloudflare.com/__down?bytes=1"
    private val uploadUrl = "https://speed.cloudflare.com/__up"
    private val ipApiUrl = "http://ip-api.com/json" // خدمة لجلب الـ IP والـ ISP

    private val parallelConnections = 4
    private val tickMs = 250L
    private val testDurationMs = 10_000L

    override fun startSpeedTest(): Flow<SpeedResult> = flow {
        // 1) جلب معلومات الـ IP والـ ISP أولاً
        emit(SpeedResult(serverName = "جاري التعرف على الشبكة..."))
        val networkInfo = fetchNetworkInfo()

        // 2) قياس Ping و Jitter
        emit(SpeedResult(
            serverName = "جاري حساب Ping...",
            ipAddress = networkInfo.first,
            isp = networkInfo.second
        ))

        val pingStats = measureRealPingStats(url = pingUrl, attempts = 15)

        var current = SpeedResult(
            ping = pingStats.minMs.roundToInt(),
            jitter = pingStats.jitterMs.roundToInt(),
            packetLoss = pingStats.lossPercent,
            ipAddress = networkInfo.first,
            isp = networkInfo.second,
            serverName = "Cloudflare Global Edge",
            networkType = "Internet",
            timestamp = Date()
        )
        emit(current)

        // 3) اختبار التحميل
        emit(current.copy(serverName = "جاري اختبار التحميل..."))
        val dl = runDownloadTest(durationMs = testDurationMs) { instant, maxVal, avgVal ->
            emit(current.copy(
                downloadSpeed = instant,
                maxDownloadSpeed = maxVal,
                avgDownloadSpeed = avgVal
            ))
        }
        current = current.copy(
            downloadSpeed = dl.avgMbps,
            maxDownloadSpeed = dl.maxMbps,
            avgDownloadSpeed = dl.avgMbps
        )

        // 4) اختبار الرفع
        emit(current.copy(serverName = "جاري اختبار الرفع..."))
        val ul = runUploadTest(durationMs = testDurationMs) { instant, maxVal, avgVal ->
            emit(current.copy(
                uploadSpeed = instant,
                maxUploadSpeed = maxVal,
                avgUploadSpeed = avgVal
            ))
        }

        val finalResult = current.copy(
            uploadSpeed = ul.avgMbps,
            maxUploadSpeed = ul.maxMbps,
            avgUploadSpeed = ul.avgMbps,
            serverName = "اكتمل الاختبار"
        )
        emit(finalResult)

    }.flowOn(Dispatchers.IO)

    private suspend fun fetchNetworkInfo(): Pair<String, String> = withContext(Dispatchers.IO) {
        // استخدمنا رابطاً يدعم HTTPS لضمان التوافق والأمان
        val apiUrl = "https://ipapi.co/json/"

        try {
            val request = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "Mozilla/5.0") // بعض الخدمات تطلب User-Agent
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.isNotEmpty()) {
                        val jsonData = JSONObject(bodyString)
                        // في ipapi.co الحقول هي 'ip' و 'org' أو 'asn'
                        val ip = jsonData.optString("ip", "Unknown IP")
                        val isp = jsonData.optString("org", "Unknown ISP")
                        Pair(ip, isp)
                    } else {
                        Pair("N/A", "Empty Response")
                    }
                } else {
                    Pair("Error ${response.code}", "Server Error")
                }
            }
        } catch (e: Exception) {
            // طباعة الخطأ في Logcat لتسهيل التصحيح
            e.printStackTrace()
            Pair("Offline", "Check Connection")
        }
    }

    private fun measureRealPingStats(url: String, attempts: Int): PingStats {
        val rtts = mutableListOf<Long>()
        var failed = 0
        try {
            client.newCall(Request.Builder().url(url).head().build()).execute().close()
        } catch (_: Exception) {}

        repeat(attempts) {
            val start = System.nanoTime()
            try {
                client.newCall(Request.Builder().url(url).head().header("Cache-Control", "no-store").build()).execute().use {
                    if (it.isSuccessful) rtts += (System.nanoTime() - start) / 1_000_000 else failed++
                }
            } catch (_: Exception) { failed++ }
        }
        return PingStats(
            minMs = rtts.minOrNull()?.toDouble() ?: 999.0,
            avgMs = rtts.average(),
            jitterMs = if (rtts.size >= 2) rtts.zipWithNext { a, b -> abs(a - b).toDouble() }.average() else 0.0,
            lossPercent = (failed.toDouble() / attempts) * 100.0
        )
    }

    private fun bytesToMbps(bytes: Long, seconds: Double): Double {
        if (seconds <= 0.0) return 0.0
        return (bytes * 8.0) / (seconds * 1_000_000.0)
    }

    private suspend fun runDownloadTest(durationMs: Long, onUpdate: suspend (Double, Double, Double) -> Unit): SpeedMeasure = coroutineScope {
        val totalBytes = AtomicLong(0L)
        val start = SystemClock.elapsedRealtime()
        val endAt = start + durationMs

        val jobs = List(parallelConnections) {
            launch(Dispatchers.IO) {
                val buffer = ByteArray(64 * 1024)
                while (isActive && SystemClock.elapsedRealtime() < endAt) {
                    try {
                        client.newCall(Request.Builder().url(downloadUrl).get().header("Cache-Control", "no-store").build()).execute().use { resp ->
                            val input = resp.body?.byteStream() ?: return@use
                            while (isActive && SystemClock.elapsedRealtime() < endAt) {
                                val n = input.read(buffer)
                                if (n <= 0) break
                                totalBytes.addAndGet(n.toLong())
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        var maxMbps = 0.0
        while (SystemClock.elapsedRealtime() < endAt) {
            delay(tickMs)
            val now = SystemClock.elapsedRealtime()
            val elapsed = (now - start) / 1000.0
            val currentMbps = bytesToMbps(totalBytes.get(), elapsed)
            maxMbps = max(maxMbps, currentMbps)
            onUpdate(currentMbps, maxMbps, currentMbps)
        }
        jobs.forEach { it.cancel() }
        SpeedMeasure(bytesToMbps(totalBytes.get(), durationMs / 1000.0), maxMbps)
    }

    private suspend fun runUploadTest(durationMs: Long, onUpdate: suspend (Double, Double, Double) -> Unit): SpeedMeasure = coroutineScope {
        val totalBytes = AtomicLong(0L)
        val start = SystemClock.elapsedRealtime()
        val endAt = start + durationMs

        val jobs = List(parallelConnections) {
            launch(Dispatchers.IO) {
                while (isActive && SystemClock.elapsedRealtime() < endAt) {
                    try {
                        val body = object : RequestBody() {
                            override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
                            override fun writeTo(sink: okio.BufferedSink) {
                                val chunk = ByteArray(64 * 1024)
                                while (SystemClock.elapsedRealtime() < endAt) {
                                    sink.write(chunk)
                                    totalBytes.addAndGet(chunk.size.toLong())
                                }
                            }
                        }
                        client.newCall(Request.Builder().url(uploadUrl).post(body).build()).execute().close()
                    } catch (_: Exception) {}
                }
            }
        }

        var maxMbps = 0.0
        while (SystemClock.elapsedRealtime() < endAt) {
            delay(tickMs)
            val elapsed = (SystemClock.elapsedRealtime() - start) / 1000.0
            val currentMbps = bytesToMbps(totalBytes.get(), elapsed)
            maxMbps = max(maxMbps, currentMbps)
            onUpdate(currentMbps, maxMbps, currentMbps)
        }
        jobs.forEach { it.cancel() }
        SpeedMeasure(bytesToMbps(totalBytes.get(), durationMs / 1000.0), maxMbps)
    }

    private data class PingStats(val minMs: Double, val avgMs: Double, val jitterMs: Double, val lossPercent: Double)
    private data class SpeedMeasure(val avgMbps: Double, val maxMbps: Double)

    override suspend fun saveResult(result: SpeedResult) { historyDao.insertResult(result.toEntity()) }
    override fun getHistory(): Flow<List<SpeedResult>> = historyDao.getAllHistory().map { it.map { entity -> entity.toDomain() } }
    override suspend fun deleteResult(result: SpeedResult) { historyDao.deleteResult(result.toEntity()) }
}

// Mappers... (كما في الكود السابق)
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
    uploadSpeed = upload, maxUploadSpeed = maxUpload, avgUploadSpeed = avgUpload,
    ping = ping, jitter = jitter, packetLoss = packetLoss, bufferbloat = bufferbloat,
    serverName = serverName, serverLocation = serverLocation, networkType = networkType,
    ssid = ssid, isp = isp, ipAddress = ipAddress, signalStrength = signalStrength,
    timestamp = Date(timestamp)
)