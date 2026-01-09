package com.mobix.speedtest.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobix.speedtest.domain.models.SpeedResult
import com.mobix.speedtest.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToHeatMap: () -> Unit
) {
    val result by viewModel.uiState.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    // تحديد الحالة الحالية للفحص (تحميل أم رفع) لتبديل ألوان العداد
    val isUploading = result?.uploadSpeed ?: 0.0 > 0.0 && result?.downloadSpeed ?: 0.0 >= (result?.maxDownloadSpeed ?: 1.0)
    val currentSpeed = if (isUploading) result?.uploadSpeed ?: 0.0 else result?.downloadSpeed ?: 0.0
    val speedLabel = if (isUploading) "UPLOAD" else "DOWNLOAD"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12))
    ) {
        // تأثير الإضاءة الخلفية المتحركة
        val infiniteTransition = rememberInfiniteTransition(label = "")
        val animOffset by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 100f,
            animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse), label = ""
        )

        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(y = animOffset.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PrimaryBlue.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
                .blur(100.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- الشريط العلوي ---
            HeaderSection(onNavigateToTools)

            Spacer(modifier = Modifier.height(24.dp))

            // --- مؤشرات الجودة ---
            QualityCardsRow(result)

            Spacer(modifier = Modifier.weight(1f))

            // --- العداد الاحترافي المطور ---
            MainSpeedometer(
                speed = currentSpeed,
                label = speedLabel,
                isTesting = isTesting,
                maxSpeed = if (isUploading) 100.0 else 200.0 // يمكن تعديله بناء على الباقة
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- تفاصيل التحميل والرفع ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpeedDetailBox(Modifier.weight(1f), "DOWNLOAD", result?.downloadSpeed ?: 0.0, PrimaryBlue)
                SpeedDetailBox(Modifier.weight(1f), "UPLOAD", result?.uploadSpeed ?: 0.0, SecondaryCyan)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- الروابط السريعة (Heatmap & Tools) ---
            ActionCard(
                title = "خريطة الحرارة (WiFi AR)",
                subtitle = "رسم خريطة الإشارة في منزلك",
                icon = Icons.Default.Layers,
                onClick = onNavigateToHeatMap
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- زر بدء الفحص الاحترافي ---
            StartButton(isTesting) { viewModel.startTest() }
        }
    }
}

@Composable
fun MainSpeedometer(speed: Double, label: String, isTesting: Boolean, maxSpeed: Double) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = tween(800, easing = FastOutSlowInEasing), label = ""
    )

    // تغيير اللون بناءً على السرعة
    val dynamicColor by animateColorAsState(
        targetValue = when {
            speed < 20 -> PrimaryBlue
            speed < 70 -> Color(0xFF00FFC2) // أخضر فوسفوري
            else -> Color(0xFFBD00FF) // أرجواني للسرعات العالية
        },
        animationSpec = tween(1000), label = ""
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()

            // الظل الخلفي (القوس الفارغ)
            drawArc(
                color = Color.White.copy(alpha = 0.03f),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // القوس الملون الديناميكي
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to dynamicColor.copy(alpha = 0.5f),
                    0.5f to dynamicColor,
                    1.0f to dynamicColor.copy(alpha = 0.5f)
                ),
                startAngle = 140f,
                sweepAngle = (animatedSpeed / maxSpeed.toFloat()).coerceIn(0f, 1f) * 260f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // الأرقام والبيانات داخل الدائرة
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = dynamicColor.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "${speed.roundToInt()}",
                fontSize = 85.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.drawWithContent {
                    // إضافة وهج بسيط للنص
                    drawContent()
                }
            )
            Text(
                "Mbps",
                fontSize = 18.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HeaderSection(onNavigateToTools: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("MOBIX SPEED", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Black)
            Text("PREMIUM NETWORK ACCESS", fontSize = 10.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
        }
        IconButton(
            onClick = onNavigateToTools,
            modifier = Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun QualityCardsRow(result: SpeedResult?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QualityItem("PING", "${result?.ping ?: "--"}", "ms")
        QualityItem("JITTER", "${result?.jitter ?: "--"}", "ms")
        QualityItem("LOSS", "${result?.packetLoss?.toInt() ?: "0"}", "%")
    }
}

@Composable
fun QualityItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("$value $unit", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SpeedDetailBox(modifier: Modifier, label: String, speed: Double, color: Color) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text("${speed.roundToInt()} Mbps", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = PrimaryBlue.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun StartButton(isTesting: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isTesting) 0.95f else 1f, label = "")

    Button(
        onClick = onClick,
        enabled = !isTesting,
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryBlue,
            disabledContainerColor = Color.Gray.copy(alpha = 0.2f)
        )
    ) {
        Text(
            if (isTesting) "TESTING..." else "START TEST",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = if (isTesting) Color.White.copy(alpha = 0.5f) else Color.Black
        )
    }
}

private fun Double.roundToInt() = this.toInt()