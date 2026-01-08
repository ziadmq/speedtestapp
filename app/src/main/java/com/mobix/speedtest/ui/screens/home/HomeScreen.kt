package com.mobix.speedtest.ui.screens.home

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val result by viewModel.uiState.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MOBIX SPEED TEST",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.weight(1f))

        // عداد السرعة (بسيط حالياً)
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = (result?.downloadSpeed?.div(100)?.toFloat() ?: 0f),
                modifier = Modifier.size(250.dp),
                strokeWidth = 12.dp,
                color = MaterialTheme.colorScheme.tertiary
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${result?.downloadSpeed?.toInt() ?: 0}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Mbps", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.startTest() },
            enabled = !isTesting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = if (isTesting) "Testing..." else "START TEST")
        }
    }
}