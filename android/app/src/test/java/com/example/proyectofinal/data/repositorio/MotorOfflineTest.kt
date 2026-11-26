package com.example.proyectofinal.data.repositorio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorOfflineTest {

    private fun ficha(id: String, titulo: String, palabras: List<String>, urgencia: String = "media") =
        FichaLocal(
            id = id, titulo = titulo, contenido = "Contenido de prueba",
            palabras = palabras, categoria = "general", urgencia = urgencia,
            origen = "base", etiquetaImagen = null
        )

    private val fichas = listOf(
        ficha("1", "Especificaciones: neumaticos", listOf("neumatico", "presion", "medida")),
        ficha("2", "Aceite de motor y filtro", listOf("aceite", "filtro", "mantencion"))
    )

    @Test
    fun normalizaTildesMayusculasYSignos() {
        assertEquals("que presion", MotorOffline.normalizar("¿Qué PRESIÓN?"))
    }

    @Test
    fun detectaSituacionesDePeligro() {
        assertTrue(MotorOffline.esPeligro("Sale humo del motor"))
        assertFalse(MotorOffline.esPeligro("¿Cada cuánto cambio las plumillas?"))
    }

    @Test
    fun encuentraLaFichaUsandoSinonimos() {
        // "llantas" no está en la ficha: debe encontrarse por el sinónimo "neumatico"
        val r = MotorOffline.buscar("¿Qué presión deben tener las llantas?", fichas)
        assertEquals("1", r.first().ficha.id)
    }

    @Test
    fun noInventaRespuestasSinCoincidencias() {
        assertTrue(MotorOffline.buscar("¿Quién ganó el partido?", fichas).isEmpty())
    }
}