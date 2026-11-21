package com.example.proyectofinal.data.repositorio

import android.content.Context
import com.example.proyectofinal.data.modelo.Notificacion
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

// Guarda las notificaciones y sus preferencias en el teléfono (SharedPreferences "ajustes").
// Para crear un aviso desde cualquier pantalla:
//   NotificacionLocal(this).agregar(NotificacionLocal.CONSULTA, "Consulta guardada", "Ya está en tu historial")
class NotificacionLocal(context: Context) {

    companion object {
        const val MANTENCION = "mantencion"
        const val CONSULTA = "consulta"
        const val CONSEJO = "consejo"
        private const val MAXIMO = 50
    }

    private val prefs = context.applicationContext
        .getSharedPreferences("ajustes", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializador = ListSerializer(Notificacion.serializer())

    // ---- Preferencias (interruptores de la pantalla) ----
    fun permitido(tipo: String): Boolean = prefs.getBoolean("notif_$tipo", true)

    fun definirPermitido(tipo: String, valor: Boolean) {
        prefs.edit().putBoolean("notif_$tipo", valor).apply()
    }

    // ---- Lista ----
    fun lista(): List<Notificacion> {
        val texto = prefs.getString("notificaciones", null) ?: return emptyList()
        return try {
            json.decodeFromString(serializador, texto).sortedByDescending { it.fecha }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun sinLeer(): Int = lista().count { !it.leida }

    // Si el usuario apagó ese tipo de aviso, no se guarda
    fun agregar(tipo: String, titulo: String, detalle: String = "") {
        if (!permitido(tipo)) return
        val nueva = Notificacion(
            id = UUID.randomUUID().toString(),
            tipo = tipo,
            titulo = titulo,
            detalle = detalle,
            fecha = System.currentTimeMillis()
        )
        guardar((listOf(nueva) + lista()).take(MAXIMO))
    }

    fun marcarLeida(id: String) =
        guardar(lista().map { if (it.id == id) it.copy(leida = true) else it })

    fun marcarTodasLeidas() = guardar(lista().map { it.copy(leida = true) })

    fun eliminar(id: String) = guardar(lista().filter { it.id != id })

    // Se usa al cerrar sesión
    fun borrar() {
        prefs.edit().remove("notificaciones").apply()
    }

    private fun guardar(lista: List<Notificacion>) {
        prefs.edit().putString("notificaciones", json.encodeToString(serializador, lista)).apply()
    }
}

