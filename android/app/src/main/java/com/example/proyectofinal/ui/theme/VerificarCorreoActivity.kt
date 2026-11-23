package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.TextView
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
import com.example.proyectofinal.data.repositorio.mensajeErrorAuth
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

// Pantalla del código de verificación que llega al correo después de registrarse
class VerificarCorreoActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CORREO = "correo"
        private const val ESPERA_REENVIO_MS = 60_000L
    }

    private val auth = AuthRepositorio()
    private lateinit var correo: String
    private lateinit var tilCodigo: TextInputLayout
    private lateinit var etCodigo: TextInputEditText
    private lateinit var btnVerificar: MaterialButton
    private lateinit var btnReenviar: MaterialButton
    private lateinit var progreso: CircularProgressIndicator
    private var temporizador: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verificar_correo)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.verificarRaiz)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        correo = intent.getStringExtra(EXTRA_CORREO).orEmpty()
        if (correo.isEmpty()) { finish(); return }

        findViewById<TextView>(R.id.tvCorreoDestino).text =
            "Te enviamos un código a $correo. Ingrésalo para activar tu cuenta."

        tilCodigo = findViewById(R.id.tilCodigo)
        etCodigo = findViewById(R.id.etCodigo)
        btnVerificar = findViewById(R.id.btnVerificar)
        btnReenviar = findViewById(R.id.btnReenviar)
        progreso = findViewById(R.id.progresoVerificar)

        etCodigo.doAfterTextChanged { tilCodigo.error = null }
        etCodigo.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_DONE) { verificar(); true } else false
        }
        btnVerificar.setOnClickListener { verificar() }
        btnReenviar.setOnClickListener { reenviar() }

        // Volver o "usar otro correo": regresa al registro
        findViewById<MaterialButton>(R.id.btnCambiarCorreo).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnVolverVerificar).setOnClickListener { finish() }

        iniciarEspera()   // el primer código acaba de enviarse
    }

    private fun verificar() {
        val codigo = etCodigo.text?.toString()?.trim().orEmpty()
        // Acepta de 6 a 10 dígitos (según "Email OTP Length" en Supabase)
        if (codigo.length !in 6..10 || !codigo.all { it.isDigit() }) {
            tilCodigo.error = "Ingresa el código completo que llegó a tu correo"
            return
        }
        if (!Conexion.hayInternet(this)) {
            Toast.makeText(this, "Necesitas internet para verificar tu correo", Toast.LENGTH_LONG).show()
            return
        }

        cargando(true)
        lifecycleScope.launch {
            try {
                auth.verificarCodigoRegistro(correo, codigo)
                auth.crearLlave(SesionLocal(this@VerificarCorreoActivity))

                Toast.makeText(this@VerificarCorreoActivity, "¡Correo verificado!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@VerificarCorreoActivity, NivelActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("VERIFICAR", "Código rechazado", e)
                tilCodigo.error = mensajeErrorAuth(e)
                cargando(false)
            }
        }
    }

    private fun reenviar() {
        if (!Conexion.hayInternet(this)) {
            Toast.makeText(this, "Necesitas internet para reenviar el código", Toast.LENGTH_LONG).show()
            return
        }
        btnReenviar.isEnabled = false
        lifecycleScope.launch {
            try {
                auth.reenviarCodigoRegistro(correo)
                Toast.makeText(this@VerificarCorreoActivity, "Te enviamos un código nuevo", Toast.LENGTH_SHORT).show()
                iniciarEspera()
            } catch (e: Exception) {
                Log.e("VERIFICAR", "No se pudo reenviar", e)
                Toast.makeText(this@VerificarCorreoActivity, mensajeErrorAuth(e), Toast.LENGTH_LONG).show()
                btnReenviar.isEnabled = true
            }
        }
    }

    // Evita reenvíos seguidos: 60 segundos de espera entre cada uno
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
        btnVerificar.isEnabled = !activo
        btnVerificar.text = if (activo) "" else "Verificar"
        progreso.visibility = if (activo) View.VISIBLE else View.GONE
        etCodigo.isEnabled = !activo
    }

    override fun onDestroy() {
        temporizador?.cancel()
        super.onDestroy()
    }
}