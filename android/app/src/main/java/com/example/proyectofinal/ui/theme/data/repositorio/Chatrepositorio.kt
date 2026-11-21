package com.example.proyectofinal.data.repositorio

import com.example.proyectofinal.data.modelo.Diagnostico
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// role = "user" o "assistant" (lo usa la pantalla y el historial local)
@Serializable
data class MensajeApi(val role: String, val content: String)

// Lo que se envía a la función "chat" de Supabase
@Serializable
private data class ChatPeticion(
    val mensaje: String,
    val conversacion_id: String? = null,
    val imagen_base64: String? = null,
    val imagen_mime: String? = null,
    val tipo: String? = null
)

@Serializable
data class FuenteWeb(val titulo: String = "", val url: String = "")

// Lo que responde la función "chat"
@Serializable
private data class ChatRespuesta(
    val conversacion_id: String? = null,
    val respuesta: String? = null,
    val derivar_taller: Boolean = false,
    val uso_web: Boolean = false,
    val fuentes: List<FuenteWeb> = emptyList(),
    val analisis: Diagnostico? = null,
    val error: String? = null
)

class ChatRepositorio {

    private val json = Json { ignoreUnknownKeys = true }

    // El servidor guarda la conversación; aquí solo se recuerda su id
    private var conversacionId: String? = null

    // Datos extra de la última respuesta (por si la pantalla quiere mostrarlos)
    var ultimaDerivacion: Boolean = false
        private set
    var ultimasFuentes: List<FuenteWeb> = emptyList()
        private set

    private suspend fun llamar(peticion: ChatPeticion): ChatRespuesta {
        val r = supabase.functions.invoke(
            function = "chat",
            body = peticion,
            headers = Headers.build { append(HttpHeaders.ContentType, "application/json") }
        )
        val datos = json.decodeFromString<ChatRespuesta>(r.bodyAsText())
        datos.error?.let { throw IllegalStateException(it) }
        datos.conversacion_id?.let { conversacionId = it }
        return datos
    }

    // Envía solo el último mensaje del usuario; el servidor ya tiene el historial
    suspend fun preguntar(
        mensajes: List<MensajeApi>,
        nivel: String?,
        vehiculo: String?,
        imagen: String? = null
    ): String {
        val ultimo = mensajes.lastOrNull { it.role == "user" }?.content
            ?.removePrefix("[Foto adjunta] ")
            ?: throw IllegalArgumentException("No hay mensaje para enviar")

        val d = llamar(
            ChatPeticion(
                mensaje = ultimo,
                conversacion_id = conversacionId,
                imagen_base64 = imagen,
                imagen_mime = imagen?.let { "image/jpeg" }
            )
        )
        ultimaDerivacion = d.derivar_taller
        ultimasFuentes = d.fuentes
        return d.respuesta ?: throw IllegalStateException("Respuesta vacía")
    }

    // Pide el resultado del análisis de la conversación actual
    suspend fun diagnosticar(
        mensajes: List<MensajeApi>,
        nivel: String?,
        vehiculo: String?,
        imagen: String? = null
    ): Diagnostico {
        val id = conversacionId
            ?: throw IllegalStateException("Primero conversa con el asistente")
        val d = llamar(
            ChatPeticion(
                mensaje = "analisis",
                conversacion_id = id,
                imagen_base64 = imagen,
                imagen_mime = imagen?.let { "image/jpeg" },
                tipo = "analisis"
            )
        )
        val a = d.analisis ?: throw IllegalStateException("La IA no devolvió un análisis")
        return a.copy(confianza = a.confianza.coerceIn(0, 100))
    }
}