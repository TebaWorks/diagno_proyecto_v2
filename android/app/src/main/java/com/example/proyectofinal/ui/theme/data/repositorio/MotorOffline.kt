package com.example.proyectofinal.data.repositorio

import java.text.Normalizer

data class ResultadoBusqueda(val ficha: FichaLocal, val puntaje: Int)

// Buscador por palabras clave para el modo sin conexión
object MotorOffline {

    // Palabras que no aportan a la búsqueda
    private val vacias = setOf(
        "el", "la", "los", "las", "de", "del", "mi", "mis", "que", "se", "un", "una", "unos", "unas",
        "y", "o", "me", "es", "en", "al", "lo", "por", "con", "para", "como", "cual", "cuando", "donde",
        "esta", "este", "hay", "tengo", "tiene", "debo", "puedo", "hago", "hacer", "auto", "vehiculo",
        "carro", "coche", "porque", "pasa", "muy", "mas", "ya", "si", "le", "su", "sus", "algo", "hoy"
    )

    // Sinónimos frecuentes: se agregan a la búsqueda (no reemplazan la palabra original)
    private val sinonimos = mapOf(
        "llanta" to "neumatico", "rueda" to "neumatico", "goma" to "neumatico", "pinchazo" to "neumatico",
        "pinchada" to "neumatico", "desinflada" to "neumatico", "piloto" to "testigo", "icono" to "testigo",
        "simbolo" to "testigo", "luz" to "testigo", "lucecita" to "testigo", "lubricante" to "aceite",
        "agua" to "refrigerante", "anticongelante" to "refrigerante", "pastilla" to "freno",
        "balata" to "freno", "frenar" to "freno", "frena" to "freno", "acumulador" to "bateria",
        "arranca" to "bateria", "parte" to "bateria", "prende" to "bateria", "enciende" to "bateria",
        "pana" to "falla", "quedo" to "falla", "muerto" to "bateria", "calienta" to "temperatura",
        "recalienta" to "temperatura", "caliente" to "temperatura", "hierve" to "temperatura",
        "foco" to "luces", "ampolleta" to "luces", "limpiaparabrisas" to "plumilla",
        "bencina" to "combustible", "nafta" to "combustible", "petroleo" to "combustible",
        "ruido" to "ruido", "chirria" to "ruido", "suena" to "ruido", "golpeteo" to "ruido",
        "humea" to "humo", "olor" to "olor", "vibra" to "vibracion", "tiembla" to "vibracion"
    )

    // Frases que indican riesgo: la respuesta siempre empieza con un aviso de seguridad
    private val peligro = listOf(
        "humo", "fuego", "no frena", "sin frenos", "luz roja", "temperatura", "recalienta",
        "presion de aceite", "olor a quemado", "se apago andando"
    )

    const val AVISO_SEGURIDAD =
        "⚠️ Si es una situación de riesgo, detén el vehículo en un lugar seguro y pide asistencia."

    fun normalizar(texto: String): String =
        Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    // Forma simple para comparar singular y plural (neumaticos → neumatico)
    private fun raiz(p: String): String = when {
        p.length > 4 && p.endsWith("es") -> p.dropLast(2)
        p.length > 3 && p.endsWith("s") -> p.dropLast(1)
        else -> p
    }

    private fun palabras(texto: String): Set<String> =
        normalizar(texto).split(" ")
            .filter { it.length >= 3 && it !in vacias }
            .map { raiz(it) }
            .toSet()

    // Palabras de la pregunta más sus sinónimos
    private fun palabrasConsulta(pregunta: String): Set<String> {
        val base = palabras(pregunta)
        return base + base.mapNotNull { sinonimos[it]?.let(::raiz) }
    }

    fun esPeligro(pregunta: String): Boolean {
        val t = normalizar(pregunta)
        return peligro.any { t.contains(it) }
    }

    // Hasta 3 fichas por relevancia: palabras clave (+3), preguntas similares (+2), título (+2)
    fun buscar(pregunta: String, fichas: List<FichaLocal>): List<ResultadoBusqueda> {
        val consulta = palabrasConsulta(pregunta)
        if (consulta.isEmpty()) return emptyList()

        return fichas.map { f ->
            val claves = f.palabras.map { raiz(it) }.toSet()
            val titulo = palabras(f.titulo)
            val preguntas = palabras(f.preguntas.joinToString(" "))
            var puntaje = 0
            for (p in consulta) {
                puntaje += when {
                    p in claves -> 3
                    p in preguntas -> 2
                    p in titulo -> 2
                    else -> 0
                }
            }
            ResultadoBusqueda(f, puntaje)
        }
            .filter { it.puntaje >= 3 }
            .sortedWith(
                compareByDescending<ResultadoBusqueda> { it.puntaje }
                    .thenByDescending { it.ficha.urgencia == "alta" }
                    .thenByDescending { it.ficha.preguntas.isNotEmpty() }
            )
            .take(3)
    }

    private val introducciones = listOf(
        "Esto es lo que tengo guardado sobre tu consulta:",
        "Según la información guardada en tu teléfono:",
        "Te puedo orientar con esto:",
        "Encontré esto en tu información guardada:"
    )

    private val sinInformacion = listOf(
        "No tengo información guardada sobre esto. Cuando tengas internet, consulta al asistente en línea y la respuesta quedará disponible sin conexión.",
        "Todavía no tengo datos sobre eso en tu teléfono. Con internet puedo responderte y guardarlo para la próxima vez.",
        "Eso no está en la información guardada. Prueba con otras palabras o consulta en línea cuando tengas señal.",
        "No encontré nada sobre eso sin conexión. Si es urgente y hay riesgo, detén el vehículo y pide asistencia."
    )

    fun respuestaSinInformacion(): String = sinInformacion.random()

    // Respuesta ordenada: título, respuesta según nivel, pasos, recomendaciones y otras sugerencias
    fun respuestaClasica(resultados: List<ResultadoBusqueda>, alerta: Boolean, nivel: String): String {
        val f = resultados.first().ficha
        val t = StringBuilder()
        if (alerta) t.append(AVISO_SEGURIDAD).append("\n\n")
        t.append(introducciones.random()).append("\n\n")
        t.append("📌 ").append(f.titulo).append("\n").append(f.respuestaPara(nivel))
        if (f.pasos.isNotEmpty()) {
            t.append("\n\nPasos:")
            f.pasos.forEachIndexed { i, p -> t.append("\n").append(i + 1).append(". ").append(p) }
        }
        if (f.recomendaciones.isNotEmpty()) {
            t.append("\n\nRecomendaciones:")
            f.recomendaciones.forEach { t.append("\n• ").append(it) }
        }
        val otras = resultados.drop(1).map { it.ficha.titulo }
        if (otras.isNotEmpty()) t.append("\n\nTambién puede servirte: ").append(otras.joinToString(" · "))
        return t.toString()
    }
}