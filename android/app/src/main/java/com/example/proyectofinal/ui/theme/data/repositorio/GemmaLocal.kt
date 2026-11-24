package com.example.proyectofinal.data.repositorio

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

// IA generativa local (Gemma) para el modo sin conexión, siempre anclada a las fichas
object GemmaLocal {

    const val NOMBRE_MODELO = "gemma-4-E2B-it.litertlm"

    private var engine: Engine? = null
    private val bloqueo = Mutex()

    private const val INSTRUCCIONES = """
Eres Diagno, un asistente de mantención automotriz PREVENTIVA que funciona sin internet.
Reglas:
- Responde SOLO con la INFORMACIÓN GUARDADA que viene más abajo. No inventes datos técnicos (medidas, intervalos, capacidades).
- Si la información guardada no responde la pregunta, dilo con claridad y sugiere consultar al asistente en línea cuando haya internet.
- Orientas, no diagnosticas fallas complejas. Ante cualquier riesgo (frenos, dirección, temperatura alta, presión de aceite, testigos rojos, humo), indica detener el vehículo y pedir asistencia.
- Responde en español de Chile, de forma breve y clara.
"""

    private val TEXTO_NIVEL = mapOf(
        "novato" to "El usuario es NOVATO: usa palabras simples y pasos muy concretos.",
        "con_conocimiento" to "El usuario tiene conocimientos básicos: puedes usar términos comunes.",
        "avanzado" to "El usuario tiene conocimientos avanzados: puedes ser técnico y directo."
    )

    // Lugares donde puede estar el modelo: la descarga de la app y la ubicación de pruebas
    fun archivoModelo(context: Context): File? {
        val candidatos = listOfNotNull(
            context.getExternalFilesDir("modelos")?.let { File(it, NOMBRE_MODELO) },
            File(context.filesDir, "models/$NOMBRE_MODELO")
        )
        return candidatos.firstOrNull { it.exists() && it.length() > 100_000_000L }
    }

    fun disponible(context: Context): Boolean = archivoModelo(context) != null

    // Carga el modelo una sola vez (tarda varios segundos)
    private suspend fun motor(context: Context): Engine = bloqueo.withLock {
        engine ?: withContext(Dispatchers.IO) {
            val archivo = archivoModelo(context) ?: error("Modelo Gemma no instalado")
            val nuevo = Engine(
                EngineConfig(
                    modelPath = archivo.absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = context.applicationContext.cacheDir.absolutePath
                )
            )
            try {
                nuevo.initialize()
            } catch (e: Throwable) {
                nuevo.close()
                throw e
            }
            nuevo
        }.also { engine = it }
    }

    suspend fun responder(
        context: Context,
        pregunta: String,
        fichas: List<FichaLocal>,
        nivel: String
    ): String = withContext(Dispatchers.IO) {
        val e = motor(context)
        val contexto = fichas.joinToString("\n\n") { "### ${it.titulo}\n${it.contenido}" }
        val sistema = INSTRUCCIONES.trimIndent() + "\n" +
                (TEXTO_NIVEL[nivel] ?: TEXTO_NIVEL.getValue("novato")) +
                "\n\n=== INFORMACIÓN GUARDADA ===\n" + contexto

        val conversacion = e.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(sistema),
                samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 0.3)
            )
        )
        try {
            val respuesta = StringBuilder()
            conversacion.sendMessageAsync(pregunta).collect { respuesta.append(it.toString()) }
            respuesta.toString().trim()
        } finally {
            conversacion.close()
        }
    }

    // Libera la memoria del modelo (por ejemplo, al cerrar sesión)
    fun liberar() {
        engine?.close()
        engine = null
    }
}