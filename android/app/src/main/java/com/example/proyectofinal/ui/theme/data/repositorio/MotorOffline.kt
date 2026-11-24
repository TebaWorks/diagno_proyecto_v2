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
        "llanta" to "neumatico", "rueda" to "neumatico", "goma" to "neumatico",
        "piloto" to "testigo", "icono" to "testigo", "simbolo" to "testigo", "luz" to "testigo",
        "lubricante" to "aceite", "agua" to "refrigerante", "anticongelante" to "refrigerante",
        "pastilla" to "freno", "balata" to "freno", "frenar" to "freno", "frena" to "freno",
        "acumulador" to "bateria", "arranca" to "bateria", "parte" to "bateria",
        "calienta" to "temperatura", "recalienta" to "temperatura", "caliente" to "temperatura",
        "foco" to "luces", "ampolleta" to "luces", "limpiaparabrisas" to "plumilla"
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

    // Devuelve hasta 3 fichas ordenadas por relevancia (puntaje mínimo: 1 palabra clave)
    fun buscar(pregunta: String, fichas: List<FichaLocal>): List<ResultadoBusqueda> {
        val consulta = palabrasConsulta(pregunta)
        if (consulta.isEmpty()) return emptyList()

        return fichas.map { f ->
            val claves = f.palabras.map { raiz(it) }.toSet()
            val titulo = palabras(f.titulo)
            var puntaje = 0
            for (p in consulta) {
                puntaje += when {
                    p in claves -> 3
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
                    .thenByDescending { it.ficha.origen == "base" }
            )
            .take(3)
    }

    // Respuesta "clásica" armada con las fichas encontradas
    fun respuestaClasica(resultados: List<ResultadoBusqueda>, alerta: Boolean): String {
        val principal = resultados.first().ficha
        val texto = StringBuilder()
        if (alerta) texto.append(AVISO_SEGURIDAD).append("\n\n")
        texto.append(principal.titulo).append("\n").append(principal.contenido)
        val otras = resultados.drop(1).map { it.ficha.titulo }
        if (otras.isNotEmpty()) {
            texto.append("\n\nTambién puede servirte: ").append(otras.joinToString(" · "))
        }
        return texto.toString()
    }
}