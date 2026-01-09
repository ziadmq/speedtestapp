package com.mobix.speedtest.ui.screens.tools

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobix.speedtest.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

// تعريف حالة الشاشة
data class NetworkToolsState(
    val localIp: String = "0.0.0.0",
    val routerIp: String = "0.0.0.0",
    val routerName: String = "",
    val ispName: String = "جاري التحميل...",
    val wifi: WifiTechDetails? = null,
    val devices: List<DeviceInfo> = emptyList(),
    val isScanning: Boolean = false
)

@HiltViewModel
class NetworkToolsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(NetworkToolsState())
    val state = _state.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            // جلب معلومات الشبكة المحلية والراوتر
            val routerIp = NetworkUtils.getGatewayIp(context)
            val routerName = NetworkUtils.getRouterHostName(routerIp)

            _state.value = _state.value.copy(
                localIp = NetworkUtils.getLocalIpAddress(),
                routerIp = routerIp,
                routerName = routerName,
                wifi = NetworkUtils.getWifiAdvancedDetails(context),
                isScanning = true
            )

            // إصلاح الخطأ: استخدام withContext بدلاً من استدعاء Dispatcher مباشرة
            try {
                withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val request = Request.Builder().url("https://ipapi.co/json/").build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string() ?: ""
                            if (bodyString.isNotEmpty()) {
                                val json = JSONObject(bodyString)
                                _state.value = _state.value.copy(
                                    ispName = json.optString("org", "Unknown ISP")
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(ispName = "Offline / No Internet")
            }

            // فحص الأجهزة المتصلة بالشبكة
            val found = NetworkUtils.scanLocalNetworkWithNames(_state.value.localIp)
            _state.value = _state.value.copy(devices = found, isScanning = false)
        }
    }
}