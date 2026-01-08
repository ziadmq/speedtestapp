package com.mobix.speedtest.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit // إضافة هذا البارامتر للتنقل
) {
    val result by viewModel.uiState.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("MOBIX SPEED TEST", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

        // زر السجل في الأعلى
        IconButton(onClick = onNavigateToHistory, modifier = Modifier.align(Alignment.End)) {
            Text("History", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { (result?.downloadSpeed?.div(100)?.toFloat() ?: 0f) },
                modifier = Modifier.size(250.dp),
                strokeWidth = 12.dp,
                color = MaterialTheme.colorScheme.tertiary
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${result?.downloadSpeed?.toInt() ?: 0}", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                Text("Mbps", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.startTest() },
            enabled = !isTesting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isTesting) "Testing..." else "START TEST")
        }
    }
}