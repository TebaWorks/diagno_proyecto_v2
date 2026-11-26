package com.example.proyectofinal.data.repositorio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionCuentaTest {

    @Test
    fun sugiereCorreccionDeDominioMalEscrito() {
        assertEquals("ana@gmail.com", ValidacionCuenta.sugerenciaCorreo("ana@gmial.com"))
    }

    @Test
    fun noSugiereNadaSiElDominioEsCorrecto() {
        assertNull(ValidacionCuenta.sugerenciaCorreo("ana@gmail.com"))
    }

    @Test
    fun rechazaCorreosTemporales() {
        assertTrue(ValidacionCuenta.esDesechable("prueba@yopmail.com"))
        assertFalse(ValidacionCuenta.esDesechable("prueba@gmail.com"))
    }

    @Test
    fun validaNombresConTildesYRechazaNumeros() {
        assertTrue(ValidacionCuenta.nombreValido("José Peña"))
        assertFalse(ValidacionCuenta.nombreValido("J4n"))
    }

    @Test
    fun calificaLaFuerzaDeLaContrasena() {
        assertEquals(0, ValidacionCuenta.fuerzaClave("abc"))
        assertEquals(2, ValidacionCuenta.fuerzaClave("Diagno2026!x"))
    }
}