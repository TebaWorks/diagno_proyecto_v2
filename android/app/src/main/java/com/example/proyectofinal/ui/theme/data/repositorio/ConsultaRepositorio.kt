package com.example.proyectofinal.data.repositorio

import com.example.proyectofinal.data.modelo.Consulta
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

// Datos que se envían al crear una consulta (sin id ni fecha: los genera Supabase)
@Serializable
private data class ConsultaNueva(
    val usuario_id: String,
    val titulo: String,
    val resumen: String?,
    val vehiculo: String?,
    val mensajes: List<MensajeApi>
)

@Serializable
private data class ConsultaMensajes(val mensajes: List<MensajeApi>)

@Serializable
private data class ConsultaId(val id: String)

class ConsultaRepositorio {

    // Guarda una consulta nueva y devuelve su id
    suspend fun crear(
        titulo: String,
        resumen: String?,
        vehiculo: String?,
        mensajes: List<MensajeApi>
    ): String {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No hay sesión iniciada")
        return supabase.from("consulta")
            .insert(ConsultaNueva(uid, titulo, resumen, vehiculo, mensajes)) { select() }
            .decodeSingle<ConsultaId>().id
    }

    // Actualiza la conversación cuando se siguen haciendo preguntas
    suspend fun actualizar(id: String, mensajes: List<MensajeApi>) {
        supabase.from("consulta")
            .update(ConsultaMensajes(mensajes)) { filter { eq("id", id) } }
    }

    // Solo las consultas del usuario con sesión iniciada, las más nuevas primero
    suspend fun obtener(): List<Consulta> {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        return supabase.from("consulta")
            .select { filter { eq("usuario_id", uid) } }
            .decodeList<Consulta>()
            .sortedByDescending { it.creado_en }
    }

    suspend fun eliminar(id: String) {
        supabase.from("consulta").delete { filter { eq("id", id) } }
    }
}