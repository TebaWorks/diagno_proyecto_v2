package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.SesionLocal
import com.example.proyectofinal.data.repositorio.mensajeErrorAuth
import kotlinx.coroutines.launch

// Pantalla "Selección de nivel": va después del login y antes del inicio/perfil
class NivelActivity : ComponentActivity() {

    private val auth = AuthRepositorio()
    private val sesion by lazy { SesionLocal(this) }

    // Valores que se guardan en perfil.nivel_conocimiento
    private val NOVATO = "novato"
    private val CONOCIMIENTO = "con_conocimiento"
    private val AVANZADO = "avanzado"

    private var nivel = NOVATO

    private class Opcion(
        val valor: String,
        val card: View,
        val icono: ImageView,
        val radio: ImageView
    )

    private lateinit var opciones: List<Opcion>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nivel)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft.coerceAtLeast(bars.left), bars.top,
                v.paddingRight.coerceAtLeast(bars.right),
                bars.bottom + 24
            )
            insets
        }

        opciones = listOf(
            Opcion(NOVATO, findViewById(R.id.cardNovato),
                findViewById(R.id.iconoNovato), findViewById(R.id.radioNovato)),
            Opcion(CONOCIMIENTO, findViewById(R.id.cardConocimiento),
                findViewById(R.id.iconoConocimiento), findViewById(R.id.radioConocimiento)),
            Opcion(AVANZADO, findViewById(R.id.cardAvanzado),
                findViewById(R.id.iconoAvanzado), findViewById(R.id.radioAvanzado))
        )
        opciones.forEach { op -> op.card.setOnClickListener { seleccionar(op.valor) } }

        // Preselecciona el nivel guardado en el teléfono y luego el de Supabase
        seleccionar(sesion.nivel())
        lifecycleScope.launch {
            try {
                auth.nivelActual()?.let { seleccionar(it) }
            } catch (e: Exception) {
                Log.e("NIVEL", "No se pudo leer el nivel", e)
            }
        }

        val btnContinuar = findViewById<TextView>(R.id.btnContinuarNivel)
        btnContinuar.setOnClickListener {
            btnContinuar.isEnabled = false
            btnContinuar.alpha = 0.6f
            lifecycleScope.launch {
                try {
                    auth.guardarNivel(nivel)
                    sesion.actualizarNivel(nivel)
                    irAlInicio()
                } catch (e: Exception) {
                    Log.e("NIVEL", "No se pudo guardar el nivel", e)
                    Toast.makeText(this@NivelActivity, mensajeErrorAuth(e), Toast.LENGTH_LONG).show()
                    btnContinuar.isEnabled = true
                    btnContinuar.alpha = 1f
                }
            }
        }

        // La flecha cancela el ingreso: cierra la sesión, borra la llave y vuelve a la bienvenida
        findViewById<View>(R.id.btnVolverNivel).setOnClickListener {
            lifecycleScope.launch {
                try { auth.cerrarSesion() } catch (e: Exception) {
                    Log.e("NIVEL", "Error al cerrar sesión", e)
                }
                sesion.borrar()
                irAlInicio()
            }
        }
    }

    private fun seleccionar(valor: String) {
        if (opciones.none { it.valor == valor }) return
        nivel = valor
        opciones.forEach { op ->
            val activo = op.valor == valor
            op.card.setBackgroundResource(
                if (activo) R.drawable.bg_nivel_seleccionado else R.drawable.bg_nivel_normal
            )
            op.icono.setBackgroundResource(
                if (activo) R.drawable.bg_nivel_icono_activo else R.drawable.bg_nivel_icono_inactivo
            )
            op.radio.setImageResource(if (activo) R.drawable.ic_radio_on else R.drawable.ic_radio_off)
        }
    }

    // MainActivity decide si muestra bienvenida o inicio según la sesión
    private fun irAlInicio() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}