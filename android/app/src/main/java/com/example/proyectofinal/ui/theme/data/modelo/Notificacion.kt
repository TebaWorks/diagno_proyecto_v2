package com.example.proyectofinal.data.modelo

import kotlinx.serialization.Serializable

// Un aviso dentro de la app (se guarda en el teléfono)
@Serializable
data class Notificacion(
    val id: String,
    val tipo: String,          // "mantencion", "consulta" o "consejo"
    val titulo: String,
    val detalle: String = "",
    val fecha: Long,           // milisegundos
    val leida: Boolean = false
)