package com.mobix.speedtest.ui.screens.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobix.speedtest.domain.models.SpeedResult
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ResultDetailScreen(
    result: SpeedResult,
    onNavigateBack: () -> Unit
) {
    val diagnosis = getAiDiagnosis(result)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B12)) // نفس خلفية الشاشة الرئيسية
            .padding(20.dp)
    ) {
        // --- الشريط العلوي ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "تفاصيل النتيجة",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- بطاقة النتائج الكبرى (التحميل، الرفع، Ping) ---
        MainResultsCard(result)

        Spacer(modifier = Modifier.height(20.dp))

        // --- بطاقة تحليل الذكاء الاصطناعي (AI Diagnosis) ---
        DiagnosisCard(diagnosis)

        Spacer(modifier = Modifier.height(20.dp))

        // --- تفاصيل السيرفر والشبكة ---
        ServerDetailsCard(result)

        Spacer(modifier = Modifier.weight(1f))

        // --- زر المشاركة الاحترافي ---
        Button(
            onClick = { /* تنفيذ منطق توليد الصورة والمشاركة */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1FF))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("مشاركة النتيجة كصورة", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun MainResultsCard(result: SpeedResult) {
    Surface(
        color = Color(0xFF121A26),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("سرعة التحميل النهائية", color = Color.Gray, fontSize = 14.sp)
            Text("${result.downloadSpeed.toInt()}", fontSize = 72.sp, fontWeight = FontWeight.Black, color = Color(0xFF00FFC2))
            Text("Mbps", color = Color(0xFF00FFC2).copy(alpha = 0.6f), fontSize = 20.sp)

            Divider(modifier = Modifier.padding(vertical = 20.dp), color = Color.White.copy(alpha = 0.1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                ResultSmallItem("UPLOAD", "${result.uploadSpeed.toInt()}", "Mbps", Icons.Default.CloudUpload)
                ResultSmallItem("PING", "${result.ping}", "ms", Icons.Default.Speed)
            }
        }
    }
}

@Composable
fun ResultSmallItem(label: String, value: String, unit: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color(0xFF00D1FF), modifier = Modifier.size(20.dp))
        Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("$value $unit", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DiagnosisCard(text: String) {
    Surface(
        color = Color(0xFF00D1FF).copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D1FF).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF00D1FF))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun ServerDetailsCard(result: SpeedResult) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(result.timestamp)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        DetailRow("المزود (ISP)", result.isp ?: "غير معروف", Icons.Default.Business)
        DetailRow("السيرفر", "${result.serverName} (${result.serverLocation})", Icons.Default.Dns)
        DetailRow("التاريخ", dateStr, Icons.Default.Event)
        DetailRow("الـ IP", result.ipAddress ?: "---.---.---.---", Icons.Default.Public)
    }
}

@Composable
fun DetailRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.width(100.dp))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}


fun getAiDiagnosis(result: SpeedResult): String {
    return when {
        result.downloadSpeed >= 100 && result.ping <= 20 ->
            "اتصالك مثالي للألعاب الاحترافية وبث محتوى 4K بدون أي تأخير ملحوظ."

        // حالة الاتصال الجيد جداً
        result.downloadSpeed >= 50 && result.ping <= 40 ->
            "اتصال ممتاز. يمكنك الاستمتاع بمكالمات فيديو عالية الدقة وبث المحتوى بسلاسة."

        // حالة الاتصال المتوسط
        result.downloadSpeed >= 20 ->
            "اتصال جيد للمهام اليومية والتصفح، قد تواجه تأخيراً بسيطاً في الألعاب الثقيلة."

        // حالة الاتصال الضعيف
        else ->
            "اتصالك ضعيف حالياً. قد تواجه بطء في تحميل الصفحات أو تقطيع في بث الفيديو."
    }
}