package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class SinConexionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DESDE_INICIO = "desde_inicio"
    }

    private var desdeInicio = false
    private var callback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_sin_conexion)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        desdeInicio = intent.getBooleanExtra(EXTRA_DESDE_INICIO, false)

        findViewById<android.view.View>(R.id.btnVerContenido).setOnClickListener { irAInicio() }

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.isItemActiveIndicatorEnabled = false
        nav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_inicio -> irAInicio()
                R.id.nav_chat -> Toast.makeText(
                    this,
                    "El chat con IA estará disponible cuando recuperes la conexión",
                    Toast.LENGTH_SHORT
                ).show()
                R.id.nav_vehiculos -> startActivity(Intent(this, VehiculoActivity::class.java))
                R.id.nav_perfil -> startActivity(Intent(this, perfil::class.java))
            }
            false // así "Inicio" sigue marcado
        }
    }

    // Se queda escuchando mientras la pantalla está visible
    override fun onStart() {
        super.onStart()
        if (Conexion.hayInternet(this)) {
            conexionRecuperada()
            return
        }
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    runOnUiThread { conexionRecuperada() }
                }
            }
        }
        callback = cb
        cm.registerDefaultNetworkCallback(cb)
    }

    override fun onStop() {
        super.onStop()
        callback?.let {
            (getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager)
                .unregisterNetworkCallback(it)
        }
        callback = null
    }

    private fun conexionRecuperada() {
        Toast.makeText(this, "Conexión recuperada", Toast.LENGTH_SHORT).show()
        if (desdeInicio) irAInicio() else finish()
    }

    private fun irAInicio() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}