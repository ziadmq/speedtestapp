package com.mobix.speedtest.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
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
import com.mobix.speedtest.ui.screens.home.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToHeatMap: () -> Unit // البارامتر الجديد للوصول للخريطة
) {
    val result by viewModel.uiState.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. الشريط العلوي (العنوان وأدوات الشبكة) ---
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
                onClick = onNavigateToTools,
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Construction,
                    contentDescription = "Network Tools",
                    tint = Color(0xFF00D1FF)
                )
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        // --- 2. مؤشرات الجودة ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            QualityIndicator("PING", "${result?.ping ?: "--"}", "ms")
            QualityIndicator("JITTER", "${result?.jitter ?: "--"}", "ms")
            QualityIndicator("LOSS", "${result?.packetLoss?.toInt() ?: "0"}", "%")
        }

        Spacer(modifier = Modifier.weight(0.8f))

        // --- 3. العداد التفاعلي ---
        InteractiveSpeedometer(
            speed = result?.downloadSpeed ?: 0.0,
            isTesting = isTesting
        )

        Spacer(modifier = Modifier.weight(0.8f))

        // --- 4. إحصائيات السرعة الحية ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            SpeedStatBox("DOWNLOAD", result?.downloadSpeed ?: 0.0, result?.maxDownloadSpeed ?: 0.0)
            SpeedStatBox("UPLOAD", result?.uploadSpeed ?: 0.0, result?.maxUploadSpeed ?: 0.0)
        }

        Spacer(modifier = Modifier.height(25.dp))

        // --- 5. ميزة خريطة الحرارة (Wi-Fi Heatmap AR) ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToHeatMap() },
            color = Color(0xFF00D1FF).copy(alpha = 0.08f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF00D1FF).copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF00D1FF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = Color.Black)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text("خريطة الحرارة (AR)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("تتبع قوة الإشارة في غرف المنزل", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF00D1FF))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 6. بطاقة الشبكة والـ IP ---
        NetworkCard(
            ip = result?.ipAddress ?: "---.---.---.---",
            isp = result?.isp ?: "جاري التعرف...",
            onClick = onNavigateToTools
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- 7. زر بدء الاختبار ---
        Button(
            onClick = { viewModel.startTest() },
            enabled = !isTesting,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00D1FF),
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            )
        ) {
            Text(
                if (isTesting) "جاري الفحص..." else "ابدأ الاختبار",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isTesting) Color.White.copy(alpha = 0.5f) else Color.Black
            )
        }
    }
}

@Composable
fun InteractiveSpeedometer(speed: Double, isTesting: Boolean) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = tween(durationMillis = 600)
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color(0xFF00D1FF),
                    0.5f to Color(0xFF00FFC2),
                    1.0f to Color(0xFF00D1FF)
                ),
                startAngle = 150f,
                sweepAngle = (animatedSpeed / 200f).coerceIn(0f, 1f) * 240f,
                useCenter = false,
                style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speed.toInt().toString(),
                fontSize = 68.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                "Mbps",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun QualityIndicator(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("$value $unit", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SpeedStatBox(label: String, current: Double, peak: Double) {
    Column {
        Text(label, color = Color(0xFF00D1FF), fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text("${current.toInt()} Mbps", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("PEAK: ${peak.toInt()}", color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun NetworkCard(ip: String, isp: String, onClick: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF00D1FF))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("IP: $ip", color = Color.White, fontSize = 11.sp)
                Text(isp, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}