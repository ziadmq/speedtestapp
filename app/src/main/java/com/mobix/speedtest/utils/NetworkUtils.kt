package com.mobix.speedtest.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtils {
    fun getGatewayIp(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifiManager.dhcpInfo
        return Formatter.formatIpAddress(dhcp.gateway)
    }

    // محاولة جلب اسم الراوتر التقني (Hostname)
    suspend fun getRouterHostName(routerIp: String): String = withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(routerIp)
            val host = address.hostName
            if (host == routerIp) "Default Gateway" else host
        } catch (e: Exception) { "Router" }
    }

    // جلب الـ IP المحلي للجهاز
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "0.0.0.0"
                    }
                }
            }
        } catch (e: Exception) {}
        return "0.0.0.0"
    }

    fun getDetailedNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val actNw = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "لا يوجد اتصال"

        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "اتصال لاسلكي (Wi-Fi)"
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "بيانات الهاتف (5G/4G)"
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ألياف ضوئية (Fiber)"
            else -> "اتصال خارجي"
        }
    }

    fun getWifiAdvancedDetails(context: Context): WifiTechDetails {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo

        // اكتشاف Wi-Fi 6 باستخدام القيمة الرقمية 6 لتجنب الأخطاء
        val standard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (info.wifiStandard) {
                6 -> "Wi-Fi 6 (802.11ax)"
                5 -> "Wi-Fi 5 (802.11ac)"
                else -> "Wi-Fi 4 / Legacy"
            }
        } else if (info.linkSpeed > 800) "Wi-Fi 6 Ready" else "Wi-Fi 5"

        // تنبيه: SSID سيظهر <unknown ssid> إذا كان الـ GPS مغلقاً في الهاتف
        val ssidName = info.ssid.replace("\"", "").let {
            if (it == "<unknown ssid>") "يرجى تفعيل الموقع (GPS)" else it
        }

        return WifiTechDetails(
            ssid = ssidName,
            routerType = standard,
            frequency = "${info.frequency} MHz",
            signalDbm = "${info.rssi} dBm",
            localIp = getLocalIpAddress()
        )
    }


    // فحص الأجهزة ومحاولة جلب أسمائها الحقيقية
    suspend fun scanLocalNetworkWithNames(localIp: String): List<DeviceInfo> = coroutineScope {
        val subnet = localIp.substringBeforeLast(".")
        if (subnet == "0.0.0") return@coroutineScope emptyList<DeviceInfo>()

        val jobs = (1..254).map { i ->
            async(Dispatchers.IO) {
                val host = "$subnet.$i"
                try {
                    val address = InetAddress.getByName(host)
                    if (address.isReachable(400)) { // زيادة الوقت لجلب الاسم
                        val hostName = address.hostName
                        val name = if (hostName == host) "جهاز متصل" else hostName
                        DeviceInfo(ip = host, name = name)
                    } else null
                } catch (e: Exception) { null }
            }
        }
        jobs.awaitAll().filterNotNull()
    }
    fun getCurrentSignalDbm(context: Context): Int {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.connectionInfo.rssi
    }

    fun getColorForSignal(dbm: Int): Int {
        return when {
            dbm > -50 -> 0xFF00FF00.toInt() // أخضر - ممتاز
            dbm > -70 -> 0xFFFFFF00.toInt() // أصفر - جيد
            else -> 0xFFFF0000.toInt()      // أحمر - ضعيف
        }
    }
}

data class WifiTechDetails(val ssid: String, val routerType: String, val frequency: String, val signalDbm: String, val localIp: String)
data class DeviceInfo(val ip: String, val name: String)