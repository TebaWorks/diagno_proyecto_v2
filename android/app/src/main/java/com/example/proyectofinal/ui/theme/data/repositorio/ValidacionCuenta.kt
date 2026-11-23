package com.example.proyectofinal.data.repositorio

// Validaciones del registro, antes de enviar nada a internet
object ValidacionCuenta {

    // Errores de tipeo frecuentes en los dominios más usados en Chile
    private val correcciones = mapOf(
        "gmial.com" to "gmail.com", "gmai.com" to "gmail.com", "gmail.co" to "gmail.com",
        "gmail.cl" to "gmail.com", "gamil.com" to "gmail.com", "gmal.com" to "gmail.com",
        "gnail.com" to "gmail.com", "gmail.con" to "gmail.com", "gmaill.com" to "gmail.com",
        "hotmial.com" to "hotmail.com", "hotmal.com" to "hotmail.com", "hotmail.co" to "hotmail.com",
        "hotmail.con" to "hotmail.com", "hotmai.com" to "hotmail.com",
        "outlok.com" to "outlook.com", "outlook.co" to "outlook.com", "outlook.con" to "outlook.com",
        "yaho.com" to "yahoo.com", "yahoo.co" to "yahoo.com", "yahoo.con" to "yahoo.com",
        "inacap.c" to "inacap.cl", "inacapmail.c" to "inacapmail.cl"
    )

    // Servicios de correo temporal o desechable
    private val desechables = setOf(
        "mailinator.com", "10minutemail.com", "tempmail.com", "temp-mail.org", "guerrillamail.com",
        "yopmail.com", "trashmail.com", "getnada.com", "dispostable.com", "sharklasers.com",
        "maildrop.cc", "fakeinbox.com", "throwawaymail.com", "mohmal.com", "emailondeck.com"
    )

    // Devuelve el correo corregido si el dominio parece mal escrito; si no, null
    fun sugerenciaCorreo(correo: String): String? {
        val partes = correo.split("@")
        if (partes.size != 2) return null
        val sugerido = correcciones[partes[1]] ?: return null
        return "${partes[0]}@$sugerido"
    }

    fun esDesechable(correo: String): Boolean =
        correo.substringAfter("@", "") in desechables

    // Letras (con tildes y ñ), espacios, apóstrofo y guion; de 2 a 50 caracteres
    fun nombreValido(nombre: String): Boolean =
        nombre.length in 2..50 && nombre.matches(Regex("^[\\p{L} '\\-]+$"))

    // 0 = débil, 1 = media, 2 = fuerte
    fun fuerzaClave(clave: String): Int {
        var puntos = 0
        if (clave.length >= 8) puntos++
        if (clave.length >= 12) puntos++
        if (clave.any { it.isUpperCase() } && clave.any { it.isLowerCase() }) puntos++
        if (clave.any { it.isDigit() }) puntos++
        if (clave.any { !it.isLetterOrDigit() }) puntos++
        return when {
            puntos >= 4 -> 2
            puntos >= 3 -> 1
            else -> 0
        }
    }
}