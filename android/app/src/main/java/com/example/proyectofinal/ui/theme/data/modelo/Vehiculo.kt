package com.example.proyectofinal.data.modelo

import kotlinx.serialization.Serializable

@Serializable
data class Vehiculo(
    val id: String? = null,
    val usuario_id: String? = null,
    val tipo: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val anio: Int? = null,
    val combustible: String? = null,
    val kilometraje: Int? = null
)