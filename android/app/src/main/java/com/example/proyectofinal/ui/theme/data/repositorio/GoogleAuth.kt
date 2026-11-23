package com.example.proyectofinal.data.repositorio

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

// Obtiene el token de Google del teléfono (selector de cuentas de Android)
object GoogleAuth {

    // ID de cliente WEB de Google Cloud (Clientes → Aplicación web). Es público, no secreto.
    const val WEB_CLIENT_ID = "516888006659-bthfb8bdv1u6muii1osu4gvln53isjdb.apps.googleusercontent.com"

    class Resultado(val idToken: String, val nonce: String)

    suspend fun obtenerToken(activity: Activity): Resultado {
        // El nonce evita que alguien reutilice un token robado:
        // a Google se le envía su hash y a Supabase el valor original
        val nonce = UUID.randomUUID().toString()
        val nonceHash = MessageDigest.getInstance("SHA-256")
            .digest(nonce.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val opcion = GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID)
            .setNonce(nonceHash)
            .build()
        val solicitud = GetCredentialRequest.Builder().addCredentialOption(opcion).build()

        val respuesta = CredentialManager.create(activity).getCredential(activity, solicitud)
        val credencial = respuesta.credential
        if (credencial is CustomCredential &&
            credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val google = GoogleIdTokenCredential.createFrom(credencial.data)
            return Resultado(google.idToken, nonce)
        }
        throw IllegalStateException("Credencial de Google no reconocida")
    }
}