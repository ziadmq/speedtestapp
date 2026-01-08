package com.mobix.speedtest.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IpIspInfo(
    val ip: String,
    val isp: String
)

class IpInfoService(
    private val client: OkHttpClient
) {

    suspend fun getIpAndIsp(): IpIspInfo = withContext(Dispatchers.IO) {

        // API بسيط ومجاني
        val request = Request.Builder()
            .url("https://ipinfo.io/json")
            .header("Cache-Control", "no-store")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to get IP info")
            }

            val body = response.body?.string() ?: ""

            fun find(key: String): String {
                val regex = """"$key"\s*:\s*"([^"]*)"""".toRegex()
                return regex.find(body)?.groupValues?.get(1) ?: "Unknown"
            }

            val ip = find("ip")
            val org = find("org") // هذا هو الـ ISP

            IpIspInfo(
                ip = ip,
                isp = org
            )
        }
    }
}
