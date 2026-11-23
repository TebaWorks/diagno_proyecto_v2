package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.SesionLocal
import com.example.proyectofinal.data.repositorio.mensajeErrorAuth
import kotlinx.coroutines.launch

// Selección de nivel: se muestra UNA sola vez, después del primer ingreso.
// Para cambiar el nivel después, se usa Configuración.
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

        seleccionar(sesion.nivel())

        val btnContinuar = findViewById<TextView>(R.id.btnContinuarNivel)
        btnContinuar.setOnClickListener {
            if (!Conexion.hayInternet(this)) {
                Toast.makeText(this, "Necesitas internet para guardar tu nivel", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            btnContinuar.isEnabled = false
            btnContinuar.alpha = 0.6f
            lifecycleScope.launch {
                try {
                    auth.guardarNivel(nivel)
                    sesion.actualizarNivel(nivel)
                    sesion.confirmarNivel()
                    val intent = Intent(this@NivelActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("NIVEL", "No se pudo guardar el nivel", e)
                    Toast.makeText(this@NivelActivity, mensajeErrorAuth(e), Toast.LENGTH_LONG).show()
                    btnContinuar.isEnabled = true
                    btnContinuar.alpha = 1f
                }
            }
        }

        // Atrás SALE de la app sin cerrar la sesión; al volver, se pide el nivel de nuevo
        findViewById<View>(R.id.btnVolverNivel).visibility = View.GONE
        onBackPressedDispatcher.addCallback(this) { finishAffinity() }
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
}