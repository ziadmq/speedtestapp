package com.mobix.speedtest.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobix.speedtest.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onNextScreen: () -> Unit) {
    val bg = Color(0xFF060B12)
    val cyan = Color(0xFF00D1FF)
    val green = Color(0xFF00FFC2)
    val purple = Color(0xFFBD00FF)

    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val dialProgress = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(600)) }

        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        dialProgress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
        textAlpha.animateTo(1f, tween(600))

        delay(1500)
        exitAlpha.animateTo(0f, tween(400))
        onNextScreen()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .alpha(exitAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        // Glow background
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(glowPulse)
                .background(
                    Brush.radialGradient(
                        listOf(cyan.copy(0.15f), Color.Transparent)
                    )
                )
                .blur(60.dp)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {

                // Arc dial
                Canvas(modifier = Modifier.size(220.dp)) {
                    val strokeWidth = 6.dp.toPx()

                    drawArc(
                        color = Color.White.copy(0.05f),
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        brush = Brush.linearGradient(listOf(cyan, purple)),
                        startAngle = 140f,
                        sweepAngle = dialProgress.value * 260f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // ✅ NEW LOGO IMAGE (replaces LightningMLogo)
                Image(
                    painter = painterResource(id = R.drawable.speedtest_logo), // <-- your new logo
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                        .clip(CircleShape) // optional (remove if your logo already rounded)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = stringResource(R.string.splash_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Text(
                text = stringResource(R.string.splash_subtitle),
                color = cyan.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            LoadingBar(progress = dialProgress.value, color = cyan)
        }
    }
}

@Composable
fun LoadingBar(progress: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(3.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(Brush.linearGradient(listOf(color.copy(0.3f), color)))
        )
    }
}
