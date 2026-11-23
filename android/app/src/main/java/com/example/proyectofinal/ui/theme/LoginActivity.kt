package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.SesionLocal
import com.example.proyectofinal.data.repositorio.esCorreoNoConfirmado
import com.example.proyectofinal.data.repositorio.mensajeErrorAuth
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private lateinit var tilCorreo: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etCorreo: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnEntrar: MaterialButton
    private lateinit var progreso: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Deja espacio para la barra de estado y la de navegación
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRaiz)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        tilCorreo = findViewById(R.id.tilLoginCorreo)
        tilPassword = findViewById(R.id.tilLoginPassword)
        etCorreo = findViewById(R.id.etLoginCorreo)
        etPassword = findViewById(R.id.etLoginPassword)
        btnEntrar = findViewById(R.id.btnEntrar)
        progreso = findViewById(R.id.progresoLogin)

        // Al escribir, se borra el error de ese campo
        etCorreo.doAfterTextChanged { tilCorreo.error = null }
        etPassword.doAfterTextChanged { tilPassword.error = null }

        // "Enter" en la contraseña = iniciar sesión
        etPassword.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_DONE) { intentarIngreso(); true } else false
        }

        btnEntrar.setOnClickListener { intentarIngreso() }

        findViewById<MaterialButton>(R.id.btnOlvideClave).setOnClickListener {
            Toast.makeText(this, "Recuperación de contraseña disponible pronto", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnIrCrearCuenta).setOnClickListener {
            startActivity(Intent(this, crear_cuenta::class.java))
        }

        findViewById<ImageButton>(R.id.btnVolverLogin).setOnClickListener { finish() }
    }

    private fun intentarIngreso() {
        val correo = etCorreo.text?.toString()?.trim()?.lowercase().orEmpty()
        val pass = etPassword.text?.toString().orEmpty()

        // Validación en la app, antes de ir a internet
        var valido = true
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            tilCorreo.error = "Ingresa un correo válido"
            valido = false
        }
        if (pass.isEmpty()) {
            tilPassword.error = "Ingresa tu contraseña"
            valido = false
        }
        if (!valido) return

        if (!Conexion.hayInternet(this)) {
            Toast.makeText(this, "Necesitas internet para iniciar sesión", Toast.LENGTH_LONG).show()
            return
        }

        ocultarTeclado()
        cargando(true)
        lifecycleScope.launch {
            try {
                val auth = AuthRepositorio()
                auth.iniciarSesion(correo, pass)
                auth.crearLlave(SesionLocal(this@LoginActivity))   // llave de acceso

                val intent = Intent(this@LoginActivity, NivelActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("LOGIN", "Error al iniciar sesión", e)
                cargando(false)
                if (esCorreoNoConfirmado(e)) {
                    // Reenvía el código y lleva a la pantalla de verificación
                    runCatching { AuthRepositorio().reenviarCodigoRegistro(correo) }
                    val intent = Intent(this@LoginActivity, VerificarCorreoActivity::class.java)
                    intent.putExtra(VerificarCorreoActivity.EXTRA_CORREO, correo)
                    startActivity(intent)
                } else {
                    tilPassword.error = mensajeErrorAuth(e)
                }
            }
        }
    }

    // Botón con indicador de carga; bloquea los campos mientras tanto
    private fun cargando(activo: Boolean) {
        btnEntrar.isEnabled = !activo
        btnEntrar.text = if (activo) "" else "Iniciar sesión"
        progreso.visibility = if (activo) View.VISIBLE else View.GONE
        etCorreo.isEnabled = !activo
        etPassword.isEnabled = !activo
    }

    private fun ocultarTeclado() {
        currentFocus?.let { vista ->
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(vista.windowToken, 0)
            vista.clearFocus()
        }
    }
}