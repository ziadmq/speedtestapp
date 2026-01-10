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

class SpeedTestRepositoryImpl : SpeedTestRepository {

    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val downloadUrl = "https://speed.cloudflare.com/__down?bytes=50000000"
    private val pingUrl = "https://speed.cloudflare.com/__down?bytes=1"
    private val uploadUrl = "https://speed.cloudflare.com/__up"

    private val parallelConnections = 4
    private val tickMs = 250L
    private val testDurationMs = 8_000L // 8 ثوانٍ كافية للاستقرار والدقة

    override fun startSpeedTest(): Flow<SpeedResult> = flow {
        emit(SpeedResult(serverName = "جاري الاتصال..."))
        val networkInfo = fetchNetworkInfo()

        // 1. حساب Ping & Jitter بدقة عالية مع Warm-up
        emit(SpeedResult(
            serverName = "حساب Ping & Jitter...",
            ipAddress = networkInfo.first,
            isp = networkInfo.second
        ))
        val pingStats = measureRealPingStats(attempts = 12)

        var current = SpeedResult(
            ping = pingStats.minMs.toInt(),
            jitter = pingStats.jitterMs.toInt(),
            packetLoss = pingStats.lossPercent,
            ipAddress = networkInfo.first,
            isp = networkInfo.second,
            serverName = "Cloudflare Global Edge",
            timestamp = Date()
        )
        emit(current)

        // 2. اختبار التحميل (Download)
        emit(current.copy(serverName = "اختبار التحميل..."))
        val dl = runSpeedTest(isDownload = true) { instant, maxVal ->
            // هنا يتم إرسال السرعة كـ Double لضمان الكسور العشرية
            emit(current.copy(downloadSpeed = instant, maxDownloadSpeed = maxVal))
        }
        current = current.copy(downloadSpeed = dl.avgMbps, maxDownloadSpeed = dl.maxMbps)

        // 3. اختبار الرفع (Upload)
        emit(current.copy(serverName = "اختبار الرفع..."))
        val ul = runSpeedTest(isDownload = false) { instant, maxVal ->
            emit(current.copy(uploadSpeed = instant, maxUploadSpeed = maxVal))
        }

        emit(current.copy(
            uploadSpeed = ul.avgMbps,
            maxUploadSpeed = ul.maxMbps,
            serverName = "اكتمل الاختبار"
        ))

    }.flowOn(Dispatchers.IO)

    private fun measureRealPingStats(attempts: Int): PingStats {
        val rtts = mutableListOf<Long>()
        // Warm-up call: تجاهل الطلب الأول لتهيئة الاتصال
        try {
            client.newCall(Request.Builder().url(pingUrl).head().build()).execute().close()
        } catch (_: Exception) {}

        repeat(attempts) {
            val start = System.nanoTime()
            try {
                val request = Request.Builder()
                    .url(pingUrl)
                    .head()
                    .header("Cache-Control", "no-store")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        rtts += (System.nanoTime() - start) / 1_000_000
                    }
                }
            } catch (_: Exception) {}
        }

        if (rtts.isEmpty()) return PingStats(999.0, 999.0, 0.0, 100.0)

        // حساب الـ Jitter كمتوسط الفروقات المطلقة بين العينات المتتالية (المعيار العالمي)
        val jitter = if (rtts.size > 1) {
            rtts.zipWithNext { a, b -> abs(a - b) }.average()
        } else 0.0

        return PingStats(rtts.minOrNull()?.toDouble() ?: 0.0, rtts.average(), jitter, 0.0)
    }

    private suspend fun runSpeedTest(isDownload: Boolean, onUpdate: suspend (Double, Double) -> Unit): SpeedMeasure = coroutineScope {
        val totalBytes = AtomicLong(0L)
        val start = SystemClock.elapsedRealtime()
        val endAt = start + testDurationMs
        var lastBytes = 0L
        var maxMbps = 0.0

        val jobs = List(parallelConnections) {
            launch(Dispatchers.IO) {
                try {
                    if (isDownload) {
                        val request = Request.Builder().url(downloadUrl).header("Cache-Control", "no-store").build()
                        client.newCall(request).execute().use { resp ->
                            val input = resp.body?.byteStream() ?: return@use
                            val buffer = ByteArray(32 * 1024)
                            while (isActive && SystemClock.elapsedRealtime() < endAt) {
                                val n = input.read(buffer)
                                if (n <= 0) break
                                totalBytes.addAndGet(n.toLong())
                            }
                        }
                    } else {
                        val body = object : RequestBody() {
                            override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
                            override fun writeTo(sink: okio.BufferedSink) {
                                val chunk = ByteArray(32 * 1024)
                                while (SystemClock.elapsedRealtime() < endAt) {
                                    sink.write(chunk)
                                    totalBytes.addAndGet(chunk.size.toLong())
                                }
                            }
                        }
                        client.newCall(Request.Builder().url(uploadUrl).post(body).build()).execute().close()
                    }
                } catch (_: Exception) {}
            }
        }

        while (SystemClock.elapsedRealtime() < endAt) {
            delay(tickMs)
            val currentTotal = totalBytes.get()
            val deltaBytes = currentTotal - lastBytes
            lastBytes = currentTotal

            // حساب السرعة اللحظية خلال الـ 250ms الأخيرة (Instant Speed) لظهور الكسور
            val instantMbps = (deltaBytes * 8.0) / (tickMs / 1000.0 * 1_000_000.0)
            if (instantMbps > 0) maxMbps = max(maxMbps, instantMbps)

            onUpdate(instantMbps, maxMbps)
        }

        jobs.forEach { it.cancel() }
        val finalAvg = (totalBytes.get() * 8.0) / (testDurationMs / 1000.0 * 1_000_000.0)
        SpeedMeasure(finalAvg, maxMbps)
    }

    private suspend fun fetchNetworkInfo(): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            client.newCall(Request.Builder().url("https://ipapi.co/json/").build()).execute().use { response ->
                val json = JSONObject(response.body?.string() ?: "")
                Pair(json.optString("ip", "N/A"), json.optString("org", "Unknown ISP"))
            }
        } catch (_: Exception) { Pair("N/A", "Unknown") }
    }

    private data class PingStats(val minMs: Double, val avgMs: Double, val jitterMs: Double, val lossPercent: Double)
    private data class SpeedMeasure(val avgMbps: Double, val maxMbps: Double)
}