package com.mobix.speedtest.data.repository

import android.os.SystemClock
import com.mobix.speedtest.domain.models.SpeedResult
import com.mobix.speedtest.domain.repository.SpeedTestRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class SpeedTestRepositoryImpl : SpeedTestRepository {

    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val downloadUrl = "https://speed.cloudflare.com/__down?bytes=50000000"
    private val pingUrl = "https://speed.cloudflare.com/__down?bytes=1"
    private val uploadUrl = "https://speed.cloudflare.com/__up"

    private val parallelConnections = 4
    private val tickMs = 250L
    private val testDurationMs = 10_000L

    override fun startSpeedTest(): Flow<SpeedResult> = flow {
        emit(SpeedResult(serverName = "جاري التعرف على الشبكة..."))
        val networkInfo = fetchNetworkInfo()

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

        emit(current.copy(serverName = "جاري اختبار التحميل..."))
        val dl = runDownloadTest(durationMs = testDurationMs) { instant, maxVal, avgVal ->
            emit(current.copy(downloadSpeed = instant, maxDownloadSpeed = maxVal, avgDownloadSpeed = avgVal))
        }
        current = current.copy(downloadSpeed = dl.avgMbps, maxDownloadSpeed = dl.maxMbps, avgDownloadSpeed = dl.avgMbps)

        emit(current.copy(serverName = "جاري اختبار الرفع..."))
        val ul = runUploadTest(durationMs = testDurationMs) { instant, maxVal, avgVal ->
            emit(current.copy(uploadSpeed = instant, maxUploadSpeed = maxVal, avgUploadSpeed = avgVal))
        }

        emit(current.copy(uploadSpeed = ul.avgMbps, maxUploadSpeed = ul.maxMbps, avgUploadSpeed = ul.avgMbps, serverName = "اكتمل الاختبار"))

    }.flowOn(Dispatchers.IO)

    // ... (دوال fetchNetworkInfo و measureRealPingStats و runDownloadTest و runUploadTest كما هي لديك)
    // ملاحظة: تأكد من بقاء منطق قياس السرعة كما هو في الكود السابق لديك

    private suspend fun fetchNetworkInfo(): Pair<String, String> = withContext(Dispatchers.IO) {
        val apiUrl = "https://ipapi.co/json/"
        try {
            val request = Request.Builder().url(apiUrl).header("User-Agent", "Mozilla/5.0").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonData = JSONObject(response.body?.string() ?: "")
                    Pair(jsonData.optString("ip", "Unknown IP"), jsonData.optString("org", "Unknown ISP"))
                } else Pair("N/A", "Server Error")
            }
        } catch (e: Exception) { Pair("Offline", "Check Connection") }
    }

    private fun measureRealPingStats(url: String, attempts: Int): PingStats {
        val rtts = mutableListOf<Long>()
        var failed = 0
        repeat(attempts) {
            val start = System.nanoTime()
            try {
                client.newCall(Request.Builder().url(url).head().header("Cache-Control", "no-store").build()).execute().use {
                    if (it.isSuccessful) rtts += (System.nanoTime() - start) / 1_000_000 else failed++
                }
            } catch (_: Exception) { failed++ }
        }
        return PingStats(rtts.minOrNull()?.toDouble() ?: 999.0, rtts.average(), if (rtts.size >= 2) rtts.zipWithNext { a, b -> abs(a - b).toDouble() }.average() else 0.0, (failed.toDouble() / attempts) * 100.0)
    }

    private fun bytesToMbps(bytes: Long, seconds: Double): Double = if (seconds <= 0.0) 0.0 else (bytes * 8.0) / (seconds * 1_000_000.0)

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
            val elapsed = (SystemClock.elapsedRealtime() - start) / 1000.0
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
}



