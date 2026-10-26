package edu.uph.m24si2.smartpondapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class JadwalAdapter(
    private var jadwalList: MutableList<JadwalPakan>,
    private val onToggleChanged: (JadwalPakan, Boolean) -> Unit,
    private val onMenuClicked: (JadwalPakan, View) -> Unit
) : RecyclerView.Adapter<JadwalAdapter.JadwalViewHolder>() {

    class JadwalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tvJadwalTime)
        val switchJadwal: SwitchMaterial = itemView.findViewById(R.id.switchJadwal)
        val tvLabel: TextView = itemView.findViewById(R.id.tvJadwalLabel)
        val btnMenu: ImageButton = itemView.findViewById(R.id.btnJadwalMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JadwalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jadwal_pakan, parent, false)
        return JadwalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JadwalViewHolder, position: Int) {
        val item = jadwalList[position]

        holder.tvTime.text = item.time
        holder.tvLabel.text = item.fullLabel

        // Prevent unwanted listener triggering during bind
        holder.switchJadwal.setOnCheckedChangeListener(null)
        holder.switchJadwal.isChecked = item.enabled

        holder.switchJadwal.setOnCheckedChangeListener { _, isChecked ->
            item.enabled = isChecked
            onToggleChanged(item, isChecked)
        }

        holder.btnMenu.setOnClickListener { v ->
            onMenuClicked(item, v)
        }
    }

    override fun getItemCount(): Int = jadwalList.size

    fun updateData(newList: List<JadwalPakan>) {
        jadwalList.clear()
        jadwalList.addAll(newList)
        notifyDataSetChanged()
    }
}
