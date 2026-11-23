package com.example.proyectofinal.data.repositorio

import android.content.Context

/**
 * Llave de acceso local de Diagno.
 *
 * Una vez iniciada la sesión, la app sigue siendo usable sin internet mientras
 * exista esta llave. Solo se borra cuando el usuario cierra sesión o elimina su cuenta.
 *
 * Minimización de datos (Ley 21.719): solo se guarda el id del usuario, su nombre
 * y su nivel. Nunca la contraseña ni el token; el token lo administra Supabase.
 */
class SesionLocal(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("sesion_diagno", Context.MODE_PRIVATE)

    fun guardar(usuarioId: String, nombre: String, nivel: String?) {
        prefs.edit()
            .putString(CLAVE_ID, usuarioId)
            .putString(CLAVE_NOMBRE, nombre)
            .putString(CLAVE_NIVEL, nivel ?: "novato")
            .apply()
    }

    fun actualizarNombre(nombre: String) {
        if (tieneLlave()) prefs.edit().putString(CLAVE_NOMBRE, nombre).apply()
    }

    fun actualizarNivel(nivel: String) {
        if (tieneLlave()) prefs.edit().putString(CLAVE_NIVEL, nivel).apply()
    }

    fun tieneLlave(): Boolean = prefs.getString(CLAVE_ID, null) != null

    fun usuarioId(): String? = prefs.getString(CLAVE_ID, null)

    fun nombre(): String = prefs.getString(CLAVE_NOMBRE, null) ?: "usuario"

    fun nivel(): String = prefs.getString(CLAVE_NIVEL, null) ?: "novato"

    // true cuando el usuario ya eligió su nivel (para no volver a preguntarle)
    fun confirmarNivel() = prefs.edit().putBoolean("nivel_confirmado", true).apply()

    fun nivelConfirmado(): Boolean = prefs.getBoolean("nivel_confirmado", false)

    // Se llama solo al cerrar sesión o eliminar la cuenta
    fun borrar() = prefs.edit().clear().apply()

    private companion object {
        const val CLAVE_ID = "usuario_id"
        const val CLAVE_NOMBRE = "nombre"
        const val CLAVE_NIVEL = "nivel"
    }
}