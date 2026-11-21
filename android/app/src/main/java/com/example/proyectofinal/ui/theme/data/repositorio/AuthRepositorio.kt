package com.example.proyectofinal.data.repositorio

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

    // Crea la cuenta en Supabase y guarda el nombre en la tabla perfil
    suspend fun registrar(nombre: String, correo: String, clave: String) {
        supabase.auth.signUpWith(Email) {
            email = correo
            password = clave
        }
        val id = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No se pudo iniciar sesión después del registro")
        supabase.from("perfil").insert(PerfilNuevoDto(id, nombre))
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
}