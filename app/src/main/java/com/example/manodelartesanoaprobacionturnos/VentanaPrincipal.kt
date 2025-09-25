package com.example.manodelartesanoaprobacionturnos

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.manodelartesanoaprobacionturnos.Adapter.AtraccionEsperaAdapter
import com.example.manodelartesanoaprobacionturnos.Adapter.TurnosAdapter
import com.example.manodelartesanoaprobacionturnos.Adapter.VerAtraccionAdapter
import com.example.manodelartesanoaprobacionturnos.Model.AtraccionEsperaModel
import com.example.manodelartesanoaprobacionturnos.Model.TurnosModel
import com.example.manodelartesanoaprobacionturnos.Model.VerAtraccionModel
import com.google.firebase.database.*

class VentanaPrincipal : AppCompatActivity() {

    private var turnoSeleccionado: String? = null
    private lateinit var ListaTurnos : java.util.ArrayList<TurnosModel>
    private lateinit var listaAtracciones: ArrayList<VerAtraccionModel>
    private lateinit var listaAtraccionesEspera: ArrayList<AtraccionEsperaModel>
    private lateinit var RevTurnosEspera: RecyclerView
    private lateinit var adapterEspera: AtraccionEsperaAdapter
    private var textoBusqueda: String = ""

    private lateinit var NombreAtraccSelect: TextView
    private lateinit var TxtTurnosEspera: TextView
    private lateinit var TxtTurnosActuales: TextView
    private lateinit var AvisoSinTurnos: LinearLayout
    private lateinit var BuscadorTurno: androidx.appcompat.widget.SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ventana_principal)

        val RevVerAtraccion = findViewById<RecyclerView>(R.id.RevVerAtraccion)
        RevTurnosEspera = findViewById(R.id.RevTurnosEspera)
        BuscadorTurno = findViewById(R.id.BuscadorTurno)
        val RevTurnosActuales = findViewById<RecyclerView>(R.id.RevTurnosActuales)
        TxtTurnosEspera = findViewById(R.id.TxtTurnosEspera)
        TxtTurnosActuales = findViewById(R.id.TxtTurnosActuales)
        NombreAtraccSelect = findViewById(R.id.NombreAtraccSelect)
        AvisoSinTurnos = findViewById(R.id.AvisoSinTurnos)

        // Ocultar inicialmente
        RevTurnosActuales.visibility = View.GONE
        TxtTurnosEspera.visibility = View.GONE
        NombreAtraccSelect.visibility = View.GONE
        TxtTurnosActuales.visibility = View.GONE
        BuscadorTurno.visibility = View.GONE
        AvisoSinTurnos.visibility = View.GONE

        var IdAtraccion = ""
        var NombreAtraccion = ""
        var TurnoAtraccion = ""

        RevVerAtraccion.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        listaAtracciones = arrayListOf()
        RevVerAtraccion.visibility = View.GONE

        FirebaseDatabase.getInstance().reference.child("Atracciones")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaAtracciones.clear()
                    if (snapshot.exists()) {
                        for (Snap in snapshot.children) {
                            val data = Snap.getValue(VerAtraccionModel::class.java)
                            data?.let { listaAtracciones.add(it) }
                        }
                    }

                    val adapter = VerAtraccionAdapter(listaAtracciones) { textoSeleccionado ->
                        IdAtraccion = textoSeleccionado.Id.toString()
                        NombreAtraccion = textoSeleccionado.Nombre.toString()
                        TurnoAtraccion = textoSeleccionado.Turno.toString()

                        NombreAtraccSelect.text = NombreAtraccion
                        TxtTurnosEspera.visibility = View.VISIBLE
                        NombreAtraccSelect.visibility = View.VISIBLE
                        TxtTurnosActuales.visibility = View.VISIBLE
                        BuscadorTurno.visibility = View.VISIBLE
                        RevTurnosActuales.visibility = View.VISIBLE
                        Toast.makeText(this@VentanaPrincipal, "Seleccionado: $NombreAtraccion", Toast.LENGTH_SHORT).show()

                        // Configurar Recycler de turnos en espera
                        RevTurnosEspera.layoutManager = LinearLayoutManager(
                            this@VentanaPrincipal,
                            RecyclerView.HORIZONTAL,
                            false
                        )
                        listaAtraccionesEspera = arrayListOf()
                        RevTurnosEspera.visibility = View.GONE

                        FirebaseDatabase.getInstance().reference.child("TurnosEnEspera")
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    listaAtraccionesEspera.clear()
                                    if (snapshot.exists()) {
                                        mostrarConTurnos()
                                        for (Snap in snapshot.children) {
                                            val data = Snap.getValue(AtraccionEsperaModel::class.java)
                                            data?.let { listaAtraccionesEspera.add(it) }
                                        }
                                    } else {
                                        mostrarSinTurnos()
                                    }

                                    adapterEspera = AtraccionEsperaAdapter(listaAtraccionesEspera)
                                    RevTurnosEspera.adapter = adapterEspera
                                    RevTurnosEspera.visibility = View.VISIBLE
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(
                                        this@VentanaPrincipal,
                                        "Error: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })

                        // Buscador
                        BuscadorTurno.setOnQueryTextListener(object :
                            androidx.appcompat.widget.SearchView.OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String): Boolean {
                                return false
                            }

                            override fun onQueryTextChange(s: String): Boolean {
                                textoBusqueda = s
                                actualizarRecyclerView()
                                return true
                            }
                        })
                    }

                    RevVerAtraccion.adapter = adapter
                    RevVerAtraccion.visibility = View.VISIBLE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@VentanaPrincipal, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

        RevTurnosActuales.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        ListaTurnos = arrayListOf<TurnosModel>()
        RevTurnosActuales.visibility = View.GONE
        FirebaseDatabase.getInstance().reference.child("turnos")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot){
                    ListaTurnos.clear()
                    if (snapshot.exists()){
                        for (Snap in snapshot.children){
                            val data = Snap.getValue(TurnosModel::class.java)
                            ListaTurnos.add(data!!)
                        }
                    }

                    val adapter = TurnosAdapter(ListaTurnos)
                    RevTurnosActuales.adapter = adapter
                    //RevTurnosActuales.visibility = View.VISIBLE
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }

    private fun actualizarRecyclerView() {
        val listaFiltrada = listaAtraccionesEspera.filter { turno ->
            val busqueda = textoBusqueda.lowercase()
            (turno.NumeroTelefonico?.lowercase()?.contains(busqueda) == true) ||
                    (turno.TurnoAsignado?.lowercase()?.contains(busqueda) == true)
        }

        adapterEspera = AtraccionEsperaAdapter(ArrayList(listaFiltrada))
        RevTurnosEspera.adapter = adapterEspera
    }

    private fun mostrarSinTurnos() {
        AvisoSinTurnos.visibility = View.VISIBLE
        NombreAtraccSelect.visibility = View.VISIBLE
        TxtTurnosActuales.visibility = View.GONE
        BuscadorTurno.visibility = View.GONE
    }

    private fun mostrarConTurnos() {
        AvisoSinTurnos.visibility = View.GONE
        TxtTurnosActuales.visibility = View.VISIBLE
        BuscadorTurno.visibility = View.VISIBLE
    }
}