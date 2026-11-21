package com.example.proyectofinal.ui.theme

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.R
import com.example.proyectofinal.data.modelo.Diagnostico
import com.example.proyectofinal.data.modelo.PasoGuia
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.serialization.json.Json

// Pantalla "Guía de revisión": indicador de pasos, título, descripción, imagen y
// sub-pasos numerados. El botón "Siguiente" avanza; la flecha de volver retrocede.
class GuiaPasosActivity : ComponentActivity() {

    private var pasos: List<PasoGuia> = emptyList()
    private var indice = 0
    private val circulos = mutableListOf<TextView>()

    private lateinit var indicador: LinearLayout
    private lateinit var tvTitulo: TextView
    private lateinit var tvDetalle: TextView
    private lateinit var ivPaso: ImageView
    private lateinit var listaAcciones: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var btnSiguiente: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_guia_pasos)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        pasos = try {
            Json { ignoreUnknownKeys = true }.decodeFromString(
                Diagnostico.serializer(),
                intent.getStringExtra(ResultadoAnalisisActivity.EXTRA_DIAGNOSTICO) ?: ""
            ).pasos
        } catch (e: Exception) {
            emptyList()
        }
        if (pasos.isEmpty()) {
            Toast.makeText(this, "No hay pasos para mostrar", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        indicador = findViewById(R.id.indicadorPasos)
        tvTitulo = findViewById(R.id.tvTituloPaso)
        tvDetalle = findViewById(R.id.tvDetallePaso)
        ivPaso = findViewById(R.id.ivPaso)
        listaAcciones = findViewById(R.id.listaAcciones)
        scroll = findViewById(R.id.scrollGuia)
        btnSiguiente = findViewById(R.id.btnSiguienteGuia)

        crearIndicador()

        findViewById<View>(R.id.btnVolverGuia).setOnClickListener { retroceder() }
        onBackPressedDispatcher.addCallback(this) { retroceder() }
        btnSiguiente.setOnClickListener { siguiente() }

        mostrar()
    }

    // Círculos numerados unidos por líneas: 1 ─ 2 ─ 3 ─ 4
    private fun crearIndicador() {
        val dp = resources.displayMetrics.density
        val tam = ((if (pasos.size > 6) 26 else 32) * dp).toInt()

        pasos.forEachIndexed { i, _ ->
            if (i > 0) {
                indicador.addView(View(this).apply {
                    setBackgroundResource(R.color.azul_claro)
                    layoutParams = LinearLayout.LayoutParams(0, (2 * dp).toInt(), 1f).apply {
                        marginStart = (4 * dp).toInt()
                        marginEnd = (4 * dp).toInt()
                    }
                })
            }
            val circulo = TextView(this).apply {
                text = (i + 1).toString()
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(tam, tam)
            }
            circulos.add(circulo)
            indicador.addView(circulo)
        }
    }

    // Dibuja el paso actual
    private fun mostrar() {
        val paso = pasos[indice]

        circulos.forEachIndexed { i, c ->
            val activo = i == indice
            c.setBackgroundResource(if (activo) R.drawable.bg_circulo_azul else R.drawable.bg_circulo_claro)
            c.setTextColor(
                ContextCompat.getColor(this, if (activo) R.color.white else R.color.texto_sec)
            )
        }

        tvTitulo.text = paso.titulo
        tvDetalle.text = paso.detalle
        tvDetalle.visibility = if (paso.detalle.isBlank()) View.GONE else View.VISIBLE

        // Imagen: busca res/drawable/img_guia_<clave>; si no existe, prueba img_guia_general;
        // y si tampoco existe, oculta la imagen.
        var id = if (paso.imagen.isBlank()) 0
        else resources.getIdentifier("img_guia_${paso.imagen}", "drawable", packageName)
        if (id == 0) id = resources.getIdentifier("img_guia_general", "drawable", packageName)
        if (id != 0) {
            ivPaso.setImageResource(id)
            ivPaso.visibility = View.VISIBLE
        } else {
            ivPaso.visibility = View.GONE
        }

        // Sub-pasos numerados
        listaAcciones.removeAllViews()
        paso.acciones.forEachIndexed { i, accion ->
            val fila = layoutInflater.inflate(R.layout.item_accion_paso, listaAcciones, false)
            fila.findViewById<TextView>(R.id.tvNumeroAccion).text = (i + 1).toString()
            fila.findViewById<TextView>(R.id.tvTextoAccion).text = accion
            listaAcciones.addView(fila)
        }

        btnSiguiente.text = if (indice == pasos.size - 1) "Finalizar" else "Siguiente"
        scroll.scrollTo(0, 0)
    }

    private fun siguiente() {
        if (indice < pasos.size - 1) {
            indice++
            mostrar()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("Guía terminada")
                .setMessage("Si el problema persiste, acude a un taller especializado.")
                .setCancelable(false)
                .setPositiveButton("Cerrar") { _, _ -> finish() }
                .show()
        }
    }

    // Flecha de volver o botón atrás: paso anterior, o salir si es el primero
    private fun retroceder() {
        if (indice > 0) {
            indice--
            mostrar()
        } else {
            finish()
        }
    }
}