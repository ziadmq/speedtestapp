package com.mobix.speedtest.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import com.mobix.speedtest.ui.screens.home.HomeScreen
import com.mobix.speedtest.ui.screens.home.HomeViewModel
import com.mobix.speedtest.ui.screens.result.ResultDetailScreen
import com.mobix.speedtest.ui.screens.tools.NetworkToolsScreen
import com.mobix.speedtest.ui.screens.splash.SplashScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }

        composable("home") {
            val viewModel: HomeViewModel = hiltViewModel()
            val isTesting by viewModel.isTesting.collectAsState()
            val result by viewModel.uiState.collectAsState()

            LaunchedEffect(isTesting) {
                if (!isTesting && result != null && result?.serverName == "اكتمل الاختبار") {
                    navController.navigate("result_detail")
                }
            }

            HomeScreen(
                viewModel = viewModel,
                onNavigateToTools = { navController.navigate("network_tools") },
            )
        }

        composable("result_detail") {
            val homeBackStackEntry = remember(it) { navController.getBackStackEntry("home") }
            val homeViewModel: HomeViewModel = hiltViewModel(homeBackStackEntry)
            val result by homeViewModel.uiState.collectAsState()
            result?.let { speedResult ->
                ResultDetailScreen(
                    result = speedResult,
                    onNavigateBack = {
                        navController.navigate("home")
                    }
                )
            }
        }

        composable("network_tools") {
            NetworkToolsScreen { navController.popBackStack() }
        }

    }
}