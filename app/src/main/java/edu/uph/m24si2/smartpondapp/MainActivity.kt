package edu.uph.m24si2.smartpondapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var btnMenu: ImageButton
    private lateinit var btnNotification: ImageButton
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

    private lateinit var ref: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        initViews()
        setupMenuNavigation()
        setupBottomNavigation()
        listenRealtimeDatabase()
    }

    private fun initViews() {
        btnMenu = findViewById(R.id.btnMenu)
        btnNotification = findViewById(R.id.btnNotification)
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

    private fun setupMenuNavigation() {
        btnMenu.setOnClickListener {
            startActivity(Intent(this, RiwayatDataActivity::class.java))
        }

        btnNotification.setOnClickListener {
            startActivity(Intent(this, NotifikasiActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(null)
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
        try {
            ref = FirebaseDatabase
                .getInstance("https://smartpondapp-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("SmartPond")

            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val sensorNode = if (snapshot.hasChild("sensor")) snapshot.child("sensor") else snapshot

                    // 1. Suhu Air
                    val suhu = sensorNode.child("suhu").value
                        ?: sensorNode.child("suhu_air").value
                    if (suhu != null) {
                        val suhuStr = suhu.toString()
                        tvSuhuValue.text = if (suhuStr.contains("°C")) suhuStr else "$suhuStr°C"
                    }

                    // 2. Kekeruhan
                    val kekeruhan = sensorNode.child("kekeruhan").value
                    if (kekeruhan != null) {
                        val kekeruhanStr = kekeruhan.toString()
                        tvKekeruhanValue.text = if (kekeruhanStr.contains("NTU")) kekeruhanStr else "$kekeruhanStr NTU"
                    }

                    val kekeruhanStatus = sensorNode.child("kekeruhan_status").value
                    if (kekeruhanStatus != null) {
                        tvKekeruhanStatusText.text = kekeruhanStatus.toString()
                    }

                    // 3. Level Air
                    val levelAir = sensorNode.child("status_air").value
                        ?: sensorNode.child("level_air").value
                    if (levelAir != null) {
                        val levelStr = levelAir.toString()
                        tvLevelAirValue.text = levelStr.replaceFirstChar { it.uppercaseChar() }
                    }

                    // 4. Baterai
                    val batVolt = snapshot.child("baterai_volt").value
                        ?: snapshot.child("baterai").child("volt").value
                    if (batVolt != null) {
                        val batVoltStr = batVolt.toString()
                        tvBateraiVolt.text = if (batVoltStr.lowercase().contains("v")) batVoltStr else "$batVoltStr v"
                    }

                    val batPercent = snapshot.child("baterai_persen").value
                        ?: snapshot.child("baterai").child("persen").value
                    if (batPercent != null) {
                        val percentInt = batPercent.toString().replace("%", "").trim().toIntOrNull()
                        if (percentInt != null) {
                            tvBateraiPercent.text = "$percentInt%"
                            pbBaterai.progress = percentInt.coerceIn(0, 100)
                        } else {
                            tvBateraiPercent.text = batPercent.toString()
                        }
                    }

                    // System status
                    val statusSys = snapshot.child("status_sistem").value
                    if (statusSys != null) {
                        tvSystemStatus.text = statusSys.toString()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity", "Database error: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase init error: ${e.message}")
        }
    }
}
