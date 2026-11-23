package com.example.proyectofinal.ui.theme

import android.content.Intent
import androidx.activity.ComponentActivity
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.SesionLocal

// Después de iniciar sesión, registrarse o verificar el correo:
// guarda la llave y decide si toca elegir nivel (solo la primera vez) o ir al inicio.
suspend fun ComponentActivity.continuarTrasIngreso() {
    val auth = AuthRepositorio()
    val sesion = SesionLocal(this)
    auth.crearLlave(sesion)

    val pendiente = runCatching { auth.nivelPendiente() }.getOrDefault(false)
    if (!pendiente) sesion.confirmarNivel()

    val destino = if (pendiente) NivelActivity::class.java else MainActivity::class.java
    val intent = Intent(this, destino)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
}