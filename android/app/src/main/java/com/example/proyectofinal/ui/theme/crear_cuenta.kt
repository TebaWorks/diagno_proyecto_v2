package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.ValidacionCuenta
import com.example.proyectofinal.data.repositorio.mensajeErrorAuth
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

// Registro en dos pasos: 1) datos de la cuenta  2) consentimientos (Ley 21.719)
class crear_cuenta : ComponentActivity() {

    private val MIN_CLAVE = 8   // debe coincidir con Supabase (Password requirements: letras y números)
    private var paso = 1

    private lateinit var tilNombre: TextInputLayout
    private lateinit var tilCorreo: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmar: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var etCorreo: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmar: TextInputEditText
    private lateinit var cbPrivacidad: MaterialCheckBox
    private lateinit var cbMayorEdad: MaterialCheckBox
    private lateinit var cbCompartir: MaterialCheckBox
    private lateinit var btnRegistrar: MaterialButton
    private lateinit var progreso: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crear_cuenta)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registroRaiz)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        tilNombre = findViewById(R.id.tilNombre)
        tilCorreo = findViewById(R.id.tilCorreo)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmar = findViewById(R.id.tilConfirmar)
        etNombre = findViewById(R.id.etNombre)
        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)
        etConfirmar = findViewById(R.id.etConfirmarPassword)
        cbPrivacidad = findViewById(R.id.cbPrivacidad)
        cbMayorEdad = findViewById(R.id.cbMayorEdad)
        cbCompartir = findViewById(R.id.cbCompartir)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        progreso = findViewById(R.id.progresoRegistro)

        // Al escribir se limpia el error del campo
        etNombre.doAfterTextChanged { tilNombre.error = null }
        etCorreo.doAfterTextChanged { tilCorreo.error = null }
        etConfirmar.doAfterTextChanged { tilConfirmar.error = null }

        // Indicador de fuerza de la contraseña mientras se escribe
        etPassword.doAfterTextChanged { texto ->
            tilPassword.error = null
            val clave = texto?.toString().orEmpty()
            tilPassword.helperText = when {
                clave.isEmpty() -> "Mínimo 8 caracteres, con letras y números"
                else -> when (ValidacionCuenta.fuerzaClave(clave)) {
                    2 -> "Seguridad: fuerte"
                    1 -> "Seguridad: media · agrega mayúsculas o símbolos"
                    else -> "Seguridad: débil · usa más caracteres, mayúsculas y números"
                }
            }
        }

        // El botón "Crear cuenta" solo se activa con los dos consentimientos obligatorios
        cbPrivacidad.setOnCheckedChangeListener { _, _ -> actualizarBoton() }
        cbMayorEdad.setOnCheckedChangeListener { _, _ -> actualizarBoton() }

        btnRegistrar.setOnClickListener { if (paso == 1) validarDatos() else crearCuenta() }

        findViewById<MaterialButton>(R.id.btnLeerPolitica).setOnClickListener {
            startActivity(Intent(this, LegalActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnIrLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Volver: en el paso 2 regresa al paso 1; en el paso 1 cierra la pantalla
        findViewById<ImageButton>(R.id.btnVolver).setOnClickListener { retroceder() }
        onBackPressedDispatcher.addCallback(this) { retroceder() }

        mostrarPaso(1)
    }

    private fun mostrarPaso(n: Int) {
        paso = n
        findViewById<LinearLayout>(R.id.pasoDatos).visibility = if (n == 1) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.pasoConsentimiento).visibility = if (n == 2) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tvPaso).text = "Paso $n de 2"
        findViewById<LinearProgressIndicator>(R.id.progresoPasos).setProgressCompat(n * 50, true)
        findViewById<TextView>(R.id.tvTituloPaso).text = if (n == 1) "Crea tu cuenta" else "Tus datos, tu decisión"
        findViewById<TextView>(R.id.tvSubtituloPaso).text =
            if (n == 1) "Tus datos para ingresar a Diagno" else "Lee y acepta cómo cuidamos tu información"
        btnRegistrar.text = if (n == 1) "Continuar" else "Crear cuenta"
        actualizarBoton()
    }

    private fun actualizarBoton() {
        btnRegistrar.isEnabled = paso == 1 || (cbPrivacidad.isChecked && cbMayorEdad.isChecked)
    }

    private fun retroceder() {
        if (paso == 2) mostrarPaso(1) else finish()
    }

    // Paso 1: validación local antes de avanzar
    private fun validarDatos() {
        val nombre = etNombre.text?.toString()?.trim()?.replace(Regex("\\s+"), " ").orEmpty()
        val correo = etCorreo.text?.toString()?.trim()?.lowercase().orEmpty()
        val pass = etPassword.text?.toString().orEmpty()
        val confirmar = etConfirmar.text?.toString().orEmpty()

        var valido = true
        if (!ValidacionCuenta.nombreValido(nombre)) {
            tilNombre.error = "Usa solo letras (entre 2 y 50 caracteres)"; valido = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            tilCorreo.error = "Ingresa un correo válido"; valido = false
        } else if (ValidacionCuenta.esDesechable(correo)) {
            tilCorreo.error = "No se aceptan correos temporales"; valido = false
        } else {
            ValidacionCuenta.sugerenciaCorreo(correo)?.let { sugerido ->
                tilCorreo.error = "¿Quisiste decir $sugerido?"
                Snackbar.make(findViewById(R.id.registroRaiz), "¿Quisiste decir $sugerido?", Snackbar.LENGTH_LONG)
                    .setAction("Corregir") { etCorreo.setText(sugerido) }
                    .show()
                valido = false
            }
        }
        if (pass.length < MIN_CLAVE || !pass.any { it.isLetter() } || !pass.any { it.isDigit() }) {
            tilPassword.error = "Mínimo $MIN_CLAVE caracteres, con letras y números"; valido = false
        }
        if (confirmar != pass) { tilConfirmar.error = "Las contraseñas no coinciden"; valido = false }

        if (valido) {
            etNombre.setText(nombre)   // guarda el nombre sin espacios dobles
            ocultarTeclado()
            mostrarPaso(2)
        }
    }

    // Paso 2: crea la cuenta con los consentimientos
    private fun crearCuenta() {
        if (!cbPrivacidad.isChecked || !cbMayorEdad.isChecked) return
        if (!Conexion.hayInternet(this)) {
            Toast.makeText(this, "Necesitas internet para crear tu cuenta", Toast.LENGTH_LONG).show()
            return
        }

        val nombre = etNombre.text?.toString()?.trim().orEmpty()
        val correo = etCorreo.text?.toString()?.trim()?.lowercase().orEmpty()
        val pass = etPassword.text?.toString().orEmpty()

        cargando(true)
        lifecycleScope.launch {
            try {
                val auth = AuthRepositorio()
                val pendiente = auth.registrar(nombre, correo, pass, aceptaCompartir = cbCompartir.isChecked)

                if (pendiente) {
                    // Confirm email activo: hay que ingresar el código que llegó al correo
                    val intent = Intent(this@crear_cuenta, VerificarCorreoActivity::class.java)
                    intent.putExtra(VerificarCorreoActivity.EXTRA_CORREO, correo)
                    startActivity(intent)
                    cargando(false)
                } else {
                    // Confirm email desactivado: la cuenta queda lista de inmediato
                    Toast.makeText(this@crear_cuenta, "¡Cuenta creada! Bienvenido, $nombre", Toast.LENGTH_SHORT).show()
                    continuarTrasIngreso()
                }
            } catch (e: Exception) {
                Log.e("REGISTRO", "Registro falló", e)
                cargando(false)
                val mensaje = mensajeErrorAuth(e)
                if ("correo" in mensaje.lowercase()) {
                    // Problema con el correo: vuelve al paso 1 y lo marca en el campo
                    mostrarPaso(1)
                    tilCorreo.error = mensaje
                } else {
                    Toast.makeText(this@crear_cuenta, mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun cargando(activo: Boolean) {
        btnRegistrar.isEnabled = !activo
        btnRegistrar.text = if (activo) "" else "Crear cuenta"
        progreso.visibility = if (activo) View.VISIBLE else View.GONE
        cbPrivacidad.isEnabled = !activo
        cbMayorEdad.isEnabled = !activo
        cbCompartir.isEnabled = !activo
    }

    private fun ocultarTeclado() {
        currentFocus?.let { vista ->
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(vista.windowToken, 0)
            vista.clearFocus()
        }
    }
}