package com.example.proyectofinal.ui.theme

import android.content.Intent
import androidx.activity.ComponentActivity
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.SesionLocal
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.proyectofinal.data.repositorio.GoogleAuth

// Decide a dónde ir con una sesión activa:
// 1) consentimiento (si falta)  2) nivel (si nunca lo eligió)  3) inicio
suspend fun destinoSegunPerfil(auth: AuthRepositorio, sesion: SesionLocal): Class<*> {
    if (!sesion.consentimientoConfirmado()) {
        val pendiente = runCatching { auth.consentimientoPendiente() }.getOrDefault(false)
        if (pendiente) return ConsentimientoActivity::class.java
        sesion.confirmarConsentimiento()
    }
    if (!sesion.nivelConfirmado()) {
        val pendiente = runCatching { auth.nivelPendiente() }.getOrDefault(false)
        if (pendiente) return NivelActivity::class.java
        sesion.confirmarNivel()
    }
    return MainActivity::class.java
}

// Después de iniciar sesión, registrarse, verificar el correo o aceptar el consentimiento
suspend fun ComponentActivity.continuarTrasIngreso() {
    val auth = AuthRepositorio()
    val sesion = SesionLocal(this)
    auth.crearLlave(sesion)

    val intent = Intent(this, destinoSegunPerfil(auth, sesion))
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
}

// Flujo completo de "Continuar con Google".
// Devuelve null si salió bien, "" si el usuario canceló, o un mensaje de error para mostrar.
suspend fun ComponentActivity.ingresarConGoogle(): String? {
    return try {
        val google = GoogleAuth.obtenerToken(this)
        AuthRepositorio().iniciarSesionConGoogle(google.idToken, google.nonce)
        continuarTrasIngreso()   // consentimiento → nivel → inicio
        null
    } catch (e: GetCredentialCancellationException) {
        ""
    } catch (e: NoCredentialException) {
        "No hay cuentas de Google en este teléfono"
    } catch (e: Exception) {
        Log.e("LOGIN", "Error con Google", e)
        "No se pudo iniciar sesión con Google"
    }
}