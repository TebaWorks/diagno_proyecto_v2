package com.example.proyectofinal.ui.theme

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Pantalla "Adjuntar imagen": cámara, galería y rotar.
// Devuelve a quien la abre la ruta del archivo en el extra "ruta".
class camara_adjuntar : ComponentActivity() {

    private lateinit var vistaPrevia: PreviewView
    private var imageCapture: ImageCapture? = null
    private var proveedor: ProcessCameraProvider? = null
    private var camaraTrasera = true

    private val pedirPermiso =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) {
                iniciarCamara()
            } else {
                Toast.makeText(
                    this,
                    "Sin permiso de cámara. Puedes elegir una foto de la galería.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val elegirDeGaleria =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) copiarYDevolver(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camara_adjuntar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            insets
        }

        vistaPrevia = findViewById(R.id.vistaPrevia)

        findViewById<View>(R.id.btnVolverCamara).setOnClickListener { finish() }
        findViewById<View>(R.id.btnObturador).setOnClickListener { tomarFoto() }
        findViewById<View>(R.id.btnGaleria).setOnClickListener {
            elegirDeGaleria.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        findViewById<View>(R.id.btnRotar).setOnClickListener {
            camaraTrasera = !camaraTrasera
            vincularCamara()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            iniciarCamara()
        } else {
            pedirPermiso.launch(Manifest.permission.CAMERA)
        }
    }

    private fun iniciarCamara() {
        val futuro = ProcessCameraProvider.getInstance(this)
        futuro.addListener({
            proveedor = futuro.get()
            vincularCamara()
        }, ContextCompat.getMainExecutor(this))
    }

    // Conecta la vista previa y la captura a la cámara elegida (trasera o frontal)
    private fun vincularCamara() {
        val p = proveedor ?: return
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(vistaPrevia.surfaceProvider)
        }
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        val selector = if (camaraTrasera) CameraSelector.DEFAULT_BACK_CAMERA
        else CameraSelector.DEFAULT_FRONT_CAMERA
        try {
            p.unbindAll()
            p.bindToLifecycle(this, selector, preview, imageCapture)
        } catch (e: Exception) {
            Log.e("CAMARA", "No se pudo abrir la cámara", e)
            Toast.makeText(this, "No se pudo abrir esa cámara", Toast.LENGTH_SHORT).show()
            camaraTrasera = true
        }
    }

    private fun tomarFoto() {
        val captura = imageCapture ?: return
        val archivo = File(cacheDir, "foto_${System.currentTimeMillis()}.jpg")
        val opciones = ImageCapture.OutputFileOptions.Builder(archivo).build()
        captura.takePicture(
            opciones,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(resultado: ImageCapture.OutputFileResults) {
                    devolver(archivo)
                }

                override fun onError(e: ImageCaptureException) {
                    Log.e("CAMARA", "No se pudo tomar la foto", e)
                    Toast.makeText(
                        this@camara_adjuntar, "No se pudo tomar la foto", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // La foto de la galería se copia a un archivo propio de la app
    private fun copiarYDevolver(uri: Uri) {
        lifecycleScope.launch {
            try {
                val archivo = withContext(Dispatchers.IO) {
                    val destino = File(cacheDir, "galeria_${System.currentTimeMillis()}.jpg")
                    contentResolver.openInputStream(uri)?.use { entrada ->
                        destino.outputStream().use { salida -> entrada.copyTo(salida) }
                    } ?: throw IllegalStateException("No se pudo leer la imagen")
                    destino
                }
                devolver(archivo)
            } catch (e: Exception) {
                Log.e("CAMARA", "Error con la galería", e)
                Toast.makeText(this@camara_adjuntar, "No se pudo usar esa imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun devolver(archivo: File) {
        setResult(RESULT_OK, Intent().putExtra("ruta", archivo.absolutePath))
        finish()
    }
}