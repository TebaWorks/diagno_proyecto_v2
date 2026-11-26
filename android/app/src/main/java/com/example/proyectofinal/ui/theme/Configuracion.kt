package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.CacheLocal
import com.example.proyectofinal.data.repositorio.NotificacionLocal
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import com.example.proyectofinal.data.repositorio.SesionLocal
import android.widget.ProgressBar
import com.example.proyectofinal.data.repositorio.ConfiguracionModeloGemma
import com.example.proyectofinal.data.repositorio.DescargaGemma
import com.example.proyectofinal.data.repositorio.GemmaLocal
import android.app.DownloadManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

// Pantalla "Configuración": se abre desde la fila "Configuración" de Perfil
class ConfiguracionActivity : ComponentActivity() {

    private val auth = AuthRepositorio()

    // Valores guardados en perfil.nivel_conocimiento y el texto que ve el usuario
    private val valores = listOf("novato", "con_conocimiento", "avanzado")
    private val etiquetas = listOf("Novato", "Con conocimiento", "Avanzado")

    private var nivel: String? = null
    private lateinit var tvNivelActual: TextView
    private lateinit var tvEstadoGemma: TextView
    private lateinit var progresoGemma: ProgressBar
    private lateinit var tvProgresoGemma: TextView
    private lateinit var btnDescargarGemma: View
    private lateinit var btnCancelarGemma: View
    private var seguimientoGemmaJob: Job? = null
    private lateinit var btnEliminarGemma: View

    private var configuracionGemma: ConfiguracionModeloGemma? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_configuracion)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        tvNivelActual = findViewById(R.id.tvNivelActual)
        tvEstadoGemma = findViewById(R.id.tvEstadoGemma)
        progresoGemma = findViewById(R.id.progresoGemma)
        tvProgresoGemma = findViewById(R.id.tvProgresoGemma)
        btnDescargarGemma = findViewById(R.id.btnDescargarGemma)
        btnCancelarGemma = findViewById(R.id.btnCancelarGemma)
        btnDescargarGemma.setOnClickListener {

            Log.i(
                "GEMMA",
                "BOTÓN PRESIONADO | " +
                        "Habilitado: ${btnDescargarGemma.isEnabled} | " +
                        "Configuración: ${configuracionGemma != null}"
            )

            Toast.makeText(
                this,
                "Clic detectado en descarga Gemma",
                Toast.LENGTH_SHORT
            ).show()

            solicitarDescargaGemma()
        }

        btnCancelarGemma.setOnClickListener {
            cancelarDescargaGemma()
        }
        btnEliminarGemma = findViewById(R.id.btnEliminarGemma)

        btnEliminarGemma.setOnClickListener {
            confirmarEliminarGemma()
        }

        findViewById<View>(R.id.btnVolverConfig).setOnClickListener { finish() }
        findViewById<View>(R.id.filaNivel).setOnClickListener { elegirNivel() }
        findViewById<View>(R.id.filaConfigNotif).setOnClickListener {
            startActivity(Intent(this, NotificacionesActivity::class.java))
        }

        findViewById<View>(R.id.filaBorrarDatos).setOnClickListener { confirmarBorrado() }

        findViewById<TextView>(R.id.tvVersion).text = versionApp()

        cargarNivel()
        cargarEstadoGemma()
    }

    private fun cargarNivel() {
        lifecycleScope.launch {
            nivel = try {
                auth.nivelActual()
            } catch (e: Exception) {
                Log.e("CONFIG", "No se pudo leer el nivel", e)
                null
            }
            mostrarNivel()
        }
    }

    private fun mostrarNivel() {
        val i = valores.indexOf(nivel)
        tvNivelActual.text = if (i >= 0) etiquetas[i] else "Sin definir"
    }

    // Lista con las 3 opciones; al elegir una se guarda en Supabase
    private fun elegirNivel() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Nivel de conocimiento")
            .setSingleChoiceItems(etiquetas.toTypedArray(), valores.indexOf(nivel)) { dialogo, cual ->
                dialogo.dismiss()
                guardarNivel(valores[cual])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarNivel(nuevo: String) {
        lifecycleScope.launch {
            try {
                auth.guardarNivel(nuevo)
                SesionLocal(this@ConfiguracionActivity).actualizarNivel(nuevo)
                nivel = nuevo
                mostrarNivel()
                Toast.makeText(this@ConfiguracionActivity, "Nivel actualizado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("CONFIG", "No se pudo guardar el nivel", e)
                Toast.makeText(
                    this@ConfiguracionActivity,
                    "No se pudo guardar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Borra solo lo guardado en el teléfono; lo que está en Supabase no se toca
    private fun confirmarBorrado() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Borrar datos del dispositivo")
            .setMessage(
                "Se borrarán los vehículos y el nombre guardados en este teléfono y tus " +
                        "notificaciones. Tu cuenta y tus datos en la nube no se eliminan."
            )
            .setPositiveButton("Borrar") { _, _ ->
                CacheLocal(this).borrar()
                NotificacionLocal(this).borrar()
                Toast.makeText(this, "Datos borrados", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun confirmarEliminarGemma() {

        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Gemma")
            .setMessage(
                "Se eliminará el modelo Gemma 4 E2B de este dispositivo. " +
                        "Las fichas locales y los datos de tus vehículos " +
                        "se conservarán."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->

                lifecycleScope.launch {
                    try {
                        btnEliminarGemma.isEnabled = false

                        val eliminado = DescargaGemma.eliminarModelo(
                            applicationContext
                        )

                        if (eliminado) {
                            Toast.makeText(
                                this@ConfiguracionActivity,
                                "Modelo eliminado correctamente",
                                Toast.LENGTH_LONG
                            ).show()

                            btnEliminarGemma.visibility = View.GONE
                            btnDescargarGemma.visibility = View.VISIBLE

                            cargarEstadoGemma()

                        } else {
                            btnEliminarGemma.isEnabled = true

                            Toast.makeText(
                                this@ConfiguracionActivity,
                                "No se pudo eliminar completamente el modelo",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: Exception) {
                        Log.e("GEMMA", "Error eliminando modelo", e)

                        btnEliminarGemma.isEnabled = true

                        Toast.makeText(
                            this@ConfiguracionActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }


    @Suppress("DEPRECATION")
    private fun versionApp(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        Log.e("CONFIG", "No se pudo leer la versión", e)
        "1.0"
    }


    private fun cargarEstadoGemma() {
        lifecycleScope.launch {

            btnDescargarGemma.isEnabled = false

            // Caso 1: Gemma ya está instalado.
            if (GemmaLocal.disponible(this@ConfiguracionActivity)) {
                tvEstadoGemma.text = "Gemma está instalado y disponible."

                btnDescargarGemma.visibility = View.GONE
                btnCancelarGemma.visibility = View.GONE
                btnEliminarGemma.visibility = View.VISIBLE

                progresoGemma.visibility = View.GONE
                tvProgresoGemma.visibility = View.GONE

                return@launch
            }

            // El modelo no está instalado: ocultar opción de eliminación.
            btnEliminarGemma.visibility = View.GONE

            // Caso 2: comprobar si existe una descarga anterior.
            val id = DescargaGemma.obtenerIdDescarga(
                this@ConfiguracionActivity
            )

            if (id != null) {
                val progreso = DescargaGemma.consultarProgreso(
                    this@ConfiguracionActivity,
                    id
                )

                if (progreso != null &&
                    progreso.estado != DownloadManager.STATUS_FAILED
                ) {
                    btnDescargarGemma.isEnabled = false
                    btnDescargarGemma.visibility = View.VISIBLE

                    btnCancelarGemma.visibility =
                        if (progreso.completada) View.GONE else View.VISIBLE

                    progresoGemma.visibility = View.VISIBLE
                    tvProgresoGemma.visibility = View.VISIBLE

                    seguirDescargaGemma(id)
                    return@launch

                } else if (progreso?.fallida == true) {
                    DescargaGemma.cancelarDescarga(
                        applicationContext
                    )
                }
            }

            // Caso 3: no hay modelo instalado ni descarga activa.
            progresoGemma.visibility = View.GONE
            tvProgresoGemma.visibility = View.GONE
            btnCancelarGemma.visibility = View.GONE
            btnDescargarGemma.visibility = View.VISIBLE

            try {
                configuracionGemma = DescargaGemma.obtenerConfiguracion()

                tvEstadoGemma.text =
                    "Modelo disponible. Preparado para descargar."

                btnDescargarGemma.isEnabled = true

            } catch (e: Exception) {
                Log.e("GEMMA", "No se pudo consultar configuración", e)

                configuracionGemma = null
                btnDescargarGemma.isEnabled = false

                tvEstadoGemma.text =
                    "Modelo todavía no disponible para descarga."
            }
        }
    }






    private fun solicitarDescargaGemma() {

        val configuracion = configuracionGemma

        if (configuracion == null) {
            Toast.makeText(
                this,
                "La configuración de Gemma no está disponible",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val opciones = arrayOf(
            "Solo Wi-Fi (2,41 GiB)",
            "Wi-Fi y datos móviles (2,41 GiB)"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Selecciona cómo deseas descargar Gemma")
            .setItems(opciones) { _, seleccion ->

                val permitirDatosMoviles = seleccion == 1

                iniciarDescargaGemma(
                    configuracion,
                    permitirDatosMoviles
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }




    private fun iniciarDescargaGemma(
        configuracion: ConfiguracionModeloGemma,
        permitirDatosMoviles: Boolean
    ) {
        try {
            val id = DescargaGemma.iniciarDescarga(
                applicationContext,
                configuracion,
                permitirDatosMoviles
            )

            btnDescargarGemma.isEnabled = false
            btnCancelarGemma.visibility = View.VISIBLE

            progresoGemma.visibility = View.VISIBLE
            tvProgresoGemma.visibility = View.VISIBLE
            progresoGemma.isIndeterminate = true

            tvEstadoGemma.text = "Descarga en curso"

            seguirDescargaGemma(id)

        } catch (e: Exception) {
            Log.e("GEMMA", "Error al iniciar descarga", e)

            tvEstadoGemma.text = "No se pudo iniciar la descarga"

            Toast.makeText(
                this,
                "Error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun cancelarDescargaGemma() {

        seguimientoGemmaJob?.cancel()
        seguimientoGemmaJob = null

        DescargaGemma.cancelarDescarga(this)

        progresoGemma.visibility = View.GONE
        tvProgresoGemma.visibility = View.GONE
        btnCancelarGemma.visibility = View.GONE

        btnDescargarGemma.isEnabled = configuracionGemma != null

        tvEstadoGemma.text = "Descarga cancelada"

        Toast.makeText(
            this,
            "Descarga cancelada",
            Toast.LENGTH_SHORT
        ).show()
    }


    private fun seguirDescargaGemma(id: Long) {

        seguimientoGemmaJob?.cancel()

        seguimientoGemmaJob = lifecycleScope.launch {

            while (true) {

                val progreso = DescargaGemma.consultarProgreso(
                    this@ConfiguracionActivity,
                    id
                )

                if (progreso == null) {
                    tvEstadoGemma.text = "No se pudo consultar la descarga"
                    btnCancelarGemma.visibility = View.GONE
                    btnDescargarGemma.isEnabled = configuracionGemma != null
                    break
                }

                progresoGemma.visibility = View.VISIBLE
                tvProgresoGemma.visibility = View.VISIBLE

                if (progreso.bytesTotales > 0L) {
                    progresoGemma.isIndeterminate = false
                    progresoGemma.progress = progreso.porcentaje

                    val descargados =
                        progreso.bytesDescargados / (1024L * 1024L)

                    val totales =
                        progreso.bytesTotales / (1024L * 1024L)

                    tvProgresoGemma.text =
                        "$descargados / $totales MiB · ${progreso.porcentaje}%"
                } else {
                    progresoGemma.isIndeterminate = true
                    tvProgresoGemma.text = "Preparando descarga..."
                }

                when (progreso.estado) {

                    DownloadManager.STATUS_PENDING -> {
                        tvEstadoGemma.text = "Descarga pendiente..."
                    }

                    DownloadManager.STATUS_RUNNING -> {
                        tvEstadoGemma.text = "Descargando Gemma..."
                    }

                    DownloadManager.STATUS_PAUSED -> {
                        tvEstadoGemma.text = "Descarga pausada. Esperando conexión..."
                    }

                    DownloadManager.STATUS_FAILED -> {

                        val motivo = progreso.motivo

                        DescargaGemma.cancelarDescarga(applicationContext)

                        progresoGemma.visibility = View.GONE
                        tvProgresoGemma.visibility = View.GONE
                        btnCancelarGemma.visibility = View.GONE
                        btnDescargarGemma.isEnabled = configuracionGemma != null

                        tvEstadoGemma.text =
                            "Descarga fallida. Código: $motivo"

                        Toast.makeText(
                            this@ConfiguracionActivity,
                            "No se pudo descargar Gemma",
                            Toast.LENGTH_LONG
                        ).show()

                        break
                    }

                    DownloadManager.STATUS_SUCCESSFUL -> {

                        btnCancelarGemma.visibility = View.GONE
                        progresoGemma.isIndeterminate = false
                        progresoGemma.progress = 100

                        tvEstadoGemma.text = "Descarga terminada. Verificando modelo..."

                        val configuracion = try {
                            configuracionGemma
                                ?: DescargaGemma.obtenerConfiguracion().also {
                                    configuracionGemma = it
                                }
                        } catch (e: Exception) {
                            Log.e("GEMMA", "No se pudo recuperar la configuración", e)

                            tvEstadoGemma.text =
                                "Descarga completa. Pendiente de verificar con Supabase."

                            Toast.makeText(
                                this@ConfiguracionActivity,
                                "No se pudo verificar la configuración del modelo",
                                Toast.LENGTH_LONG
                            ).show()

                            break
                        }

                        try {
                            val instalado = DescargaGemma.verificarEInstalar(
                                applicationContext,
                                configuracion
                            )

                            if (instalado &&
                                GemmaLocal.disponible(applicationContext)
                            ) {
                                tvEstadoGemma.text =
                                    "Gemma instalado y disponible."

                                progresoGemma.visibility = View.GONE
                                tvProgresoGemma.visibility = View.GONE
                                btnDescargarGemma.visibility = View.GONE

                                Toast.makeText(
                                    this@ConfiguracionActivity,
                                    "Gemma instalado correctamente",
                                    Toast.LENGTH_LONG
                                ).show()

                            } else {
                                tvEstadoGemma.text =
                                    "No se pudo completar la instalación."
                            }

                        } catch (e: Exception) {
                            Log.e("GEMMA", "Error verificando el modelo", e)

                            DescargaGemma.cancelarDescarga(applicationContext)

                            tvEstadoGemma.text =
                                "El modelo no superó la verificación."

                            Toast.makeText(
                                this@ConfiguracionActivity,
                                "Error de integridad: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        break
                    }
                }

                delay(1000)
            }
        }
    }



}