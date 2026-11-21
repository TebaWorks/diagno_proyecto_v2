package com.example.proyectofinal.ui.theme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.modelo.Vehiculo
import com.example.proyectofinal.data.repositorio.CacheLocal
import com.example.proyectofinal.data.repositorio.VehiculoRepositorio
import kotlinx.coroutines.launch

class VehiculoActivity : AppCompatActivity() {

    private lateinit var lista: LinearLayout
    private lateinit var tvVacio: TextView
    private val repo = VehiculoRepositorio()
    private val cache by lazy { CacheLocal(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehiculo)

        lista = findViewById(R.id.listaVehiculos)
        tvVacio = findViewById(R.id.tvVacio)

        findViewById<View>(R.id.btnVolver).setOnClickListener { finish() }

        val irAgregar = View.OnClickListener {
            startActivity(Intent(this, AgregarVehiculoActivity::class.java))
        }
        findViewById<View>(R.id.btnAgregarArriba).setOnClickListener(irAgregar)
        findViewById<View>(R.id.cardAgregar).setOnClickListener(irAgregar)
    }

    // Se actualiza la lista cada vez que se vuelve de "Agregar vehículo"
    override fun onResume() {
        super.onResume()
        mostrar()
    }

    private fun mostrar() {
        lifecycleScope.launch {
            val vehiculos: List<Vehiculo> = try {
                repo.obtenerVehiculos().also { cache.guardarVehiculos(it) }
            } catch (e: Exception) {
                Log.e("VEHICULO", "No se pudo cargar", e)
                val guardados = cache.vehiculos()
                Toast.makeText(
                    this@VehiculoActivity,
                    if (guardados.isEmpty()) "No se pudo cargar: ${e.message}"
                    else "Sin conexión: mostrando tus vehículos guardados",
                    Toast.LENGTH_LONG
                ).show()
                guardados
            }

            lista.removeAllViews()
            tvVacio.visibility = if (vehiculos.isEmpty()) View.VISIBLE else View.GONE

            vehiculos.forEachIndexed { i, v ->
                val nombre = "${v.marca} ${v.modelo}"
                val detalle = "${v.anio} · ${v.combustible} · ${v.kilometraje} km"

                val item = layoutInflater.inflate(R.layout.item_vehiculo, lista, false)
                item.findViewById<TextView>(R.id.tvVehNombre).text = nombre
                item.findViewById<TextView>(R.id.tvVehAnio).text = detalle
                item.findViewById<View>(R.id.chipPrincipal).visibility =
                    if (i == 0) View.VISIBLE else View.GONE

                // Mantén presionado para eliminar
                item.setOnLongClickListener {
                    AlertDialog.Builder(this@VehiculoActivity)
                        .setTitle("Eliminar vehículo")
                        .setMessage("¿Eliminar $nombre?")
                        .setPositiveButton("Eliminar") { _, _ -> eliminar(v) }
                        .setNegativeButton("Cancelar", null)
                        .show()
                    true
                }
                lista.addView(item)
            }
        }
    }

    private fun eliminar(v: Vehiculo) {
        val id = v.id ?: return
        lifecycleScope.launch {
            try {
                repo.eliminar(id)
                mostrar()
            } catch (e: Exception) {
                Log.e("VEHICULO", "No se pudo eliminar", e)
                Toast.makeText(
                    this@VehiculoActivity,
                    "No se pudo eliminar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}