package edu.uph.m24si2.smartpondapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class NotifikasiAdapter(
    private var notifList: MutableList<NotifikasiItem>
) : RecyclerView.Adapter<NotifikasiAdapter.NotifikasiViewHolder>() {

    class NotifikasiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: View = itemView.findViewById(R.id.cardNotifContainer)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivNotifIcon)
        val tvTitle: TextView = itemView.findViewById(R.id.tvNotifTitle)
        val tvMessage: TextView = itemView.findViewById(R.id.tvNotifMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvNotifTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifikasiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notifikasi, parent, false)
        return NotifikasiViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotifikasiViewHolder, position: Int) {
        val item = notifList[position]
        val context = holder.itemView.context

        holder.tvTitle.text = item.title
        holder.tvMessage.text = item.message
        holder.tvTime.text = item.time

        when (item.type.lowercase()) {
            "suhu", "suhu_alert" -> {
                holder.container.background = ContextCompat.getDrawable(context, R.drawable.bg_card_notif_red)
                holder.ivIcon.setImageResource(R.drawable.ic_notif_temp_red)
                holder.tvTime.setTextColor(Color.parseColor("#E53935"))
            }
            "kekeruhan", "kekeruhan_alert" -> {
                holder.container.background = ContextCompat.getDrawable(context, R.drawable.bg_card_notif_amber)
                holder.ivIcon.setImageResource(R.drawable.ic_notif_kekeruhan_amber)
                holder.tvTime.setTextColor(Color.parseColor("#D97706"))
            }
            "level", "level_air", "level_alert" -> {
                holder.container.background = ContextCompat.getDrawable(context, R.drawable.bg_card_notif_blue)
                holder.ivIcon.setImageResource(R.drawable.ic_notif_water_blue)
                holder.tvTime.setTextColor(Color.parseColor("#0288D1"))
            }
            "pompa", "pompa_info" -> {
                holder.container.background = ContextCompat.getDrawable(context, R.drawable.bg_card_notif_green)
                holder.ivIcon.setImageResource(R.drawable.ic_notif_pump_green)
                holder.tvTime.setTextColor(Color.parseColor("#20B978"))
            }
            else -> { // "feeder", "feeder_info", or default
                holder.container.background = ContextCompat.getDrawable(context, R.drawable.bg_card_notif_grey)
                holder.ivIcon.setImageResource(R.drawable.ic_notif_feeder_grey)
                holder.tvTime.setTextColor(Color.parseColor("#546E7A"))
            }
        }
    }

    override fun getItemCount(): Int = notifList.size

    fun updateData(newList: List<NotifikasiItem>) {
        notifList.clear()
        notifList.addAll(newList)
        notifyDataSetChanged()
    }
}
