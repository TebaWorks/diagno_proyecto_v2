package com.example.proyectofinal.ui.theme

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.modelo.Consulta
import com.example.proyectofinal.data.repositorio.ConsultaRepositorio
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Pantalla "Historial": consultas hechas en el chat con IA (y, más adelante, chequeos)
class HistorialActivity : ComponentActivity() {

    private val repo = ConsultaRepositorio()

    private lateinit var lista: LinearLayout
    private lateinit var tvVacio: TextView
    private lateinit var panelConsultas: View
    private lateinit var panelChequeos: View
    private lateinit var tabConsultas: TextView
    private lateinit var tabChequeos: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historial)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        lista = findViewById(R.id.listaConsultas)
        tvVacio = findViewById(R.id.tvVacioConsultas)
        panelConsultas = findViewById(R.id.panelConsultas)
        panelChequeos = findViewById(R.id.panelChequeos)
        tabConsultas = findViewById(R.id.tabConsultas)
        tabChequeos = findViewById(R.id.tabChequeos)

        findViewById<View>(R.id.btnVolverHistorial).setOnClickListener { finish() }
        tabConsultas.setOnClickListener { seleccionarTab(consultas = true) }
        tabChequeos.setOnClickListener { seleccionarTab(consultas = false) }
        seleccionarTab(consultas = true)
    }

    // Se actualiza cada vez que se vuelve a esta pantalla
    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun seleccionarTab(consultas: Boolean) {
        fun marcar(tab: TextView, activo: Boolean) {
            tab.setBackgroundResource(if (activo) R.drawable.bg_toggle_activo else 0)
            tab.setTextColor(
                ContextCompat.getColor(this, if (activo) R.color.white else R.color.texto)
            )
        }
        marcar(tabConsultas, consultas)
        marcar(tabChequeos, !consultas)
        panelConsultas.visibility = if (consultas) View.VISIBLE else View.GONE
        panelChequeos.visibility = if (consultas) View.GONE else View.VISIBLE
    }

    private fun cargar() {
        lifecycleScope.launch {
            val consultas: List<Consulta> = try {
                repo.obtener()
            } catch (e: Exception) {
                Log.e("HISTORIAL", "No se pudo cargar", e)
                Toast.makeText(
                    this@HistorialActivity,
                    "No se pudo cargar el historial: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                emptyList()
            }

            lista.removeAllViews()
            tvVacio.visibility = if (consultas.isEmpty()) View.VISIBLE else View.GONE

            consultas.forEach { c ->
                val item = layoutInflater.inflate(R.layout.item_consulta, lista, false)
                item.findViewById<TextView>(R.id.tvConsultaTitulo).text = c.titulo

                val fecha = fecha(c.creado_en)
                item.findViewById<TextView>(R.id.tvConsultaMeta).text =
                    listOfNotNull(c.vehiculo?.ifBlank { null }, fecha.ifBlank { null })
                        .joinToString(" · ")

                val tvResumen = item.findViewById<TextView>(R.id.tvConsultaResumen)
                if (c.resumen.isNullOrBlank()) {
                    tvResumen.visibility = View.GONE
                } else {
                    tvResumen.text = c.resumen
                }

                // Tocar: ver la conversación. Mantener presionado: eliminar
                item.setOnClickListener { verConversacion(c) }
                item.setOnLongClickListener {
                    MaterialAlertDialogBuilder(this@HistorialActivity)
                        .setTitle("Eliminar consulta")
                        .setMessage("¿Eliminar \"${c.titulo}\"?")
                        .setPositiveButton("Eliminar") { _, _ -> eliminar(c) }
                        .setNegativeButton("Cancelar", null)
                        .show()
                    true
                }
                lista.addView(item)
            }
        }
    }

    private fun verConversacion(c: Consulta) {
        val texto = c.mensajes.joinToString("\n\n") { m ->
            if (m.role == "user") {
                "Tú: " + m.content.replace("[Foto adjunta]", "(foto adjunta)")
            } else {
                "Diagno: " + m.content
            }
        }.ifBlank { "Esta consulta no tiene mensajes." }

        MaterialAlertDialogBuilder(this)
            .setTitle(c.titulo)
            .setMessage(texto)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun eliminar(c: Consulta) {
        val id = c.id ?: return
        lifecycleScope.launch {
            try {
                repo.eliminar(id)
                cargar()
            } catch (e: Exception) {
                Log.e("HISTORIAL", "No se pudo eliminar", e)
                Toast.makeText(
                    this@HistorialActivity,
                    "No se pudo eliminar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // "2026-11-06T05:14:00+00:00" -> "6 nov 2026" (en la hora del teléfono)
    private fun fecha(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            OffsetDateTime.parse(iso)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-CL")))
                .replace(".", "")
        } catch (e: Exception) {
            iso.take(10)
        }
    }
}