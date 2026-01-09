package com.mobix.speedtest.ui.screens.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobix.speedtest.domain.models.SpeedResult
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultDetailScreen(
    result: SpeedResult,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val diagnosis = remember(result) { getAiDiagnosisV2(result) }
    val tags = remember(result) { buildSmartTags(result) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF060B12),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "تفاصيل النتيجة",
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { shareAsText(context, result) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF060B12)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            FlowRowChips(tags)

            Spacer(modifier = Modifier.height(14.dp))

            MainResultsCardV2(
                result = result,
                diagnosisLevel = diagnosis.level,
                diagnosisAccent = diagnosis.accentColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            DiagnosisCardV2(
                title = diagnosis.title,
                text = diagnosis.text,
                icon = diagnosis.icon,
                accent = diagnosis.accentColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            ServerDetailsCardV2(
                result = result,
                snackbarHostState = snackbarHostState
            )

            Spacer(modifier = Modifier.weight(1f))

            BottomActionsRow(
                onCopy = {
                    copyToClipboard(context, "SpeedTest", buildShareText(result))
                    scope.launch { snackbarHostState.showSnackbar("تم نسخ النتيجة ✅") }
                },
                onShareText = { shareAsText(context, result) },
                onShareImage = {
                    // TODO: implement share as image (Bitmap)
                }
            )
        }
    }
}

/* ----------------------------- UI Blocks ----------------------------- */

@Composable
private fun FlowRowChips(tags: List<SmartTag>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.chunked(3).forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                line.forEach { tag ->
                    AssistChip(
                        onClick = { },
                        label = { Text(tag.text, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(tag.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = tag.color.copy(alpha = 0.12f),
                            labelColor = Color.White
                        ),
                        border = BorderStroke(1.dp, tag.color.copy(alpha = 0.25f))
                    )
                }
            }
        }
    }
}

@Composable
private fun MainResultsCardV2(
    result: SpeedResult,
    diagnosisLevel: String,
    diagnosisAccent: Color
) {
    val downloadAnim = remember { Animatable(0f) }
    val uploadAnim = remember { Animatable(0f) }
    val pingAnim = remember { Animatable(0f) }

    LaunchedEffect(result) {
        downloadAnim.animateTo(result.downloadSpeed.toFloat(), tween(900, easing = FastOutSlowInEasing))
        uploadAnim.animateTo(result.uploadSpeed.toFloat(), tween(900, easing = FastOutSlowInEasing))
        pingAnim.animateTo(result.ping.toFloat(), tween(700, easing = FastOutSlowInEasing))
    }

    Surface(
        color = Color(0xFF121A26),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, diagnosisAccent.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("النتيجة النهائية", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(
                        diagnosisLevel,
                        color = diagnosisAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(diagnosisAccent.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Mbps",
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("DOWNLOAD", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${downloadAnim.value.roundToInt()}",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00FFC2)
                )
                Spacer(Modifier.width(8.dp))
                Text("Mbps", color = Color(0xFF00FFC2).copy(alpha = 0.65f), fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            MiniBars(
                download = result.downloadSpeed,
                upload = result.uploadSpeed,
                ping = result.ping.toDouble()
            )

            Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.08f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                ResultSmallItemV2("UPLOAD", uploadAnim.value, "Mbps", Icons.Default.CloudUpload)
                ResultSmallItemV2("PING", pingAnim.value, "ms", Icons.Default.Speed)
                ResultSmallItemV2("JITTER", (result.jitter ?: 0).toFloat(), "ms", Icons.Default.Tune)
            }
        }
    }
}

@Composable
private fun MiniBars(download: Double, upload: Double, ping: Double) {
    val dl = (download / 200.0).coerceIn(0.0, 1.0).toFloat()
    val ul = (upload / 100.0).coerceIn(0.0, 1.0).toFloat()
    val pg = (1.0 - (ping / 150.0).coerceIn(0.0, 1.0)).toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricBar("Download strength", dl, Color(0xFF00FFC2))
        MetricBar("Upload strength", ul, Color(0xFF00D1FF))
        MetricBar("Latency quality", pg, Color(0xFFFFD60A))
    }
}

@Composable
private fun MetricBar(label: String, value: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
            Text("${(value * 100).roundToInt()}%", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value)
                    .fillMaxHeight()
                    .background(color.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun ResultSmallItemV2(label: String, value: Float, unit: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color(0xFF00D1FF), modifier = Modifier.size(20.dp))
        Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("${value.roundToInt()} $unit", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DiagnosisCardV2(
    title: String,
    text: String,
    icon: ImageVector,
    accent: Color
) {
    Surface(
        color = accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(text, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun ServerDetailsCardV2(
    result: SpeedResult,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateStr = remember(result.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(result.timestamp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DetailRowV2("المزود (ISP)", result.isp ?: "غير معروف", Icons.Default.Business, context) {
            scope.launch { snackbarHostState.showSnackbar("تم نسخ المزود ✅") }
        }
        DetailRowV2("السيرفر", "${result.serverName} (${result.serverLocation})", Icons.Default.Dns, context) {
            scope.launch { snackbarHostState.showSnackbar("تم نسخ السيرفر ✅") }
        }
        DetailRowV2("التاريخ", dateStr, Icons.Default.Event, context) {
            scope.launch { snackbarHostState.showSnackbar("تم نسخ التاريخ ✅") }
        }
        DetailRowV2("الـ IP", result.ipAddress ?: "---.---.---.---", Icons.Default.Public, context) {
            scope.launch { snackbarHostState.showSnackbar("تم نسخ الـ IP ✅") }
        }
    }
}

@Composable
private fun DetailRowV2(
    label: String,
    value: String,
    icon: ImageVector,
    context: Context,
    onCopied: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = {
                    copyToClipboard(context, label, value)
                    onCopied()
                },
                onLongClick = {
                    copyToClipboard(context, label, value)
                    onCopied()
                }
            )
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BottomActionsRow(
    onCopy: () -> Unit,
    onShareText: () -> Unit,
    onShareImage: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCopy,
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("نسخ", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onShareText,
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1FF))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            Text("مشاركة نص", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Button(
        onClick = onShareImage,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2))
    ) {
        Icon(Icons.Default.Image, contentDescription = null, tint = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "مشاركة النتيجة كصورة",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

/* ----------------------------- Diagnosis ----------------------------- */

private data class DiagnosisV2(
    val title: String,
    val text: String,
    val level: String,
    val accentColor: Color,
    val icon: ImageVector
)

private fun getAiDiagnosisV2(result: SpeedResult): DiagnosisV2 {
    val dl = result.downloadSpeed
    val ping = result.ping
    return when {
        dl >= 150 && ping <= 15 -> DiagnosisV2(
            title = "تشخيص AI: ممتاز جدًا",
            text = "اتصال قوي جدًا للألعاب الاحترافية، بث 4K، واجتماعات فيديو بدون تقطيع.",
            level = "EXCELLENT",
            accentColor = Color(0xFF00FFC2),
            icon = Icons.Default.AutoAwesome
        )
        dl >= 80 && ping <= 30 -> DiagnosisV2(
            title = "تشخيص AI: ممتاز",
            text = "اتصال ممتاز لمعظم الاستخدامات: بث عالي الجودة ومكالمات فيديو مستقرة.",
            level = "GREAT",
            accentColor = Color(0xFF00D1FF),
            icon = Icons.Default.EmojiEvents
        )
        dl >= 30 -> DiagnosisV2(
            title = "تشخيص AI: جيد",
            text = "مناسب للتصفح والعمل اليومي. قد يظهر تأخير بسيط في الألعاب الثقيلة.",
            level = "OK",
            accentColor = Color(0xFFFFD60A),
            icon = Icons.Default.Info
        )
        else -> DiagnosisV2(
            title = "تشخيص AI: ضعيف",
            text = "قد تواجه بطء في التحميل أو تقطيع بالفيديو. جرّب إعادة تشغيل الراوتر أو تغيير مكانه.",
            level = "POOR",
            accentColor = Color(0xFFFF3B30),
            icon = Icons.Default.Warning
        )
    }
}

private data class SmartTag(val text: String, val icon: ImageVector, val color: Color)

private fun buildSmartTags(result: SpeedResult): List<SmartTag> {
    val tags = mutableListOf<SmartTag>()

    if (result.ping <= 25 && result.downloadSpeed >= 50) {
        tags += SmartTag("Gaming Ready", Icons.Default.SportsEsports, Color(0xFF00FFC2))
    } else {
        tags += SmartTag("Gaming متوسط", Icons.Default.SportsEsports, Color(0xFFFFD60A))
    }

    if (result.downloadSpeed >= 60) {
        tags += SmartTag("4K Streaming", Icons.Default.OndemandVideo, Color(0xFF00D1FF))
    } else if (result.downloadSpeed >= 25) {
        tags += SmartTag("HD Streaming", Icons.Default.OndemandVideo, Color(0xFFFFD60A))
    } else {
        tags += SmartTag("Streaming ضعيف", Icons.Default.OndemandVideo, Color(0xFFFF3B30))
    }

    if (result.ping <= 40 && result.downloadSpeed >= 20) {
        tags += SmartTag("Video Calls", Icons.Default.VideoCall, Color(0xFF00D1FF))
    } else {
        tags += SmartTag("Calls قد تتقطع", Icons.Default.VideoCall, Color(0xFFFF3B30))
    }

    return tags
}

/* ----------------------------- Share/Copy ----------------------------- */

private fun buildShareText(result: SpeedResult): String {
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(result.timestamp)
    return buildString {
        append("📶 Speed Test Result\n")
        append("Download: ${result.downloadSpeed.roundToInt()} Mbps\n")
        append("Upload: ${result.uploadSpeed.roundToInt()} Mbps\n")
        append("Ping: ${result.ping} ms\n")
        append("Jitter: ${(result.jitter ?: 0)} ms\n")
        append("Loss: ${(result.packetLoss ?: 0.0).toInt()}%\n")
        append("ISP: ${result.isp ?: "Unknown"}\n")
        append("Server: ${result.serverName} (${result.serverLocation})\n")
        append("IP: ${result.ipAddress ?: "---.---.---.---"}\n")
        append("Date: $dateStr\n")
    }
}

private fun shareAsText(context: Context, result: SpeedResult) {
    val text = buildShareText(result)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share result"))
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}
