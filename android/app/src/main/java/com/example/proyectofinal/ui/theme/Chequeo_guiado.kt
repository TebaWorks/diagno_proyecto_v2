package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.R

// Pantalla "Chequeo guiado": elige qué parte del vehículo quieres revisar
class ChequeoGuiadoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chequeo_guiado)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btnVolverChequeo).setOnClickListener { finish() }

        val lista = findViewById<LinearLayout>(R.id.listaChequeo)
        ChequeoDatos.categorias.forEachIndexed { indice, c ->
            val item = layoutInflater.inflate(R.layout.item_chequeo, lista, false)
            item.findViewById<ImageView>(R.id.imgChequeoIcono).setImageResource(c.icono)
            item.findViewById<TextView>(R.id.tvChequeoTitulo).text = c.titulo
            item.findViewById<TextView>(R.id.tvChequeoDescripcion).text = c.descripcion
            item.setOnClickListener { abrirDetalle(indice) }
            lista.addView(item)
        }
    }

    // Abre el detalle de la categoría tocada (0 = Motor, 1 = Frenos, ...)
    private fun abrirDetalle(indice: Int) {
        startActivity(
            Intent(this, ChequeoDetalleActivity::class.java)
                .putExtra(ChequeoDetalleActivity.EXTRA_CATEGORIA, indice)
        )
    }
}