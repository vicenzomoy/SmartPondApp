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

    private lateinit var btnNotification: ImageButton
    private lateinit var tabSuhu: TextView
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

    private var activeTab: String = "suhu" // "suhu", "level_air"
    private var activeTimeRange: String = "1d" // "6h", "1d", "1w", "1m"

    // Live sensor values
    private var currentSuhuFloat: Float = 26.8f
    private var minSuhuFloat: Float = 25.1f
    private var maxSuhuFloat: Float = 29.2f
    private var avgSuhuFloat: Float = 27.1f
    private var minSuhuTime: String = "04.30"
    private var maxSuhuTime: String = "14.20"

    private var currentLevelAirFloat: Float = 95.0f
    private var minLevelAirFloat: Float = 90.0f
    private var maxLevelAirFloat: Float = 98.0f
    private var avgLevelAirFloat: Float = 94.5f
    private var minLevelAirTime: String = "03.15"
    private var maxLevelAirTime: String = "11.45"

    private var liveSuhuHistory: List<Float> = listOf(25.2f, 24.3f, 24.0f, 25.5f, 26.8f, 28.5f, 26.0f, 25.2f, 25.8f, 26.2f, 25.9f)
    private var liveLevelAirHistory: List<Float> = listOf(90.0f, 92.0f, 95.0f, 94.0f, 96.0f, 95.0f, 97.0f, 95.0f, 96.0f, 95.0f, 95.0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sensor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainSensor)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        initViews()
        setupListeners()
        setupBottomNavigation()
        listenRealtimeDatabase()
    }

    private fun initViews() {
        btnNotification = findViewById(R.id.btnNotification)
        tabSuhu = findViewById(R.id.tabSuhu)
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
        btnNotification.setOnClickListener {
            startActivity(Intent(this, NotifikasiActivity::class.java))
        }

        tabSuhu.setOnClickListener { selectTab("suhu") }
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
                tvMainSensorValue.text = String.format(Locale.getDefault(), "%.1f °C", currentSuhuFloat)
                tvMainSensorSubtext.text = "Rentang Ideal: 24 - 30 °C"
                tvMainSensorBadge.text = if (currentSuhuFloat in 24.0f..30.0f) "Normal" else "Perlu Cek"

                tvMinTitle.text = "Suhu Terendah"
                ivMinIcon.setImageResource(R.drawable.ic_thermometer_blue)
                tvMinVal.text = String.format(Locale.getDefault(), "%.1f °C", minSuhuFloat)
                tvMinTime.text = minSuhuTime

                tvMaxTitle.text = "Suhu Tertinggi"
                ivMaxIcon.setImageResource(R.drawable.ic_thermometer_orange)
                tvMaxVal.text = String.format(Locale.getDefault(), "%.1f °C", maxSuhuFloat)
                tvMaxTime.text = maxSuhuTime

                tvAvgTitle.text = "Rata-rata Hari Ini"
                ivAvgIcon.setImageResource(R.drawable.ic_thermometer)
                tvAvgVal.text = String.format(Locale.getDefault(), "%.1f °C", avgSuhuFloat)
            }
            "level_air" -> {
                ivMainSensorIcon.setImageResource(R.drawable.ic_droplet)
                tvMainSensorTitle.text = "Level Air Saat Ini"
                tvMainSensorValue.text = String.format(Locale.getDefault(), "%.1f %%", currentLevelAirFloat)
                tvMainSensorSubtext.text = "Rentang Ideal: 70 - 100 %"
                tvMainSensorBadge.text = if (currentLevelAirFloat >= 50.0f) "Normal" else "Rendah"

                tvMinTitle.text = "Level Terendah"
                ivMinIcon.setImageResource(R.drawable.ic_droplet)
                tvMinVal.text = String.format(Locale.getDefault(), "%.1f %%", minLevelAirFloat)
                tvMinTime.text = minLevelAirTime

                tvMaxTitle.text = "Level Tertinggi"
                ivMaxIcon.setImageResource(R.drawable.ic_droplet)
                tvMaxVal.text = String.format(Locale.getDefault(), "%.1f %%", maxLevelAirFloat)
                tvMaxTime.text = maxLevelAirTime

                tvAvgTitle.text = "Rata-rata Hari Ini"
                ivAvgIcon.setImageResource(R.drawable.ic_droplet)
                tvAvgVal.text = String.format(Locale.getDefault(), "%.1f %%", avgLevelAirFloat)
            }
        }
        updateChart()
    }

    private fun updateChart() {
        val (history, labels, suffix) = when (activeTab) {
            "level_air" -> Triple(liveLevelAirHistory, getTimeLabels(activeTimeRange), "%")
            else -> Triple(liveSuhuHistory, getTimeLabels(activeTimeRange), "°C")
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
        try {
            val database = FirebaseDatabase.getInstance("https://smartpondapp-default-rtdb.asia-southeast1.firebasedatabase.app/")
            val ref = database.getReference("SmartPond")

            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val hasUnreadNotif = snapshot.child("ada_notifikasi").value == true
                        || snapshot.child("notifikasi_unread").value == true
                    btnNotification.setImageResource(if (hasUnreadNotif) R.drawable.ic_bell_badge_24 else R.drawable.ic_bell_24)

                    val sensorNode = if (snapshot.hasChild("sensor")) snapshot.child("sensor") else snapshot

                    // 1. Current Temperature
                    val suhuVal = sensorNode.child("suhu").value
                        ?: sensorNode.child("suhu_air").value
                    if (suhuVal != null) {
                        val sStr = suhuVal.toString().replace("°C", "").trim()
                        val sFloat = sStr.toFloatOrNull()
                        if (sFloat != null) {
                            currentSuhuFloat = sFloat
                        }
                    }

                    // 2. Current Level Air (Float percentage 0-100%)
                    val levelVal = sensorNode.child("level_air").value
                        ?: sensorNode.child("level_air_percent").value
                        ?: sensorNode.child("status_air").value
                    if (levelVal != null) {
                        val lStr = levelVal.toString().replace("%", "").trim()
                        val lFloat = lStr.toFloatOrNull()
                        if (lFloat != null) {
                            currentLevelAirFloat = lFloat
                        }
                    }

                    // 3. Min/Max/Avg Suhu
                    val minS = sensorNode.child("suhu_terendah").value ?: sensorNode.child("temp_min").value
                    if (minS != null) minSuhuFloat = minS.toString().replace("°C", "").trim().toFloatOrNull() ?: minSuhuFloat

                    val maxS = sensorNode.child("suhu_tertinggi").value ?: sensorNode.child("temp_max").value
                    if (maxS != null) maxSuhuFloat = maxS.toString().replace("°C", "").trim().toFloatOrNull() ?: maxSuhuFloat

                    val avgS = sensorNode.child("suhu_ratarata").value ?: sensorNode.child("temp_avg").value
                    if (avgS != null) avgSuhuFloat = avgS.toString().replace("°C", "").trim().toFloatOrNull() ?: avgSuhuFloat

                    val minSTimeVal = sensorNode.child("suhu_terendah_waktu").value
                    if (minSTimeVal != null) minSuhuTime = minSTimeVal.toString()

                    val maxSTimeVal = sensorNode.child("suhu_tertinggi_waktu").value
                    if (maxSTimeVal != null) maxSuhuTime = maxSTimeVal.toString()

                    // 4. Min/Max/Avg Level Air
                    val minL = sensorNode.child("level_air_terendah").value ?: sensorNode.child("level_min").value
                    if (minL != null) minLevelAirFloat = minL.toString().replace("%", "").trim().toFloatOrNull() ?: minLevelAirFloat

                    val maxL = sensorNode.child("level_air_tertinggi").value ?: sensorNode.child("level_max").value
                    if (maxL != null) maxLevelAirFloat = maxL.toString().replace("%", "").trim().toFloatOrNull() ?: maxLevelAirFloat

                    val avgL = sensorNode.child("level_air_ratarata").value ?: sensorNode.child("level_avg").value
                    if (avgL != null) avgLevelAirFloat = avgL.toString().replace("%", "").trim().toFloatOrNull() ?: avgLevelAirFloat

                    val minLTimeVal = sensorNode.child("level_air_terendah_waktu").value
                    if (minLTimeVal != null) minLevelAirTime = minLTimeVal.toString()

                    val maxLTimeVal = sensorNode.child("level_air_tertinggi_waktu").value
                    if (maxLTimeVal != null) maxLevelAirTime = maxLTimeVal.toString()

                    // 5. Suhu History
                    val historySuhuNode = sensorNode.child("riwayat_suhu")
                    if (historySuhuNode.exists()) {
                        val pointsList = mutableListOf<Float>()
                        for (child in historySuhuNode.children) {
                            val pVal = child.value?.toString()?.replace("°C", "")?.toFloatOrNull()
                            if (pVal != null) {
                                pointsList.add(pVal)
                            }
                        }
                        if (pointsList.isNotEmpty()) {
                            liveSuhuHistory = pointsList
                        }
                    }

                    // 6. Level Air History
                    val historyLevelNode = sensorNode.child("riwayat_level_air")
                    if (historyLevelNode.exists()) {
                        val pointsList = mutableListOf<Float>()
                        for (child in historyLevelNode.children) {
                            val pVal = child.value?.toString()?.replace("%", "")?.toFloatOrNull()
                            if (pVal != null) {
                                pointsList.add(pVal)
                            }
                        }
                        if (pointsList.isNotEmpty()) {
                            liveLevelAirHistory = pointsList
                        }
                    }

                    updateUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SensorActivity", "Database error: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("SensorActivity", "Firebase init error: ${e.message}")
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(null)
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
