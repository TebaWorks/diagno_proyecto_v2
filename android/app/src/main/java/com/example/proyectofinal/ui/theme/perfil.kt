package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.materialswitch.MaterialSwitch
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.CacheLocal
import com.example.proyectofinal.data.repositorio.EstadoSesion
import com.example.proyectofinal.data.repositorio.SesionLocal
import com.example.proyectofinal.data.repositorio.mensajeErrorAuth
import com.example.proyectofinal.data.repositorio.NotificacionLocal
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import android.text.InputType
import android.widget.FrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
class perfil : ComponentActivity() {

    private val auth = AuthRepositorio()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val tvNombre = findViewById<TextView>(R.id.tvPerfilNombre)
        val tvCorreo = findViewById<TextView>(R.id.tvPerfilCorreo)
        val etNombre = findViewById<EditText>(R.id.etPerfilNombre)
        val etCorreo = findViewById<EditText>(R.id.etPerfilCorreo)
        val etPassword = findViewById<EditText>(R.id.etPerfilPassword)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPerfil)
        val tvIniciales = findViewById<TextView>(R.id.tvIniciales)
        val tvEditar = findViewById<TextView>(R.id.tvEditar)
        val cardEditar = findViewById<View>(R.id.cardEditar)
        val switchOffline = findViewById<MaterialSwitch>(R.id.switchOffline)

        // ===== Privacidad (Ley 21.719) =====

        // Consentimiento opcional: se carga el valor actual y luego se escucha el cambio
        val switchCompartir = findViewById<MaterialSwitch>(R.id.switchCompartir)
        lifecycleScope.launch {
            runCatching { auth.aceptaCompartir() }.onSuccess { switchCompartir.isChecked = it }
            switchCompartir.setOnCheckedChangeListener { boton, activado ->
                if (!Conexion.hayInternet(this@perfil)) {
                    boton.isChecked = !activado
                    toast("Necesitas internet para cambiar esta opción")
                    return@setOnCheckedChangeListener
                }
                lifecycleScope.launch {
                    try {
                        auth.actualizarCompartir(activado)
                        toast(if (activado) "Tus soluciones podrán ayudar a otros usuarios" else "Ya no se compartirán tus soluciones")
                    } catch (e: Exception) {
                        Log.e("PERFIL", "No se pudo cambiar el consentimiento", e)
                        boton.isChecked = !activado
                        toast(mensajeErrorAuth(e))
                    }
                }
            }
        }

        findViewById<View>(R.id.filaPrivacidad).setOnClickListener {
            startActivity(Intent(this, LegalActivity::class.java))
        }

        findViewById<View>(R.id.filaEliminarCuenta).setOnClickListener { confirmarEliminarCuenta() }

        findViewById<View>(R.id.btnVolverPerfil).setOnClickListener { finish() }

        // "Editar" muestra u oculta el formulario de datos personales
        tvEditar.setOnClickListener {
            val mostrar = cardEditar.visibility != View.VISIBLE
            cardEditar.visibility = if (mostrar) View.VISIBLE else View.GONE
            tvEditar.text = if (mostrar) "Listo" else "Editar"
        }

        // Modo sin conexión: se guarda la preferencia en el dispositivo
        val prefs = getSharedPreferences("ajustes", MODE_PRIVATE)
        switchOffline.isChecked = prefs.getBoolean("modo_sin_conexion", true)
        switchOffline.setOnCheckedChangeListener { _, activo ->
            prefs.edit().putBoolean("modo_sin_conexion", activo).apply()
            if (!activo) CacheLocal(this).borrar()   // al apagarlo se borran los datos guardados
        }

        // El correo es el identificador de la cuenta en Supabase: solo lectura
        etCorreo.isEnabled = false

        // Cargar datos: primero lo guardado en el teléfono, luego Supabase si hay internet.
        // La falta de internet NUNCA cierra la sesión.
        val sesion = SesionLocal(this)

        fun mostrar(nombre: String, correo: String) {
            tvNombre.text = nombre
            tvCorreo.text = correo
            tvIniciales.text = iniciales(nombre)
            etNombre.setText(nombre)
            etCorreo.setText(correo)
        }

        lifecycleScope.launch {
            val estado = auth.estadoSesion(sesion, Conexion.hayInternet(this@perfil))
            if (estado == EstadoSesion.NINGUNA || estado == EstadoSesion.REVOCADA) {
                cerrarSesion()
                return@launch
            }
            mostrar(sesion.nombre(), auth.correoActual() ?: "")

            if (estado == EstadoSesion.ACTIVA) {
                runCatching { auth.nombreUsuario() }.onSuccess { nombre ->
                    sesion.actualizarNombre(nombre)
                    CacheLocal(this@perfil).guardarNombre(nombre)
                    mostrar(nombre, auth.correoActual() ?: "")
                }
            }
        }

        btnGuardar.setOnClickListener {
            val nuevoNombre = etNombre.text.toString().trim()
            val nuevaPass = etPassword.text.toString()

            when {
                nuevoNombre.isEmpty() ->
                    etNombre.error = "Ingresa tu nombre"
                nuevaPass.isNotEmpty() && nuevaPass.length < 8 ->
                    etPassword.error = "Mínimo 8 caracteres"
                !Conexion.hayInternet(this) ->
                    toast("Necesitas internet para guardar los cambios")
                else -> {
                    btnGuardar.isEnabled = false
                    lifecycleScope.launch {
                        try {
                            auth.actualizarPerfil(
                                nuevoNombre,
                                if (nuevaPass.isNotEmpty()) nuevaPass else null
                            )
                            sesion.actualizarNombre(nuevoNombre)
                            CacheLocal(this@perfil).guardarNombre(nuevoNombre)
                            tvNombre.text = nuevoNombre
                            tvIniciales.text = iniciales(nuevoNombre)
                            etPassword.text.clear()
                            toast("Cambios guardados")
                        } catch (e: Exception) {
                            Log.e("PERFIL", "No se pudo guardar", e)
                            toast(mensajeErrorAuth(e))
                        } finally {
                            btnGuardar.isEnabled = true
                        }
                    }
                }
            }
        }

        findViewById<View>(R.id.filaVehiculos).setOnClickListener { abrirVehiculos() }
        findViewById<View>(R.id.filaHistorial).setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }
        findViewById<View>(R.id.filaChequeo).setOnClickListener {
            startActivity(Intent(this, ChequeoGuiadoActivity::class.java))
        }
        findViewById<View>(R.id.filaConfiguracion).setOnClickListener {
            startActivity(Intent(this, ConfiguracionActivity::class.java))
        }
        findViewById<View>(R.id.filaNotificaciones).setOnClickListener {
            startActivity(Intent(this, NotificacionesActivity::class.java))
        }
        findViewById<View>(R.id.filaCerrarSesion).setOnClickListener { cerrarSesion() }

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.selectedItemId = R.id.nav_perfil
        nav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_inicio -> { finish(); true }
                R.id.nav_chat -> { toast("Chat"); true }
                R.id.nav_vehiculos -> {
                    abrirVehiculos()
                    false // así "Perfil" sigue marcado al volver
                }
                R.id.nav_perfil -> true
                else -> false
            }
        }
    }

    // "Mauricio Robles" -> "MR", "Mauricio" -> "M"
    private fun iniciales(nombre: String): String =
        nombre.trim().split(" ").filter { it.isNotEmpty() }
            .take(2).joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }

    private fun abrirVehiculos() {
        startActivity(Intent(this, VehiculoActivity::class.java))
    }

    // Pide confirmación escrita antes de borrar la cuenta de forma permanente
    private fun confirmarEliminarCuenta() {
        val campo = EditText(this).apply {
            hint = "Escribe ELIMINAR"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        val margen = (20 * resources.displayMetrics.density).toInt()
        val contenedor = FrameLayout(this).apply {
            setPadding(margen, margen / 2, margen, 0)
            addView(campo)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar mi cuenta")
            .setMessage(
                "Se borrarán de forma permanente tu cuenta, tus vehículos, tus consultas, " +
                        "tus fotos y tus fichas guardadas. Esta acción no se puede deshacer.\n\n" +
                        "Para confirmar, escribe ELIMINAR."
            )
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                if (campo.text.toString().trim() == "ELIMINAR") eliminarCuenta()
                else toast("No se eliminó: debes escribir ELIMINAR")
            }
            .show()
    }

    // Borra la cuenta en el servidor y todos los datos guardados en el teléfono
    private fun eliminarCuenta() {
        if (!Conexion.hayInternet(this)) {
            toast("Necesitas internet para eliminar tu cuenta")
            return
        }
        lifecycleScope.launch {
            try {
                auth.eliminarCuenta()
                CacheLocal(this@perfil).borrar()
                NotificacionLocal(this@perfil).borrar()
                SesionLocal(this@perfil).borrar()
                toast("Tu cuenta fue eliminada")
                val intent = Intent(this@perfil, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("PERFIL", "No se pudo eliminar la cuenta", e)
                toast("No se pudo eliminar la cuenta. Intenta de nuevo.")
            }
        }
    }

    // Cierra la sesión en Supabase y vuelve a la pantalla de bienvenida
    // Cierra la sesión: es lo ÚNICO que borra la llave y los datos del teléfono
    private fun cerrarSesion() {
        lifecycleScope.launch {
            CacheLocal(this@perfil).borrar()                       // datos guardados
            NotificacionLocal(this@perfil).borrar()                // notificaciones
            com.example.proyectofinal.data.repositorio.SesionLocal(this@perfil).borrar()   // llave
            try {
                auth.cerrarSesion()
            } catch (e: Exception) {
                Log.e("PERFIL", "Error al cerrar sesión", e)
            }
            val intent = Intent(this@perfil, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}