package com.example.proyectofinal.data.repositorio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class FuenteOffline { GEMMA, FICHAS, SIN_INFORMACION }

data class RespuestaOffline(val texto: String, val fuente: FuenteOffline)

// Asistente sin conexión: busca fichas y responde con Gemma (si está) o con la mejor ficha
object AsistenteOffline {

    suspend fun responder(context: Context, pregunta: String, nivel: String): RespuestaOffline {
        val fichas = withContext(Dispatchers.IO) { ConocimientoLocal(context).listar() }
        val resultados = MotorOffline.buscar(pregunta, fichas)
        val alerta = MotorOffline.esPeligro(pregunta) || resultados.firstOrNull()?.ficha?.urgencia == "alta"

        // Sin fichas relevantes: no se inventa nada
        if (resultados.isEmpty()) {
            val texto = (if (alerta) MotorOffline.AVISO_SEGURIDAD + "\n\n" else "") +
                    "No tengo información guardada sobre esto. Cuando tengas internet, " +
                    "consulta al asistente en línea y la respuesta quedará disponible sin conexión."
            return RespuestaOffline(texto, FuenteOffline.SIN_INFORMACION)
        }

        // Con Gemma instalado: respuesta conversacional anclada a las fichas
        if (GemmaLocal.disponible(context)) {
            try {
                val texto = GemmaLocal.responder(context, pregunta, resultados.map { it.ficha }, nivel)
                if (texto.isNotBlank()) {
                    val conAviso = if (alerta) MotorOffline.AVISO_SEGURIDAD + "\n\n" + texto else texto
                    return RespuestaOffline(conAviso, FuenteOffline.GEMMA)
                }
            } catch (e: Throwable) {
                // Incluye falta de memoria: el modo offline nunca se cae por Gemma
                Log.e("OFFLINE", "Gemma no pudo responder; se usan las fichas", e)
            }
        }

        // Sin Gemma (o si falló): respuesta clásica con fichas
        return RespuestaOffline(MotorOffline.respuestaClasica(resultados, alerta), FuenteOffline.FICHAS)
    }
}