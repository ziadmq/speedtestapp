package com.mobix.speedtest.ui.screens.tools

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobix.speedtest.utils.DeviceInfo
import com.mobix.speedtest.utils.NetworkUtils
import com.mobix.speedtest.utils.WifiTechDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max

data class NetworkToolsState(
    val localIp: String = "0.0.0.0",
    val routerIp: String = "0.0.0.0",
    val routerName: String = "",
    val ispName: String = "جاري التحميل...",
    val wifi: WifiTechDetails? = null,
    val devices: List<DeviceInfo> = emptyList(),
    val isScanning: Boolean = false,
    val lastUpdated: String = "",
    val note: String = "" // show why scan may be empty
)

@HiltViewModel
class NetworkToolsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(NetworkToolsState())
    val state = _state.asStateFlow()

    private val httpClient = OkHttpClient()

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isScanning = true, lastUpdated = nowString(), note = "")

            try {
                val pack = withContext(Dispatchers.IO) {
                    // Must be on Wi-Fi for local LAN scan
                    if (!isOnWifi(context)) {
                        return@withContext Pack(
                            localIp = NetworkUtils.getLocalIpAddress().ifBlank { "0.0.0.0" },
                            routerIp = "0.0.0.0",
                            routerName = "",
                            wifi = runCatching { NetworkUtils.getWifiAdvancedDetails(context) }.getOrNull(),
                            isp = fetchIspSafe(),
                            devices = emptyList(),
                            note = "⚠️ يجب الاتصال بالواي فاي لعرض الأجهزة المتصلة."
                        )
                    }

                    val dhcp = getDhcpInfoSafe(context)
                    val localIp = dhcp.localIp
                    val routerIp = dhcp.gatewayIp
                    val routerName = runCatching { NetworkUtils.getRouterHostName(routerIp) }.getOrNull().orEmpty()
                    val wifi = runCatching { NetworkUtils.getWifiAdvancedDetails(context) }.getOrNull()
                    val isp = fetchIspSafe()

                    val devices = scanDevicesUsingRealSubnet(
                        localIp = localIp,
                        gatewayIp = routerIp,
                        netmask = dhcp.netmask
                    ).let { list ->
                        // Always insert router at top
                        val routerItem = DeviceInfo(
                            name = if (routerName.isNotBlank()) "Router • $routerName" else "Router",
                            ip = routerIp
                        )
                        val others = list.filter { it.ip != routerIp }
                        buildList {
                            add(routerItem)
                            addAll(others)
                        }
                    }

                    val note = if (devices.size <= 1) {
                        "⚠️ لم يتم العثور على أجهزة أخرى. قد يكون Client Isolation مفعل في الراوتر/الشبكة."
                    } else ""

                    Pack(
                        localIp = localIp,
                        routerIp = routerIp,
                        routerName = routerName,
                        wifi = wifi,
                        isp = isp,
                        devices = devices,
                        note = note
                    )
                }

                _state.value = _state.value.copy(
                    localIp = pack.localIp,
                    routerIp = pack.routerIp,
                    routerName = pack.routerName,
                    wifi = pack.wifi,
                    ispName = pack.isp,
                    devices = pack.devices,
                    isScanning = false,
                    lastUpdated = nowString(),
                    note = pack.note
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isScanning = false,
                    lastUpdated = nowString(),
                    devices = emptyList(),
                    note = "⚠️ حدث خطأ أثناء الفحص."
                )
            }
        }
    }

    /* ----------------------------- Real Subnet Scan ----------------------------- */

    private data class ArpEntry(val ip: String, val mac: String)

    private suspend fun scanDevicesUsingRealSubnet(
        localIp: String,
        gatewayIp: String,
        netmask: Int
    ): List<DeviceInfo> = withContext(Dispatchers.IO) {
        val localInt = ipToInt(localIp)
        val mask = if (netmask == 0) 0xFFFFFF00.toInt() else netmask // fallback /24

        val network = localInt and mask
        val broadcast = network or mask.inv()

        // Host range
        val start = network + 1
        val end = broadcast - 1

        // Safety clamp: don’t scan crazy huge subnets
        val maxHosts = 512
        val hostCount = max(0, end - start + 1)
        val step = if (hostCount > maxHosts) (hostCount / maxHosts) else 1

        // 1) Warm ARP by UDP + TCP probes (fast)
        activeProbeRange(
            start = start,
            end = end,
            step = step,
            skip = localInt
        )

        // 2) Read ARP table
        val arp = readArpTable()
        val valid = arp
            .filter { it.ip != "0.0.0.0" }
            .distinctBy { it.ip }

        // 3) Build names (fallback)
        valid
            .filter { it.ip != localIp }
            .map { e ->
                val name = if (e.ip == gatewayIp) "Router" else buildFallbackName(e.mac, e.ip)
                DeviceInfo(name = name, ip = e.ip)
            }
            .sortedBy { lastOctetOrMax(it.ip) }
    }

    private suspend fun activeProbeRange(
        start: Int,
        end: Int,
        step: Int,
        skip: Int
    ) = coroutineScope {
        val concurrency = 80
        val ports = intArrayOf(80, 443, 22, 53)
        val timeoutMs = 220

        val tasks = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()

        var i = start
        var idx = 0
        while (i <= end) {
            if (i != skip) {
                val ipStr = intToIp(i)

                if (tasks.size >= concurrency) {
                    tasks.awaitAll()
                    tasks.clear()
                }

                val p1 = ports[idx % ports.size]
                val p2 = ports[(idx + 2) % ports.size]
                idx++

                tasks += async(Dispatchers.IO) {
                    // UDP poke (fills ARP in many routers)
                    udpPoke(ipStr, 9)
                    udpPoke(ipStr, 5353)

                    // TCP connect attempt (also triggers ARP)
                    tcpTry(ipStr, p1, timeoutMs)
                    tcpTry(ipStr, p2, timeoutMs)
                }
            }
            i += step
        }

        if (tasks.isNotEmpty()) tasks.awaitAll()
    }

    private fun udpPoke(ip: String, port: Int) {
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 150
                val addr = InetAddress.getByName(ip)
                val payload = byteArrayOf(0x0)
                val packet = DatagramPacket(payload, payload.size, addr, port)
                socket.send(packet)
            }
        } catch (_: Exception) {}
    }

    private fun tcpTry(ip: String, port: Int, timeoutMs: Int) {
        try {
            Socket().use { s ->
                s.tcpNoDelay = true
                s.connect(InetSocketAddress(ip, port), timeoutMs)
            }
        } catch (_: Exception) {}
    }

    private fun readArpTable(): List<ArpEntry> {
        return try {
            val file = File("/proc/net/arp")
            if (!file.exists()) return emptyList()

            val result = mutableListOf<ArpEntry>()
            BufferedReader(FileReader(file)).use { br ->
                br.readLine() // header
                while (true) {
                    val line = br.readLine() ?: break
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 4) {
                        val ip = parts[0].trim()
                        val mac = parts[3].trim().lowercase(Locale.US)
                        if (ip.isNotBlank() && mac.contains(":") && mac != "00:00:00:00:00:00") {
                            result += ArpEntry(ip, mac)
                        }
                    }
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildFallbackName(mac: String, ip: String): String {
        val suffix = mac.takeLast(5).replace(":", "").uppercase(Locale.US)
        val last = ip.substringAfterLast(".")
        return "Device • $suffix (#$last)"
    }

    /* ----------------------------- DHCP / Wi-Fi helpers ----------------------------- */

    private data class DhcpPack(val localIp: String, val gatewayIp: String, val netmask: Int)

    private fun getDhcpInfoSafe(ctx: Context): DhcpPack {
        val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifiManager.dhcpInfo

        val localIp = intToIp(dhcp.ipAddress)
        val gateway = intToIp(dhcp.gateway)
        val mask = dhcp.netmask // can be 0 on some devices; handled above

        return DhcpPack(
            localIp = if (localIp.isBlank()) "0.0.0.0" else localIp,
            gatewayIp = if (gateway.isBlank()) "0.0.0.0" else gateway,
            netmask = mask
        )
    }

    private fun isOnWifi(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    // DHCP ints are little-endian on Android
    private fun intToIp(ip: Int): String {
        return String.format(
            Locale.US,
            "%d.%d.%d.%d",
            (ip and 0xff),
            (ip shr 8 and 0xff),
            (ip shr 16 and 0xff),
            (ip shr 24 and 0xff)
        )
    }

    private fun ipToInt(ip: String): Int {
        val parts = ip.split(".")
        if (parts.size != 4) return 0
        val b0 = parts[0].toIntOrNull() ?: 0
        val b1 = parts[1].toIntOrNull() ?: 0
        val b2 = parts[2].toIntOrNull() ?: 0
        val b3 = parts[3].toIntOrNull() ?: 0
        return (b0 and 0xff) or ((b1 and 0xff) shl 8) or ((b2 and 0xff) shl 16) or ((b3 and 0xff) shl 24)
    }

    private fun lastOctetOrMax(ip: String): Int {
        val last = ip.substringAfterLast(".", "")
        return last.toIntOrNull() ?: Int.MAX_VALUE
    }

    /* ----------------------------- ISP + time ----------------------------- */

    private fun fetchIspSafe(): String {
        return try {
            val request = Request.Builder()
                .url("https://ipapi.co/json/")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "Unknown ISP"
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return "Unknown ISP"
                val json = JSONObject(body)
                json.optString("org", "Unknown ISP")
            }
        } catch (_: Exception) {
            "Offline / No Internet"
        }
    }

    private fun nowString(): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    }

    private data class Pack(
        val localIp: String,
        val routerIp: String,
        val routerName: String,
        val wifi: WifiTechDetails?,
        val isp: String,
        val devices: List<DeviceInfo>,
        val note: String
    )
}
