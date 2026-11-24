package com.example.proyectofinal.data.repositorio

import android.content.ContentValues
import android.content.Context
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
    val etiquetaImagen: String?
)

// Base SQLite local con las fichas descargadas de Supabase (conocimiento_offline)
class ConocimientoLocal(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "diagno_fichas.db", null, 1) {

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
                etiqueta_imagen TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS fichas")
        onCreate(db)
    }

    // Reemplaza todas las fichas por las recién descargadas, en una sola transacción
    fun reemplazarTodo(fichas: List<FichaLocal>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("fichas", null, null)
            for (f in fichas) {
                val valores = ContentValues().apply {
                    put("id", f.id)
                    put("titulo", f.titulo)
                    put("contenido", f.contenido)
                    put("palabras", f.palabras.joinToString(" "))
                    put("categoria", f.categoria)
                    put("urgencia", f.urgencia)
                    put("origen", f.origen)
                    put("etiqueta_imagen", f.etiquetaImagen)
                }
                db.insertWithOnConflict("fichas", null, valores, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun listar(): List<FichaLocal> {
        val resultado = mutableListOf<FichaLocal>()
        readableDatabase.query("fichas", null, null, null, null, null, null).use { c ->
            while (c.moveToNext()) {
                resultado.add(
                    FichaLocal(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        titulo = c.getString(c.getColumnIndexOrThrow("titulo")),
                        contenido = c.getString(c.getColumnIndexOrThrow("contenido")),
                        palabras = c.getString(c.getColumnIndexOrThrow("palabras"))
                            .split(" ").filter { it.isNotBlank() },
                        categoria = c.getString(c.getColumnIndexOrThrow("categoria")) ?: "",
                        urgencia = c.getString(c.getColumnIndexOrThrow("urgencia")),
                        origen = c.getString(c.getColumnIndexOrThrow("origen")),
                        etiquetaImagen = c.getString(c.getColumnIndexOrThrow("etiqueta_imagen"))
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

    // Se llama al cerrar sesión o eliminar la cuenta
    fun borrarTodo() {
        writableDatabase.delete("fichas", null, null)
    }
}