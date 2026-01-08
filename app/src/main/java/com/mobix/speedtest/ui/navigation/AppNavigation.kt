package com.mobix.speedtest.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.mobix.speedtest.ui.screens.home.HomeScreen
import com.mobix.speedtest.ui.screens.history.HistoryScreen // تأكد من الاستيراد

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onNavigateToHistory = { navController.navigate("history") })
        }
        composable("history") {
            HistoryScreen()
        }
    }
}