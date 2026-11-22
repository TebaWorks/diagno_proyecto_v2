package com.example.proyectofinal.data.repositorio

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class PerfilNuevoDto(
    val id: String,
    @SerialName("nombre_completo") val nombreCompleto: String,
    @SerialName("nivel_conocimiento") val nivelConocimiento: String = "novato"
)

@Serializable
private data class PerfilNombreDto(
    @SerialName("nombre_completo") val nombreCompleto: String
)

@Serializable
private data class PerfilNivelDto(
    @SerialName("nivel_conocimiento") val nivelConocimiento: String? = null
)

class AuthRepositorio {

    // Versión de la política de privacidad que acepta el usuario al registrarse
    val VERSION_POLITICA = "1.0"

    // Crea la cuenta. El perfil y el consentimiento los crea la base de datos (trigger)
    // en la misma operación, así nunca queda una cuenta sin perfil.
    suspend fun registrar(
        nombre: String,
        correo: String,
        clave: String,
        aceptaCompartir: Boolean = false
    ) {
        supabase.auth.signUpWith(Email) {
            email = correo
            password = clave
            data = buildJsonObject {
                put("nombre", nombre)
                put("acepta_privacidad", true)        // obligatorio para registrarse
                put("acepta_compartir", aceptaCompartir)
                put("version_politica", VERSION_POLITICA)
            }
        }
        if (supabase.auth.currentUserOrNull() == null) {
            throw IllegalStateException("No se pudo iniciar sesión después del registro")
        }
    }

    // Inicia sesión; si la clave es incorrecta, lanza un error
    suspend fun iniciarSesion(correo: String, clave: String) {
        supabase.auth.signInWith(Email) {
            email = correo
            password = clave
        }
    }

    suspend fun cerrarSesion() = supabase.auth.signOut()

    // true si hay un usuario con sesión iniciada
    suspend fun haySesion(): Boolean {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentUserOrNull() != null
    }

    fun correoActual(): String? = supabase.auth.currentUserOrNull()?.email

    suspend fun nombreUsuario(): String {
        val id = supabase.auth.currentUserOrNull()?.id ?: return "usuario"
        return supabase.from("perfil").select { filter { eq("id", id) } }
            .decodeSingleOrNull<PerfilNombreDto>()?.nombreCompleto ?: "usuario"
    }

    // Nivel de conocimiento guardado en la tabla perfil (null si no hay)
    suspend fun nivelActual(): String? {
        val id = supabase.auth.currentUserOrNull()?.id ?: return null
        return supabase.from("perfil").select { filter { eq("id", id) } }
            .decodeSingleOrNull<PerfilNivelDto>()?.nivelConocimiento
    }

    // Guarda el nivel elegido: "novato", "con_conocimiento" o "avanzado"
    suspend fun guardarNivel(nivel: String) {
        val id = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No hay sesión iniciada")
        supabase.from("perfil").update({ set("nivel_conocimiento", nivel) }) {
            filter { eq("id", id) }
        }
    }

    // NUEVO: actualiza el nombre en la tabla perfil y, si se indica, la contraseña
    suspend fun actualizarPerfil(nombre: String, nuevaClave: String?) {
        val id = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No hay sesión iniciada")
        supabase.from("perfil").update({ set("nombre_completo", nombre) }) {
            filter { eq("id", id) }
        }
        if (nuevaClave != null) {
            supabase.auth.updateUser { password = nuevaClave }
        }
    }
    // Crea o actualiza la llave local con los datos del usuario conectado
    suspend fun crearLlave(sesion: SesionLocal) {
        val id = supabase.auth.currentUserOrNull()?.id ?: return
        val nombre = runCatching { nombreUsuario() }.getOrDefault("usuario")
        val nivel = runCatching { nivelActual() }.getOrNull()
        sesion.guardar(id, nombre, nivel)
    }

    // Estado del acceso, tolerante a la falta de internet
    suspend fun estadoSesion(sesion: SesionLocal, hayInternet: Boolean): EstadoSesion {
        val enSupabase = runCatching {
            supabase.auth.awaitInitialization()
            supabase.auth.currentUserOrNull() != null
        }.getOrDefault(false)

        return when {
            enSupabase -> EstadoSesion.ACTIVA
            sesion.tieneLlave() && !hayInternet -> EstadoSesion.SIN_CONEXION
            sesion.tieneLlave() && hayInternet -> EstadoSesion.REVOCADA
            else -> EstadoSesion.NINGUNA
        }
    }
}
// ACTIVA: sesión válida en Supabase.
// SIN_CONEXION: hay llave local, pero no hay internet para confirmar; se deja usar la app.
// REVOCADA: hay internet y Supabase ya no reconoce la sesión; hay que volver a entrar.
// NINGUNA: nunca inició sesión en este teléfono.
enum class EstadoSesion { ACTIVA, SIN_CONEXION, REVOCADA, NINGUNA }