
package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.repositorio.AuthRepositorio
import com.example.proyectofinal.data.repositorio.CacheLocal
import com.example.proyectofinal.data.repositorio.ConocimientoLocal
import com.example.proyectofinal.data.repositorio.SesionLocal
import com.example.proyectofinal.data.repositorio.mensajeErrorAuth
import com.example.proyectofinal.data.repositorio.GoogleAuth
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException

// Se muestra si la cuenta no tiene registrado el consentimiento vigente
// (ingreso con Google o cuentas creadas antes de la política)
class ConsentimientoActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FLUJO_GOOGLE = "extra_flujo_google"
    }

    private val auth = AuthRepositorio()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_consentimiento)

        val flujoGoogle = intent.getBooleanExtra(
            EXTRA_FLUJO_GOOGLE,
            false
        )

        Log.d("CONSENTIMIENTO", "Pantalla abierta. flujoGoogle=$flujoGoogle")

        Toast.makeText(
            this,
            "Pantalla de consentimiento abierta",
            Toast.LENGTH_LONG
        ).show()

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.consentimientoRaiz)
        ) { v, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            v.setPadding(
                barras.left,
                barras.top,
                barras.right,
                barras.bottom
            )
            insets
        }

        val cbPrivacidad =
            findViewById<MaterialCheckBox>(R.id.cbPrivacidadC)

        val cbMayorEdad =
            findViewById<MaterialCheckBox>(R.id.cbMayorEdadC)

        val cbCompartir =
            findViewById<MaterialCheckBox>(R.id.cbCompartirC)

        val btnAceptar =
            findViewById<MaterialButton>(R.id.btnAceptarC)

        val progreso =
            findViewById<CircularProgressIndicator>(R.id.progresoC)

        val actualizar = {
            btnAceptar.isEnabled =
                cbPrivacidad.isChecked && cbMayorEdad.isChecked
        }

        cbPrivacidad.setOnCheckedChangeListener { _, _ ->
            actualizar()
        }

        cbMayorEdad.setOnCheckedChangeListener { _, _ ->
            actualizar()
        }

        findViewById<MaterialButton>(R.id.btnLeerPoliticaC)
            .setOnClickListener {
                startActivity(
                    Intent(this, LegalActivity::class.java)
                )
            }

        btnAceptar.setOnClickListener {
            if (!Conexion.hayInternet(this)) {
                Toast.makeText(
                    this,
                    "Necesitas internet para continuar",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            btnAceptar.isEnabled = false
            btnAceptar.text = ""
            progreso.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    if (flujoGoogle) {
                        // 1. Autenticar con Google
                        val google = GoogleAuth.obtenerToken(
                            this@ConsentimientoActivity
                        )

                        auth.iniciarSesionConGoogle(
                            google.idToken,
                            google.nonce
                        )
                    }

                    // 2. Registrar el consentimiento con la cuenta autenticada
                    auth.registrarConsentimiento(
                        cbCompartir.isChecked
                    )

                    // 3. Confirmar consentimiento local
                    SesionLocal(
                        this@ConsentimientoActivity
                    ).confirmarConsentimiento()

                    // 4. Continuar al nivel o al inicio
                    continuarTrasIngreso()

                } catch (e: GetCredentialCancellationException) {
                    // El usuario canceló Google
                    progreso.visibility = View.GONE
                    btnAceptar.text = "Aceptar y continuar"
                    actualizar()

                } catch (e: NoCredentialException) {
                    progreso.visibility = View.GONE
                    btnAceptar.text = "Aceptar y continuar"
                    actualizar()

                    Toast.makeText(
                        this@ConsentimientoActivity,
                        "No hay cuentas de Google en este teléfono",
                        Toast.LENGTH_LONG
                    ).show()

                } catch (e: Exception) {
                    Log.e(
                        "CONSENTIMIENTO",
                        "No se pudo completar el ingreso",
                        e
                    )

                    progreso.visibility = View.GONE
                    btnAceptar.text = "Aceptar y continuar"
                    actualizar()

                    Toast.makeText(
                        this@ConsentimientoActivity,
                        mensajeErrorAuth(e),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Sin aceptar no se puede usar la app
        findViewById<MaterialButton>(R.id.btnRechazarC)
            .setOnClickListener {

                // En el flujo Google todavía no hay sesión iniciada
                if (flujoGoogle) {
                    finish()
                    return@setOnClickListener
                }

                // Comportamiento original para ingreso con correo
                lifecycleScope.launch {
                    runCatching {
                        auth.cerrarSesion()
                    }

                    SesionLocal(
                        this@ConsentimientoActivity
                    ).borrar()

                    ConocimientoLocal(
                        this@ConsentimientoActivity
                    ).borrarTodo()

                    CacheLocal(
                        this@ConsentimientoActivity
                    ).borrar()

                    val intent = Intent(
                        this@ConsentimientoActivity,
                        MainActivity::class.java
                    )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                }
            }

        // Atrás sale de la app sin cerrar la sesión;
        // al volver se pedirá de nuevo
        onBackPressedDispatcher.addCallback(this) {
            finishAffinity()
        }
    }
}
