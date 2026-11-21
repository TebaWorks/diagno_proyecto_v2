package com.example.proyectofinal.ui.theme

import android.content.Intent
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
import com.example.proyectofinal.data.repositorio.SesionLocal
import com.example.proyectofinal.data.repositorio.mensajeErrorAuth
import kotlinx.coroutines.launch

class crear_cuenta : ComponentActivity() {

    private val MIN_CLAVE = 8   // debe coincidir con la configuración de Supabase

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
                pass.length < MIN_CLAVE ->
                    etPassword.error = "Mínimo $MIN_CLAVE caracteres"
                pass != confirmar ->
                    etConfirmar.error = "Las contraseñas no coinciden"
                !Conexion.hayInternet(this) ->
                    Toast.makeText(this, "Necesitas internet para crear tu cuenta", Toast.LENGTH_LONG).show()
                else -> {
                    btnRegistrar.isEnabled = false
                    lifecycleScope.launch {
                        try {
                            val auth = AuthRepositorio()
                            auth.registrar(nombre, correo, pass)
                            auth.crearLlave(SesionLocal(this@crear_cuenta))   // ya queda con sesión

                            Toast.makeText(this@crear_cuenta, "Cuenta creada para $nombre", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@crear_cuenta, NivelActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("DIAGNO", "Registro falló", e)
                            Toast.makeText(this@crear_cuenta, mensajeErrorAuth(e), Toast.LENGTH_LONG).show()
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