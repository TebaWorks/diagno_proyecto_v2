package com.example.proyectofinal.ui.theme

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object Conexion {
    // true si el teléfono tiene internet de verdad (no solo WiFi sin salida)
    fun hayInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}