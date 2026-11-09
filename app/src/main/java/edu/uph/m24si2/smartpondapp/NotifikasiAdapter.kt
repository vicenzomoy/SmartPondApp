package edu.uph.m24si2.smartpondapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class NotifikasiAdapter(
    private var list: MutableList<NotifikasiItem>,
    private val onItemClicked: (NotifikasiItem) -> Unit
) : RecyclerView.Adapter<NotifikasiAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val containerIcon: View = itemView.findViewById(R.id.containerIcon)
        val ivIconCategory: ImageView = itemView.findViewById(R.id.ivIconCategory)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        val viewUnreadDot: View = itemView.findViewById(R.id.viewUnreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notifikasi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val context = holder.itemView.context

        holder.tvTitle.text = item.title
        holder.tvMessage.text = item.message
        holder.tvTime.text = item.time

        // Show/hide unread indicator dot
        holder.viewUnreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE

        // Configure category icon and badge color based on type and category
        when (item.category.lowercase()) {
            "suhu" -> {
                holder.ivIconCategory.setImageResource(R.drawable.ic_thermometer)
                val bgColor = if (item.type == "alert") "#FFEBEE" else "#E6F7ED"
                val tintColor = if (item.type == "alert") "#FF3B30" else "#20B978"
                holder.containerIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
                holder.ivIconCategory.imageTintList = ColorStateList.valueOf(Color.parseColor(tintColor))
            }
            "level_air" -> {
                holder.ivIconCategory.setImageResource(R.drawable.ic_droplet)
                val bgColor = if (item.type == "alert") "#FFF3E0" else "#E1F5FE"
                val tintColor = if (item.type == "alert") "#FF9800" else "#0288D1"
                holder.containerIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
                holder.ivIconCategory.imageTintList = ColorStateList.valueOf(Color.parseColor(tintColor))
            }
            "baterai" -> {
                holder.ivIconCategory.setImageResource(R.drawable.ic_battery)
                val bgColor = if (item.type == "alert" || item.type == "warning") "#FFF8E1" else "#E6F7ED"
                val tintColor = if (item.type == "alert" || item.type == "warning") "#FFB300" else "#20B978"
                holder.containerIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
                holder.ivIconCategory.imageTintList = ColorStateList.valueOf(Color.parseColor(tintColor))
            }
            "feeder" -> {
                holder.ivIconCategory.setImageResource(R.drawable.ic_play_24)
                val bgColor = "#E8EAF6"
                val tintColor = "#3F51B5"
                holder.containerIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
                holder.ivIconCategory.imageTintList = ColorStateList.valueOf(Color.parseColor(tintColor))
            }
            else -> {
                holder.ivIconCategory.setImageResource(R.drawable.ic_bell_24)
                holder.containerIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F0F4F8"))
                holder.ivIconCategory.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sp_navy))
            }
        }

        holder.itemView.setOnClickListener {
            item.isRead = true
            notifyItemChanged(position)
            onItemClicked(item)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<NotifikasiItem>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
