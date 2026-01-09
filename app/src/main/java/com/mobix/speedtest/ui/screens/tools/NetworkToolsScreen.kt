package com.mobix.speedtest.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun NetworkToolsScreen(
    viewModel: NetworkToolsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadData() }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF060B12)).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text("أدوات ومعلومات الشبكة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // داخل الـ LazyColumn في ملف NetworkToolsScreen.kt
            item {
                InfoGroupCard("تفاصيل الشبكة والمزود") {
                    val wifi = state.wifi
                    DetailItem("مزود الخدمة (ISP)", state.ispName, Icons.Default.Business)
                    DetailItem("اسم الشبكة (SSID)", wifi?.ssid ?: "N/A", Icons.Default.Wifi)
                    DetailItem("عنوان IP الراوتر", state.routerIp, Icons.Default.Router)
                    DetailItem("اسم الراوتر", state.routerName, Icons.Default.Dns)
                    DetailItem("إصدار الواي فاي", wifi?.routerType ?: "N/A", Icons.Default.SettingsInputAntenna)
                    DetailItem("الـ IP المحلي", state.localIp, Icons.Default.Language)
                }
            }

            item {
                Text("الأجهزة المتصلة (${state.devices.size})", color = Color(0xFF00D1FF), fontWeight = FontWeight.Bold)
                if (state.isScanning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = Color(0xFF00D1FF))
                }
            }

            items(state.devices) { device ->
                DeviceItem(device.name, device.ip)
            }
        }
    }
}

@Composable
fun DeviceItem(name: String, ip: String) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // أيقونة تتغير حسب الاسم إذا أردت لاحقاً
            Icon(
                imageVector = if (name.contains("Android", true) || name.contains("phone", true))
                    Icons.Default.PhoneAndroid else Icons.Default.Devices,
                contentDescription = null,
                tint = Color(0xFF00D1FF)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("IP: $ip", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun InfoGroupCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color(0xFF121A26), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFF00D1FF), fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}