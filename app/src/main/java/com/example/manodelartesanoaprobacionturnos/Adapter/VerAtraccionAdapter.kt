package com.example.manodelartesanoaprobacionturnos.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.manodelartesanoaprobacionturnos.Model.VerAtraccionModel
import com.example.manodelartesanoaprobacionturnos.R

class VerAtraccionAdapter (
    private var atraccionLista: List<VerAtraccionModel>,
    private val onItemClick: (VerAtraccionModel) -> Unit
) : RecyclerView.Adapter<VerAtraccionAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.ver_atraccion_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val Latraccion = atraccionLista[position]

        holder.nombreAtraccion.text = Latraccion.Nombre

        holder.ContenidoAtraccion.setOnClickListener {
            onItemClick(Latraccion)
        }
    }

    override fun getItemCount(): Int {
        return atraccionLista.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreAtraccion: TextView = itemView.findViewById(R.id.nombreAtraccion)
        val ContenidoAtraccion: ConstraintLayout = itemView.findViewById(R.id.ContenidoAtraccion)
    }
}