package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.ExifInterface
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.modelo.Diagnostico
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.ChatRepositorio
import com.example.proyectofinal.data.repositorio.ConsultaRepositorio
import com.example.proyectofinal.data.repositorio.MensajeApi
import com.example.proyectofinal.data.repositorio.VehiculoRepositorio
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.proyectofinal.data.repositorio.AsistenteOffline
import com.example.proyectofinal.data.repositorio.FuenteOffline
import com.example.proyectofinal.data.repositorio.SesionLocal

// Pantalla "Chat con IA" (Asistente Diagno), con envío de fotos
class chat_con_ia : ComponentActivity() {

    private val chatRepo = ChatRepositorio()
    private val auth = AuthRepositorio()
    private val vehiculoRepo = VehiculoRepositorio()
    private val consultaRepo = ConsultaRepositorio()

    // Conversación que se manda a la IA (sin el saludo inicial)
    private val historial = mutableListOf<MensajeApi>()
    private var nivel: String? = null
    private var vehiculoTexto: String? = null
    private var vehiculoNombre: String? = null     // "Marca Modelo", para el historial
    private var consultaId: String? = null         // id de esta conversación en Supabase
    private var esperando = false

    // Foto elegida que todavía no se ha enviado
    private var adjunto: File? = null

    // Última foto enviada (base64), para que el análisis también la tome en cuenta
    private var ultimaImagen: String? = null

    // Botón "Ver resultado del análisis" que aparece bajo la última respuesta de la IA
    private var chipAnalisis: View? = null

    private lateinit var contenedor: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: ImageView
    private lateinit var barraAdjunto: View
    private lateinit var ivAdjunto: ImageView

    // Recibe la foto desde la pantalla camara_adjuntar
    private val abrirCamara =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            if (r.resultCode == RESULT_OK) {
                r.data?.getStringExtra("ruta")?.let { ruta -> mostrarAdjunto(File(ruta)) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_con_ia)

        // Barras del sistema + teclado
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val m = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(m.left, m.top, m.right, m.bottom)
            insets
        }

        contenedor = findViewById(R.id.contenedorMensajes)
        scroll = findViewById(R.id.scrollMensajes)
        etMensaje = findViewById(R.id.etMensaje)
        btnEnviar = findViewById(R.id.btnEnviar)
        barraAdjunto = findViewById(R.id.barraAdjunto)
        ivAdjunto = findViewById(R.id.ivAdjunto)

        findViewById<View>(R.id.btnVolverChat).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAdjuntar).setOnClickListener {
            abrirCamara.launch(Intent(this, camara_adjuntar::class.java))
        }
        findViewById<View>(R.id.btnQuitarAdjunto).setOnClickListener { quitarAdjunto() }
        btnEnviar.setOnClickListener { enviar() }

        agregarMensaje(
            "¡Hola! Soy Diagno, tu asistente automotriz. Puedes contarme qué problema " +
                    "tienes, hacerme cualquier consulta o enviarme una foto de tu vehículo.",
            esUsuario = false
        )
        if (!Conexion.hayInternet(this)) {
            agregarMensaje(
                "Estás sin conexión. Responderé con la información guardada en tu teléfono. " +
                        "Las fotos y el análisis completo necesitan internet.",
                esUsuario = false
            )
        }

        // Nivel y vehículo principal, para que la IA adapte sus respuestas
        lifecycleScope.launch {
            try {
                nivel = auth.nivelActual()
                val v = vehiculoRepo.obtenerVehiculos().firstOrNull()
                vehiculoTexto = v?.let {
                    listOfNotNull(
                        listOfNotNull(it.marca, it.modelo, it.anio?.toString())
                            .joinToString(" ").ifBlank { null },
                        it.combustible,
                        it.kilometraje?.let { km -> "$km km" }
                    ).joinToString(", ")
                }
                vehiculoNombre = v?.let {
                    listOfNotNull(it.marca, it.modelo).joinToString(" ").ifBlank { null }
                }
            } catch (e: Exception) {
                Log.e("CHAT", "No se pudo cargar el contexto", e)
            }
        }
    }

    // ---------- Historial ----------

    // Guarda la conversación en Supabase: crea la consulta con la primera respuesta
    // y la va actualizando con las siguientes. Si falla, el chat sigue funcionando.
    private suspend fun guardarConsulta() {
        try {
            val id = consultaId
            if (id == null) {
                val primera = historial.firstOrNull { it.role == "user" }?.content ?: return
                val respuesta = historial.firstOrNull { it.role == "assistant" }?.content
                consultaId = consultaRepo.crear(
                    titulo = recortar(primera.removePrefix("[Foto adjunta] "), 60),
                    resumen = respuesta?.let { primeraLinea(it) },
                    vehiculo = vehiculoNombre,
                    mensajes = historial.toList()
                )
            } else {
                consultaRepo.actualizar(id, historial.toList())
            }
        } catch (e: Exception) {
            Log.e("CHAT", "No se pudo guardar en el historial", e)
        }
    }

    private fun recortar(texto: String, max: Int): String {
        val limpio = texto.replace("\n", " ").trim()
        return if (limpio.length > max) limpio.take(max - 1).trimEnd() + "…" else limpio
    }

    // Primera línea con texto de la respuesta (sin símbolos de formato)
    private fun primeraLinea(texto: String): String {
        val linea = texto.replace("*", "").replace("#", "").lines()
            .map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: ""
        return recortar(linea, 80)
    }

    // ---------- Foto adjunta ----------

    private fun mostrarAdjunto(archivo: File) {
        try {
            ivAdjunto.setImageBitmap(cargarBitmap(archivo, 200))
            adjunto = archivo
            barraAdjunto.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e("CHAT", "No se pudo leer la foto", e)
            Toast.makeText(this, "No se pudo leer la foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun quitarAdjunto() {
        adjunto = null
        barraAdjunto.visibility = View.GONE
    }

    // Abre la foto, la gira según su orientación y la reduce a un máximo de "maxLado" píxeles
    private fun cargarBitmap(archivo: File, maxLado: Int): Bitmap {
        val bordes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.absolutePath, bordes)

        var muestra = 1
        while (maxOf(bordes.outWidth, bordes.outHeight) / muestra > maxLado * 2) muestra *= 2

        val original = BitmapFactory.decodeFile(
            archivo.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = muestra }
        ) ?: throw IllegalStateException("Imagen no válida")

        val grados = when (
            ExifInterface(archivo.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val escala = (maxLado / maxOf(original.width, original.height).toFloat()).coerceAtMost(1f)
        val matriz = Matrix().apply {
            postRotate(grados)
            postScale(escala, escala)
        }
        return Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
    }

    // JPEG reducido a 1024 px en base64, listo para mandar a la IA
    private fun aBase64(archivo: File): String {
        val bmp = cargarBitmap(archivo, 1024)
        val salida = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, salida)
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
    }

    // ---------- Enviar ----------

    private fun enviar() {
        val textoEscrito = etMensaje.text.toString().trim()
        val foto = adjunto
        if ((textoEscrito.isEmpty() && foto == null) || esperando) return

        // Sin internet: responde el asistente local (fichas o Gemma)
        if (!Conexion.hayInternet(this)) {
            if (foto != null) {
                Toast.makeText(this, "Las fotos necesitan internet. Escribe tu consulta.", Toast.LENGTH_LONG).show()
                return
            }
            responderSinConexion(textoEscrito)
            return
        }

        val texto = textoEscrito.ifEmpty { "Mira esta foto y dime qué podría estar pasando." }
        quitarChipAnalisis()

        etMensaje.text.clear()
        quitarAdjunto()

        val miniatura = foto?.let {
            try { cargarBitmap(it, 600) } catch (e: Exception) { null }
        }
        agregarMensaje(textoEscrito, esUsuario = true, foto = miniatura)
        historial.add(MensajeApi("user", if (foto != null) "[Foto adjunta] $texto" else texto))

        esperando = true
        btnEnviar.alpha = 0.5f
        val escribiendo = agregarMensaje("Escribiendo…", esUsuario = false, conHora = false)
        escribiendo.alpha = 0.6f

        lifecycleScope.launch {
            try {
                // La foto se reduce y se convierte fuera del hilo principal
                val imagen = foto?.let { f -> withContext(Dispatchers.IO) { aBase64(f) } }
                if (imagen != null) ultimaImagen = imagen
                val aEnviar = historial.takeLast(20).dropWhile { it.role != "user" }
                val respuesta = chatRepo.preguntar(aEnviar, nivel, vehiculoTexto, imagen)

                historial.add(MensajeApi("assistant", respuesta))
                contenedor.removeView(escribiendo)
                agregarMensaje(respuesta, esUsuario = false)
                mostrarChipAnalisis()
                guardarConsulta()
            } catch (e: Exception) {
                Log.e("CHAT", "Error al consultar la IA", e)
                historial.removeLastOrNull()
                contenedor.removeView(escribiendo)
                agregarMensaje(
                    "No pude responder en este momento. Revisa tu conexión e inténtalo de nuevo.",
                    esUsuario = false
                )
            } finally {
                esperando = false
                btnEnviar.alpha = 1f
            }
        }
    }

    // Respuesta sin conexión: fichas guardadas o Gemma local
    private fun responderSinConexion(texto: String) {
        quitarChipAnalisis()
        etMensaje.text.clear()
        agregarMensaje(texto, esUsuario = true)

        esperando = true
        btnEnviar.alpha = 0.5f
        val escribiendo = agregarMensaje("Buscando en la información guardada…", esUsuario = false, conHora = false)
        escribiendo.alpha = 0.6f

        lifecycleScope.launch {
            try {
                val nivelUsuario = nivel ?: SesionLocal(this@chat_con_ia).nivel()
                val r = AsistenteOffline.responder(this@chat_con_ia, texto, nivelUsuario)
                val pie = when (r.fuente) {
                    FuenteOffline.GEMMA -> "\n\n— Respuesta sin conexión (IA local)"
                    FuenteOffline.FICHAS -> "\n\n— Respuesta sin conexión (información guardada)"
                    FuenteOffline.SIN_INFORMACION -> ""
                }
                contenedor.removeView(escribiendo)
                agregarMensaje(r.texto + pie, esUsuario = false)
            } catch (e: Exception) {
                Log.e("OFFLINE", "Error en el asistente sin conexión", e)
                contenedor.removeView(escribiendo)
                agregarMensaje("No pude buscar en la información guardada. Inténtalo de nuevo.", esUsuario = false)
            } finally {
                esperando = false
                btnEnviar.alpha = 1f
            }
        }
    }

    // ---------- Resultado del análisis ----------

    private fun quitarChipAnalisis() {
        chipAnalisis?.let { contenedor.removeView(it) }
        chipAnalisis = null
    }

    // Botón bajo la última respuesta: abre el resultado del análisis de esta consulta
    private fun mostrarChipAnalisis() {
        quitarChipAnalisis()
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        val chip = TextView(this).apply {
            text = "Ver resultado del análisis"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.azul))
            setPadding(px(16), px(10), px(16), px(10))
            background = GradientDrawable().apply {
                cornerRadius = px(20).toFloat()
                setColor(ContextCompat.getColor(context, R.color.azul_claro))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = px(12) }
            setOnClickListener { abrirAnalisis() }
        }
        chipAnalisis = chip
        contenedor.addView(chip)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    // Pide a la IA el diagnóstico + guía y abre la pantalla de resultado
    private fun abrirAnalisis() {
        if (esperando) return
        if (!Conexion.hayInternet(this)) {
            Toast.makeText(this, "El análisis completo necesita internet", Toast.LENGTH_LONG).show()
            return
        }
        esperando = true
        val dialogo = MaterialAlertDialogBuilder(this)
            .setMessage("Analizando tu consulta…")
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            try {
                val aEnviar = historial.takeLast(20).dropWhile { it.role != "user" }
                val d: Diagnostico = chatRepo.diagnosticar(aEnviar, nivel, vehiculoTexto, ultimaImagen)
                startActivity(
                    Intent(this@chat_con_ia, ResultadoAnalisisActivity::class.java)
                        .putExtra(
                            ResultadoAnalisisActivity.EXTRA_DIAGNOSTICO,
                            Json.encodeToString(Diagnostico.serializer(), d)
                        )
                )
            } catch (e: Exception) {
                Log.e("CHAT", "No se pudo generar el análisis", e)
                Toast.makeText(
                    this@chat_con_ia,
                    "No pude generar el análisis. Inténtalo de nuevo.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                dialogo.dismiss()
                esperando = false
            }
        }
    }

    // Agrega una burbuja (con foto opcional) y devuelve la fila completa
    private fun agregarMensaje(
        texto: String,
        esUsuario: Boolean,
        conHora: Boolean = true,
        foto: Bitmap? = null
    ): View {
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (esUsuario) Gravity.END else Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = px(12) }
        }

        if (foto != null) {
            fila.addView(ImageView(this).apply {
                setImageBitmap(foto)
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply {
                    cornerRadius = px(16).toFloat()
                    setColor(ContextCompat.getColor(context, R.color.azul_claro))
                }
                outlineProvider = ViewOutlineProvider.BACKGROUND
                clipToOutline = true
                layoutParams = LinearLayout.LayoutParams(px(220), px(180)).apply {
                    if (texto.isNotBlank()) bottomMargin = px(4)
                }
            })
        }

        if (texto.isNotBlank() || foto == null) {
            fila.addView(TextView(this).apply {
                this.text = texto
                textSize = 14f
                setPadding(px(14), px(10), px(14), px(10))
                maxWidth = px(280)
                setTextIsSelectable(true)
                setTextColor(
                    if (esUsuario) ContextCompat.getColor(context, android.R.color.white)
                    else ContextCompat.getColor(context, R.color.texto)
                )
                background = GradientDrawable().apply {
                    setColor(
                        if (esUsuario) ContextCompat.getColor(context, R.color.azul)
                        else ContextCompat.getColor(context, android.R.color.white)
                    )
                    val g = px(16).toFloat()
                    val p = px(4).toFloat()
                    // esquinas: sup-izq, sup-der, inf-der, inf-izq (2 valores cada una)
                    cornerRadii = if (esUsuario) floatArrayOf(g, g, p, p, g, g, g, g)
                    else floatArrayOf(p, p, g, g, g, g, g, g)
                }
            })
        }

        if (conHora) {
            fila.addView(TextView(this).apply {
                this.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                textSize = 11f
                setTypeface(typeface, Typeface.NORMAL)
                setTextColor(ContextCompat.getColor(context, R.color.texto_sec))
                setPadding(px(4), px(2), px(4), 0)
            })
        }

        contenedor.addView(fila)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        return fila
    }
}