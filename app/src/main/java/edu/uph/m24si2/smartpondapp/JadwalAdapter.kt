package edu.uph.m24si2.smartpondapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class JadwalAdapter(
    private val jadwalList: MutableList<JadwalPakan>,
    private val onToggleChanged: (JadwalPakan, Boolean) -> Unit,
    private val onMenuClicked: (JadwalPakan, View) -> Unit
) : RecyclerView.Adapter<JadwalAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val switchJadwal: SwitchMaterial = itemView.findViewById(R.id.switchJadwal)
        val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)
        val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jadwal_pakan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = jadwalList[position]
        holder.tvTime.text = item.time
        holder.tvLabel.text = "${item.label} (${item.portion})"

        holder.switchJadwal.setOnCheckedChangeListener(null)
        holder.switchJadwal.isChecked = item.enabled

        holder.switchJadwal.setOnCheckedChangeListener { _, isChecked ->
            item.enabled = isChecked
            onToggleChanged(item, isChecked)
        }

        holder.btnMore.setOnClickListener { view ->
            onMenuClicked(item, view)
        }
    }

    override fun getItemCount(): Int = jadwalList.size
}
