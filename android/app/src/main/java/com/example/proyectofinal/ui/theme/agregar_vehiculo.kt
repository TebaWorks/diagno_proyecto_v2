package com.example.proyectofinal.ui.theme

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal.R
import com.example.proyectofinal.data.repositorio.VehiculoRepositorio
import kotlinx.coroutines.launch

class AgregarVehiculoActivity : AppCompatActivity() {

    private var tipo = "Auto"

    private val marcasAuto = mapOf(
        "Toyota" to listOf("Corolla", "Yaris", "Hilux"),
        "Chevrolet" to listOf("Sail", "Spark", "Onix"),
        "Hyundai" to listOf("Accent", "Tucson", "Elantra"),
        "MG" to listOf("GT", "ZS", "3")
    )
    private val marcasMoto = mapOf(
        "Honda" to listOf("CB190R", "XR150L", "Wave"),
        "Yamaha" to listOf("FZ25", "XTZ150", "R15"),
        "Suzuki" to listOf("GN125", "Gixxer", "V-Strom")
    )

    private lateinit var spMarca: Spinner
    private lateinit var spModelo: Spinner
    private lateinit var spAnio: Spinner
    private lateinit var spCombustible: Spinner
    private lateinit var etKm: EditText
    private lateinit var btnAuto: TextView
    private lateinit var btnMoto: TextView
    private lateinit var btnGuardar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_vehiculo)

        spMarca = findViewById(R.id.spMarca)
        spModelo = findViewById(R.id.spModelo)
        spAnio = findViewById(R.id.spAnio)
        spCombustible = findViewById(R.id.spCombustible)
        etKm = findViewById(R.id.etKm)
        btnAuto = findViewById(R.id.btnAuto)
        btnMoto = findViewById(R.id.btnMoto)
        btnGuardar = findViewById(R.id.btnGuardar)

        findViewById<View>(R.id.btnVolver).setOnClickListener { finish() }

        // Años (del actual hacia atrás)
        val anios = listOf("Agregar año") + (2026 downTo 1990).map { it.toString() }
        spAnio.adapter = adaptador(anios)

        // Combustible
        spCombustible.adapter = adaptador(
            listOf("Agregar combustible", "Bencina", "Diésel", "Híbrido", "Eléctrico", "Gas")
        )

        cargarMarcas()

        btnAuto.setOnClickListener { cambiarTipo("Auto") }
        btnMoto.setOnClickListener { cambiarTipo("Moto") }

        // Al elegir marca, se cargan sus modelos
        spMarca.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                cargarModelos(spMarca.selectedItem.toString())
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnGuardar.setOnClickListener { guardar() }
    }

    private fun adaptador(lista: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lista)

    private fun cambiarTipo(nuevo: String) {
        tipo = nuevo
        val activo = R.drawable.bg_toggle_activo
        if (nuevo == "Auto") {
            btnAuto.setBackgroundResource(activo)
            btnAuto.setTextColor(getColor(android.R.color.white))
            btnMoto.background = null
            btnMoto.setTextColor(getColor(R.color.texto))
        } else {
            btnMoto.setBackgroundResource(activo)
            btnMoto.setTextColor(getColor(android.R.color.white))
            btnAuto.background = null
            btnAuto.setTextColor(getColor(R.color.texto))
        }
        cargarMarcas()
    }

    private fun cargarMarcas() {
        val mapa = if (tipo == "Auto") marcasAuto else marcasMoto
        spMarca.adapter = adaptador(listOf("Agregar marca") + mapa.keys)
        cargarModelos("")
    }

    private fun cargarModelos(marca: String) {
        val mapa = if (tipo == "Auto") marcasAuto else marcasMoto
        val modelos = mapa[marca] ?: emptyList()
        spModelo.adapter = adaptador(listOf("Agregar modelo") + modelos)
    }

    // Guarda el vehículo en Supabase (tabla "vehiculo")
    private fun guardar() {
        if (spMarca.selectedItemPosition == 0 ||
            spModelo.selectedItemPosition == 0 ||
            spAnio.selectedItemPosition == 0 ||
            spCombustible.selectedItemPosition == 0 ||
            etKm.text.isNullOrBlank()
        ) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val km = etKm.text.toString().trim().toIntOrNull()
        if (km == null) {
            etKm.error = "Kilometraje no válido"
            return
        }

        val marca = spMarca.selectedItem.toString()
        val modelo = spModelo.selectedItem.toString()
        val anio = spAnio.selectedItem.toString().toInt()
        val combustible = spCombustible.selectedItem.toString()

        btnGuardar.isEnabled = false
        lifecycleScope.launch {
            try {
                VehiculoRepositorio().agregar(tipo, marca, modelo, anio, combustible, km)
                Toast.makeText(this@AgregarVehiculoActivity, "Vehículo guardado", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Log.e("VEHICULO", "No se pudo guardar", e)
                Toast.makeText(
                    this@AgregarVehiculoActivity,
                    "No se pudo guardar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnGuardar.isEnabled = true
            }
        }
    }
}