package com.mobix.speedtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mobix.speedtest.ui.navigation.AppNavigation
import com.mobix.speedtest.ui.theme.MobixSpeedTestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // ضروري لعمل Hilt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobixSpeedTestTheme { // تأكد من استخدام الاسم الصحيح للثيم
                AppNavigation()
            }
        }
    }
}