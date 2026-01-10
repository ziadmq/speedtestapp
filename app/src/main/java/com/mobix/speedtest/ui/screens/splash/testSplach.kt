//package com.mobix.speedtest.ui.screens.splash
//
//
//import androidx.compose.animation.core.FastOutSlowInEasing
//import androidx.compose.animation.core.LinearEasing
//import androidx.compose.animation.core.RepeatMode
//import androidx.compose.animation.core.animateFloat
//import androidx.compose.animation.core.infiniteRepeatable
//import androidx.compose.animation.core.keyframes
//import androidx.compose.animation.core.rememberInfiniteTransition
//import androidx.compose.animation.core.spring
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.alpha
//import androidx.compose.ui.draw.blur
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.rotate
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.StrokeCap
//import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.IntOffset
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.mobix.speedtest.R
//import kotlinx.coroutines.delay
//import kotlin.math.PI
//import kotlin.math.cos
//import kotlin.math.roundToInt
//import kotlin.math.sin
//import androidx.compose.foundation.Canvas
//
//@Composable
//fun SplashScreen(onNextScreen: () -> Unit) {
//    // Theme colors (match your app)
//    val bg = Color(0xFF060B12)
//    val card = Color(0xFF121A26)
//    val cyan = Color(0xFF00D1FF)
//    val green = Color(0xFF00FFC2)
//    val purple = Color(0xFFBD00FF)
//
//    // Entry animations (professional)
//    val logoScale = remember { androidx.compose.animation.core.Animatable(0.80f) }
//    val logoAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
//    val textAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
//    val exitAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
//
//    // Infinite animations (speed-test feel)
//    val inf = rememberInfiniteTransition(label = "splashInf")
//    val sweep by inf.animateFloat(
//        initialValue = 0.08f,
//        targetValue = 1f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(1400, easing = FastOutSlowInEasing),
//            repeatMode = RepeatMode.Restart
//        ),
//        label = "sweep"
//    )
//
//    // Needle “testing” motion (like speedometer)
//    val needleAngle by inf.animateFloat(
//        initialValue = -120f,
//        targetValue = 120f,
//        animationSpec = infiniteRepeatable(
//            animation = keyframes {
//                durationMillis = 1300
//                -120f at 0 with FastOutSlowInEasing
//                -40f at 260 with FastOutSlowInEasing
//                75f at 620 with FastOutSlowInEasing
//                20f at 920 with FastOutSlowInEasing
//                120f at 1300 with FastOutSlowInEasing
//            },
//            repeatMode = RepeatMode.Restart
//        ),
//        label = "needle"
//    )
//
//    // Orbit dot for “alive”
//    val orbitDeg by inf.animateFloat(
//        initialValue = 0f,
//        targetValue = 360f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(1200, easing = LinearEasing),
//            repeatMode = RepeatMode.Restart
//        ),
//        label = "orbit"
//    )
//
//    // Pulse glow
//    val pulse by inf.animateFloat(
//        initialValue = 0.85f,
//        targetValue = 1.15f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(850, easing = FastOutSlowInEasing),
//            repeatMode = RepeatMode.Reverse
//        ),
//        label = "pulse"
//    )
//
//    LaunchedEffect(Unit) {
//        // Smooth entrance
//        logoAlpha.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
//        logoScale.animateTo(
//            1f,
//            animationSpec = spring(dampingRatio = 0.55f, stiffness = 240f)
//        )
//        textAlpha.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
//
//        // Hold then exit
//        delay(1500)
//        exitAlpha.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
//        onNextScreen()
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(bg)
//            .alpha(exitAlpha.value),
//        contentAlignment = Alignment.Center
//    ) {
//        BackgroundGlows(
//            cyan = cyan.copy(alpha = 0.18f),
//            green = green.copy(alpha = 0.12f),
//            purple = purple.copy(alpha = 0.14f)
//        )
//
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            modifier = Modifier.padding(horizontal = 22.dp)
//        ) {
//            // SPEED TEST HERO (Gauge + logo)
//            SpeedGaugeHero(
//                logoRes = R.drawable.ic_app,
//                cardColor = card,
//                cyan = cyan,
//                green = green,
//                purple = purple,
//                sweep = sweep,
//                needleAngle = needleAngle,
//                orbitDegrees = orbitDeg,
//                pulse = pulse,
//                logoScale = logoScale.value,
//                logoAlpha = logoAlpha.value
//            )
//
//            Spacer(modifier = Modifier.height(22.dp))
//
//            Text(
//                text = "MOBIX SPEED",
//                style = MaterialTheme.typography.headlineMedium,
//                color = Color.White,
//                fontWeight = FontWeight.Black,
//                letterSpacing = 3.sp,
//                modifier = Modifier.alpha(textAlpha.value)
//            )
//
//            Spacer(modifier = Modifier.height(6.dp))
//
//            Text(
//                text = "INTERNET SPEED TEST",
//                color = Color.White.copy(alpha = 0.55f),
//                fontSize = 12.sp,
//                fontWeight = FontWeight.SemiBold,
//                letterSpacing = 2.sp,
//                modifier = Modifier.alpha(textAlpha.value)
//            )
//
//            Spacer(modifier = Modifier.height(18.dp))
//
//            LoadingLine(progress = sweep, accent = cyan)
//        }
//    }
//}
//
///* -------------------------------- HERO -------------------------------- */
//
//@Composable
//private fun SpeedGaugeHero(
//    logoRes: Int,
//    cardColor: Color,
//    cyan: Color,
//    green: Color,
//    purple: Color,
//    sweep: Float,          // 0..1
//    needleAngle: Float,    // degrees
//    orbitDegrees: Float,   // degrees
//    pulse: Float,
//    logoScale: Float,
//    logoAlpha: Float
//) {
//    val ringSize = 220.dp
//    val logoBox = 182.dp
//
//    Box(
//        modifier = Modifier.size(ringSize),
//        contentAlignment = Alignment.Center
//    ) {
//        // Outer soft ring glow (pulse)
//        Box(
//            modifier = Modifier
//                .size(ringSize)
//                .scale(pulse)
//                .clip(CircleShape)
//                .border(
//                    width = 8.dp,
//                    brush = Brush.sweepGradient(listOf(cyan, green, purple, cyan)),
//                    shape = CircleShape
//                )
//                .alpha(0.12f)
//                .blur(1.5.dp)
//        )
//
//        // Speedometer gauge (Canvas) — makes it obviously speed test
//        SpeedometerGauge(
//            gaugeSize = ringSize,
//            sweep = sweep,
//            cyan = cyan,
//            green = green
//        )
//
//        // Orbit dot
//        OrbitDot(ringSize = ringSize, degrees = orbitDegrees, dotColor = Color.White.copy(alpha = 0.75f))
//
//        // Center card behind logo
//        Box(
//            modifier = Modifier
//                .size(logoBox)
//                .clip(RoundedCornerShape(34.dp))
//                .background(cardColor.copy(alpha = 0.92f))
//                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(34.dp))
//        )
//
//        // Logo
//        Image(
//            painter = painterResource(id = logoRes),
//            contentDescription = "Mobix Logo",
//            modifier = Modifier
//                .size(logoBox)
//                .padding(26.dp)
//                .scale(logoScale)
//                .alpha(logoAlpha)
//        )
//
//        // Needle overlay (above logo)
//        NeedleOverlay(
//            size = ringSize,
//            angleDeg = needleAngle,
//            color = Color.White.copy(alpha = 0.92f)
//        )
//    }
//}
//
//@Composable
//private fun SpeedometerGauge(
//    gaugeSize: androidx.compose.ui.unit.Dp,
//    sweep: Float,
//    cyan: Color,
//    green: Color
//) {
//    val p = sweep.coerceIn(0f, 1f)
//
//    Canvas(modifier = Modifier.size(gaugeSize)) {
//        val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
//
//        // Background track
//        drawArc(
//            color = Color.White.copy(alpha = 0.06f),
//            startAngle = 150f,
//            sweepAngle = 240f,
//            useCenter = false,
//            style = stroke
//        )
//
//        // Foreground progress
//        drawArc(
//            brush = Brush.sweepGradient(listOf(cyan, green, cyan)),
//            startAngle = 150f,
//            sweepAngle = 240f * p,
//            useCenter = false,
//            style = stroke
//        )
//
//        // ---- FIXED PART ----
//        val centerPoint = center
//        val radius =
//            (kotlin.math.min(size.width, size.height) / 2f) - 22.dp.toPx()
//
//        val tickLen = 8.dp.toPx()
//
//        for (i in 0..12) {
//            val angle =
//                Math.toRadians(150.0 + (240.0 / 12.0) * i).toFloat()
//
//            val x1 = centerPoint.x + kotlin.math.cos(angle) * radius
//            val y1 = centerPoint.y + kotlin.math.sin(angle) * radius
//            val x2 = centerPoint.x + kotlin.math.cos(angle) * (radius - tickLen)
//            val y2 = centerPoint.y + kotlin.math.sin(angle) * (radius - tickLen)
//
//            drawLine(
//                color = Color.White.copy(alpha = if (i % 3 == 0) 0.22f else 0.12f),
//                start = Offset(x1, y1),
//                end = Offset(x2, y2),
//                strokeWidth = if (i % 3 == 0) 3.dp.toPx() else 2.dp.toPx(),
//                cap = StrokeCap.Round
//            )
//        }
//    }
//}
//
//@Composable
//private fun NeedleOverlay(
//    size: androidx.compose.ui.unit.Dp,
//    angleDeg: Float,
//    color: Color
//) {
//    // Needle is a thin rounded rectangle rotated around center
//    Box(
//        modifier = Modifier
//            .size(size)
//            .rotate(angleDeg)
//    ) {
//        Box(
//            modifier = Modifier
//                .align(Alignment.Center)
//                .offset(y = (-34).dp) // move needle tip upwards
//                .width(6.dp)
//                .height(64.dp)
//                .clip(RoundedCornerShape(99.dp))
//                .background(color)
//        )
//
//        // Center hub
//        Box(
//            modifier = Modifier
//                .align(Alignment.Center)
//                .size(14.dp)
//                .clip(CircleShape)
//                .background(Color(0xFF00D1FF).copy(alpha = 0.95f))
//                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
//        )
//    }
//}
//
///* -------------------------------- BACKGROUND -------------------------------- */
//
//@Composable
//private fun BackgroundGlows(
//    cyan: Color,
//    green: Color,
//    purple: Color
//) {
//    Box(modifier = Modifier.fillMaxSize()) {
//        Box(
//            modifier = Modifier
//                .size(260.dp)
//                .offset(x = (-120).dp, y = (-120).dp)
//                .clip(CircleShape)
//                .background(cyan)
//                .blur(70.dp)
//        )
//        Box(
//            modifier = Modifier
//                .size(260.dp)
//                .offset(x = 140.dp, y = (-40).dp)
//                .clip(CircleShape)
//                .background(purple)
//                .blur(80.dp)
//        )
//        Box(
//            modifier = Modifier
//                .size(280.dp)
//                .offset(x = 40.dp, y = 240.dp)
//                .clip(CircleShape)
//                .background(green)
//                .blur(85.dp)
//        )
//    }
//}
//
///* -------------------------------- SMALL PIECES -------------------------------- */
//
//@Composable
//private fun OrbitDot(
//    ringSize: androidx.compose.ui.unit.Dp,
//    degrees: Float,
//    dotColor: Color
//) {
//    val density = LocalDensity.current
//    val radiusPx = with(density) { (ringSize / 2f - 10.dp).toPx() }
//    val angleRad = (degrees * PI / 180.0).toFloat()
//
//    val x = (cos(angleRad) * radiusPx).toFloat()
//    val y = (sin(angleRad) * radiusPx).toFloat()
//
//    Box(
//        modifier = Modifier
//            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
//            .size(10.dp)
//            .clip(CircleShape)
//            .background(dotColor)
//            .blur(0.3.dp)
//    )
//}
//
//@Composable
//private fun LoadingLine(
//    progress: Float,
//    accent: Color
//) {
//    val p = progress.coerceIn(0f, 1f)
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(6.dp)
//            .clip(CircleShape)
//            .background(Color.White.copy(alpha = 0.07f))
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxWidth(p)
//                .fillMaxHeight()
//                .background(accent.copy(alpha = 0.85f))
//        )
//    }
//}
