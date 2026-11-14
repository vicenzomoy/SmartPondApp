package edu.uph.m24si2.smartpondapp

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var btnSettings: ImageButton
    private lateinit var btnNotification: ImageButton
    private lateinit var viewStatusDot: View
    private lateinit var ivWifiIcon: ImageView
    private lateinit var tvSystemStatus: TextView
    private lateinit var tvSystemConnection: TextView

    private lateinit var tvSuhuValue: TextView
    private lateinit var tvSuhuRange: TextView
    private lateinit var tvSuhuStatus: TextView

    private lateinit var tvLevelAirValue: TextView

    private lateinit var tvBateraiVolt: TextView
    private lateinit var tvBateraiPercent: TextView
    private lateinit var pbBaterai: ProgressBar

    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var ref: DatabaseReference
    private var lastSavedTimeKey: String = ""

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
        btnSettings = findViewById(R.id.btnSettings)
        btnNotification = findViewById(R.id.btnNotification)
        viewStatusDot = findViewById(R.id.viewStatusDot)
        ivWifiIcon = findViewById(R.id.ivWifiIcon)
        tvSystemStatus = findViewById(R.id.tvSystemStatus)
        tvSystemConnection = findViewById(R.id.tvSystemConnection)

        tvSuhuValue = findViewById(R.id.tvSuhuValue)
        tvSuhuRange = findViewById(R.id.tvSuhuRange)
        tvSuhuStatus = findViewById(R.id.tvSuhuStatus)

        tvLevelAirValue = findViewById(R.id.tvLevelAirValue)

        tvBateraiVolt = findViewById(R.id.tvBateraiVolt)
        tvBateraiPercent = findViewById(R.id.tvBateraiPercent)
        pbBaterai = findViewById(R.id.pbBaterai)

        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupMenuNavigation() {
        btnSettings.setOnClickListener {
            startActivity(Intent(this, PengaturanActivity::class.java))
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
                else -> false
            }
        }
    }

    private fun listenRealtimeDatabase() {
        setSystemStatusState()

        try {
            val dbUrl = "https://smartpondapp-default-rtdb.asia-southeast1.firebasedatabase.app/"
            val db = FirebaseDatabase.getInstance(dbUrl)

            ref = db.getReference("SmartPond")
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val statusSysVal = snapshot.child("status_sistem").value?.toString()
                    setSystemStatusState(
                        statusText = statusSysVal ?: getString(R.string.status_system_online),
                        connectionText = getString(R.string.status_connected)
                    )

                    val hasUnreadNotif = snapshot.child("ada_notifikasi").value == true
                        || snapshot.child("notifikasi_unread").value == true
                    btnNotification.setImageResource(if (hasUnreadNotif) R.drawable.ic_bell_badge_24 else R.drawable.ic_bell_24)

                    val sensorNode = if (snapshot.hasChild("sensor")) snapshot.child("sensor") else snapshot

                    var suhuFloat: Float? = null
                    var levelAirFloat: Float? = null
                    var dayaFloat: Float? = null

                    // 1. Suhu Air
                    val suhu = sensorNode.child("suhu").value
                        ?: sensorNode.child("suhu_air").value
                    if (suhu != null) {
                        val suhuStr = suhu.toString()
                        val sNum = suhuStr.replace("°C", "").trim().toFloatOrNull()
                        if (sNum != null) {
                            suhuFloat = sNum
                            tvSuhuValue.text = String.format(Locale.getDefault(), "%.1f°C", sNum)
                        } else {
                            tvSuhuValue.text = if (suhuStr.contains("°C")) suhuStr else "$suhuStr°C"
                        }
                    }

                    // 2. Level Air
                    val levelAir = sensorNode.child("level_air").value
                        ?: sensorNode.child("level_air_percent").value
                        ?: sensorNode.child("status_air").value
                    if (levelAir != null) {
                        val levelStr = levelAir.toString()
                        val lNum = levelStr.replace("%", "").trim().toFloatOrNull()
                        if (lNum != null) {
                            levelAirFloat = lNum
                            tvLevelAirValue.text = String.format(Locale.getDefault(), "%.1f %%", lNum)
                        } else {
                            tvLevelAirValue.text = levelStr.replaceFirstChar { it.uppercaseChar() }
                        }
                    }

                    // 3. Baterai Volt & Percent
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

                    // 4. Daya Turbin
                    val dayaVal = snapshot.child("daya_turbin").value
                        ?: sensorNode.child("daya").value
                    if (dayaVal != null) {
                        dayaFloat = dayaVal.toString().replace("W", "").trim().toFloatOrNull()
                    }

                    // Project live data into Cloud Firestore history collection & RTDB
                    recordHistoryToFirestore(suhuFloat, levelAirFloat, dayaFloat)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity", "Database error: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase init error: ${e.message}")
        }
    }

    private fun recordHistoryToFirestore(
        suhuFloat: Float?,
        levelAirFloat: Float?,
        energiFloat: Float?
    ) {
        try {
            val now = Calendar.getInstance().time
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            val jamStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)

            if (jamStr == lastSavedTimeKey) return
            lastSavedTimeKey = jamStr

            val firestore = FirebaseFirestore.getInstance()
            val logData = hashMapOf<String, Any>(
                "timestamp" to com.google.firebase.Timestamp.now(),
                "date" to dateStr,
                "jam" to jamStr
            )

            suhuFloat?.let { logData["suhu"] = it }
            levelAirFloat?.let { logData["level_air"] = it }
            energiFloat?.let { logData["energi"] = it }

            // Write to Firestore collection "riwayat" -> doc(dateStr) -> subcollection "logs" -> doc(jamStr)
            firestore.collection("riwayat")
                .document(dateStr)
                .collection("logs")
                .document(jamStr)
                .set(logData, SetOptions.merge())

            // Also update Realtime Database under SmartPond/riwayat
            val riwayatRef = ref.child("riwayat")
            val updates = mutableMapOf<String, Any>()
            suhuFloat?.let {
                updates["$dateStr/suhu/$jamStr"] = "$it °C"
                updates["suhu/$jamStr"] = "$it °C"
            }
            levelAirFloat?.let {
                updates["$dateStr/level_air/$jamStr"] = "$it %"
                updates["level_air/$jamStr"] = "$it %"
            }
            energiFloat?.let {
                updates["$dateStr/energi/$jamStr"] = "$it W"
                updates["energi/$jamStr"] = "$it W"
            }
            if (updates.isNotEmpty()) {
                riwayatRef.updateChildren(updates)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "History recording error: ${e.message}")
        }
    }

    private fun setSystemStatusState(
        statusText: String? = null,
        connectionText: String? = null
    ) {
        val greenColor = ContextCompat.getColor(this, R.color.sp_green)
        tvSystemStatus.text = statusText ?: getString(R.string.status_system_online)
        tvSystemConnection.text = connectionText ?: getString(R.string.status_connected)
        tvSystemConnection.setTextColor(ContextCompat.getColor(this, R.color.sp_muted))

        viewStatusDot.backgroundTintList = ColorStateList.valueOf(greenColor)
        ivWifiIcon.imageTintList = ColorStateList.valueOf(greenColor)
    }
}
