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
                val destino: Class<*> = if (Conexion.hayInternet(this@SplashActivity)) {
                    val haySesion = try {
                        AuthRepositorio().haySesion()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        Log.e("SPLASH", "Error al revisar sesión", e)
                        false
                    }
                    if (haySesion) MainActivity::class.java else BienvenidaActivity::class.java
                } else {
                    SinConexionActivity::class.java
                }

                Toast.makeText(
                    this@SplashActivity, "Voy a: ${destino.simpleName}", Toast.LENGTH_SHORT
                ).show()

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
                    this@SplashActivity, "Error en Splash: $e", Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}