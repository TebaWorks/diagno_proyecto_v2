package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.CacheLocal
import com.example.proyectofinal.data.repositorio.EstadoSesion
import com.example.proyectofinal.data.repositorio.SesionLocal
import com.example.proyectofinal.data.repositorio.SincronizacionOffline

class MainActivity : ComponentActivity() {

    private lateinit var layoutBienvenida: View
    private lateinit var layoutInicio: View
    private lateinit var tvSaludoInicio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_activity)

        layoutBienvenida = findViewById(R.id.layoutBienvenida)
        layoutInicio = findViewById(R.id.layoutInicio)
        tvSaludoInicio = findViewById(R.id.tvSaludoInicio)

        // Margen para barras del sistema, solo en el inicio
        ViewCompat.setOnApplyWindowInsetsListener(layoutInicio) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Bienvenida
        findViewById<Button>(R.id.btnIniciarSesion).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        findViewById<Button>(R.id.btnCrearCuenta).setOnClickListener {
            startActivity(Intent(this, crear_cuenta::class.java))
        }

        // Continuar con Google desde la pantalla de acceso
        // Continuar con Google desde la pantalla de bienvenida
        val btnGoogle = findViewById<Button>(R.id.btnGoogleBienvenida)

        btnGoogle.setOnClickListener {
            if (!Conexion.hayInternet(this)) {
                toast("Necesitas internet para iniciar sesión")
                return@setOnClickListener
            }

            startActivity(
                Intent(this, ConsentimientoActivity::class.java)
                    .putExtra(
                        ConsentimientoActivity.EXTRA_FLUJO_GOOGLE,
                        true
                    )
            )
        }

        // Términos y privacidad
        findViewById<View>(R.id.tvLegalBienvenida).setOnClickListener {
            startActivity(Intent(this, LegalActivity::class.java))
        }

        // Inicio
        findViewById<View>(R.id.cardChat).setOnClickListener {
            startActivity(Intent(this, chat_con_ia::class.java))
        }
        findViewById<View>(R.id.cardChequeo).setOnClickListener {
            startActivity(Intent(this, ChequeoGuiadoActivity::class.java))
        }
        findViewById<View>(R.id.cardVehiculo).setOnClickListener {
            startActivity(Intent(this, VehiculoActivity::class.java))
        }
        findViewById<View>(R.id.cardHistorial).setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_inicio -> true
                R.id.nav_chat -> {
                    startActivity(Intent(this, chat_con_ia::class.java))
                    false
                }
                R.id.nav_vehiculos -> {
                    startActivity(Intent(this, VehiculoActivity::class.java))
                    false // así "Inicio" sigue marcado al volver
                }
                R.id.nav_perfil -> {
                    startActivity(Intent(this, perfil::class.java))
                    false
                }
                else -> false
            }
        }

    }

    // Se ejecuta al abrir y al volver de otras pantallas.
    // Con la llave local, la app funciona aunque no haya internet.
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val auth = AuthRepositorio()
            val sesion = SesionLocal(this@MainActivity)
            val cache = CacheLocal(this@MainActivity)
            val estado = auth.estadoSesion(sesion, Conexion.hayInternet(this@MainActivity))

            when (estado) {
                EstadoSesion.NINGUNA, EstadoSesion.REVOCADA -> {
                    if (estado == EstadoSesion.REVOCADA) {
                        sesion.borrar()
                        cache.borrar()
                        toast("Tu sesión expiró. Vuelve a iniciar sesión.")
                    }
                    layoutBienvenida.visibility = View.VISIBLE
                    layoutInicio.visibility = View.GONE
                }
                else -> {
                    if (estado == EstadoSesion.ACTIVA && !sesion.tieneLlave()) {
                        runCatching { auth.crearLlave(sesion) }
                    }
                    tvSaludoInicio.text = "Hola, ${sesion.nombre()}"
                    layoutBienvenida.visibility = View.GONE
                    layoutInicio.visibility = View.VISIBLE

                    // Con internet: actualiza el nombre y descarga las fichas para el modo sin conexión
                    if (estado == EstadoSesion.ACTIVA) {
                        runCatching { auth.nombreUsuario() }.onSuccess { nombre ->
                            sesion.actualizarNombre(nombre)
                            cache.guardarNombre(nombre)
                            tvSaludoInicio.text = "Hola, $nombre"
                        }
                        runCatching { SincronizacionOffline.sincronizar(this@MainActivity) }
                            .onFailure { Log.e("OFFLINE", "No se pudieron sincronizar las fichas", it) }
                    }
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}