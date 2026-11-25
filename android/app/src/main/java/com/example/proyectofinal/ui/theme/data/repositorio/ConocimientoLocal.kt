package com.example.proyectofinal.data.repositorio

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Ficha guardada en el teléfono para el modo sin conexión
data class FichaLocal(
    val id: String,
    val titulo: String,
    val contenido: String,
    val palabras: List<String>,
    val categoria: String,
    val urgencia: String,
    val origen: String,
    val etiquetaImagen: String?,
    val preguntas: List<String> = emptyList(),
    val respuestaNovato: String? = null,
    val respuestaIntermedio: String? = null,
    val respuestaAvanzado: String? = null,
    val pasos: List<String> = emptyList(),
    val recomendaciones: List<String> = emptyList()
) {
    // Respuesta según el nivel del usuario (si no hay, el contenido general)
    fun respuestaPara(nivel: String): String = when (nivel) {
        "novato" -> respuestaNovato
        "avanzado" -> respuestaAvanzado
        else -> respuestaIntermedio
    }?.takeIf { it.isNotBlank() } ?: contenido
}

// Base SQLite local con las fichas descargadas de Supabase (conocimiento_offline)
class ConocimientoLocal(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "diagno_fichas.db", null, 2) {

    private val SEP = "\n"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE fichas (
                id TEXT PRIMARY KEY,
                titulo TEXT NOT NULL,
                contenido TEXT NOT NULL,
                palabras TEXT NOT NULL,
                categoria TEXT,
                urgencia TEXT NOT NULL,
                origen TEXT NOT NULL,
                etiqueta_imagen TEXT,
                preguntas TEXT,
                resp_novato TEXT,
                resp_intermedio TEXT,
                resp_avanzado TEXT,
                pasos TEXT,
                recomendaciones TEXT
            )
            """.trimIndent()
        )
    }

    // Las fichas se vuelven a descargar en la próxima sincronización
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS fichas")
        onCreate(db)
    }

    fun reemplazarTodo(fichas: List<FichaLocal>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("fichas", null, null)
            for (f in fichas) {
                val v = ContentValues().apply {
                    put("id", f.id)
                    put("titulo", f.titulo)
                    put("contenido", f.contenido)
                    put("palabras", f.palabras.joinToString(" "))
                    put("categoria", f.categoria)
                    put("urgencia", f.urgencia)
                    put("origen", f.origen)
                    put("etiqueta_imagen", f.etiquetaImagen)
                    put("preguntas", f.preguntas.joinToString(SEP))
                    put("resp_novato", f.respuestaNovato)
                    put("resp_intermedio", f.respuestaIntermedio)
                    put("resp_avanzado", f.respuestaAvanzado)
                    put("pasos", f.pasos.joinToString(SEP))
                    put("recomendaciones", f.recomendaciones.joinToString(SEP))
                }
                db.insertWithOnConflict("fichas", null, v, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun Cursor.texto(col: String): String? = getString(getColumnIndexOrThrow(col))
    private fun Cursor.lista(col: String, sep: String): List<String> =
        texto(col)?.split(sep)?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    fun listar(): List<FichaLocal> {
        val resultado = mutableListOf<FichaLocal>()
        readableDatabase.query("fichas", null, null, null, null, null, null).use { c ->
            while (c.moveToNext()) {
                resultado.add(
                    FichaLocal(
                        id = c.texto("id")!!,
                        titulo = c.texto("titulo")!!,
                        contenido = c.texto("contenido")!!,
                        palabras = c.lista("palabras", " "),
                        categoria = c.texto("categoria") ?: "",
                        urgencia = c.texto("urgencia") ?: "media",
                        origen = c.texto("origen") ?: "consulta",
                        etiquetaImagen = c.texto("etiqueta_imagen"),
                        preguntas = c.lista("preguntas", SEP),
                        respuestaNovato = c.texto("resp_novato"),
                        respuestaIntermedio = c.texto("resp_intermedio"),
                        respuestaAvanzado = c.texto("resp_avanzado"),
                        pasos = c.lista("pasos", SEP),
                        recomendaciones = c.lista("recomendaciones", SEP)
                    )
                )
            }
        }
        return resultado
    }

    fun cantidad(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM fichas", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }

    fun borrarTodo() {
        writableDatabase.delete("fichas", null, null)
    }
}