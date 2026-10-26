package edu.uph.m24si2.smartpondapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var tvSystemStatus: TextView
    private lateinit var tvSystemConnection: TextView

    private lateinit var tvSuhuValue: TextView
    private lateinit var tvSuhuRange: TextView
    private lateinit var tvSuhuStatus: TextView

    private lateinit var tvKekeruhanValue: TextView
    private lateinit var tvKekeruhanStatusText: TextView
    private lateinit var tvKekeruhanStatus: TextView

    private lateinit var tvLevelAirValue: TextView

    private lateinit var tvBateraiVolt: TextView
    private lateinit var tvBateraiPercent: TextView
    private lateinit var pbBaterai: ProgressBar

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupBottomNavigation()
        listenRealtimeDatabase()
    }

    private fun initViews() {
        tvSystemStatus = findViewById(R.id.tvSystemStatus)
        tvSystemConnection = findViewById(R.id.tvSystemConnection)

        tvSuhuValue = findViewById(R.id.tvSuhuValue)
        tvSuhuRange = findViewById(R.id.tvSuhuRange)
        tvSuhuStatus = findViewById(R.id.tvSuhuStatus)

        tvKekeruhanValue = findViewById(R.id.tvKekeruhanValue)
        tvKekeruhanStatusText = findViewById(R.id.tvKekeruhanStatusText)
        tvKekeruhanStatus = findViewById(R.id.tvKekeruhanStatus)

        tvLevelAirValue = findViewById(R.id.tvLevelAirValue)

        tvBateraiVolt = findViewById(R.id.tvBateraiVolt)
        tvBateraiPercent = findViewById(R.id.tvBateraiPercent)
        pbBaterai = findViewById(R.id.pbBaterai)

        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_beranda
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> true
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
                R.id.nav_energi -> {
                    startActivity(Intent(this, EnergiActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_pengaturan -> {
                    startActivity(Intent(this, PengaturanActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun listenRealtimeDatabase() {
        val database = FirebaseDatabase.getInstance()
        val ref = database.reference

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                // Check if data is nested inside "smartpond" or directly under root
                val dataNode = if (snapshot.hasChild("smartpond")) {
                    snapshot.child("smartpond")
                } else if (snapshot.hasChild("pond")) {
                    snapshot.child("pond")
                } else {
                    snapshot
                }

                // 1. Suhu Air
                val suhu = dataNode.child("suhu").value
                    ?: dataNode.child("suhu_air").value
                    ?: dataNode.child("temperature").value
                if (suhu != null) {
                    val suhuStr = suhu.toString()
                    tvSuhuValue.text = if (suhuStr.contains("°C")) suhuStr else "$suhuStr°C"
                }

                // 2. Kekeruhan
                val kekeruhan = dataNode.child("kekeruhan").value
                    ?: dataNode.child("turbidity").value
                if (kekeruhan != null) {
                    val kekeruhanStr = kekeruhan.toString()
                    tvKekeruhanValue.text = if (kekeruhanStr.contains("NTU")) kekeruhanStr else "$kekeruhanStr NTU"
                }

                val kekeruhanStatus = dataNode.child("kekeruhan_status").value
                    ?: dataNode.child("turbidity_status").value
                if (kekeruhanStatus != null) {
                    tvKekeruhanStatusText.text = kekeruhanStatus.toString()
                }

                // 3. Level Air
                val levelAir = dataNode.child("level_air").value
                    ?: dataNode.child("water_level").value
                    ?: dataNode.child("levelAir").value
                if (levelAir != null) {
                    tvLevelAirValue.text = levelAir.toString()
                }

                // 4. Baterai
                val batVolt = dataNode.child("baterai_volt").value
                    ?: dataNode.child("voltage").value
                    ?: dataNode.child("baterai").value
                if (batVolt != null) {
                    val batVoltStr = batVolt.toString()
                    tvBateraiVolt.text = if (batVoltStr.lowercase().contains("v")) batVoltStr else "$batVoltStr v"
                }

                val batPercent = dataNode.child("baterai_persen").value
                    ?: dataNode.child("battery_percent").value
                    ?: dataNode.child("battery").value
                if (batPercent != null) {
                    val percentInt = batPercent.toString().replace("%", "").toIntOrNull()
                    if (percentInt != null) {
                        tvBateraiPercent.text = "$percentInt%"
                        pbBaterai.progress = percentInt
                    } else {
                        tvBateraiPercent.text = batPercent.toString()
                    }
                }

                // System status
                val statusSys = dataNode.child("status_sistem").value
                    ?: dataNode.child("system_status").value
                if (statusSys != null) {
                    tvSystemStatus.text = statusSys.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Database error: ${error.message}")
            }
        })
    }
}
