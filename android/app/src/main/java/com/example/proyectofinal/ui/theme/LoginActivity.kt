package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val etCorreo = findViewById<EditText>(R.id.etLoginCorreo)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnEntrar = findViewById<Button>(R.id.btnEntrar)

        btnEntrar.setOnClickListener {
            val correo = etCorreo.text.toString().trim().lowercase()
            val pass = etPassword.text.toString()

            if (correo.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnEntrar.isEnabled = false
            lifecycleScope.launch {
                try {
                    AuthRepositorio().iniciarSesion(correo, pass)
                    val intent = Intent(this@LoginActivity, NivelActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("LOGIN", "Error al iniciar sesión", e)
                    Toast.makeText(
                        this@LoginActivity,
                        "Correo o contraseña incorrectos",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    btnEntrar.isEnabled = true
                }
            }
        }

        findViewById<Button>(R.id.btnIrCrearCuenta).setOnClickListener {
            startActivity(Intent(this, crear_cuenta::class.java))
        }

        findViewById<Button>(R.id.btnVolverLogin).setOnClickListener { finish() }
    }
}