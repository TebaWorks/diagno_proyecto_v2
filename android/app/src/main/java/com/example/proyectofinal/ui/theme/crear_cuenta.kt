package com.example.proyectofinal.ui.theme

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import kotlinx.coroutines.launch

class crear_cuenta : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crear_cuenta)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmar = findViewById<EditText>(R.id.etConfirmarPassword)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim().lowercase()
            val pass = etPassword.text.toString()
            val confirmar = etConfirmar.text.toString()

            when {
                nombre.isEmpty() || correo.isEmpty() || pass.isEmpty() ->
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                !Patterns.EMAIL_ADDRESS.matcher(correo).matches() ->
                    etCorreo.error = "Correo no válido"
                pass.length < 6 ->
                    etPassword.error = "Mínimo 6 caracteres"
                pass != confirmar ->
                    etConfirmar.error = "Las contraseñas no coinciden"
                else -> {
                    btnRegistrar.isEnabled = false
                    lifecycleScope.launch {
                        try {
                            AuthRepositorio().registrar(nombre, correo, pass)
                            Toast.makeText(this@crear_cuenta, "Cuenta creada para $nombre", Toast.LENGTH_SHORT).show()
                            finish()
                        } catch (e: Exception) {
                            Log.e("DIAGNO", "Registro falló", e)
                            Toast.makeText(this@crear_cuenta, "No se pudo registrar: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            btnRegistrar.isEnabled = true
                        }
                    }
                }
            }
        }

        findViewById<Button>(R.id.btnVolver).setOnClickListener { finish() }
    }
}
