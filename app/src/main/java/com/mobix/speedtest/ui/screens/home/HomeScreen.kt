package com.mobix.speedtest.ui.screens.home

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobix.speedtest.domain.models.SpeedResult
import com.mobix.speedtest.ui.theme.PrimaryBlue
import com.mobix.speedtest.ui.theme.SecondaryCyan
import kotlin.math.roundToInt
import com.mobix.speedtest.R

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToTools: () -> Unit,
) {
    val result by viewModel.uiState.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Unit toggle: Mbps <-> MB/s
    var showMbps by rememberSaveable { mutableStateOf(true) }

    // Quality info dialog
    var infoDialog by rememberSaveable { mutableStateOf<QualityInfo?>(null) }

    // Capture strings for use in logic/LaunchedEffect
    val uploadLabel = stringResource(R.string.upload)
    val downloadLabel = stringResource(R.string.download)
    val pingFinishedMsg = stringResource(R.string.ping_finished)
    val downloadFinishedMsg = stringResource(R.string.download_finished)
    val uploadStartedMsg = stringResource(R.string.upload_started)
    val testCompletedMsg = stringResource(R.string.test_completed)

    // Stages logic
    val isPingFinished = (result?.ping ?: 0) > 0
    val isDownloadFinished =
        (result?.downloadSpeed ?: 0.0) >= (result?.maxDownloadSpeed ?: 1.0) && isPingFinished
    val isUploading = (result?.uploadSpeed ?: 0.0) > 0.0 && isDownloadFinished

    val currentSpeed = if (isUploading) result?.uploadSpeed ?: 0.0 else result?.downloadSpeed ?: 0.0
    val speedLabel = if (isUploading) uploadLabel else downloadLabel
    val themeColor = if (isUploading) Color(0xFFBD00FF) else PrimaryBlue

    // Snackbar & haptics on stage changes
    var lastStage by rememberSaveable { mutableStateOf(TestStage.IDLE) }
    val stage = when {
        !isTesting -> TestStage.IDLE
        isUploading -> TestStage.UPLOAD
        isDownloadFinished -> TestStage.DOWNLOAD_DONE
        isPingFinished -> TestStage.PING_DONE
        else -> TestStage.RUNNING
    }

    LaunchedEffect(stage) {
        if (stage != lastStage) {
            lastStage = stage

            when (stage) {
                TestStage.PING_DONE -> {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    snackbarHostState.showSnackbar(pingFinishedMsg)
                }
                TestStage.DOWNLOAD_DONE -> {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    snackbarHostState.showSnackbar(downloadFinishedMsg)
                }
                TestStage.UPLOAD -> {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    snackbarHostState.showSnackbar(uploadStartedMsg)
                }
                TestStage.IDLE -> {
                    if (result != null && (result?.downloadSpeed ?: 0.0) > 0.0) {
                        snackbarHostState.showSnackbar(testCompletedMsg)
                    }
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFF060B12)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF060B12))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeaderSection(
                    onNavigateToTools = onNavigateToTools,
                    isTesting = isTesting
                )

                Spacer(modifier = Modifier.height(18.dp))

                QualityCardsRow(
                    result = result,
                    isPingFinished = isPingFinished,
                    onLongPressItem = { infoDialog = it }
                )

                Spacer(modifier = Modifier.weight(1f))

                MainSpeedometerInteractive(
                    speed = currentSpeed,
                    label = speedLabel,
                    isTesting = isTesting,
                    maxSpeed = if (isUploading) 100.0 else 200.0,
                    themeColor = themeColor,
                    showMbps = showMbps,
                    onToggleUnit = {
                        showMbps = !showMbps
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                if (isTesting) {
                    TestProgressLine(isPingFinished, isDownloadFinished, isUploading)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpeedDetailBox(
                        modifier = Modifier.weight(1f),
                        label = downloadLabel,
                        speedMbps = result?.downloadSpeed ?: 0.0,
                        color = PrimaryBlue,
                        showMbps = showMbps
                    )
                    SpeedDetailBox(
                        modifier = Modifier.weight(1f),
                        label = uploadLabel,
                        speedMbps = result?.uploadSpeed ?: 0.0,
                        color = SecondaryCyan,
                        showMbps = showMbps
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isTesting && result != null && (result?.downloadSpeed ?: 0.0) > 0.0) {
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            shareResult(context, result!!, showMbps)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.share_result), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                StartOrCancelButton(
                    isTesting = isTesting,
                    onStart = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.startTest()
                    },
                    onCancel = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        // viewModel.cancelTest()
                    }
                )
            }

            if (infoDialog != null) {
                QualityInfoDialog(
                    info = infoDialog!!,
                    onDismiss = { infoDialog = null }
                )
            }
        }
    }
}

/* ----------------------------- Header ----------------------------- */

@Composable
private fun HeaderSection(
    onNavigateToTools: () -> Unit,
    isTesting: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                // Using app_name from strings.xml
                text = stringResource(R.string.app_name).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            Text(
                // Dynamic text based on test state
                text = if (isTesting) {
                    stringResource(R.string.test_in_progress)
                } else {
                    stringResource(R.string.premium_access)
                },
                fontSize = 10.sp,
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(
                onClick = onNavigateToTools,
                modifier = Modifier.background(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

private enum class QualityInfo { PING, JITTER, LOSS }

@Composable
private fun QualityCardsRow(
    result: SpeedResult?,
    isPingFinished: Boolean,
    onLongPressItem: (QualityInfo) -> Unit
) {
    val activeColor by animateColorAsState(
        targetValue = if (isPingFinished) PrimaryBlue else Color.Gray,
        animationSpec = tween(500),
        label = "qualityColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QualityItem(
            label = stringResource(R.string.ping),
            value = if ((result?.ping ?: 0) > 0) "${result?.ping}" else "--",
            unit = stringResource(R.string.unit_ms),
            valueColor = activeColor,
            onLongPress = { onLongPressItem(QualityInfo.PING) }
        )
        QualityItem(
            label = stringResource(R.string.jitter),
            value = if ((result?.jitter ?: 0) > 0) "${result?.jitter}" else "--",
            unit = stringResource(R.string.unit_ms),
            valueColor = activeColor,
            onLongPress = { onLongPressItem(QualityInfo.JITTER) }
        )
        QualityItem(
            label = stringResource(R.string.loss),
            value = "${result?.packetLoss?.toInt() ?: 0}",
            unit = stringResource(R.string.unit_percent),
            valueColor = activeColor,
            onLongPress = { onLongPressItem(QualityInfo.LOSS) }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class) // Needed for combinedClickable
@Composable
private fun QualityItem(
    label: String,
    value: String,
    unit: String,
    valueColor: Color,
    onLongPress: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = { /* Optional click action */ },
                onLongClick = onLongPress
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // The label is already translated in the parent (QualityCardsRow)
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        // Using stringResource for formatting ensures the unit is placed correctly
        // based on the phone's language (RTL vs LTR)
        Text(
            text = stringResource(R.string.quality_value_unit, value, unit),
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QualityInfoDialog(
    info: QualityInfo,
    onDismiss: () -> Unit
) {
    // Get the correct resource IDs
    val (titleRes, bodyRes) = when (info) {
        QualityInfo.PING -> R.string.ping to R.string.ping_desc
        QualityInfo.JITTER -> R.string.jitter to R.string.jitter_desc
        QualityInfo.LOSS -> R.string.loss to R.string.loss_desc
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes), fontWeight = FontWeight.Black) },
        text = { Text(stringResource(bodyRes), color = Color(0xFFB8C2D1)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF0B1220)
    )
}

@Composable
private fun TestProgressLine(pingDone: Boolean, dlDone: Boolean, ulActive: Boolean) {
    val progress by animateFloatAsState(
        targetValue = when {
            ulActive -> 1f
            dlDone -> 0.66f
            pingDone -> 0.33f
            else -> 0.1f
        },
        animationSpec = tween(500),
        label = "progressLine"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.ping), color = if (pingDone) PrimaryBlue else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.download), color = if (dlDone) PrimaryBlue else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.upload), color = if (ulActive) Color(0xFFBD00FF) else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        // ... rest of the Box UI remains the same ...
    }
}

@Composable
private fun MainSpeedometerInteractive(
    speed: Double,
    label: String,
    isTesting: Boolean,
    maxSpeed: Double,
    themeColor: Color,
    showMbps: Boolean,
    onToggleUnit: () -> Unit
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "speedAnim"
    )

    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val ringAlpha by animateFloatAsState(
        targetValue = if (isTesting) 0.35f else 0.0f,
        animationSpec = tween(400),
        label = "ringAlpha"
    )

    // Using stringResource for unit labels
    val speedToShow = if (showMbps) speed else (speed / 8.0)
    val unitLabel = if (showMbps) stringResource(R.string.mbps) else stringResource(R.string.mbs)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(300.dp)
            .scale(if (isTesting) pulse else 1f)
            .clickable { onToggleUnit() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()

            // Background arc
            drawArc(
                color = Color.White.copy(alpha = 0.03f),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(themeColor.copy(0.45f), themeColor, themeColor.copy(0.45f))
                ),
                startAngle = 140f,
                sweepAngle = (animatedSpeed / maxSpeed.toFloat()).coerceIn(0f, 1f) * 260f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Pulse ring (while testing)
            if (ringAlpha > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(themeColor.copy(ringAlpha), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 0.55f
                    ),
                    radius = size.minDimension * 0.48f,
                    center = center
                )
            }

            // Small marker dot
            drawCircle(
                color = themeColor.copy(alpha = 0.7f),
                radius = 4.dp.toPx(),
                center = Offset(center.x, center.y + size.minDimension * 0.33f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = themeColor, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(
                text = "${speedToShow.roundToInt()}",
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(unitLabel, fontSize = 16.sp, color = Color.Gray)
            Spacer(Modifier.height(6.dp))

            // Updated with stringResource
            Text(
                text = stringResource(R.string.tap_to_change_unit),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.35f)
            )
        }
    }
}


@Composable
private fun SpeedDetailBox(
    modifier: Modifier,
    label: String,
    speedMbps: Double,
    color: Color,
    showMbps: Boolean
) {
    val value = if (showMbps) speedMbps else (speedMbps / 8.0)

    val unit = if (showMbps) stringResource(R.string.mbps) else stringResource(R.string.mbs)

    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = stringResource(R.string.speed_value_unit, value.roundToInt(), unit),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
private fun StartOrCancelButton(
    isTesting: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isTesting) 0.98f else 1f,
        animationSpec = tween(250),
        label = "btnScale"
    )

    Button(
        onClick = { if (isTesting) onCancel() else onStart() },
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isTesting) Color(0xFFFF3B30) else PrimaryBlue
        )
    ) {
        Icon(
            imageVector = if (isTesting) Icons.Default.Close else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.Black
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (isTesting) stringResource(R.string.cancel_test) else stringResource(R.string.start_test),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.Black
        )
    }
}


private fun shareResult(context: Context, result: SpeedResult, showMbps: Boolean) {
    val dl = if (showMbps) result.downloadSpeed else (result.downloadSpeed / 8.0)
    val ul = if (showMbps) result.uploadSpeed else (result.uploadSpeed / 8.0)
    val unit = if (showMbps) "Mbps" else "MB/s"

    val text = buildString {
        append("📶 Speed Test Result\n")
        append("Ping: ${result.ping} ms\n")
        append("Download: ${dl.roundToInt()} $unit\n")
        append("Upload: ${ul.roundToInt()} $unit\n")
        append("Jitter: ${result.jitter} ms\n")
        append("Loss: ${(result.packetLoss ?: 0.0).toInt()}%\n")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share result"))
}


private enum class TestStage { IDLE, RUNNING, PING_DONE, DOWNLOAD_DONE, UPLOAD }

private fun Double.roundToInt(): Int = this.roundToInt()
