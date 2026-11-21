package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.R
import com.example.proyectofinal.data.modelo.Diagnostico
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.serialization.json.Json

// Pantalla "Resultado del análisis": diagnóstico preliminar + recomendaciones.
// La abre el chat con IA y recibe el diagnóstico (en JSON) en el extra "diagnostico".
class ResultadoAnalisisActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DIAGNOSTICO = "diagnostico"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_resultado_analisis)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val texto = intent.getStringExtra(EXTRA_DIAGNOSTICO)
        val diagnostico = try {
            Json { ignoreUnknownKeys = true }.decodeFromString(Diagnostico.serializer(), texto ?: "")
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo mostrar el análisis", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<View>(R.id.btnVolverResultado).setOnClickListener { finish() }
        findViewById<View>(R.id.btnInfoResultado).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Diagnóstico preliminar")
                .setMessage(
                    "Es una estimación hecha por la IA a partir de lo que describiste y de la " +
                            "foto. La confianza indica qué tan probable es esta causa, pero no " +
                            "reemplaza la revisión de un mecánico."
                )
                .setPositiveButton("Entendido", null)
                .show()
        }

        findViewById<TextView>(R.id.tvTituloDiagnostico).text = diagnostico.titulo
        findViewById<TextView>(R.id.tvConfianza).text = "Confianza: ${diagnostico.confianza}%"
        findViewById<TextView>(R.id.tvDescripcion).text = diagnostico.descripcion

        val lista = findViewById<LinearLayout>(R.id.listaRecomendaciones)
        diagnostico.recomendaciones.forEach { reco ->
            val fila = layoutInflater.inflate(R.layout.item_recomendacion, lista, false)
            fila.findViewById<TextView>(R.id.tvRecomendacion).text = reco
            lista.addView(fila)
        }

        // Si la IA no entregó pasos, no tiene sentido mostrar la guía
        val btnGuia = findViewById<Button>(R.id.btnVerGuia)
        if (diagnostico.pasos.isEmpty()) {
            btnGuia.visibility = View.GONE
        } else {
            btnGuia.setOnClickListener {
                startActivity(
                    Intent(this, GuiaPasosActivity::class.java)
                        .putExtra(EXTRA_DIAGNOSTICO, texto)
                )
            }
        }
    }
}