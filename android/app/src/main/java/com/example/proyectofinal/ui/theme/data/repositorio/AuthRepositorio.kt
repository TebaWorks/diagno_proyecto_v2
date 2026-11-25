package com.example.proyectofinal.data.repositorio

import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

@Serializable
private data class PerfilNombreDto(
    @SerialName("nombre_completo") val nombreCompleto: String
)

@Serializable
private data class PerfilNivelDto(
    @SerialName("nivel_conocimiento") val nivelConocimiento: String? = null
)

@Serializable
private data class PerfilCompartirDto(
    @SerialName("acepta_compartir") val aceptaCompartir: Boolean = false
)

@Serializable
private data class PerfilNivelElegidoDto(
    @SerialName("nivel_elegido_en") val nivelElegidoEn: String? = null
)

@Serializable
private data class PerfilConsentimientoDto(
    @SerialName("acepta_privacidad") val aceptaPrivacidad: Boolean = false,
    @SerialName("version_politica") val versionPolitica: String? = null
)

class AuthRepositorio {

    // Versión de la política de privacidad vigente
    val VERSION_POLITICA = "1.1"

    // ===== Registro y verificación del correo =====

    // Crea la cuenta. El perfil y el consentimiento los crea la base de datos (trigger).
    // Devuelve true si la cuenta quedó pendiente de verificar el correo (Confirm email activo).
    suspend fun registrar(
        nombre: String,
        correo: String,
        clave: String,
        aceptaCompartir: Boolean = false
    ): Boolean {
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
        return supabase.auth.currentUserOrNull() == null
    }

    // Verifica el código que llegó al correo; si es correcto, deja la sesión iniciada
    suspend fun verificarCodigoRegistro(correo: String, codigo: String) {
        try {
            supabase.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = correo, token = codigo)
        } catch (e: Exception) {
            // Algunas versiones del servidor esperan el tipo "signup" para la confirmación
            supabase.auth.verifyEmailOtp(type = OtpType.Email.SIGNUP, email = correo, token = codigo)
        }
    }

    // Envía un código nuevo de confirmación al correo
    suspend fun reenviarCodigoRegistro(correo: String) {
        supabase.auth.resendEmail(OtpType.Email.SIGNUP, correo)
    }

    // ===== Recuperación de contraseña =====

    // Envía el código de recuperación (no revela si el correo existe)
    suspend fun enviarCodigoRecuperacion(correo: String) {
        supabase.auth.resetPasswordForEmail(correo)
    }

    // Verifica el código: si es correcto, abre una sesión temporal solo para cambiar la clave
    suspend fun verificarCodigoRecuperacion(correo: String, codigo: String) {
        supabase.auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = correo, token = codigo)
    }

    // Guarda la contraseña nueva y cierra la sesión temporal: el usuario debe entrar de nuevo
    suspend fun cambiarClaveRecuperada(nuevaClave: String) {
        supabase.auth.updateUser { password = nuevaClave }
        runCatching { supabase.auth.signOut() }
    }

    // ===== Inicio y cierre de sesión =====

    suspend fun iniciarSesion(correo: String, clave: String) {
        supabase.auth.signInWith(Email) {
            email = correo
            password = clave
        }
    }

    // Inicia sesión con el token de Google; Supabase crea la cuenta si es la primera vez
    suspend fun iniciarSesionConGoogle(idToken: String, nonce: String) {
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
            this.nonce = nonce
        }
    }

    suspend fun cerrarSesion() {
        try {
            supabase.auth.signOut()

        } catch (e: CancellationException) {

            // Aunque se cancele la petición, limpiar la sesión local.
            withContext(NonCancellable) {
                try {
                    supabase.auth.clearSession()
                } catch (errorLimpieza: Exception) {
                    e.addSuppressed(errorLimpieza)
                }
            }

            throw e

        } catch (e: Exception) {

            // Si falla la conexión, no mantener la sesión
            // autenticada dentro del dispositivo.
            try {
                supabase.auth.clearSession()
            } catch (errorLimpieza: Exception) {
                e.addSuppressed(errorLimpieza)
            }

            throw e
        }
    }

    suspend fun haySesion(): Boolean {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentUserOrNull() != null
    }

    fun correoActual(): String? = supabase.auth.currentUserOrNull()?.email

    // ===== Perfil =====

    suspend fun nombreUsuario(): String {
        val id = supabase.auth.currentUserOrNull()?.id ?: return "usuario"
        return supabase.from("perfil").select { filter { eq("id", id) } }
            .decodeSingleOrNull<PerfilNombreDto>()?.nombreCompleto ?: "usuario"
    }

    suspend fun nivelActual(): String? {
        val id = supabase.auth.currentUserOrNull()?.id ?: return null
        return supabase.from("perfil").select { filter { eq("id", id) } }
            .decodeSingleOrNull<PerfilNivelDto>()?.nivelConocimiento
    }

    // Guarda el nivel ("novato", "con_conocimiento" o "avanzado") y marca que ya lo eligió
    suspend fun guardarNivel(nivel: String) {
        val id = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No hay sesión iniciada")
        supabase.from("perfil").update({
            set("nivel_conocimiento", nivel)
            set("nivel_elegido_en", java.time.Instant.now().toString())
        }) {
            filter { eq("id", id) }
        }
    }

    // true si el usuario todavía no elige su nivel (primer ingreso)
    suspend fun nivelPendiente(): Boolean {
        val id = supabase.auth.currentUserOrNull()?.id ?: return false
        return supabase.from("perfil").select { filter { eq("id", id) } }
            .decodeSingleOrNull<PerfilNivelElegidoDto>()?.nivelElegidoEn == null
    }

    // Actualiza el nombre y, si se indica, la contraseña
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

    // ===== Consentimiento (Ley 21.719) =====

    // true si no ha aceptado la política vigente (cuentas de Google o anteriores a la política)
    suspend fun consentimientoPendiente(): Boolean {
        val id = supabase.auth.currentUserOrNull()?.id ?: return false
        val p = supabase.from("perfil").select { filter { eq("id", id) } }
            .decodeSingleOrNull<PerfilConsentimientoDto>() ?: return true
        return !p.aceptaPrivacidad || p.versionPolitica != VERSION_POLITICA
    }

    // Registra el consentimiento con su fecha y versión
    suspend fun registrarConsentimiento(aceptaCompartir: Boolean) {
        val id = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No hay sesión iniciada")
        supabase.from("perfil").update({
            set("acepta_privacidad", true)
            set("acepta_compartir", aceptaCompartir)
            set("version_politica", VERSION_POLITICA)
            set("consentimiento_en", java.time.Instant.now().toString())
        }) {
            filter { eq("id", id) }
        }
    }

    // true si el usuario aceptó compartir soluciones anonimizadas
    suspend fun aceptaCompartir(): Boolean {
        val id = supabase.auth.currentUserOrNull()?.id ?: return false
        return supabase.from("perfil").select { filter { eq("id", id) } }
            .decodeSingleOrNull<PerfilCompartirDto>()?.aceptaCompartir ?: false
    }

    // Cambia el consentimiento opcional (derecho de oposición)
    suspend fun actualizarCompartir(valor: Boolean) {
        val id = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No hay sesión iniciada")
        supabase.from("perfil").update({ set("acepta_compartir", valor) }) {
            filter { eq("id", id) }
        }
    }

    // Elimina la cuenta en el servidor (derecho de supresión) y cierra la sesión local
    suspend fun eliminarCuenta() {
        supabase.functions.invoke(
            function = "eliminar-cuenta",
            body = buildJsonObject { put("confirmacion", "ELIMINAR") }
        )
        runCatching { supabase.auth.signOut() }
        runCatching { supabase.auth.clearSession() }
    }

    // ===== Llave de sesión =====

    // Crea o actualiza la llave local con los datos del usuario conectado
    suspend fun crearLlave(sesion: SesionLocal) {
        val id = supabase.auth.currentUserOrNull()?.id ?: return
        val nombre = runCatching { nombreUsuario() }.getOrDefault("usuario")
        val nivel = runCatching { nivelActual() }.getOrNull()
        sesion.guardar(id, nombre, nivel)
    }

    // Estado del acceso, tolerante a la falta de internet
    @Suppress("UNUSED_PARAMETER")
    suspend fun estadoSesion(
        sesion: SesionLocal,
        hayInternet: Boolean
    ): EstadoSesion {

        val estadoAuth = try {
            supabase.auth.awaitInitialization()

            supabase.auth.sessionStatus.first {
                it !is SessionStatus.Initializing
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        val tieneLlave = sesion.tieneLlave()

        return when (estadoAuth) {

            is SessionStatus.Authenticated -> {
                EstadoSesion.ACTIVA
            }

            is SessionStatus.RefreshFailure -> {
                if (tieneLlave) EstadoSesion.SIN_CONEXION
                else EstadoSesion.NINGUNA
            }

            is SessionStatus.NotAuthenticated -> {
                when {
                    estadoAuth.isSignOut && tieneLlave ->
                        EstadoSesion.REVOCADA

                    tieneLlave ->
                        EstadoSesion.SIN_CONEXION

                    else ->
                        EstadoSesion.NINGUNA
                }
            }

            else -> {
                if (tieneLlave) EstadoSesion.SIN_CONEXION
                else EstadoSesion.NINGUNA
            }
        }
    }
}

// ACTIVA: sesión válida en Supabase.
// SIN_CONEXION: hay llave local, pero no hay internet para confirmar; se deja usar la app.
// REVOCADA: hay internet y Supabase ya no reconoce la sesión; hay que volver a entrar.
// NINGUNA: nunca inició sesión en este teléfono.
enum class EstadoSesion { ACTIVA, SIN_CONEXION, REVOCADA, NINGUNA }