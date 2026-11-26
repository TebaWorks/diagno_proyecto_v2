
package com.example.proyectofinal.data.repositorio

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

@Serializable
data class ConfiguracionModeloGemma(
    val id: String,
    @SerialName("nombre_archivo")
    val nombreArchivo: String,
    val version: String,
    @SerialName("url_descarga")
    val urlDescarga: String,
    @SerialName("tamanio_bytes")
    val tamanioBytes: Long,
    val sha256: String,
    val activo: Boolean = true
)

data class ProgresoDescargaGemma(
    val estado: Int,
    val bytesDescargados: Long,
    val bytesTotales: Long,
    val motivo: Int
) {
    val porcentaje: Int
        get() = if (bytesTotales > 0L) {
            ((bytesDescargados * 100L) / bytesTotales)
                .toInt()
                .coerceIn(0, 100)
        } else {
            0
        }

    val completada: Boolean
        get() = estado == DownloadManager.STATUS_SUCCESSFUL

    val fallida: Boolean
        get() = estado == DownloadManager.STATUS_FAILED
}


object DescargaGemma {

    private const val ID_MODELO = "gemma-4-e2b"
    private const val PREFERENCIAS = "descarga_gemma"
    private const val CLAVE_ID_DESCARGA = "download_id"

    suspend fun obtenerConfiguracion(): ConfiguracionModeloGemma {

        val configuracion = supabase
            .from("configuracion_modelos")
            .select {
                filter {
                    eq("id", ID_MODELO)
                    eq("activo", true)
                }
            }
            .decodeSingleOrNull<ConfiguracionModeloGemma>()
            ?: throw IllegalStateException(
                "No existe una configuración activa para Gemma en Supabase"
            )

        return configuracion
    }


    fun iniciarDescarga(
        context: Context,
        configuracion: ConfiguracionModeloGemma,
        permitirDatosMoviles: Boolean
    ): Long {

        require(configuracion.activo) {
            "El modelo no está habilitado para descarga"
        }

        require(configuracion.nombreArchivo == GemmaLocal.NOMBRE_MODELO) {
            "El nombre del modelo no coincide con el esperado"
        }

        require(configuracion.urlDescarga.startsWith("https://")) {
            "La URL de descarga debe utilizar HTTPS"
        }

        require(configuracion.tamanioBytes > 0L) {
            "El tamaño del modelo no es válido"
        }

        require(configuracion.sha256.matches(Regex("^[A-Fa-f0-9]{64}$"))) {
            "El SHA-256 configurado no es válido"
        }

        val directorio = context.getExternalFilesDir("modelos")
            ?: throw IllegalStateException(
                "No se encuentra disponible el almacenamiento de modelos"
            )

        if (!directorio.exists() && !directorio.mkdirs()) {
            throw IllegalStateException(
                "No se pudo crear el directorio de modelos"
            )
        }

        val archivoTemporal = File(
            directorio,
            "${configuracion.nombreArchivo}.part"
        )

        if (archivoTemporal.exists() && !archivoTemporal.delete()) {
            throw IllegalStateException(
                "No se pudo limpiar el archivo temporal anterior"
            )
        }

        val uri = Uri.parse(configuracion.urlDescarga)

        require(uri.scheme == "https" && !uri.host.isNullOrBlank()) {
            "La dirección de descarga no es válida"
        }

        val redesPermitidas =
            if (permitirDatosMoviles) {
                DownloadManager.Request.NETWORK_WIFI or
                        DownloadManager.Request.NETWORK_MOBILE
            } else {
                DownloadManager.Request.NETWORK_WIFI
            }

        val solicitud = DownloadManager.Request(uri)
            .setTitle("Diagno - Gemma 4 E2B")
            .setDescription("Descargando asistente sin conexión")
            .setMimeType("application/octet-stream")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setAllowedNetworkTypes(redesPermitidas)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(
                context,
                "modelos",
                archivoTemporal.name
            )

        val gestor = context.applicationContext.getSystemService(
            Context.DOWNLOAD_SERVICE
        ) as? DownloadManager
            ?: throw IllegalStateException(
                "Android DownloadManager no está disponible"
            )

        val id = gestor.enqueue(solicitud)

        context.getSharedPreferences(
            PREFERENCIAS,
            Context.MODE_PRIVATE
        ).edit()
            .putLong(CLAVE_ID_DESCARGA, id)
            .apply()

        return id
    }

    fun obtenerIdDescarga(context: Context): Long? {
        val preferencias = context.getSharedPreferences(
            PREFERENCIAS,
            Context.MODE_PRIVATE
        )

        if (!preferencias.contains(CLAVE_ID_DESCARGA)) {
            return null
        }

        return preferencias.getLong(CLAVE_ID_DESCARGA, -1L)
            .takeIf { it > 0L }
    }

    fun consultarProgreso(
        context: Context,
        id: Long
    ): ProgresoDescargaGemma? {

        val gestor = context.getSystemService(
            Context.DOWNLOAD_SERVICE
        ) as DownloadManager

        val cursor = gestor.query(
            DownloadManager.Query().setFilterById(id)
        ) ?: return null

        cursor.use {
            if (!it.moveToFirst()) return null

            return ProgresoDescargaGemma(
                estado = it.getInt(
                    it.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_STATUS
                    )
                ),
                bytesDescargados = it.getLong(
                    it.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                    )
                ),
                bytesTotales = it.getLong(
                    it.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                    )
                ),
                motivo = it.getInt(
                    it.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_REASON
                    )
                )
            )
        }
    }

    fun cancelarDescarga(context: Context) {
        val id = obtenerIdDescarga(context)

        if (id != null) {
            val gestor = context.getSystemService(
                Context.DOWNLOAD_SERVICE
            ) as DownloadManager

            gestor.remove(id)
        }

        context.getExternalFilesDir("modelos")?.let {
            File(it, "${GemmaLocal.NOMBRE_MODELO}.part").delete()
        }

        context.getSharedPreferences(
            PREFERENCIAS,
            Context.MODE_PRIVATE
        ).edit()
            .remove(CLAVE_ID_DESCARGA)
            .apply()
    }


    suspend fun eliminarModelo(context: Context): Boolean =
        withContext(Dispatchers.IO) {

            // Liberar el motor antes de eliminar el archivo.
            GemmaLocal.liberar()

            val directorio = context.getExternalFilesDir("modelos")
                ?: throw IllegalStateException(
                    "No se encuentra el almacenamiento de modelos"
                )

            // Retirar cualquier descarga registrada.
            obtenerIdDescarga(context)?.let { id ->
                val gestor = context.getSystemService(
                    Context.DOWNLOAD_SERVICE
                ) as DownloadManager

                gestor.remove(id)
            }

            val definitivo = File(
                directorio,
                GemmaLocal.NOMBRE_MODELO
            )

            val temporal = File(
                directorio,
                "${GemmaLocal.NOMBRE_MODELO}.part"
            )

            val definitivoEliminado =
                !definitivo.exists() || definitivo.delete()

            val temporalEliminado =
                !temporal.exists() || temporal.delete()

            if (!definitivoEliminado || !temporalEliminado) {
                return@withContext false
            }

            context.getSharedPreferences(
                PREFERENCIAS,
                Context.MODE_PRIVATE
            ).edit()
                .clear()
                .apply()

            true
        }



    suspend fun verificarEInstalar(
        context: Context,
        configuracion: ConfiguracionModeloGemma
    ): Boolean = withContext(Dispatchers.IO) {

        val id = obtenerIdDescarga(context)
            ?: return@withContext false

        val progreso = consultarProgreso(context, id)
            ?: return@withContext false

        if (!progreso.completada) {
            return@withContext false
        }

        require(configuracion.nombreArchivo == GemmaLocal.NOMBRE_MODELO) {
            "El nombre del modelo no coincide"
        }

        val directorio = context.getExternalFilesDir("modelos")
            ?: throw IllegalStateException(
                "Almacenamiento de modelos no disponible"
            )

        val temporal = File(
            directorio,
            "${configuracion.nombreArchivo}.part"
        )

        if (!temporal.isFile) {
            throw IllegalStateException(
                "No se encontró el archivo descargado"
            )
        }

        if (temporal.length() != configuracion.tamanioBytes) {
            temporal.delete()
            throw IllegalStateException(
                "El tamaño del modelo no coincide"
            )
        }

        val hashCalculado = calcularSha256(temporal)

        if (!hashCalculado.equals(configuracion.sha256, ignoreCase = true)) {
            temporal.delete()
            throw IllegalStateException(
                "La verificación SHA-256 falló"
            )
        }

        val archivoFinal = File(
            directorio,
            configuracion.nombreArchivo
        )

        Files.move(
            temporal.toPath(),
            archivoFinal.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )

        context.getSharedPreferences(
            PREFERENCIAS,
            Context.MODE_PRIVATE
        ).edit()
            .remove(CLAVE_ID_DESCARGA)
            .apply()

        true
    }

    private fun calcularSha256(archivo: File): String {

        val digest = MessageDigest.getInstance("SHA-256")

        archivo.inputStream().buffered().use { entrada ->

            val buffer = ByteArray(64 * 1024)

            while (true) {
                val leidos = entrada.read(buffer)

                if (leidos == -1) break

                digest.update(buffer, 0, leidos)
            }
        }

        return digest.digest().joinToString("") {
            (it.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }



}
