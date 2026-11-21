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
        "invalid login credentials" in texto -> "Correo o contraseña incorrectos."
        "already registered" in texto || "already been registered" in texto ->
            "Ese correo ya tiene una cuenta. Inicia sesión."
        "password" in texto && ("least" in texto || "weak" in texto || "short" in texto) ->
            "La contraseña debe tener al menos 8 caracteres."
        "rate limit" in texto || "too many" in texto -> "Demasiados intentos. Espera unos minutos."
        "email" in texto && "invalid" in texto -> "El correo no es válido."
        else -> "Ocurrió un problema. Intenta nuevamente."
    }
}