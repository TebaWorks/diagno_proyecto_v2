package com.example.proyectofinal.ui.theme

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectofinal.R
import com.example.proyectofinal.data.modelo.Notificacion
import com.example.proyectofinal.data.repositorio.NotificacionLocal
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

// Pantalla "Notificaciones": se abre desde la fila "Notificaciones" de Perfil
class NotificacionesActivity : ComponentActivity() {

    private lateinit var store: NotificacionLocal
    private lateinit var lista: LinearLayout
    private lateinit var tvVacio: TextView
    private lateinit var tvAyuda: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notificaciones)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        store = NotificacionLocal(this)
        lista = findViewById(R.id.listaNotif)
        tvVacio = findViewById(R.id.tvVacioNotif)
        tvAyuda = findViewById(R.id.tvAyudaNotif)

        findViewById<View>(R.id.btnVolverNotif).setOnClickListener { finish() }

        findViewById<View>(R.id.tvMarcarLeidas).setOnClickListener {
            store.marcarTodasLeidas()
            cargar()
        }

        // Interruptores: cada uno guarda su preferencia en el teléfono
        enlazar(R.id.switchMantencion, NotificacionLocal.MANTENCION)
        enlazar(R.id.switchConsultas, NotificacionLocal.CONSULTA)
        enlazar(R.id.switchConsejos, NotificacionLocal.CONSEJO)
    }

    // Se actualiza cada vez que se vuelve a esta pantalla
    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun enlazar(idSwitch: Int, tipo: String) {
        val sw = findViewById<MaterialSwitch>(idSwitch)
        sw.isChecked = store.permitido(tipo)
        sw.setOnCheckedChangeListener { _, activo -> store.definirPermitido(tipo, activo) }
    }

    private fun cargar() {
        val avisos = store.lista()
        lista.removeAllViews()
        tvVacio.visibility = if (avisos.isEmpty()) View.VISIBLE else View.GONE
        tvAyuda.visibility = if (avisos.isEmpty()) View.GONE else View.VISIBLE

        avisos.forEach { n ->
            val item = layoutInflater.inflate(R.layout.item_notificacion, lista, false)

            item.findViewById<ImageView>(R.id.ivTipoNotif).setImageResource(icono(n.tipo))

            val titulo = item.findViewById<TextView>(R.id.tvNotifTitulo)
            titulo.text = n.titulo
            titulo.setTypeface(null, if (n.leida) android.graphics.Typeface.NORMAL else android.graphics.Typeface.BOLD)

            val detalle = item.findViewById<TextView>(R.id.tvNotifDetalle)
            if (n.detalle.isBlank()) detalle.visibility = View.GONE else detalle.text = n.detalle

            item.findViewById<TextView>(R.id.tvNotifFecha).text = DateUtils.getRelativeTimeSpanString(
                n.fecha, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )

            item.findViewById<View>(R.id.puntoNoLeida).visibility =
                if (n.leida) View.INVISIBLE else View.VISIBLE

            // Tocar: marcar como leída. Mantener presionado: eliminar
            item.setOnClickListener {
                if (!n.leida) {
                    store.marcarLeida(n.id)
                    cargar()
                }
            }
            item.setOnLongClickListener {
                confirmarEliminar(n)
                true
            }
            lista.addView(item)
        }
    }

    private fun confirmarEliminar(n: Notificacion) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar notificación")
            .setMessage("¿Eliminar \"${n.titulo}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                store.eliminar(n.id)
                cargar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun icono(tipo: String): Int = when (tipo) {
        NotificacionLocal.MANTENCION -> R.drawable.ic_perfil_auto
        NotificacionLocal.CONSULTA -> R.drawable.ic_perfil_historial
        else -> R.drawable.ic_perfil_notif
    }
}