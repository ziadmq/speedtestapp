package com.mobix.speedtest.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
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

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit
) {
    val result by viewModel.uiState.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12)) // خلفية داكنة احترافية
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. الجزء العلوي (العنوان والسجل) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "MOBIX SPEED",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            IconButton(
                onClick = onNavigateToHistory,
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.History, contentDescription = "History", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- 2. مؤشرات الجودة (Ping, Jitter) ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            QualityIndicator("PING", "${result?.ping ?: "--"}", "ms")
            QualityIndicator("JITTER", "${result?.jitter ?: "--"}", "ms")
            QualityIndicator("LOSS", "${result?.packetLoss?.toInt() ?: "0"}", "%")
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- 3. العداد التفاعلي (Interactive Speedometer) ---
        InteractiveSpeedometer(
            speed = result?.downloadSpeed ?: 0.0,
            isTesting = isTesting
        )

        Spacer(modifier = Modifier.weight(1f))

        // --- 4. تفاصيل السرعة الحية (Live Stats) ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            SpeedStatBox("DOWNLOAD", result?.downloadSpeed ?: 0.0, result?.maxDownloadSpeed ?: 0.0)
            SpeedStatBox("UPLOAD", result?.uploadSpeed ?: 0.0, result?.maxUploadSpeed ?: 0.0)
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- 5. معلومات الشبكة والـ IP ---
        NetworkCard(ip = result?.ipAddress ?: "---.---.---.---", isp = result?.isp ?: "جاري التعرف...")

        Spacer(modifier = Modifier.height(24.dp))

        // --- 6. زر التحكم الرئيسي ---
        Button(
            onClick = { viewModel.startTest() },
            enabled = !isTesting,
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00D1FF),
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            )
        ) {
            Text(
                if (isTesting) "جاري الفحص الحقيقي..." else "ابدأ الاختبار الآن",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isTesting) Color.White.copy(alpha = 0.5f) else Color.Black
            )
        }
    }
}

@Composable
fun InteractiveSpeedometer(speed: Double, isTesting: Boolean) {
    // حركة العداد السلسة
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = tween(durationMillis = 500)
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // خلفية العداد الرمادية
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                style = Stroke(width = 25.dp.toPx(), cap = StrokeCap.Round)
            )
            // شريط السرعة الملون المتفاعل
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color(0xFF00D1FF),
                    0.5f to Color(0xFF00FFC2),
                    1.0f to Color(0xFF00D1FF)
                ),
                startAngle = 150f,
                sweepAngle = (animatedSpeed / 200f).coerceIn(0f, 1.2f) * 240f,
                useCenter = false,
                style = Stroke(width = 25.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speed.toInt().toString(),
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                "Mbps",
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun QualityIndicator(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("$value $unit", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SpeedStatBox(label: String, current: Double, peak: Double) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, color = Color(0xFF00D1FF), fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text("${current.toInt()} Mbps", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("PEAK: ${peak.toInt()}", color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
fun NetworkCard(ip: String, isp: String) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF00D1FF))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("IP: $ip", color = Color.White, fontSize = 12.sp)
                Text(isp, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}