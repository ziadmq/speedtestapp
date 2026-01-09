package com.mobix.speedtest.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import com.mobix.speedtest.ui.screens.home.HomeScreen
import com.mobix.speedtest.ui.screens.home.HomeViewModel
import com.mobix.speedtest.ui.screens.result.ResultDetailScreen
import com.mobix.speedtest.ui.screens.tools.NetworkToolsScreen
import com.mobix.speedtest.ui.screens.heatmap.WifiHeatMapScreen // استيراد شاشة خريطة الحرارة

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        // 1. الشاشة الرئيسية (Home)
        composable("home") {
            val viewModel: HomeViewModel = hiltViewModel()
            val isTesting by viewModel.isTesting.collectAsState()
            val result by viewModel.uiState.collectAsState()

            // منطق الانتقال التلقائي لشاشة النتائج فور اكتمال الفحص
            LaunchedEffect(isTesting) {
                if (!isTesting && result != null && result?.serverName == "اكتمل الاختبار") {
                    navController.navigate("result_detail")
                }
            }

            HomeScreen(
                viewModel = viewModel,
                onNavigateToHistory = {
                    // مساحة محجوزة للسجل في حال أردت تفعيله لاحقاً
                },
                onNavigateToTools = {
                    navController.navigate("network_tools")
                },
                onNavigateToHeatMap = {
                    navController.navigate("wifi_heatmap") // الانتقال لخريطة الحرارة
                }
            )
        }

        // 2. شاشة تفاصيل النتيجة (Shared ViewModel)
        composable("result_detail") {
            val homeBackStackEntry = remember(it) { navController.getBackStackEntry("home") }
            val homeViewModel: HomeViewModel = hiltViewModel(homeBackStackEntry)
            val result by homeViewModel.uiState.collectAsState()

            result?.let { speedResult ->
                ResultDetailScreen(result = speedResult) {
                    navController.popBackStack()
                }
            }
        }

        // 3. شاشة أدوات الشبكة
        composable("network_tools") {
            NetworkToolsScreen {
                navController.popBackStack()
            }
        }

        // 4. شاشة خريطة الحرارة (Wi-Fi Heatmap AR)
        composable("wifi_heatmap") {
            WifiHeatMapScreen {
                navController.popBackStack()
            }
        }
    }
}