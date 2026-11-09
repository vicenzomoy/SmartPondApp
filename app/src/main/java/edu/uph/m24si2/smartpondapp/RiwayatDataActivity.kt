package edu.uph.m24si2.smartpondapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RiwayatDataActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var cardDatePicker: ConstraintLayout
    private lateinit var tvSelectedDate: TextView

    private lateinit var tabSuhu: TextView
    private lateinit var tabLevelAir: TextView
    private lateinit var tabEnergi: TextView

    private lateinit var historyChartView: HistoryChartView
    private lateinit var rvRiwayatData: RecyclerView

    private lateinit var adapter: RiwayatAdapter
    private val riwayatList = mutableListOf<RiwayatData>()

    private val selectedCalendar: Calendar = Calendar.getInstance()
    private var selectedTabIndex = 0 // 0: Suhu, 1: Level Air, 2: Energi

    private var firestoreListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_riwayat_data)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRiwayatData)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        initViews()
        setupRecyclerView()
        setupListeners()

        updateDateDisplay()
        selectTab(0)
    }

    override fun onDestroy() {
        super.onDestroy()
        firestoreListenerRegistration?.remove()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        cardDatePicker = findViewById(R.id.cardDatePicker)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)

        tabSuhu = findViewById(R.id.tabSuhu)
        tabLevelAir = findViewById(R.id.tabLevelAir)
        tabEnergi = findViewById(R.id.tabEnergi)

        historyChartView = findViewById(R.id.historyChartView)
        rvRiwayatData = findViewById(R.id.rvRiwayatData)
    }

    private fun setupRecyclerView() {
        adapter = RiwayatAdapter(riwayatList)
        rvRiwayatData.layoutManager = LinearLayoutManager(this)
        rvRiwayatData.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        cardDatePicker.setOnClickListener {
            showDatePicker()
        }

        tabSuhu.setOnClickListener { selectTab(0) }
        tabLevelAir.setOnClickListener { selectTab(1) }
        tabEnergi.setOnClickListener { selectTab(2) }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                updateDateDisplay()
                loadHistoryDataFromFirestore()
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun updateDateDisplay() {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
        tvSelectedDate.text = sdf.format(selectedCalendar.time)
    }

    private fun selectTab(tabIndex: Int) {
        selectedTabIndex = tabIndex

        val tabs = listOf(tabSuhu, tabLevelAir, tabEnergi)
        for (i in tabs.indices) {
            val isSelected = (i == tabIndex)
            tabs[i].background = ContextCompat.getDrawable(this, if (isSelected) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
            tabs[i].setTextColor(if (isSelected) ContextCompat.getColor(this, android.R.color.white) else ContextCompat.getColor(this, R.color.sp_muted))
        }

        loadHistoryDataFromFirestore()
    }

    private fun loadHistoryDataFromFirestore() {
        firestoreListenerRegistration?.remove()

        val firestore = FirebaseFirestore.getInstance()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCalendar.time)

        // Read real-time history logs for selected date from Cloud Firestore collection "riwayat" -> doc(dateStr) -> subcollection "logs"
        firestoreListenerRegistration = firestore.collection("riwayat")
            .document(dateStr)
            .collection("logs")
            .orderBy("timestamp")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("RiwayatDataActivity", "Firestore error: ${error.message}")
                    listenRealtimeDatabaseFallback(dateStr)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val newList = mutableListOf<RiwayatData>()
                    val pointsList = mutableListOf<Float>()

                    for (doc in querySnapshot.documents) {
                        val jam = doc.getString("jam") ?: doc.id
                        when (selectedTabIndex) {
                            0 -> { // Suhu
                                val valNum = doc.getDouble("suhu")?.toFloat()
                                if (valNum != null) {
                                    newList.add(RiwayatData(jam, String.format(Locale.getDefault(), "%.1f °C", valNum)))
                                    pointsList.add(valNum)
                                }
                            }
                            1 -> { // Level Air
                                val valNum = doc.getDouble("level_air")?.toFloat()
                                if (valNum != null) {
                                    newList.add(RiwayatData(jam, String.format(Locale.getDefault(), "%.1f %%", valNum)))
                                    pointsList.add(valNum)
                                }
                            }
                            2 -> { // Energi
                                val valNum = doc.getDouble("energi")?.toFloat()
                                if (valNum != null) {
                                    newList.add(RiwayatData(jam, String.format(Locale.getDefault(), "%.1f W", valNum)))
                                    pointsList.add(valNum)
                                }
                            }
                        }
                    }

                    if (newList.isNotEmpty() && pointsList.isNotEmpty()) {
                        adapter.updateData(newList)
                        val colorHex = when (selectedTabIndex) {
                            0 -> "#FF5252"
                            1 -> "#0288D1"
                            else -> "#20B978"
                        }
                        historyChartView.setChartData(pointsList, colorHex)
                        return@addSnapshotListener
                    }
                }

                // If Firestore collection for this date is empty, fallback to RTDB or baseline data
                listenRealtimeDatabaseFallback(dateStr)
            }
    }

    private fun listenRealtimeDatabaseFallback(dateStr: String) {
        try {
            val database = FirebaseDatabase.getInstance("https://smartpondapp-default-rtdb.asia-southeast1.firebasedatabase.app/")
            val ref = database.getReference("SmartPond").child("riwayat")

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        loadFallbackBaselineData()
                        return
                    }

                    val sensorKey = when (selectedTabIndex) {
                        0 -> "suhu"
                        1 -> "level_air"
                        else -> "energi"
                    }

                    val targetNode = if (snapshot.hasChild(dateStr) && snapshot.child(dateStr).hasChild(sensorKey)) {
                        snapshot.child(dateStr).child(sensorKey)
                    } else if (snapshot.hasChild(sensorKey)) {
                        snapshot.child(sensorKey)
                    } else {
                        null
                    }

                    if (targetNode != null && targetNode.childrenCount > 0) {
                        val newList = mutableListOf<RiwayatData>()
                        val points = mutableListOf<Float>()

                        for (child in targetNode.children) {
                            val timeKey = child.key ?: continue
                            val valStr = child.value?.toString() ?: continue
                            newList.add(RiwayatData(timeKey, valStr))

                            val floatVal = valStr.replace(Regex("[^0-9.]"), "").toFloatOrNull()
                            if (floatVal != null) {
                                points.add(floatVal)
                            }
                        }

                        if (newList.isNotEmpty()) {
                            adapter.updateData(newList)
                            val colorHex = when (selectedTabIndex) {
                                0 -> "#FF5252"
                                1 -> "#0288D1"
                                else -> "#20B978"
                            }
                            historyChartView.setChartData(points, colorHex)
                            return
                        }
                    }

                    loadFallbackBaselineData()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("RiwayatDataActivity", "RTDB Fallback Error: ${error.message}")
                    loadFallbackBaselineData()
                }
            })
        } catch (e: Exception) {
            Log.e("RiwayatDataActivity", "Firebase init error: ${e.message}")
            loadFallbackBaselineData()
        }
    }

    private fun loadFallbackBaselineData() {
        val chartPoints: List<Float>
        val colorHex: String
        val detailDataList = mutableListOf<RiwayatData>()

        when (selectedTabIndex) {
            0 -> {
                colorHex = "#FF5252"
                chartPoints = listOf(25.0f, 24.3f, 24.0f, 24.2f, 23.8f, 24.1f, 24.9f, 25.8f, 26.2f, 25.9f, 26.8f, 27.5f, 28.6f, 27.2f, 26.1f, 25.7f, 26.3f, 25.5f, 24.9f, 25.4f, 25.9f, 26.1f, 25.8f)
                detailDataList.addAll(listOf(
                    RiwayatData("00:00", "25.3 °C"),
                    RiwayatData("01:00", "25.1 °C"),
                    RiwayatData("02:00", "25.0 °C"),
                    RiwayatData("03:00", "25.2 °C"),
                    RiwayatData("04:00", "24.8 °C"),
                    RiwayatData("05:00", "24.5 °C"),
                    RiwayatData("06:00", "24.9 °C"),
                    RiwayatData("07:00", "25.8 °C"),
                    RiwayatData("08:00", "26.3 °C"),
                    RiwayatData("09:00", "26.9 °C"),
                    RiwayatData("10:00", "27.5 °C"),
                    RiwayatData("11:00", "28.1 °C"),
                    RiwayatData("12:00", "28.6 °C"),
                    RiwayatData("13:00", "28.4 °C"),
                    RiwayatData("14:00", "27.8 °C"),
                    RiwayatData("15:00", "27.1 °C")
                ))
            }
            1 -> {
                colorHex = "#0288D1"
                chartPoints = listOf(90.0f, 91.5f, 92.0f, 94.0f, 95.0f, 95.5f, 96.0f, 95.0f, 94.5f, 95.0f)
                detailDataList.addAll(listOf(
                    RiwayatData("00:00", "90.0 %"),
                    RiwayatData("01:00", "91.5 %"),
                    RiwayatData("02:00", "92.0 %"),
                    RiwayatData("03:00", "94.0 %"),
                    RiwayatData("04:00", "95.0 %"),
                    RiwayatData("05:00", "95.5 %"),
                    RiwayatData("06:00", "96.0 %"),
                    RiwayatData("07:00", "95.0 %")
                ))
            }
            else -> {
                colorHex = "#20B978"
                chartPoints = listOf(3.6f, 2.1f, 2.3f, 4.2f, 2.8f, 2.0f, 2.2f, 4.9f, 6.5f, 4.0f, 6.4f, 3.8f, 5.0f, 2.8f, 3.8f)
                detailDataList.addAll(listOf(
                    RiwayatData("00:00", "3.6 W"),
                    RiwayatData("01:00", "2.1 W"),
                    RiwayatData("02:00", "2.3 W"),
                    RiwayatData("03:00", "4.2 W"),
                    RiwayatData("04:00", "2.8 W"),
                    RiwayatData("05:00", "2.0 W")
                ))
            }
        }

        historyChartView.setChartData(chartPoints, colorHex)
        adapter.updateData(detailDataList)
    }
}
