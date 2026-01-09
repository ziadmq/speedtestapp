package com.mobix.speedtest.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.mobix.speedtest.ui.screens.heatmap.WifiHeatMapScreen
import com.mobix.speedtest.ui.screens.home.HomeScreen
import com.mobix.speedtest.ui.screens.home.HomeViewModel
import com.mobix.speedtest.ui.screens.result.ResultDetailScreen
import com.mobix.speedtest.ui.screens.tools.NetworkToolsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            val viewModel: HomeViewModel = hiltViewModel()
            val isTesting by viewModel.isTesting.collectAsState()
            val result by viewModel.uiState.collectAsState()

            // يمنع تكرار الانتقال للنتيجة
            var didNavigateToResult by rememberSaveable { mutableStateOf(false) }

            // Reset flag لما يبدأ اختبار جديد
            LaunchedEffect(isTesting) {
                if (isTesting) didNavigateToResult = false
            }

            // انتقل للنتيجة عند انتهاء الاختبار ووجود نتيجة منطقية
            LaunchedEffect(isTesting, result) {
                val r = result
                val hasValidResult =
                    r != null && (r.downloadSpeed > 0.0 || r.uploadSpeed > 0.0) && r.ping > 0

                if (!isTesting && hasValidResult && !didNavigateToResult) {
                    didNavigateToResult = true

                    navController.navigate("result_detail") {
                        launchSingleTop = true
                    }
                }
            }

            HomeScreen(
                viewModel = viewModel,
                onNavigateToHistory = {
                    // TODO
                },
                onNavigateToTools = {
                    navController.navigate("network_tools") { launchSingleTop = true }
                },
                onNavigateToHeatMap = {
                    navController.navigate("wifi_heatmap") { launchSingleTop = true }
                }
            )
        }

        composable("result_detail") {
            val homeBackStackEntry = remember(it) { navController.getBackStackEntry("home") }
            val homeViewModel: HomeViewModel = hiltViewModel(homeBackStackEntry)
            val result by homeViewModel.uiState.collectAsState()

            result?.let { speedResult ->
                ResultDetailScreen(
                    result = speedResult,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable("network_tools") {
            NetworkToolsScreen {
                navController.popBackStack()
            }
        }

        composable("wifi_heatmap") {
            WifiHeatMapScreen {
                navController.popBackStack()
            }
        }
    }
}
