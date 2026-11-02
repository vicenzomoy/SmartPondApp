package edu.uph.m24si2.smartpondapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RiwayatAdapter(
    private var list: MutableList<RiwayatData>
) : RecyclerView.Adapter<RiwayatAdapter.RiwayatViewHolder>() {

    class RiwayatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tvRiwayatTime)
        val tvValue: TextView = itemView.findViewById(R.id.tvRiwayatValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiwayatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_data, parent, false)
        return RiwayatViewHolder(view)
    }

    override fun onBindViewHolder(holder: RiwayatViewHolder, position: Int) {
        val item = list[position]
        holder.tvTime.text = item.time
        holder.tvValue.text = item.value
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<RiwayatData>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
