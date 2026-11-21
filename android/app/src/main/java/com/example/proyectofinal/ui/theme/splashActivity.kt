package com.example.proyectofinal.ui.theme

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.CacheLocal
import com.example.proyectofinal.data.repositorio.EstadoSesion
import com.example.proyectofinal.data.repositorio.SesionLocal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {

    private val duracionMs = 2500L   // cuánto dura la portada

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // Barra de carga que se llena sola
        val barra = findViewById<ProgressBar>(R.id.progresoSplash)
        ObjectAnimator.ofInt(barra, "progress", 0, 100).apply {
            duration = duracionMs
            interpolator = LinearInterpolator()
            start()
        }

        lifecycleScope.launch {
            delay(duracionMs)
            try {
                val hayInternet = Conexion.hayInternet(this@SplashActivity)
                val sesion = SesionLocal(this@SplashActivity)
                val auth = AuthRepositorio()

                val destino: Class<*> = when (auth.estadoSesion(sesion, hayInternet)) {
                    EstadoSesion.ACTIVA -> {
                        // Usuarios que iniciaron sesión antes de existir la llave
                        if (!sesion.tieneLlave()) runCatching { auth.crearLlave(sesion) }
                        MainActivity::class.java
                    }
                    EstadoSesion.SIN_CONEXION -> MainActivity::class.java
                    EstadoSesion.REVOCADA -> {
                        sesion.borrar()
                        CacheLocal(this@SplashActivity).borrar()
                        Toast.makeText(
                            this@SplashActivity,
                            "Tu sesión expiró. Vuelve a iniciar sesión.",
                            Toast.LENGTH_LONG
                        ).show()
                        BienvenidaActivity::class.java
                    }
                    EstadoSesion.NINGUNA ->
                        if (hayInternet) BienvenidaActivity::class.java
                        else SinConexionActivity::class.java
                }

                val intent = Intent(this@SplashActivity, destino)
                if (destino == SinConexionActivity::class.java) {
                    intent.putExtra(SinConexionActivity.EXTRA_DESDE_INICIO, true)
                }
                startActivity(intent)
                finish()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e("SPLASH", "Error al abrir la siguiente pantalla", e)
                Toast.makeText(
                    this@SplashActivity, "No se pudo iniciar la app. Intenta de nuevo.", Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}