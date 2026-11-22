package com.example.proyectofinal.ui.theme

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.R

// Muestra los Términos de uso y la Política de privacidad (res/raw/terminos_privacidad.txt)
class LegalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_legal)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.legalRaiz)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        findViewById<TextView>(R.id.tvTextoLegal).text =
            resources.openRawResource(R.raw.terminos_privacidad).bufferedReader().use { it.readText() }

        findViewById<ImageButton>(R.id.btnVolverLegal).setOnClickListener { finish() }
    }
}