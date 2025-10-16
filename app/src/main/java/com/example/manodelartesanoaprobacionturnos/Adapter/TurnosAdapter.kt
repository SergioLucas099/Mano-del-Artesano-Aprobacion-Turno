package com.example.manodelartesanoaprobacionturnos.Adapter

import android.content.DialogInterface
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.manodelartesanoaprobacionturnos.Model.TurnosModel
import com.example.manodelartesanoaprobacionturnos.R
import com.google.firebase.database.FirebaseDatabase

class TurnosAdapter(
    private var turnosList: List<TurnosModel>
) : RecyclerView.Adapter<TurnosAdapter.ViewHolder>() {

    // Para guardar contadores activos y evitar que se dupliquen por el reciclaje del RecyclerView
    private val timers = mutableMapOf<Int, CountDownTimer?>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.turnos_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val Lturno = turnosList[position]

        holder.txtNombre.text = Lturno.nombre
        holder.txtTurno.text = Lturno.turno
        holder.txtEstadoTurno.text = Lturno.estado
        holder.txtTiempoTurno.text = Lturno.tiempo

        val dbRef = FirebaseDatabase.getInstance()
            .getReference("turnos")
            .child(Lturno.id.toString())

        // Detenemos cualquier contador anterior asociado a este ViewHolder
        timers[position]?.cancel()
        timers[position] = null

        // 🕒 Cuando se presiona el botón para iniciar el cronómetro
        holder.iniciarCrono.setOnClickListener {
            val tiempoTexto = holder.txtTiempoTurno.text.toString()
            val partes = tiempoTexto.split(":")
            if (partes.size == 2) {
                val minutos = partes[0].toIntOrNull() ?: 0
                val segundos = partes[1].toIntOrNull() ?: 0
                val totalMillis = ((minutos * 60) + segundos) * 1000L

                // Cancelar si ya hay uno corriendo
                timers[position]?.cancel()

                // Cambiamos el estado visual
                holder.txtEstadoTurno.text = "En curso"

                // Creamos el nuevo temporizador
                val timer = object : CountDownTimer(totalMillis, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val totalSegundos = millisUntilFinished / 1000
                        val min = totalSegundos / 60
                        val seg = totalSegundos % 60
                        holder.txtTiempoTurno.text = String.format("%02d:%02d", min, seg)
                    }

                    override fun onFinish() {
                        holder.txtTiempoTurno.text = "00:00"
                        holder.txtEstadoTurno.text = "Finalizado"
                    }
                }
                timers[position] = timer
                timer.start()
            }
        }

        // 🔒 Botón para cerrar el turno
        holder.BtnCerrarturno.setOnClickListener {
            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setTitle("Cerrar turno")
            builder.setMessage("¿Estás seguro de cerrar este turno?")

            builder.setPositiveButton("Sí") { _: DialogInterface, _: Int ->
                val tiempoRef = FirebaseDatabase.getInstance().getReference("TiempoAcumulado")

                tiempoRef.get().addOnSuccessListener { snapshot ->
                    val tiempoAcumuladoStr = snapshot.getValue(String::class.java) ?: "00:00"
                    val tiempoTurnoStr = holder.txtTiempoTurno.text.toString()

                    val tiempoAcumuladoMin = convertirATotalMinutos(tiempoAcumuladoStr)
                    val tiempoTurnoMin = convertirATotalMinutos(tiempoTurnoStr)

                    var nuevoTiempoMin = tiempoAcumuladoMin - tiempoTurnoMin
                    if (nuevoTiempoMin < 0) nuevoTiempoMin = 0

                    val nuevoTiempoStr = convertirAFormatoTiempo(nuevoTiempoMin)

                    tiempoRef.setValue(nuevoTiempoStr).addOnSuccessListener {
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

    override fun getItemCount(): Int = turnosList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombre)
        val txtTiempoTurno: TextView = itemView.findViewById(R.id.txtTiempoTurno)
        val txtTurno: TextView = itemView.findViewById(R.id.txtTurno)
        val txtEstadoTurno: TextView = itemView.findViewById(R.id.txtEstadoTurno)
        val BtnCerrarturno: ImageView = itemView.findViewById(R.id.BtnCerrarturno)
        val iniciarCrono: ImageView = itemView.findViewById(R.id.iniciarCrono)
    }

    // Funciones auxiliares
    fun convertirATotalMinutos(tiempo: String): Int {
        val partes = tiempo.split(":")
        val minutos = partes.getOrNull(0)?.toIntOrNull() ?: 0
        val segundos = partes.getOrNull(1)?.toIntOrNull() ?: 0
        return minutos * 60 + segundos
    }

    fun convertirAFormatoTiempo(totalSegundos: Int): String {
        val minutos = totalSegundos / 60
        val segundos = totalSegundos % 60
        return String.format("%02d:%02d", minutos, segundos)
    }

    // Detenemos todos los timers cuando se destruye el adaptador
    fun cancelarTodosLosTimers() {
        timers.values.forEach { it?.cancel() }
        timers.clear()
    }
}