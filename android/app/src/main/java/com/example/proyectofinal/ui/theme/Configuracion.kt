package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.CacheLocal
import com.example.proyectofinal.data.repositorio.NotificacionLocal
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import com.example.proyectofinal.data.repositorio.SesionLocal

// Pantalla "Configuración": se abre desde la fila "Configuración" de Perfil
class ConfiguracionActivity : ComponentActivity() {

    private val auth = AuthRepositorio()

    // Valores guardados en perfil.nivel_conocimiento y el texto que ve el usuario
    private val valores = listOf("novato", "con_conocimiento", "avanzado")
    private val etiquetas = listOf("Novato", "Con conocimiento", "Avanzado")

    private var nivel: String? = null
    private lateinit var tvNivelActual: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_configuracion)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        tvNivelActual = findViewById(R.id.tvNivelActual)

        findViewById<View>(R.id.btnVolverConfig).setOnClickListener { finish() }
        findViewById<View>(R.id.filaNivel).setOnClickListener { elegirNivel() }
        findViewById<View>(R.id.filaConfigNotif).setOnClickListener {
            startActivity(Intent(this, NotificacionesActivity::class.java))
        }
        findViewById<View>(R.id.filaBorrarDatos).setOnClickListener { confirmarBorrado() }

        findViewById<TextView>(R.id.tvVersion).text = versionApp()

        cargarNivel()
    }

    private fun cargarNivel() {
        lifecycleScope.launch {
            nivel = try {
                auth.nivelActual()
            } catch (e: Exception) {
                Log.e("CONFIG", "No se pudo leer el nivel", e)
                null
            }
            mostrarNivel()
        }
    }

    private fun mostrarNivel() {
        val i = valores.indexOf(nivel)
        tvNivelActual.text = if (i >= 0) etiquetas[i] else "Sin definir"
    }

    // Lista con las 3 opciones; al elegir una se guarda en Supabase
    private fun elegirNivel() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Nivel de conocimiento")
            .setSingleChoiceItems(etiquetas.toTypedArray(), valores.indexOf(nivel)) { dialogo, cual ->
                dialogo.dismiss()
                guardarNivel(valores[cual])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarNivel(nuevo: String) {
        lifecycleScope.launch {
            try {
                auth.guardarNivel(nuevo)
                SesionLocal(this@ConfiguracionActivity).actualizarNivel(nuevo)
                nivel = nuevo
                mostrarNivel()
                Toast.makeText(this@ConfiguracionActivity, "Nivel actualizado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("CONFIG", "No se pudo guardar el nivel", e)
                Toast.makeText(
                    this@ConfiguracionActivity,
                    "No se pudo guardar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Borra solo lo guardado en el teléfono; lo que está en Supabase no se toca
    private fun confirmarBorrado() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Borrar datos del dispositivo")
            .setMessage(
                "Se borrarán los vehículos y el nombre guardados en este teléfono y tus " +
                        "notificaciones. Tu cuenta y tus datos en la nube no se eliminan."
            )
            .setPositiveButton("Borrar") { _, _ ->
                CacheLocal(this).borrar()
                NotificacionLocal(this).borrar()
                Toast.makeText(this, "Datos borrados", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun versionApp(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        Log.e("CONFIG", "No se pudo leer la versión", e)
        "1.0"
    }
}