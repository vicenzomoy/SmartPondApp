package edu.uph.m24si2.smartpondapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
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
import java.util.Locale

class SensorActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tabSuhu: TextView
    private lateinit var tabKekeruhan: TextView
    private lateinit var tabLevelAir: TextView

    private lateinit var ivMainSensorIcon: ImageView
    private lateinit var tvMainSensorTitle: TextView
    private lateinit var tvMainSensorValue: TextView
    private lateinit var tvMainSensorSubtext: TextView
    private lateinit var tvMainSensorBadge: TextView

    private lateinit var btnTime6h: TextView
    private lateinit var btnTime1d: TextView
    private lateinit var btnTime1w: TextView
    private lateinit var btnTime1m: TextView

    private lateinit var sensorChartView: SensorChartView

    private lateinit var ivMinIcon: ImageView
    private lateinit var tvMinTitle: TextView
    private lateinit var tvMinVal: TextView
    private lateinit var tvMinTime: TextView

    private lateinit var ivMaxIcon: ImageView
    private lateinit var tvMaxTitle: TextView
    private lateinit var tvMaxVal: TextView
    private lateinit var tvMaxTime: TextView

    private lateinit var ivAvgIcon: ImageView
    private lateinit var tvAvgTitle: TextView
    private lateinit var tvAvgVal: TextView

    private lateinit var bottomNavigation: BottomNavigationView

    private var activeTab: String = "suhu" // "suhu", "kekeruhan", "level_air"
    private var activeTimeRange: String = "1d" // "6h", "1d", "1w", "1m"

    // Stored live RTDB data
    private var liveSuhuHistory: List<Float> = listOf(25.2f, 24.3f, 24.0f, 25.5f, 26.8f, 28.5f, 26.0f, 25.2f, 25.8f, 26.2f, 25.9f)
    private var liveKekeruhanHistory: List<Float> = listOf(14f, 13f, 11f, 12f, 12f, 15f, 13f, 12f, 12f, 13f, 12f)
    private var liveLevelAirHistory: List<Float> = listOf(90f, 92f, 95f, 94f, 96f, 95f, 97f, 95f, 96f, 95f, 95f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sensor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainSensor)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
        setupBottomNavigation()
        listenRealtimeDatabase()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tabSuhu = findViewById(R.id.tabSuhu)
        tabKekeruhan = findViewById(R.id.tabKekeruhan)
        tabLevelAir = findViewById(R.id.tabLevelAir)

        ivMainSensorIcon = findViewById(R.id.ivMainSensorIcon)
        tvMainSensorTitle = findViewById(R.id.tvMainSensorTitle)
        tvMainSensorValue = findViewById(R.id.tvMainSensorValue)
        tvMainSensorSubtext = findViewById(R.id.tvMainSensorSubtext)
        tvMainSensorBadge = findViewById(R.id.tvMainSensorBadge)

        btnTime6h = findViewById(R.id.btnTime6h)
        btnTime1d = findViewById(R.id.btnTime1d)
        btnTime1w = findViewById(R.id.btnTime1w)
        btnTime1m = findViewById(R.id.btnTime1m)

        sensorChartView = findViewById(R.id.sensorChartView)

        ivMinIcon = findViewById(R.id.ivMinIcon)
        tvMinTitle = findViewById(R.id.tvMinTitle)
        tvMinVal = findViewById(R.id.tvMinVal)
        tvMinTime = findViewById(R.id.tvMinTime)

        ivMaxIcon = findViewById(R.id.ivMaxIcon)
        tvMaxTitle = findViewById(R.id.tvMaxTitle)
        tvMaxVal = findViewById(R.id.tvMaxVal)
        tvMaxTime = findViewById(R.id.tvMaxTime)

        ivAvgIcon = findViewById(R.id.ivAvgIcon)
        tvAvgTitle = findViewById(R.id.tvAvgTitle)
        tvAvgVal = findViewById(R.id.tvAvgVal)

        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        tabSuhu.setOnClickListener { selectTab("suhu") }
        tabKekeruhan.setOnClickListener { selectTab("kekeruhan") }
        tabLevelAir.setOnClickListener { selectTab("level_air") }

        btnTime6h.setOnClickListener { selectTimeRange("6h") }
        btnTime1d.setOnClickListener { selectTimeRange("1d") }
        btnTime1w.setOnClickListener { selectTimeRange("1w") }
        btnTime1m.setOnClickListener { selectTimeRange("1m") }
    }

    private fun selectTab(tab: String) {
        activeTab = tab

        val selectedBg = ContextCompat.getDrawable(this, R.drawable.bg_tab_selected)
        val unselectedBg = ContextCompat.getDrawable(this, R.drawable.bg_tab_unselected)
        val selectedColor = ContextCompat.getColor(this, android.R.color.white)
        val unselectedColor = ContextCompat.getColor(this, R.color.sp_muted)

        tabSuhu.background = if (tab == "suhu") selectedBg else unselectedBg
        tabSuhu.setTextColor(if (tab == "suhu") selectedColor else unselectedColor)

        tabKekeruhan.background = if (tab == "kekeruhan") selectedBg else unselectedBg
        tabKekeruhan.setTextColor(if (tab == "kekeruhan") selectedColor else unselectedColor)

        tabLevelAir.background = if (tab == "level_air") selectedBg else unselectedBg
        tabLevelAir.setTextColor(if (tab == "level_air") selectedColor else unselectedColor)

        updateUI()
    }

    private fun selectTimeRange(range: String) {
        activeTimeRange = range

        val selectedBg = ContextCompat.getDrawable(this, R.drawable.bg_tab_selected)
        val unselectedBg = ContextCompat.getDrawable(this, R.drawable.bg_tab_unselected)
        val selectedColor = ContextCompat.getColor(this, android.R.color.white)
        val unselectedColor = ContextCompat.getColor(this, R.color.sp_muted)

        btnTime6h.background = if (range == "6h") selectedBg else unselectedBg
        btnTime6h.setTextColor(if (range == "6h") selectedColor else unselectedColor)

        btnTime1d.background = if (range == "1d") selectedBg else unselectedBg
        btnTime1d.setTextColor(if (range == "1d") selectedColor else unselectedColor)

        btnTime1w.background = if (range == "1w") selectedBg else unselectedBg
        btnTime1w.setTextColor(if (range == "1w") selectedColor else unselectedColor)

        btnTime1m.background = if (range == "1m") selectedBg else unselectedBg
        btnTime1m.setTextColor(if (range == "1m") selectedColor else unselectedColor)

        updateChart()
    }

    private fun updateUI() {
        when (activeTab) {
            "suhu" -> {
                ivMainSensorIcon.setImageResource(R.drawable.ic_thermometer)
                tvMainSensorTitle.text = "Suhu Air Saat Ini"
                tvMainSensorSubtext.text = "Rentang Ideal: 24 - 30 °C"

                tvMinTitle.text = "Suhu Terendah"
                ivMinIcon.setImageResource(R.drawable.ic_thermometer_blue)

                tvMaxTitle.text = "Suhu Tertinggi"
                ivMaxIcon.setImageResource(R.drawable.ic_thermometer_orange)

                tvAvgTitle.text = "Rata-rata Hari Ini"
                ivAvgIcon.setImageResource(R.drawable.ic_thermometer)
            }
            "kekeruhan" -> {
                ivMainSensorIcon.setImageResource(R.drawable.ic_waves)
                tvMainSensorTitle.text = "Kekeruhan Saat Ini"
                tvMainSensorSubtext.text = "Rentang Ideal: < 25 NTU"

                tvMinTitle.text = "Kekeruhan Terendah"
                ivMinIcon.setImageResource(R.drawable.ic_waves)

                tvMaxTitle.text = "Kekeruhan Tertinggi"
                ivMaxIcon.setImageResource(R.drawable.ic_waves)

                tvAvgTitle.text = "Rata-rata Hari Ini"
                ivAvgIcon.setImageResource(R.drawable.ic_waves)
            }
            "level_air" -> {
                ivMainSensorIcon.setImageResource(R.drawable.ic_droplet)
                tvMainSensorTitle.text = "Level Air Saat Ini"
                tvMainSensorSubtext.text = "Status: Normal"

                tvMinTitle.text = "Level Terendah"
                ivMinIcon.setImageResource(R.drawable.ic_droplet)

                tvMaxTitle.text = "Level Tertinggi"
                ivMaxIcon.setImageResource(R.drawable.ic_droplet)

                tvAvgTitle.text = "Rata-rata Hari Ini"
                ivAvgIcon.setImageResource(R.drawable.ic_droplet)
            }
        }
        updateChart()
    }

    private fun updateChart() {
        val (history, labels, suffix) = when (activeTab) {
            "kekeruhan" -> Triple(
                liveKekeruhanHistory,
                getTimeLabels(activeTimeRange),
                "NTU"
            )
            "level_air" -> Triple(
                liveLevelAirHistory,
                getTimeLabels(activeTimeRange),
                "%"
            )
            else -> Triple(
                liveSuhuHistory,
                getTimeLabels(activeTimeRange),
                "°C"
            )
        }

        val filteredPoints = filterPointsByTimeRange(history, activeTimeRange)
        sensorChartView.setData(filteredPoints, labels, suffix)
    }

    private fun getTimeLabels(range: String): List<String> {
        return when (range) {
            "6h" -> listOf("12:00", "13:30", "15:00", "16:30", "18:00")
            "1w" -> listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
            "1m" -> listOf("Minggu 1", "Minggu 2", "Minggu 3", "Minggu 4")
            else -> listOf("00:00", "06:00", "12:00", "18:00", "24:00")
        }
    }

    private fun filterPointsByTimeRange(list: List<Float>, range: String): List<Float> {
        if (list.isEmpty()) return listOf(25.0f, 26.0f, 27.0f)
        return when (range) {
            "6h" -> if (list.size >= 5) list.takeLast(5) else list
            "1w" -> if (list.size >= 7) list.takeLast(7) else list
            "1m" -> if (list.size >= 4) list.takeLast(4) else list
            else -> list
        }
    }

    private fun listenRealtimeDatabase() {
        val database = FirebaseDatabase.getInstance()
        val ref = database.reference

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val dataNode = if (snapshot.hasChild("smartpond")) {
                    snapshot.child("smartpond")
                } else if (snapshot.hasChild("pond")) {
                    snapshot.child("pond")
                } else {
                    snapshot
                }

                // 1. Current Temperature
                val suhuVal = dataNode.child("suhu").value
                    ?: dataNode.child("suhu_air").value
                    ?: dataNode.child("temperature").value
                if (suhuVal != null) {
                    val sStr = suhuVal.toString().replace("°C", "").trim()
                    val sFloat = sStr.toFloatOrNull()
                    if (sFloat != null) {
                        tvMainSensorValue.text = String.format(Locale.getDefault(), "%.1f °C", sFloat)
                    } else {
                        tvMainSensorValue.text = suhuVal.toString()
                    }
                }

                // 2. Min Temp & Time
                val minVal = dataNode.child("suhu_terendah").value
                    ?: dataNode.child("temp_min").value
                    ?: dataNode.child("min_temp").value
                if (minVal != null) {
                    val mStr = minVal.toString().replace("°C", "").trim()
                    val mFloat = mStr.toFloatOrNull()
                    if (mFloat != null) {
                        tvMinVal.text = String.format(Locale.getDefault(), "%.1f °C", mFloat)
                    } else {
                        tvMinVal.text = minVal.toString()
                    }
                }

                val minTime = dataNode.child("suhu_terendah_waktu").value
                    ?: dataNode.child("min_temp_time").value
                if (minTime != null) {
                    tvMinTime.text = minTime.toString()
                }

                // 3. Max Temp & Time
                val maxVal = dataNode.child("suhu_tertinggi").value
                    ?: dataNode.child("temp_max").value
                    ?: dataNode.child("max_temp").value
                if (maxVal != null) {
                    val mxStr = maxVal.toString().replace("°C", "").trim()
                    val mxFloat = mxStr.toFloatOrNull()
                    if (mxFloat != null) {
                        tvMaxVal.text = String.format(Locale.getDefault(), "%.1f °C", mxFloat)
                    } else {
                        tvMaxVal.text = maxVal.toString()
                    }
                }

                val maxTime = dataNode.child("suhu_tertinggi_waktu").value
                    ?: dataNode.child("max_temp_time").value
                if (maxTime != null) {
                    tvMaxTime.text = maxTime.toString()
                }

                // 4. Average Temp
                val avgVal = dataNode.child("suhu_ratarata").value
                    ?: dataNode.child("temp_avg").value
                    ?: dataNode.child("avg_temp").value
                if (avgVal != null) {
                    val aStr = avgVal.toString().replace("°C", "").trim()
                    val aFloat = aStr.toFloatOrNull()
                    if (aFloat != null) {
                        tvAvgVal.text = String.format(Locale.getDefault(), "%.1f °C", aFloat)
                    } else {
                        tvAvgVal.text = avgVal.toString()
                    }
                }

                // 5. Temperature History (Chart Points)
                val historyNode = dataNode.child("riwayat_suhu")
                if (historyNode.exists()) {
                    val pointsList = mutableListOf<Float>()
                    for (child in historyNode.children) {
                        val pVal = child.value?.toString()?.toFloatOrNull()
                        if (pVal != null) {
                            pointsList.add(pVal)
                        }
                    }
                    if (pointsList.isNotEmpty()) {
                        liveSuhuHistory = pointsList
                        updateChart()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SensorActivity", "Database error: ${error.message}")
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_sensor
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_sensor -> true
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
}
