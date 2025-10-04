package com.example.manodelartesanoaprobacionturnos.Adapter

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.manodelartesanoaprobacionturnos.Model.AtraccionCanceladosModel
import com.example.manodelartesanoaprobacionturnos.Model.AtraccionEsperaModel
import com.example.manodelartesanoaprobacionturnos.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AtraccionCanceladosAdapter (
    private var atraccionLista: List<AtraccionCanceladosModel>
) : RecyclerView.Adapter<AtraccionCanceladosAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.turno_cancelado_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val LTurno = atraccionLista[position]
        val BDTurnosAcumulados = FirebaseDatabase.getInstance().getReference("TurnosAcumulados")

        holder.tiempoTurnoCan.text = LTurno.Tiempo
        holder.tiempoEsperaCan.text = LTurno.TiempoEspera
        holder.personasTurnoCan.text = LTurno.NumeroPersonas
        holder.TurnoAsignadoCan.text = LTurno.TurnoAsignado

        holder.VerificarInfoTurnoCan.setOnClickListener {
            val dialogView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.dialog_confirmar_info, null)

            val txtDatosAtraccionTurno = dialogView.findViewById<TextView>(R.id.txtDatosAtraccionTurno)
            txtDatosAtraccionTurno.text = LTurno.Atraccion.toString()
            val txtDatosNumeroTurno = dialogView.findViewById<TextView>(R.id.txtDatosNumeroTurno)
            txtDatosNumeroTurno.text = LTurno.TurnoAsignado.toString()
            val txtDatosPersonasTurno = dialogView.findViewById<TextView>(R.id.txtDatosPersonasTurno)
            txtDatosPersonasTurno.text = LTurno.NumeroPersonas.toString()
            val txtDatosTelefonoTurno = dialogView.findViewById<TextView>(R.id.txtDatosTelefonoTurno)
            txtDatosTelefonoTurno.text = LTurno.NumeroTelefonico.toString()
            val txtDatosTiempoTurno = dialogView.findViewById<TextView>(R.id.txtDatosTiempoTurno)
            txtDatosTiempoTurno.text = "${LTurno.Tiempo.toString()} min"
            val txtDatosTiempoEsperaTurno = dialogView.findViewById<TextView>(R.id.txtDatosTiempoEsperaTurno)
            txtDatosTiempoEsperaTurno.text = "${LTurno.TiempoEspera.toString()} min"

            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setView(dialogView)
                .setPositiveButton("Aceptar") { dialog, _ ->
                    // acción si el usuario confirma
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }

            val dialog = builder.create()
            dialog.show()
        }

        holder.DevolverTurnoCan.setOnClickListener {

            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setTitle("Advertencia")
            builder.setMessage("¿Confirmar devolución del turno ${LTurno.TurnoAsignado}?")

            builder.setPositiveButton("Aceptar") { dialog, _ ->

                BDTurnosAcumulados
                    .child(LTurno.Id.toString())
                    .child("Estado")
                    .setValue("En Espera") // 🔹 Lo devolvemos al estado original
                    .addOnCompleteListener {
                        Toast.makeText(
                            holder.itemView.context,
                            "Turno ${LTurno.TurnoAsignado} devuelto correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        // 🔹 Tiempo a sumar
                        val tiempoASumar = convertirATiempoSegundos(LTurno.Tiempo.toString())

                        // === Aumentar tiempo en TurnosAcumulados ===
                        val refTurnosAcumulados = FirebaseDatabase.getInstance().getReference("TurnosAcumulados")
                        refTurnosAcumulados.get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                for (child in snapshot.children) {
                                    val tiempoStr = child.child("TiempoEspera").getValue(String::class.java) ?: "00:00"
                                    val tiempoActual = convertirATiempoSegundos(tiempoStr)

                                    // 🔹 Suma el tiempo
                                    val nuevoTiempoSeg = tiempoActual + tiempoASumar
                                    val nuevoTiempoStr = convertirAStrTiempo(nuevoTiempoSeg)

                                    child.ref.child("TiempoEspera").setValue(nuevoTiempoStr)
                                }
                            }
                        }

                        // === Aumentar tiempo en TiempoAcumulado (tabla independiente) ===
                        val refTiempoAcum = FirebaseDatabase.getInstance().getReference("TiempoAcumulado")
                        refTiempoAcum.get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                val tiempoGlobalStr = snapshot.getValue(String::class.java) ?: "00:00"
                                val tiempoGlobal = convertirATiempoSegundos(tiempoGlobalStr)

                                val nuevoTiempoGlobal = tiempoGlobal + tiempoASumar
                                val nuevoTiempoGlobalStr = convertirAStrTiempo(nuevoTiempoGlobal)

                                refTiempoAcum.setValue(nuevoTiempoGlobalStr)
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            holder.itemView.context,
                            "Error al devolver turno",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                dialog.dismiss()
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

    }

    override fun getItemCount(): Int = atraccionLista.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tiempoTurnoCan: TextView = itemView.findViewById(R.id.tiempoTurnoCan)
        val tiempoEsperaCan: TextView = itemView.findViewById(R.id.tiempoEsperaCan)
        val personasTurnoCan: TextView = itemView.findViewById(R.id.personasTurnoCan)
        val TurnoAsignadoCan: TextView = itemView.findViewById(R.id.TurnoAsignadoCan)
        val DevolverTurnoCan: ImageView = itemView.findViewById(R.id.DevolverTurnoCan)
        val VerificarInfoTurnoCan: LinearLayout = itemView.findViewById(R.id.VerificarInfoTurnoCan)
    }
}