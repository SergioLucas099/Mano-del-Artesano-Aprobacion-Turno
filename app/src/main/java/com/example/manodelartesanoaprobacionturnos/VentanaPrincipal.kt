package com.example.manodelartesanoaprobacionturnos

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.manodelartesanoaprobacionturnos.Adapter.AtraccionCanceladosAdapter
import com.example.manodelartesanoaprobacionturnos.Adapter.AtraccionEsperaAdapter
import com.example.manodelartesanoaprobacionturnos.Adapter.TurnosAdapter
import com.example.manodelartesanoaprobacionturnos.Adapter.VerAtraccionAdapter
import com.example.manodelartesanoaprobacionturnos.Model.AtraccionCanceladosModel
import com.example.manodelartesanoaprobacionturnos.Model.AtraccionEsperaModel
import com.example.manodelartesanoaprobacionturnos.Model.TurnosModel
import com.example.manodelartesanoaprobacionturnos.Model.VerAtraccionModel
import com.google.firebase.database.*

class VentanaPrincipal : AppCompatActivity() {

    private var turnoSeleccionado: String? = null
    private lateinit var ListaTurnos : java.util.ArrayList<TurnosModel>
    private lateinit var listaAtracciones: ArrayList<VerAtraccionModel>
    private lateinit var listaAtraccionesEspera: ArrayList<AtraccionEsperaModel>
    private lateinit var listaAtraccionesCancelado: ArrayList<AtraccionCanceladosModel>
    private lateinit var RevTurnosEspera: RecyclerView
    private lateinit var adapterEspera: AtraccionEsperaAdapter
    private lateinit var adapterCancelado: AtraccionCanceladosAdapter
    private var textoBusqueda: String = ""
    private var textoBusquedaC: String = ""

    private lateinit var NombreAtraccSelect: TextView
    private lateinit var TxtTurnosEspera: TextView
    private lateinit var TxtTurnosActuales: TextView
    private lateinit var AvisoSinTurnos: LinearLayout
    private lateinit var AvisoSinTurnosActuales: LinearLayout
    private lateinit var AvisoSinTurnosCancelados: LinearLayout
    private lateinit var TxtTurnosCancelados: TextView
    private lateinit var RevTurnosCancelados: RecyclerView
    private lateinit var BuscadorTurno: androidx.appcompat.widget.SearchView
    private lateinit var BuscadorTurnoCancelado: androidx.appcompat.widget.SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ventana_principal)

        val RevVerAtraccion = findViewById<RecyclerView>(R.id.RevVerAtraccion)
        RevTurnosEspera = findViewById(R.id.RevTurnosEspera)
        BuscadorTurno = findViewById(R.id.BuscadorTurno)
        BuscadorTurnoCancelado = findViewById(R.id.BuscadorTurnoCancelado)
        val RevTurnosActuales = findViewById<RecyclerView>(R.id.RevTurnosActuales)
        TxtTurnosEspera = findViewById(R.id.TxtTurnosEspera)
        TxtTurnosActuales = findViewById(R.id.TxtTurnosActuales)
        NombreAtraccSelect = findViewById(R.id.NombreAtraccSelect)
        AvisoSinTurnos = findViewById(R.id.AvisoSinTurnos)
        AvisoSinTurnosActuales = findViewById(R.id.AvisoSinTurnosActuales)
        AvisoSinTurnosCancelados = findViewById(R.id.AvisoSinTurnosCancelados)
        TxtTurnosCancelados = findViewById(R.id.TxtTurnosCancelados)
        RevTurnosCancelados = findViewById(R.id.RevTurnosCancelados)

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
                        BuscadorTurnoCancelado.visibility = View.VISIBLE
                        RevTurnosActuales.visibility = View.VISIBLE
                        TxtTurnosCancelados.visibility = View.VISIBLE
                        RevTurnosCancelados.visibility = View.VISIBLE

                        Toast.makeText(this@VentanaPrincipal, "Seleccionado: $NombreAtraccion", Toast.LENGTH_SHORT).show()

                        // Configurar Recycler de turnos en espera
                        RevTurnosEspera.layoutManager = LinearLayoutManager(
                            this@VentanaPrincipal,
                            RecyclerView.HORIZONTAL,
                            false
                        )
                        listaAtraccionesEspera = arrayListOf()
                        RevTurnosEspera.visibility = View.GONE

                        FirebaseDatabase.getInstance().reference.child("TurnosAcumulados")
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    listaAtraccionesEspera.clear()

                                    if (snapshot.exists()) {
                                        mostrarConTurnos()

                                        for (Snap in snapshot.children) {
                                            Log.d("DEBUG_FIREBASE", "${Snap.key} = ${Snap.value} (${Snap.value!!::class.java})")
                                            val data = Snap.getValue(AtraccionEsperaModel::class.java)
                                            // 🔹 Verificamos que el estado sea "En Espera"
                                            if (data?.Estado == "En Espera") {
                                                listaAtraccionesEspera.add(data)
                                            }
                                        }

                                        // 👇 Revisamos después del bucle
                                        if (listaAtraccionesEspera.isNotEmpty()) {
                                            AvisoSinTurnos.visibility = View.GONE
                                        } else {
                                            AvisoSinTurnos.visibility = View.VISIBLE
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

                        // Configurar Recycler de turnos cancelados
                        RevTurnosCancelados.layoutManager = LinearLayoutManager(
                            this@VentanaPrincipal,
                            RecyclerView.HORIZONTAL,
                            false
                        )
                        listaAtraccionesCancelado = arrayListOf()
                        RevTurnosCancelados.visibility = View.GONE

                        FirebaseDatabase.getInstance().reference.child("TurnosAcumulados")
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    listaAtraccionesCancelado.clear()

                                    if (snapshot.exists()) {
                                        for (Snap in snapshot.children) {
                                            val data = Snap.getValue(AtraccionCanceladosModel::class.java)
                                            if (data?.Estado == "Cancelado") {
                                                listaAtraccionesCancelado.add(data)
                                            }
                                        }

                                        // 👇 Después de recorrer verificamos la lista
                                        if (listaAtraccionesCancelado.isNotEmpty()) {
                                            AvisoSinTurnosCancelados.visibility = View.GONE
                                            BuscadorTurnoCancelado.visibility = View.VISIBLE

                                        } else {
                                            AvisoSinTurnosCancelados.visibility = View.VISIBLE
                                            BuscadorTurnoCancelado.visibility = View.GONE
                                        }

                                    } else {
                                        AvisoSinTurnosCancelados.visibility = View.VISIBLE
                                        BuscadorTurnoCancelado.visibility = View.GONE
                                    }

                                    adapterCancelado = AtraccionCanceladosAdapter(listaAtraccionesCancelado)
                                    RevTurnosCancelados.adapter = adapterCancelado
                                    RevTurnosCancelados.visibility = View.VISIBLE
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(
                                        this@VentanaPrincipal,
                                        "Error: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })

                        // Buscador Cancelado
                        BuscadorTurnoCancelado.setOnQueryTextListener(object :
                            androidx.appcompat.widget.SearchView.OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String): Boolean {
                                return false
                            }

                            override fun onQueryTextChange(s: String): Boolean {
                                textoBusquedaC = s
                                actualizarRecyclerViewCancelado()
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
                        AvisoSinTurnosActuales.visibility = View.GONE
                        for (Snap in snapshot.children){
                            val data = Snap.getValue(TurnosModel::class.java)
                            ListaTurnos.add(data!!)
                        }
                    }else{
                        AvisoSinTurnosActuales.visibility = View.VISIBLE
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

    private fun actualizarRecyclerViewCancelado() {
        val listaFiltrada = listaAtraccionesCancelado.filter { turno ->
            val busqueda = textoBusquedaC.lowercase()
            (turno.NumeroTelefonico?.lowercase()?.contains(busqueda) == true) ||
                    (turno.TurnoAsignado?.lowercase()?.contains(busqueda) == true)
        }

        adapterCancelado = AtraccionCanceladosAdapter(ArrayList(listaFiltrada))
        RevTurnosCancelados.adapter = adapterCancelado
    }

    private fun mostrarSinTurnos() {
        AvisoSinTurnosCancelados.visibility = View.VISIBLE
        AvisoSinTurnos.visibility = View.VISIBLE
        NombreAtraccSelect.visibility = View.VISIBLE
        TxtTurnosActuales.visibility = View.GONE
        BuscadorTurno.visibility = View.GONE
    }

    private fun mostrarConTurnos() {
        AvisoSinTurnosCancelados.visibility = View.GONE
        AvisoSinTurnos.visibility = View.GONE
        TxtTurnosActuales.visibility = View.VISIBLE
        BuscadorTurno.visibility = View.VISIBLE
    }
}