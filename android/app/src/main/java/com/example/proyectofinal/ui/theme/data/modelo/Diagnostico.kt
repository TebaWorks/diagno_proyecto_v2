package com.example.proyectofinal.data.modelo

import kotlinx.serialization.Serializable

// Un paso de la guía (ej. "Revisar bujías")
@Serializable
data class PasoGuia(
    val titulo: String = "",                       // "Revisar bujías"
    val detalle: String = "",                      // por qué se revisa (1 frase)
    val imagen: String = "",                       // clave de la imagen: "bujias", "bateria", ...
    val acciones: List<String> = emptyList()       // sub-pasos numerados (2 a 4)
)

// Resultado del análisis que devuelve la IA a partir de la consulta (y la foto)
@Serializable
data class Diagnostico(
    val titulo: String = "",                          // "Posible falla en bujías o sistema de encendido"
    val confianza: Int = 0,                           // 0 a 100
    val descripcion: String = "",
    val recomendaciones: List<String> = emptyList(),
    val pasos: List<PasoGuia> = emptyList()
)