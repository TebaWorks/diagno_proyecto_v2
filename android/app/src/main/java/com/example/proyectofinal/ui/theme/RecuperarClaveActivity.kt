package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
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
import com.example.proyectofinal.data.repositorio.mensajeErrorAuth
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Recuperación de contraseña: 1) correo  2) código  3) contraseña nueva → volver a iniciar sesión
class RecuperarClaveActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CORREO = "correo"
        private const val ESPERA_REENVIO_MS = 60_000L
        private const val MIN_CLAVE = 8
    }

    private val auth = AuthRepositorio()
    private var paso = 1
    private var correo = ""
    private var temporizador: CountDownTimer? = null

    // true entre la verificación del código y el cambio de clave: hay una sesión temporal abierta
    private var sesionTemporal = false

    private lateinit var tilCorreo: TextInputLayout
    private lateinit var tilCodigo: TextInputLayout
    private lateinit var tilNueva: TextInputLayout
    private lateinit var tilConfirmar: TextInputLayout
    private lateinit var etCorreo: TextInputEditText
    private lateinit var etCodigo: TextInputEditText
    private lateinit var etNueva: TextInputEditText
    private lateinit var etConfirmar: TextInputEditText
    private lateinit var btnAccion: MaterialButton
    private lateinit var btnReenviar: MaterialButton
    private lateinit var progreso: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recuperar_clave)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recuperarRaiz)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        tilCorreo = findViewById(R.id.tilCorreoRecuperar)
        tilCodigo = findViewById(R.id.tilCodigoRecuperar)
        tilNueva = findViewById(R.id.tilNuevaClave)
        tilConfirmar = findViewById(R.id.tilConfirmarClave)
        etCorreo = findViewById(R.id.etCorreoRecuperar)
        etCodigo = findViewById(R.id.etCodigoRecuperar)
        etNueva = findViewById(R.id.etNuevaClave)
        etConfirmar = findViewById(R.id.etConfirmarClave)
        btnAccion = findViewById(R.id.btnAccionRecuperar)
        btnReenviar = findViewById(R.id.btnReenviarRecuperar)
        progreso = findViewById(R.id.progresoRecuperar)

        etCorreo.setText(intent.getStringExtra(EXTRA_CORREO).orEmpty())

        etCorreo.doAfterTextChanged { tilCorreo.error = null }
        etCodigo.doAfterTextChanged { tilCodigo.error = null }
        etNueva.doAfterTextChanged { tilNueva.error = null }
        etConfirmar.doAfterTextChanged { tilConfirmar.error = null }

        btnAccion.setOnClickListener {
            when (paso) {
                1 -> enviarCodigo()
                2 -> verificarCodigo()
                else -> guardarClave()
            }
        }
        btnReenviar.setOnClickListener { enviarCodigo(reenvio = true) }

        findViewById<ImageButton>(R.id.btnVolverRecuperar).setOnClickListener { retroceder() }
        onBackPressedDispatcher.addCallback(this) { retroceder() }

        mostrarPaso(1)
    }

    private fun limpiarErrores() {
        tilCorreo.error = null
        tilCodigo.error = null
        tilNueva.error = null
        tilConfirmar.error = null
    }

    private fun retroceder() {
        when (paso) {
            1 -> finish()
            2 -> mostrarPaso(1)
            else -> {
                // Abandona sin cambiar la clave: cierra la sesión temporal y vuelve al inicio
                cerrarSesionTemporal()
                mostrarPaso(1)
            }
        }
    }

    private fun mostrarPaso(n: Int) {
        paso = n
        limpiarErrores()
        tilCorreo.visibility = if (n == 1) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.pasoCodigo).visibility = if (n == 2) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.pasoNuevaClave).visibility = if (n == 3) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tvNotaRecuperar).visibility = if (n == 3) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.tvPasoRecuperar).text = "Paso $n de 3"

        val (titulo, subtitulo, boton) = when (n) {
            1 -> Triple("Recupera tu contraseña", "Escribe tu correo y te enviaremos un código", "Enviar código")
            2 -> Triple(
                "Ingresa el código",
                "Si $correo tiene una cuenta, le enviamos un código de verificación.",
                "Verificar código"
            )
            else -> Triple("Crea una contraseña nueva", "Úsala la próxima vez que inicies sesión.", "Guardar contraseña")
        }
        findViewById<TextView>(R.id.tvTituloRecuperar).text = titulo
        findViewById<TextView>(R.id.tvSubtituloRecuperar).text = subtitulo
        btnAccion.text = boton

        if (n == 2) etCodigo.text?.clear()
        if (n == 3) { etNueva.text?.clear(); etConfirmar.text?.clear() }
    }

    // Paso 1: envía (o reenvía) el código
    private fun enviarCodigo(reenvio: Boolean = false) {
        limpiarErrores()
        if (!reenvio) {
            correo = etCorreo.text?.toString()?.trim()?.lowercase().orEmpty()
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                tilCorreo.error = "Ingresa un correo válido"
                return
            }
        }
        if (!sinInternetAviso()) return

        cargando(true)
        lifecycleScope.launch {
            try {
                auth.enviarCodigoRecuperacion(correo)
                if (reenvio) aviso("Te enviamos un código nuevo") else mostrarPaso(2)
                iniciarEspera()
            } catch (e: Exception) {
                Log.e("RECUPERAR", "No se pudo enviar el código", e)
                aviso(mensajeErrorAuth(e))
            } finally {
                cargando(false)
            }
        }
    }

    // Paso 2: verifica el código
    private fun verificarCodigo() {
        limpiarErrores()
        val codigo = etCodigo.text?.toString()?.trim().orEmpty()
        if (codigo.length !in 6..10 || !codigo.all { it.isDigit() }) {
            tilCodigo.error = "Ingresa el código completo que llegó a tu correo"
            return
        }
        if (!sinInternetAviso()) return

        ocultarTeclado()
        cargando(true)
        lifecycleScope.launch {
            try {
                auth.verificarCodigoRecuperacion(correo, codigo)
                sesionTemporal = true
                temporizador?.cancel()
                mostrarPaso(3)
            } catch (e: Exception) {
                Log.e("RECUPERAR", "Código rechazado", e)
                tilCodigo.error = mensajeErrorAuth(e)
            } finally {
                cargando(false)
            }
        }
    }

    // Paso 3: guarda la contraseña nueva y vuelve al inicio para entrar con ella
    private fun guardarClave() {
        limpiarErrores()
        val nueva = etNueva.text?.toString().orEmpty()
        val confirmar = etConfirmar.text?.toString().orEmpty()

        var valido = true
        if (nueva.length < MIN_CLAVE || !nueva.any { it.isLetter() } || !nueva.any { it.isDigit() }) {
            tilNueva.error = "Mínimo $MIN_CLAVE caracteres, con letras y números"; valido = false
        }
        if (confirmar != nueva) { tilConfirmar.error = "Las contraseñas no coinciden"; valido = false }
        if (!valido || !sinInternetAviso()) return

        ocultarTeclado()
        cargando(true)
        lifecycleScope.launch {
            try {
                auth.cambiarClaveRecuperada(nueva)   // también cierra la sesión temporal
                sesionTemporal = false
                aviso("Contraseña actualizada. Inicia sesión con tu contraseña nueva.")
                val intent = Intent(this@RecuperarClaveActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("RECUPERAR", "No se pudo cambiar la contraseña", e)
                aviso(mensajeErrorAuth(e))
                cargando(false)
            }
        }
    }

    // Cierra la sesión temporal si el usuario abandona antes de cambiar la clave
    private fun cerrarSesionTemporal() {
        if (!sesionTemporal) return
        sesionTemporal = false
        CoroutineScope(Dispatchers.IO).launch { runCatching { auth.cerrarSesion() } }
    }

    private fun sinInternetAviso(): Boolean {
        if (Conexion.hayInternet(this)) return true
        aviso("Necesitas internet para recuperar tu contraseña")
        return false
    }

    private fun aviso(texto: String) = Toast.makeText(this, texto, Toast.LENGTH_LONG).show()

    private fun iniciarEspera() {
        temporizador?.cancel()
        btnReenviar.isEnabled = false
        temporizador = object : CountDownTimer(ESPERA_REENVIO_MS, 1_000) {
            override fun onTick(restante: Long) {
                btnReenviar.text = "Reenviar código en ${restante / 1000} s"
            }
            override fun onFinish() {
                btnReenviar.text = "Reenviar código"
                btnReenviar.isEnabled = true
            }
        }.start()
    }

    private fun cargando(activo: Boolean) {
        btnAccion.isEnabled = !activo
        btnAccion.text = if (activo) "" else when (paso) {
            1 -> "Enviar código"
            2 -> "Verificar código"
            else -> "Guardar contraseña"
        }
        progreso.visibility = if (activo) View.VISIBLE else View.GONE
    }

    private fun ocultarTeclado() {
        currentFocus?.let { vista ->
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(vista.windowToken, 0)
            vista.clearFocus()
        }
    }

    override fun onDestroy() {
        temporizador?.cancel()
        cerrarSesionTemporal()
        super.onDestroy()
    }
}