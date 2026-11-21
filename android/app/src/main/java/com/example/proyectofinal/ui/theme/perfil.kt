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
import com.example.proyectofinal.data.repositorio.NotificacionLocal
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

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

        // Cargar datos del usuario desde Supabase (si no hay sesión, vuelve al inicio)
        lifecycleScope.launch {
            val cache = CacheLocal(this@perfil)
            val haySesion = try {
                auth.haySesion()
            } catch (e: Exception) {
                cache.nombre() != null
            }
            if (!haySesion) {
                cerrarSesion()
                return@launch
            }
            val nombre = try {
                auth.nombreUsuario().also { cache.guardarNombre(it) }
            } catch (e: Exception) {
                cache.nombre() ?: "usuario"
            }
            val correo = auth.correoActual() ?: ""
            tvNombre.text = nombre
            tvCorreo.text = correo
            tvIniciales.text = iniciales(nombre)
            etNombre.setText(nombre)
            etCorreo.setText(correo)
        }

        btnGuardar.setOnClickListener {
            val nuevoNombre = etNombre.text.toString().trim()
            val nuevaPass = etPassword.text.toString()

            when {
                nuevoNombre.isEmpty() ->
                    etNombre.error = "Ingresa tu nombre"
                nuevaPass.isNotEmpty() && nuevaPass.length < 8 ->
                    etPassword.error = "Mínimo 8 caracteres"
                else -> {
                    btnGuardar.isEnabled = false
                    lifecycleScope.launch {
                        try {
                            auth.actualizarPerfil(
                                nuevoNombre,
                                if (nuevaPass.isNotEmpty()) nuevaPass else null
                            )
                            tvNombre.text = nuevoNombre
                            tvIniciales.text = iniciales(nuevoNombre)
                            etPassword.text.clear()
                            toast("Cambios guardados")
                        } catch (e: Exception) {
                            Log.e("PERFIL", "No se pudo guardar", e)
                            toast("No se pudo guardar: ${e.message}")
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