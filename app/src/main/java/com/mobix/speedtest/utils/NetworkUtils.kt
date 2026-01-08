package com.mobix.speedtest.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {
    fun getNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return "Unknown"
        val actNw = cm.getNetworkCapabilities(nw) ?: return "Unknown"
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            else -> "Other"
        }
    }
}