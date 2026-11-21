package com.example.proyectofinal.data.repositorio

import com.example.proyectofinal.data.modelo.Vehiculo
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

// Datos que se envían al crear un vehículo (sin id: lo genera Supabase)
@Serializable
private data class VehiculoNuevo(
    val usuario_id: String,
    val tipo: String,
    val marca: String,
    val modelo: String,
    val anio: Int,
    val combustible: String,
    val kilometraje: Int
)

class VehiculoRepositorio {

    // Solo los vehículos del usuario con sesión iniciada
    suspend fun obtenerVehiculos(): List<Vehiculo> {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        return supabase.from("vehiculo")
            .select { filter { eq("usuario_id", uid) } }
            .decodeList<Vehiculo>()
    }

    suspend fun agregar(
        tipo: String,
        marca: String,
        modelo: String,
        anio: Int,
        combustible: String,
        kilometraje: Int
    ) {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No hay sesión iniciada")
        supabase.from("vehiculo").insert(
            VehiculoNuevo(uid, tipo.lowercase(), marca, modelo, anio, combustible, kilometraje)
        )
    }

    suspend fun eliminar(id: String) {
        supabase.from("vehiculo").delete { filter { eq("id", id) } }
    }
}