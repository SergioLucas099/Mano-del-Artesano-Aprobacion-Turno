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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.manodelartesanoaprobacionturnos.Model.AtraccionEsperaModel
import com.example.manodelartesanoaprobacionturnos.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AtraccionEsperaAdapter(
    private var atraccionLista: List<AtraccionEsperaModel>
) : RecyclerView.Adapter<AtraccionEsperaAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.turno_espera_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val LTurno = atraccionLista[position]
        val BDTurnosAcumulados = FirebaseDatabase.getInstance().getReference("TurnosAcumulados")
        var countDownTimer: CountDownTimer? = null
        val BDLlamandoTurno = FirebaseDatabase.getInstance().getReference("LlamandoTurno")
        val BDTurnos = FirebaseDatabase.getInstance().getReference("turnos")
        val BDDatosLlamadoTurno = FirebaseDatabase.getInstance().getReference("DatosLlamadoTurno")

        var TiempoEsperaLlamado = "01:00"
        var estadoBotonLlamar = "On"      // Llamar activo
        var estadoBotonCancelar = "Off"


        fun actualizarFirebase(key: String, atraccion: String) {
            val mapLlamado: MutableMap<String, Any> = HashMap()
            mapLlamado["BotonLLamar"] = estadoBotonLlamar
            mapLlamado["BotonCancelar"] = estadoBotonCancelar
            mapLlamado["Tiempo"] = TiempoEsperaLlamado

            BDDatosLlamadoTurno.child(atraccion).child(key).setValue(mapLlamado)
        }

        holder.tiempoTurno.text = LTurno.Tiempo
        holder.tiempoEspera.text = LTurno.TiempoEspera
        holder.personasTurno.text = LTurno.NumeroPersonas
        holder.TurnoAsignado.text = LTurno.TurnoAsignado
        holder.TextViewTiempoEsperaLLamado.visibility = View.INVISIBLE

        val map: MutableMap<String, Any> = HashMap()
        map["Tiempo"] = LTurno.Tiempo ?: ""
        map["NumeroPersonas"] = LTurno.NumeroPersonas ?: ""
        map["TurnoAsignado"] = LTurno.TurnoAsignado ?: ""
        map["NombreAtraccion"] = LTurno.Atraccion ?: ""

        // 🔹 Usamos el Id del turno como clave en Firebase
        val key = LTurno.Id ?: return

        holder.VerificarInfoTurno.setOnClickListener {
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

            val dialog = builder.create()
            dialog.show()
        }

        holder.LlamarTurno.setOnClickListener {
            BDLlamandoTurno.child(key).setValue(map)
                .addOnSuccessListener {
                    Toast.makeText(holder.itemView.context, "Llamando Turno...", Toast.LENGTH_SHORT).show()

                    estadoBotonLlamar = "Off"
                    estadoBotonCancelar = "On"
                    actualizarFirebase(key, LTurno.Atraccion.toString())

                    holder.LlamarTurno.visibility = View.INVISIBLE
                    holder.CancelarTurno.visibility = View.VISIBLE
                    holder.TextViewTiempoEsperaLLamado.visibility = View.VISIBLE

                    // Inicia cuenta regresiva
                    countDownTimer = object : CountDownTimer(60_000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            val segundosRestantes = millisUntilFinished / 1000
                            val minutos = segundosRestantes / 60
                            val segundos = segundosRestantes % 60

                            TiempoEsperaLlamado = String.format("%02d:%02d", minutos, segundos)
                            holder.TextViewTiempoEsperaLLamado.text = TiempoEsperaLlamado
                            actualizarFirebase(key, LTurno.Atraccion.toString())
                        }

                        override fun onFinish() {
                            TiempoEsperaLlamado = "00:00"
                            holder.TextViewTiempoEsperaLLamado.text = TiempoEsperaLlamado

                            BDLlamandoTurno.child(key).removeValue()

                            estadoBotonLlamar = "On"
                            estadoBotonCancelar = "Off"
                            actualizarFirebase(key, LTurno.Atraccion.toString())

                            holder.LlamarTurno.visibility = View.VISIBLE
                            holder.CancelarTurno.visibility = View.INVISIBLE
                            holder.TextViewTiempoEsperaLLamado.visibility = View.INVISIBLE

                            Toast.makeText(holder.itemView.context, "Tiempo de llamado terminado", Toast.LENGTH_SHORT).show()
                        }
                    }.start()
                }
                .addOnFailureListener {
                    Toast.makeText(holder.itemView.context, "Error al llamar al turno", Toast.LENGTH_SHORT).show()
                }
        }

        holder.CancelarTurno.setOnClickListener {
            BDLlamandoTurno.child(key).removeValue()
                .addOnSuccessListener {
                    countDownTimer?.cancel()
                    TiempoEsperaLlamado = "01:00"

                    estadoBotonLlamar = "On"
                    estadoBotonCancelar = "Off"
                    actualizarFirebase(key, LTurno.Atraccion.toString())

                    holder.TextViewTiempoEsperaLLamado.visibility = View.INVISIBLE
                    holder.LlamarTurno.visibility = View.VISIBLE
                    holder.CancelarTurno.visibility = View.INVISIBLE

                    Toast.makeText(holder.itemView.context, "Turno Cancelado...", Toast.LENGTH_SHORT).show()
                    BDDatosLlamadoTurno.child(LTurno.Atraccion.toString()).child(key).removeValue()
                }
                .addOnFailureListener {
                    Toast.makeText(holder.itemView.context, "Error al cancelar el turno", Toast.LENGTH_SHORT).show()
                }
        }

        BDDatosLlamadoTurno.child(LTurno.Atraccion.toString()).child(key)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val estadoLlamar = snapshot.child("BotonLLamar").getValue(String::class.java) ?: "On"
                    val estadoCancelar = snapshot.child("BotonCancelar").getValue(String::class.java) ?: "Off"
                    val tiempo = snapshot.child("Tiempo").getValue(String::class.java) ?: "01:00"

                    holder.TextViewTiempoEsperaLLamado.text = tiempo

                    holder.LlamarTurno.visibility = if (estadoLlamar == "On") View.VISIBLE else View.INVISIBLE
                    holder.CancelarTurno.visibility = if (estadoCancelar == "On") View.VISIBLE else View.INVISIBLE
                    holder.TextViewTiempoEsperaLLamado.visibility = if (tiempo != "01:00") View.VISIBLE else View.INVISIBLE
                }

                override fun onCancelled(error: DatabaseError) {}
            })


        val idGenerado = BDTurnos.push().key
        val map2: MutableMap<String, Any> = HashMap()
        map2["id"] = idGenerado.toString()
        map2["turno"] = LTurno.TurnoAsignado ?: ""
        map2["nombre"] = LTurno.Atraccion ?: ""
        map2["tiempo"] = LTurno.Tiempo ?: ""
        map2["estado"] = "Activo"

        holder.AceptarTurno.setOnClickListener {
            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setTitle("Advertencia")
            builder.setMessage("Confirmar Ingreso Turno ${LTurno.TurnoAsignado}?")

            builder.setPositiveButton("Aceptar") { dialog, _ ->
                BDDatosLlamadoTurno.child(LTurno.Atraccion.toString()).child(key).removeValue()

                BDTurnos.child(idGenerado.toString()).setValue(map2)
                    .addOnSuccessListener {
                        BDTurnosAcumulados
                            .child(LTurno.Id.toString())
                            .child("Estado")
                            .setValue("Finalizado")
                            .addOnCompleteListener {
                                Toast.makeText(
                                    holder.itemView.context,
                                    "Turno ${LTurno.TurnoAsignado} Recibido",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        BDLlamandoTurno.child(key).removeValue()
                        FirebaseDatabase.getInstance()
                            .getReference("TurnosActualesAtracciones")
                            .child("Mano Del Artesano")
                            .setValue(LTurno.TurnoAsignado ?: "")

                        // 🔹 Restar tiempo en TurnosAcumulados
                        val tiempoARestar = convertirATiempoSegundos(LTurno.Tiempo.toString())
                        val refTurnosAcumulados = FirebaseDatabase.getInstance().getReference("TurnosAcumulados")

                        refTurnosAcumulados.get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                for (child in snapshot.children) {
                                    val tiempoStr = child.child("TiempoEspera").getValue(String::class.java) ?: "00:00"
                                    val tiempoActual = convertirATiempoSegundos(tiempoStr)

                                    // Resta el tiempo (evita negativos)
                                    val nuevoTiempoSeg = (tiempoActual - tiempoARestar).coerceAtLeast(0)
                                    val nuevoTiempoStr = convertirAStrTiempo(nuevoTiempoSeg)

                                    // 🔹 Actualiza en Firebase
                                    child.ref.child("TiempoEspera").setValue(nuevoTiempoStr)
                                }
                            }
                        }

                        countDownTimer?.cancel()
                        TiempoEsperaLlamado = "01:00"
                        holder.TextViewTiempoEsperaLLamado.visibility = View.INVISIBLE
                        holder.LlamarTurno.visibility = View.VISIBLE
                        holder.CancelarTurno.visibility = View.INVISIBLE
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            holder.itemView.context,
                            "Error al llamar al turno",
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

        holder.EliminarTurno.setOnClickListener {

            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setTitle("Advertencia")
            builder.setMessage("Confirmar Cancelación Turno ${LTurno.TurnoAsignado}?")

            builder.setPositiveButton("Aceptar") { dialog, _ ->

                BDTurnosAcumulados
                    .child(LTurno.Id.toString())
                    .child("Estado")
                    .setValue("Cancelado")
                    .addOnCompleteListener {
                        Toast.makeText(
                            holder.itemView.context,
                            "Turno ${LTurno.TurnoAsignado} cancelado",
                            Toast.LENGTH_SHORT
                        ).show()

                        // 🔹 Restar tiempo en TurnosAcumulados
                        val tiempoARestar = convertirATiempoSegundos(LTurno.Tiempo.toString())
                        val refTurnosAcumulados = FirebaseDatabase.getInstance().getReference("TurnosAcumulados")

                        refTurnosAcumulados.get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                for (child in snapshot.children) {
                                    val tiempoStr = child.child("TiempoEspera").getValue(String::class.java) ?: "00:00"
                                    val tiempoActual = convertirATiempoSegundos(tiempoStr)

                                    // Resta el tiempo (evita negativos)
                                    val nuevoTiempoSeg = (tiempoActual - tiempoARestar).coerceAtLeast(0)
                                    val nuevoTiempoStr = convertirAStrTiempo(nuevoTiempoSeg)

                                    // 🔹 Actualiza en Firebase
                                    child.ref.child("TiempoEspera").setValue(nuevoTiempoStr)
                                }
                            }
                        }

                        // 🔹 Restar tiempo en TiempoAcumulado (tabla independiente)
                        val refTiempoAcum = FirebaseDatabase.getInstance().getReference("TiempoAcumulado")
                        refTiempoAcum.get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                val tiempoGlobalStr = snapshot.getValue(String::class.java) ?: "00:00"
                                val tiempoGlobal = convertirATiempoSegundos(tiempoGlobalStr)

                                val nuevoTiempoGlobal = (tiempoGlobal - tiempoARestar).coerceAtLeast(0)
                                val nuevoTiempoGlobalStr = convertirAStrTiempo(nuevoTiempoGlobal)

                                refTiempoAcum.setValue(nuevoTiempoGlobalStr)
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            holder.itemView.context,
                            "Error al cancelar turno",
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
        val TextViewTiempoEsperaLLamado: TextView = itemView.findViewById(R.id.TextViewTiempoEsperaLLamado)
        val tiempoTurno: TextView = itemView.findViewById(R.id.tiempoTurno)
        val tiempoEspera: TextView = itemView.findViewById(R.id.tiempoEspera)
        val personasTurno: TextView = itemView.findViewById(R.id.personasTurno)
        val TurnoAsignado: TextView = itemView.findViewById(R.id.TurnoAsignado)
        val LlamarTurno: ImageView = itemView.findViewById(R.id.LlamarTurno)
        val CancelarTurno: ImageView = itemView.findViewById(R.id.CancelarTurno)
        val AceptarTurno: ImageView = itemView.findViewById(R.id.AceptarTurno)
        val EliminarTurno: ImageView = itemView.findViewById(R.id.EliminarTurno)
        val VerificarInfoTurno: LinearLayout = itemView.findViewById(R.id.VerificarInfoTurno)
    }
}

// === Función utilitaria para convertir tiempo a segundos ===
fun convertirATiempoSegundos(tiempo: String): Int {
    val partes = tiempo.split(":")
    val horas = partes.getOrNull(0)?.toIntOrNull() ?: 0
    val minutos = partes.getOrNull(1)?.toIntOrNull() ?: 0
    return horas * 60 + minutos
}

// === Función utilitaria para convertir segundos a formato HH:MM ===
fun convertirAStrTiempo(segundos: Int): String {
    val horas = segundos / 60
    val minutos = segundos % 60
    return String.format("%02d:%02d", horas, minutos)
}

// === Nueva función reutilizable para restar tiempo ===
fun restarTiempoFirebase(ref: DatabaseReference, tiempoARestar: Int, esGlobal: Boolean = false) {
    ref.get().addOnSuccessListener { snapshot ->
        if (snapshot.exists()) {
            if (esGlobal) {
                // 🔹 Caso de un solo valor (ej: TiempoAcumulado)
                val tiempoStr = snapshot.getValue(String::class.java) ?: "00:00"
                val tiempoActual = convertirATiempoSegundos(tiempoStr)
                val nuevoTiempo = (tiempoActual - tiempoARestar).coerceAtLeast(0)
                ref.setValue(convertirAStrTiempo(nuevoTiempo))
            } else {
                // 🔹 Caso de lista (ej: TurnosAcumulados)
                for (child in snapshot.children) {
                    val tiempoStr = child.child("TiempoEspera").getValue(String::class.java) ?: "00:00"
                    val tiempoActual = convertirATiempoSegundos(tiempoStr)
                    val nuevoTiempo = (tiempoActual - tiempoARestar).coerceAtLeast(0)
                    child.ref.child("TiempoEspera").setValue(convertirAStrTiempo(nuevoTiempo))
                }
            }
        }
    }
}