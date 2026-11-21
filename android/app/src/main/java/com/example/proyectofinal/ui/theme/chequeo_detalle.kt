package com.example.proyectofinal.ui.theme

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.R
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator

// Detalle del chequeo: lista de puntos de una categoría, con "Siguiente" para pasar a la próxima
class ChequeoDetalleActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CATEGORIA = "categoria"
    }

    private val categorias = ChequeoDatos.categorias
    private var indice = 0

    // Lo marcado en cada categoría (solo mientras la pantalla está abierta)
    private val marcados = categorias.map { BooleanArray(it.puntos.size) }

    private lateinit var tvTitulo: TextView
    private lateinit var tvPaso: TextView
    private lateinit var progreso: LinearProgressIndicator
    private lateinit var lista: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var btnSiguiente: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chequeo_detalle)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        tvTitulo = findViewById(R.id.tvTituloChequeo)
        tvPaso = findViewById(R.id.tvPasoChequeo)
        progreso = findViewById(R.id.progresoChequeo)
        lista = findViewById(R.id.listaPuntos)
        scroll = findViewById(R.id.scrollChequeo)
        btnSiguiente = findViewById(R.id.btnSiguienteChequeo)

        progreso.max = categorias.size
        indice = intent.getIntExtra(EXTRA_CATEGORIA, 0).coerceIn(0, categorias.size - 1)

        findViewById<View>(R.id.btnVolverDetalle).setOnClickListener { retroceder() }
        onBackPressedDispatcher.addCallback(this) { retroceder() }
        btnSiguiente.setOnClickListener { siguiente() }

        mostrar()
    }

    // Dibuja la categoría actual
    private fun mostrar() {
        val cat = categorias[indice]
        val actual = indice

        tvTitulo.text = "Chequeo de ${cat.titulo.lowercase()}"
        tvPaso.text = "${indice + 1}/${categorias.size}"
        progreso.setProgressCompat(indice + 1, true)
        btnSiguiente.text = if (indice == categorias.size - 1) "Finalizar" else "Siguiente"

        val densidad = resources.displayMetrics.density
        lista.removeAllViews()
        cat.puntos.forEachIndexed { i, punto ->
            if (i > 0) {
                // línea separadora entre puntos
                lista.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (1 * densidad).toInt()
                    ).apply { marginStart = (16 * densidad).toInt() }
                    setBackgroundResource(R.color.fondo)
                })
            }

            val fila = layoutInflater.inflate(R.layout.item_chequeo_punto, lista, false)
            val check = fila.findViewById<MaterialCheckBox>(R.id.checkPunto)
            check.text = punto.titulo
            check.isChecked = marcados[actual][i]
            check.setOnCheckedChangeListener { _, marcado -> marcados[actual][i] = marcado }

            // El botón ⓘ muestra la explicación de ese punto
            fila.findViewById<ImageView>(R.id.btnInfoPunto).setOnClickListener {
                MaterialAlertDialogBuilder(this)
                    .setTitle(punto.titulo)
                    .setMessage(punto.info)
                    .setPositiveButton("Entendido", null)
                    .show()
            }
            lista.addView(fila)
        }
        scroll.scrollTo(0, 0)
    }

    private fun siguiente() {
        if (indice < categorias.size - 1) {
            indice++
            mostrar()
        } else {
            finalizar()
        }
    }

    // Flecha de volver o botón atrás: categoría anterior, o salir si es la primera
    private fun retroceder() {
        if (indice > 0) {
            indice--
            mostrar()
        } else {
            finish()
        }
    }

    private fun finalizar() {
        val total = categorias.sumOf { it.puntos.size }
        val hechos = marcados.sumOf { fila -> fila.count { it } }
        MaterialAlertDialogBuilder(this)
            .setTitle("Chequeo terminado")
            .setMessage("Revisaste $hechos de $total puntos.")
            .setCancelable(false)
            .setPositiveButton("Cerrar") { _, _ -> finish() }
            .show()
    }
}