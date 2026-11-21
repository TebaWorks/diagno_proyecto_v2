package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.R

class BienvenidaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_bienvenida)

            // Margen para las barras del sistema
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutBienvenida)) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }

            // "Comenzar" abre MainActivity (ahí están Iniciar sesión / Crear cuenta)
            findViewById<Button>(R.id.btnComenzar).setOnClickListener {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        } catch (e: Throwable) {
            Log.e("BIENVENIDA", "Error al abrir la bienvenida", e)
            Toast.makeText(this, "Error en Bienvenida: $e", Toast.LENGTH_LONG).show()
        }
    }
}