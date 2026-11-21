package com.example.proyectofinal.data.modelo

import com.example.proyectofinal.data.repositorio.MensajeApi
import kotlinx.serialization.Serializable

// Una consulta = una conversación con el asistente de IA (tabla "consulta" de Supabase)
@Serializable
data class Consulta(
    val id: String? = null,
    val usuario_id: String? = null,
    val titulo: String = "",
    val resumen: String? = null,
    val vehiculo: String? = null,
    val mensajes: List<MensajeApi> = emptyList(),
    val creado_en: String? = null
)