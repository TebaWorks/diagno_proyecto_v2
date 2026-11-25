package com.example.proyectofinal.data.repositorio

import android.content.Context
import android.util.Log
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class FichaRemotaDto(
    val id: String,
    val titulo: String,
    val contenido: String,
    @SerialName("palabras_clave") val palabrasClave: List<String> = emptyList(),
    val categoria: String? = null,
    val urgencia: String = "media",
    val origen: String = "consulta",
    @SerialName("etiqueta_imagen") val etiquetaImagen: String? = null,
    val preguntas: List<String> = emptyList(),
    @SerialName("respuesta_novato") val respuestaNovato: String? = null,
    @SerialName("respuesta_intermedio") val respuestaIntermedio: String? = null,
    @SerialName("respuesta_avanzado") val respuestaAvanzado: String? = null,
    val pasos: List<String> = emptyList(),
    val recomendaciones: List<String> = emptyList()
)

// Descarga las fichas al teléfono. RLS entrega solo las del usuario, las base y las comunitarias.
object SincronizacionOffline {

    suspend fun sincronizar(context: Context): Int {
        val remotas = supabase.from("conocimiento_offline")
            .select { limit(2000) }
            .decodeList<FichaRemotaDto>()

        ConocimientoLocal(context).reemplazarTodo(
            remotas.map {
                FichaLocal(
                    id = it.id, titulo = it.titulo, contenido = it.contenido,
                    palabras = it.palabrasClave, categoria = it.categoria ?: "",
                    urgencia = it.urgencia, origen = it.origen, etiquetaImagen = it.etiquetaImagen,
                    preguntas = it.preguntas,
                    respuestaNovato = it.respuestaNovato,
                    respuestaIntermedio = it.respuestaIntermedio,
                    respuestaAvanzado = it.respuestaAvanzado,
                    pasos = it.pasos, recomendaciones = it.recomendaciones
                )
            }
        )
        Log.i("OFFLINE", "${remotas.size} fichas sincronizadas")
        return remotas.size
    }
}