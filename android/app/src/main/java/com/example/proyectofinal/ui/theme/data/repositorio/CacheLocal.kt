package com.example.proyectofinal.data.repositorio

import android.content.Context
import com.example.proyectofinal.data.modelo.Vehiculo
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// Guarda en el teléfono el nombre y los vehículos para verlos sin internet.
// Solo guarda si el interruptor "Modo sin conexión" de Perfil está activo.
class CacheLocal(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ajustes", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializador = ListSerializer(Vehiculo.serializer())

    private val activo: Boolean
        get() = prefs.getBoolean("modo_sin_conexion", true)

    fun guardarNombre(nombre: String) {
        if (activo) prefs.edit().putString("cache_nombre", nombre).apply()
    }

    fun nombre(): String? = prefs.getString("cache_nombre", null)

    fun guardarVehiculos(lista: List<Vehiculo>) {
        if (!activo) return
        prefs.edit()
            .putString("cache_vehiculos", json.encodeToString(serializador, lista))
            .apply()
    }

    fun vehiculos(): List<Vehiculo> {
        val texto = prefs.getString("cache_vehiculos", null) ?: return emptyList()
        return try {
            json.decodeFromString(serializador, texto)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Se usa al cerrar sesión o al apagar el interruptor
    fun borrar() {
        prefs.edit().remove("cache_nombre").remove("cache_vehiculos").apply()
    }
}