package edu.uph.m24si2.smartpondapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class JadwalPakanAdapter(
    private var list: MutableList<JadwalPakan>,
    private val onToggleChanged: (JadwalPakan, Boolean) -> Unit,
    private val onDeleteClicked: (JadwalPakan) -> Unit
) : RecyclerView.Adapter<JadwalPakanAdapter.ViewHolder>() {

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
        val item = list[position]
        holder.tvTime.text = item.time
        holder.tvLabel.text = "${item.label} (${item.portion})"

        // Remove listener temporarily before setting checked to avoid triggering callbacks during bind
        holder.switchJadwal.setOnCheckedChangeListener(null)
        holder.switchJadwal.isChecked = item.enabled

        holder.switchJadwal.setOnCheckedChangeListener { _, isChecked ->
            item.enabled = isChecked
            onToggleChanged(item, isChecked)
        }

        holder.btnMore.setOnClickListener {
            onDeleteClicked(item)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<JadwalPakan>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
