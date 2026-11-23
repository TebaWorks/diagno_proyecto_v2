package com.example.proyectofinal.data.repositorio

import java.io.IOException

// Traduce los errores de Supabase a mensajes claros para el usuario
fun mensajeErrorAuth(e: Throwable): String {
    val texto = (e.message ?: "").lowercase()
    val esRed = e is IOException ||
            e::class.simpleName?.contains("HttpRequest") == true ||
            "unable to resolve host" in texto ||
            "failed to connect" in texto ||
            "timeout" in texto

    return when {
        esRed -> "Sin conexión a internet. Revisa tu red e intenta de nuevo."
        esCorreoNoConfirmado(e) -> "Debes verificar tu correo. Te enviamos un código."
        "token has expired" in texto || "invalid otp" in texto || "otp" in texto && "invalid" in texto ->
            "El código es incorrecto o ya venció. Pide uno nuevo."
        "invalid login credentials" in texto -> "Correo o contraseña incorrectos."
        "already registered" in texto || "already been registered" in texto ->
            "Ese correo ya tiene una cuenta. Inicia sesión."
        "password" in texto && ("least" in texto || "weak" in texto || "short" in texto || "should contain" in texto) ->
            "La contraseña debe tener al menos 8 caracteres, con letras y números."
        "rate limit" in texto || "too many" in texto || "security purposes" in texto ->
            "Demasiados intentos. Espera un minuto e intenta de nuevo."
        "email" in texto && "invalid" in texto -> "El correo no es válido."
        else -> "Ocurrió un problema. Intenta nuevamente."
    }
}

// true si el error es porque el correo todavía no se verifica
fun esCorreoNoConfirmado(e: Throwable): Boolean =
    "email not confirmed" in (e.message ?: "").lowercase()