package com.mobix.speedtest.ui.screens.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ar.core.Frame
import io.github.sceneview.ar.ARScene

@Composable
fun WifiHeatMapScreen(
    viewModel: HeatMapViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    // حفظ آخر Frame للحصول على Pose الكاميرا
    var currentFrame by remember { mutableStateOf<Frame?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        // واجهة الواقع المعزز (Compose)
        ARScene(
            modifier = Modifier.fillMaxSize(),
            onSessionUpdated = { _, frame ->
                currentFrame = frame
            }
        )

        // زر تثبيت نقطة
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp, start = 20.dp, end = 20.dp)
        ) {
            Button(
                onClick = {
                    val pose = currentFrame?.camera?.pose
                    if (pose != null) {
                        viewModel.addPoint(pose.tx(), pose.ty(), pose.tz())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1FF))
            ) {
                Text(
                    text = "تثبيت نقطة إشارة هنا",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // زر الرجوع
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(20.dp)
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}
