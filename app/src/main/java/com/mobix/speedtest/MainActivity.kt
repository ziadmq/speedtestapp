package com.mobix.speedtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mobix.speedtest.ui.navigation.AppNavigation
import com.mobix.speedtest.ui.theme.MobixSpeedTestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // طلب صلاحيات الموقع والواي فاي عند بدء التطبيق
        val permissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        androidx.core.app.ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)

        enableEdgeToEdge()
        setContent {
            MobixSpeedTestTheme {
                AppNavigation()
            }
        }
    }
}