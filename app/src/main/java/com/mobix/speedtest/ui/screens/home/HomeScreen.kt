package com.mobix.speedtest.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobix.speedtest.domain.models.SpeedResult

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit
) {
    val result by viewModel.uiState.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    // تعريف الألوان هنا لاستخدامها داخل الـ Canvas لاحقاً (حل الخطأ)
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // الشريط العلوي
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MOBIX SPEED", fontWeight = FontWeight.Black, color = primaryColor)
            IconButton(onClick = onNavigateToHistory) {
                Icon(Icons.Default.History, contentDescription = "History", tint = primaryColor)
            }
        }

        // كروت الجودة (Ping, Jitter, Loss)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem("PING", "${result?.ping ?: "--"}", "ms")
            StatItem("JITTER", "${result?.jitter ?: "--"}", "ms")
            StatItem("LOSS", "${result?.packetLoss ?: "0.0"}", "%")
        }

        Spacer(modifier = Modifier.weight(1f))

        // العداد الاحترافي مع حل مشكلة الـ Composable context
        Speedometer(
            speed = result?.downloadSpeed ?: 0.0,
            color = primaryColor
        )

        Spacer(modifier = Modifier.weight(1f))

        // إحصائيات التحميل والرفع المفصلة
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            DetailedSpeedView("DOWNLOAD", result?.downloadSpeed ?: 0.0, result?.maxDownloadSpeed ?: 0.0, result?.avgDownloadSpeed ?: 0.0)
            DetailedSpeedView("UPLOAD", result?.uploadSpeed ?: 0.0, result?.maxUploadSpeed ?: 0.0, result?.avgUploadSpeed ?: 0.0)
        }

        // بطاقة معلومات الشبكة
        NetworkInfoCard(result)

        Spacer(modifier = Modifier.height(24.dp))

        // زر التحكم
        Button(
            onClick = { viewModel.startTest() },
            enabled = !isTesting,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isTesting) "جاري الاختبار..." else "ابدأ الفحص", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Speedometer(speed: Double, color: Color) {
    val animatedSpeed by animateFloatAsState(targetValue = speed.toFloat(), label = "speed")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // شريط الخلفية
            drawArc(color.copy(alpha = 0.1f), 150f, 240f, false, style = Stroke(30f, cap = StrokeCap.Round))
            // شريط السرعة الملون (استخدام المتغير المحضر مسبقاً)
            drawArc(
                brush = Brush.sweepGradient(listOf(Color.Cyan, color, Color.Magenta)),
                startAngle = 150f,
                sweepAngle = (animatedSpeed / 150f).coerceIn(0f, 1f) * 240f,
                useCenter = false,
                style = Stroke(30f, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(speed.toInt().toString(), fontSize = 64.sp, fontWeight = FontWeight.Black)
            Text("Mbps", color = Color.Gray)
        }
    }
}

@Composable
fun StatItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text("$value $unit", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailedSpeedView(label: String, current: Double, max: Double, avg: Double) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("الآن: ${current.toInt()}", fontWeight = FontWeight.Bold)
        Text("القمة: ${max.toInt()}", fontSize = 11.sp, color = Color.Gray)
        Text("المتوسط: ${avg.toInt()}", fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun NetworkInfoCard(result: SpeedResult?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("IP: ${result?.ipAddress ?: "---"}", fontSize = 11.sp)
                Text("ISP: ${result?.isp ?: "Detecting..."}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(result?.networkType ?: "UNKNOWN", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}