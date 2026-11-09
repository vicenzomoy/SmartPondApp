package edu.uph.m24si2.smartpondapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotifikasiActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnClearAll: TextView

    private lateinit var btnFilterAll: TextView
    private lateinit var btnFilterAlerts: TextView
    private lateinit var btnFilterInfo: TextView

    private lateinit var rvNotifikasi: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout

    private lateinit var adapter: NotifikasiAdapter
    private val masterNotifList = mutableListOf<NotifikasiItem>()
    private var activeFilter = "all" // "all", "alert", "info"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifikasi)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainNotifikasi)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupRecyclerView()
        setupListeners()
        loadDefaultEssentialNotifications()
        listenRealtimeDatabase()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnClearAll = findViewById(R.id.btnClearAll)

        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterAlerts = findViewById(R.id.btnFilterAlerts)
        btnFilterInfo = findViewById(R.id.btnFilterInfo)

        rvNotifikasi = findViewById(R.id.rvNotifikasi)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
    }

    private fun setupRecyclerView() {
        adapter = NotifikasiAdapter(mutableListOf()) { item ->
            // Update unread status in list
            item.isRead = true
        }
        rvNotifikasi.layoutManager = LinearLayoutManager(this)
        rvRiwayatDataOrNotif()
    }

    private fun rvRiwayatDataOrNotif() {
        rvNotifikasi.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnClearAll.setOnClickListener {
            masterNotifList.clear()
            applyCurrentFilter()
            Toast.makeText(this, "Semua notifikasi dibersihkan", Toast.LENGTH_SHORT).show()
        }

        btnFilterAll.setOnClickListener { selectFilter("all") }
        btnFilterAlerts.setOnClickListener { selectFilter("alert") }
        btnFilterInfo.setOnClickListener { selectFilter("info") }
    }

    private fun selectFilter(filter: String) {
        activeFilter = filter

        val selectedBg = ContextCompat.getDrawable(this, R.drawable.bg_tab_selected)
        val unselectedBg = ContextCompat.getDrawable(this, R.drawable.bg_tab_unselected)
        val selectedColor = ContextCompat.getColor(this, android.R.color.white)
        val unselectedColor = ContextCompat.getColor(this, R.color.sp_muted)

        btnFilterAll.background = if (filter == "all") selectedBg else unselectedBg
        btnFilterAll.setTextColor(if (filter == "all") selectedColor else unselectedColor)

        btnFilterAlerts.background = if (filter == "alert") selectedBg else unselectedBg
        btnFilterAlerts.setTextColor(if (filter == "alert") selectedColor else unselectedColor)

        btnFilterInfo.background = if (filter == "info") selectedBg else unselectedBg
        btnFilterInfo.setTextColor(if (filter == "info") selectedColor else unselectedColor)

        applyCurrentFilter()
    }

    private fun applyCurrentFilter() {
        val filteredList = when (activeFilter) {
            "alert" -> masterNotifList.filter { it.type == "alert" || it.type == "warning" }
            "info" -> masterNotifList.filter { it.type == "info" || it.type == "success" }
            else -> masterNotifList
        }

        if (filteredList.isEmpty()) {
            rvNotifikasi.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            rvNotifikasi.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
            adapter.updateList(filteredList)
        }
    }

    private fun loadDefaultEssentialNotifications() {
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)

        val defaultItems = listOf(
            NotifikasiItem(
                id = "n1",
                title = "Suhu Air Optimal",
                message = "Suhu air kolam saat ini berada di rentang ideal 26.8 °C.",
                time = timeNow,
                type = "info",
                category = "suhu",
                isRead = false
            ),
            NotifikasiItem(
                id = "n2",
                title = "Level Air Normal",
                message = "Ketinggian air kolam berada pada kondisi aman (Normal).",
                time = "10:15",
                type = "info",
                category = "level_air",
                isRead = false
            ),
            NotifikasiItem(
                id = "n3",
                title = "Pengisian Daya Turbin",
                message = "Turbin angin menghasilkan daya 3.8 W untuk mendukung daya baterai.",
                time = "09:30",
                type = "info",
                category = "baterai",
                isRead = true
            ),
            NotifikasiItem(
                id = "n4",
                title = "Jadwal Pakan Pagi Selesai",
                message = "Pemberian pakan otomatis pukul 08:00 (Porsi 1x) telah berhasil dilaksanakan.",
                time = "08:01",
                type = "success",
                category = "feeder",
                isRead = true
            )
        )

        masterNotifList.clear()
        masterNotifList.addAll(defaultItems)
        applyCurrentFilter()
    }

    private fun listenRealtimeDatabase() {
        try {
            val database = FirebaseDatabase.getInstance("https://smartpondapp-default-rtdb.asia-southeast1.firebasedatabase.app/")
            val ref = database.getReference("SmartPond")

            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
                    val dynamicAlerts = mutableListOf<NotifikasiItem>()

                    val sensorNode = if (snapshot.hasChild("sensor")) snapshot.child("sensor") else snapshot

                    // 1. Suhu Air thresholds
                    val suhuVal = sensorNode.child("suhu").value?.toString()?.replace("°C", "")?.trim()?.toFloatOrNull()
                    if (suhuVal != null) {
                        if (suhuVal > 30.0f) {
                            dynamicAlerts.add(
                                NotifikasiItem(
                                    id = "alert_suhu_high",
                                    title = "Peringatan Suhu Tinggi!",
                                    message = "Suhu air kolam mencapai $suhuVal °C, melebihi batas ideal (24 - 30 °C). Segera cek pendinginan.",
                                    time = timeNow,
                                    type = "alert",
                                    category = "suhu",
                                    isRead = false
                                )
                            )
                        } else if (suhuVal < 24.0f) {
                            dynamicAlerts.add(
                                NotifikasiItem(
                                    id = "alert_suhu_low",
                                    title = "Peringatan Suhu Rendah!",
                                    message = "Suhu air kolam turun ke $suhuVal °C, di bawah batas ideal (24 - 30 °C).",
                                    time = timeNow,
                                    type = "alert",
                                    category = "suhu",
                                    isRead = false
                                )
                            )
                        }
                    }

                    // 2. Level Air alert
                    val statusAir = sensorNode.child("status_air").value?.toString()
                    if (statusAir != null && (statusAir.equals("Rendah", ignoreCase = true) || statusAir.equals("Low", ignoreCase = true))) {
                        dynamicAlerts.add(
                            NotifikasiItem(
                                id = "alert_level_low",
                                title = "Level Air Rendah!",
                                message = "Tingkat air kolam berada pada kondisi rendah. Harap segera lakukan pengisian air.",
                                time = timeNow,
                                type = "alert",
                                category = "level_air",
                                isRead = false
                            )
                        )
                    }

                    // 3. Battery alert
                    val batPercent = snapshot.child("baterai_persen").value?.toString()?.replace("%", "")?.trim()?.toIntOrNull()
                    if (batPercent != null && batPercent < 20) {
                        dynamicAlerts.add(
                            NotifikasiItem(
                                id = "alert_bat_low",
                                title = "Peringatan Baterai Lemah!",
                                message = "Kapasitas baterai tersisa $batPercent%. Harap periksa pengisian daya turbin angin.",
                                time = timeNow,
                                type = "warning",
                                category = "baterai",
                                isRead = false
                            )
                        )
                    }

                    // 4. Feeder feeding status
                    val isFeeding = snapshot.child("feed_now").getValue(Boolean::class.java) ?: false
                    if (isFeeding) {
                        dynamicAlerts.add(
                            NotifikasiItem(
                                id = "info_feeding",
                                title = "Memberi Pakan Berjalan",
                                message = "Perangkat otomatis Fish Feeder sedang memberikan pakan ke kolam.",
                                time = timeNow,
                                type = "info",
                                category = "feeder",
                                isRead = false
                            )
                        )
                    }

                    // Merge dynamic alerts into master list without duplicates
                    for (alert in dynamicAlerts) {
                        if (masterNotifList.none { it.id == alert.id }) {
                            masterNotifList.add(0, alert)
                        }
                    }

                    applyCurrentFilter()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("NotifikasiActivity", "Database error: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("NotifikasiActivity", "Firebase init error: ${e.message}")
        }
    }
}
