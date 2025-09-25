package com.example.manodelartesanoaprobacionturnos.Adapter

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.manodelartesanoaprobacionturnos.Model.TurnosModel
import com.example.manodelartesanoaprobacionturnos.R
import com.google.firebase.database.FirebaseDatabase

class TurnosAdapter (
    private var turnosList: List<TurnosModel>
) : RecyclerView.Adapter<TurnosAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.turnos_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val Lturno = turnosList[position]

        holder.txtNombre.text = Lturno.nombre
        holder.txtTurno.text = Lturno.turno
        val tiempo = Lturno.tiempo
        val estado = Lturno.estado

        val dbRef = FirebaseDatabase.getInstance()
            .getReference("turnos")
            .child(Lturno.id.toString())

        holder.txtEstadoTurno.setText(estado)
        holder.txtTiempoTurno.setText(tiempo)

        holder.BtnCerrarturno.setOnClickListener {
            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setTitle("Cerrar turno")
            builder.setMessage("¿Estás seguro de cerrar este turno?")

            builder.setPositiveButton("Sí") { _: DialogInterface, _: Int ->

                // Referencia al nodo de TiempoAcumulado
                val tiempoRef = FirebaseDatabase.getInstance().getReference("TiempoAcumulado")

                tiempoRef.get().addOnSuccessListener { snapshot ->
                    val tiempoAcumuladoStr = snapshot.getValue(String::class.java) ?: "00:00"
                    val tiempoTurnoStr = holder.txtTiempoTurno.text.toString()

                    // Convertir a minutos
                    val tiempoAcumuladoMin = convertirATotalMinutos(tiempoAcumuladoStr)
                    val tiempoTurnoMin = convertirATotalMinutos(tiempoTurnoStr)

                    // Restar
                    var nuevoTiempoMin = tiempoAcumuladoMin - tiempoTurnoMin
                    if (nuevoTiempoMin < 0) nuevoTiempoMin = 0 // evitar negativos

                    // Convertir a formato mm:ss
                    val nuevoTiempoStr = convertirAFormatoTiempo(nuevoTiempoMin)

                    // Guardar en Firebase
                    tiempoRef.setValue(nuevoTiempoStr).addOnSuccessListener {
                        // Ahora eliminar el turno
                        dbRef.removeValue().addOnSuccessListener {
                            Toast.makeText(holder.itemView.context, "Turno cerrado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }

    }

    override fun getItemCount(): Int {
        return turnosList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombre)
        val txtTiempoTurno: TextView = itemView.findViewById(R.id.txtTiempoTurno)
        val txtTurno: TextView = itemView.findViewById(R.id.txtTurno)

        val txtEstadoTurno: TextView = itemView.findViewById(R.id.txtEstadoTurno)
        val BtnCerrarturno: ImageView = itemView.findViewById(R.id.BtnCerrarturno)
    }

    // Funciones auxiliares
    fun convertirATotalMinutos(tiempo: String): Int {
        val partes = tiempo.split(":")
        val minutos = partes[0].toIntOrNull() ?: 0
        val segundos = partes[1].toIntOrNull() ?: 0
        return minutos * 60 + segundos
    }

    fun convertirAFormatoTiempo(totalSegundos: Int): String {
        val minutos = totalSegundos / 60
        val segundos = totalSegundos % 60
        return String.format("%02d:%02d", minutos, segundos)
    }
}