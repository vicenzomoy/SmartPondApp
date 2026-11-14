package edu.uph.m24si2.smartpondapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EnergiActivity : AppCompatActivity() {

    private lateinit var btnNotification: ImageButton
    private lateinit var tvBateraiPercentMain: TextView
    private lateinit var tvBateraiVoltMain: TextView
    private lateinit var tvBateraiStatusBadge: TextView

    private lateinit var tvDayaTurbinValue: TextView
    private lateinit var tvEnergiHariIniValue: TextView

    private lateinit var btnFilterDayaMasuk: TextView
    private lateinit var btnFilterTegangan: TextView
    private lateinit var btnFilterEnergiHarian: TextView

    private lateinit var tvChartTitle: TextView
    private lateinit var energyChartView: EnergyChartView

    private lateinit var tvRataRataValue: TextView
    private lateinit var tvTotalEnergiValue: TextView

    private lateinit var bottomNavigation: BottomNavigationView

    private var selectedFilterIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_energi)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainEnergi)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        initViews()
        setupListeners()
        setupBottomNavigation()
        updateFilterTabs(0)
        listenRealtimeDatabase()
    }

    private fun initViews() {
        btnNotification = findViewById(R.id.btnNotification)
        tvBateraiPercentMain = findViewById(R.id.tvBateraiPercentMain)
        tvBateraiVoltMain = findViewById(R.id.tvBateraiVoltMain)
        tvBateraiStatusBadge = findViewById(R.id.tvBateraiStatusBadge)

        tvDayaTurbinValue = findViewById(R.id.tvDayaTurbinValue)
        tvEnergiHariIniValue = findViewById(R.id.tvEnergiHariIniValue)

        btnFilterDayaMasuk = findViewById(R.id.btnFilterDayaMasuk)
        btnFilterTegangan = findViewById(R.id.btnFilterTegangan)
        btnFilterEnergiHarian = findViewById(R.id.btnFilterEnergiHarian)

        tvChartTitle = findViewById(R.id.tvChartTitle)
        energyChartView = findViewById(R.id.energyChartView)

        tvRataRataValue = findViewById(R.id.tvRataRataValue)
        tvTotalEnergiValue = findViewById(R.id.tvTotalEnergiValue)

        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupListeners() {
        btnNotification.setOnClickListener {
            startActivity(Intent(this, NotifikasiActivity::class.java))
        }

        btnFilterDayaMasuk.setOnClickListener { updateFilterTabs(0) }
        btnFilterTegangan.setOnClickListener { updateFilterTabs(1) }
        btnFilterEnergiHarian.setOnClickListener { updateFilterTabs(2) }
    }

    private fun updateFilterTabs(index: Int) {
        selectedFilterIndex = index

        btnFilterDayaMasuk.background = ContextCompat.getDrawable(this, if (index == 0) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
        btnFilterDayaMasuk.setTextColor(if (index == 0) ContextCompat.getColor(this, android.R.color.white) else ContextCompat.getColor(this, R.color.sp_muted))

        btnFilterTegangan.background = ContextCompat.getDrawable(this, if (index == 1) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
        btnFilterTegangan.setTextColor(if (index == 1) ContextCompat.getColor(this, android.R.color.white) else ContextCompat.getColor(this, R.color.sp_muted))

        btnFilterEnergiHarian.background = ContextCompat.getDrawable(this, if (index == 2) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
        btnFilterEnergiHarian.setTextColor(if (index == 2) ContextCompat.getColor(this, android.R.color.white) else ContextCompat.getColor(this, R.color.sp_muted))

        when (index) {
            0 -> {
                tvChartTitle.text = "Grafik Daya Turbin"
                energyChartView.setData(
                    listOf(3.6f, 2.1f, 2.3f, 4.2f, 2.8f, 2.0f, 2.2f, 4.9f, 6.5f, 4.0f, 6.4f, 3.8f, 5.0f, 2.8f, 3.8f),
                    listOf("00:00", "06:00", "12:00", "18:00", "24:00"),
                    "W"
                )
            }
            1 -> {
                tvChartTitle.text = "Grafik Tegangan Baterai"
                energyChartView.setData(
                    listOf(12.2f, 12.1f, 12.0f, 12.3f, 12.5f, 12.4f, 12.3f, 12.6f, 12.7f, 12.5f, 12.4f),
                    listOf("00:00", "06:00", "12:00", "18:00", "24:00"),
                    "V"
                )
            }
            2 -> {
                tvChartTitle.text = "Grafik Energi Harian"
                energyChartView.setData(
                    listOf(1.5f, 2.0f, 3.2f, 4.5f, 6.0f, 7.8f, 9.1f, 10.5f, 11.2f, 12.5f),
                    listOf("00:00", "06:00", "12:00", "18:00", "24:00"),
                    "Wh"
                )
            }
        }
    }

    private fun listenRealtimeDatabase() {
        try {
            val database = FirebaseDatabase.getInstance("https://smartpondapp-default-rtdb.asia-southeast1.firebasedatabase.app/")
            val ref = database.getReference("SmartPond")

            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val hasUnreadNotif = snapshot.child("ada_notifikasi").value == true
                        || snapshot.child("notifikasi_unread").value == true
                    btnNotification.setImageResource(if (hasUnreadNotif) R.drawable.ic_bell_badge_24 else R.drawable.ic_bell_24)

                    // 1. Battery percent
                    val batPercent = snapshot.child("baterai_persen").value
                        ?: snapshot.child("baterai").child("persen").value
                    if (batPercent != null) {
                        val percentStr = batPercent.toString()
                        tvBateraiPercentMain.text = if (percentStr.endsWith("%")) percentStr else "$percentStr%"
                    }

                    // 2. Battery voltage
                    val batVolt = snapshot.child("baterai_volt").value
                        ?: snapshot.child("baterai").child("volt").value
                    if (batVolt != null) {
                        val voltStr = batVolt.toString()
                        tvBateraiVoltMain.text = if (voltStr.lowercase().contains("v")) voltStr else "$voltStr V"
                    }

                    // 3. Battery status
                    val batStatus = snapshot.child("baterai_status").value
                    if (batStatus != null) {
                        tvBateraiStatusBadge.text = batStatus.toString()
                    }

                    // 4. Daya turbin (power)
                    val dayaTurbin = snapshot.child("daya_turbin").value
                        ?: snapshot.child("energi").child("daya_turbin").value
                    if (dayaTurbin != null) {
                        val dayaStr = dayaTurbin.toString()
                        tvDayaTurbinValue.text = if (dayaStr.lowercase().contains("w")) dayaStr else "$dayaStr W"
                    }

                    // 5. Energi hari ini
                    val energiHariIni = snapshot.child("energi_hari_ini").value
                        ?: snapshot.child("energi").child("hari_ini").value
                    if (energiHariIni != null) {
                        val energiStr = energiHariIni.toString()
                        tvEnergiHariIniValue.text = if (energiStr.lowercase().contains("wh")) energiStr else "$energiStr Wh"
                    }

                    // 6. Rata-rata hari ini
                    val rataRata = snapshot.child("rata_rata_daya").value
                    if (rataRata != null) {
                        val avgStr = rataRata.toString()
                        tvRataRataValue.text = if (avgStr.lowercase().contains("w")) avgStr else "$avgStr W"
                    }

                    // 7. Total energi
                    val totalEnergi = snapshot.child("total_energi").value
                    if (totalEnergi != null) {
                        val totalStr = totalEnergi.toString()
                        tvTotalEnergiValue.text = if (totalStr.lowercase().contains("wh")) totalStr else "$totalStr Wh"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("EnergiActivity", "Database error: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("EnergiActivity", "Firebase init error: ${e.message}")
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(null)
        bottomNavigation.selectedItemId = R.id.nav_energi
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_sensor -> {
                    startActivity(Intent(this, SensorActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_kontrol -> {
                    startActivity(Intent(this, KontrolActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_energi -> true
                else -> false
            }
        }
    }
}
