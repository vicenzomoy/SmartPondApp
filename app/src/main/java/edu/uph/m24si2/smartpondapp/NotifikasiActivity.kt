package edu.uph.m24si2.smartpondapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotifikasiActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnHeaderSettings: ImageButton
    private lateinit var rvNotifikasi: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var adapter: NotifikasiAdapter
    private val notifList = mutableListOf<NotifikasiItem>()

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
        setupBottomNavigation()
        loadDefaultNotificationsIfEmpty()
        listenRealtimeDatabase()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnHeaderSettings = findViewById(R.id.btnHeaderSettings)
        rvNotifikasi = findViewById(R.id.rvNotifikasi)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupRecyclerView() {
        adapter = NotifikasiAdapter(notifList)
        rvNotifikasi.layoutManager = LinearLayoutManager(this)
        rvNotifikasi.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        btnHeaderSettings.setOnClickListener {
            startActivity(Intent(this, PengaturanActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun loadDefaultNotificationsIfEmpty() {
        if (notifList.isEmpty()) {
            val defaults = listOf(
                NotifikasiItem("notif_1", "Suhu Air Terlalu Tinggi", "Suhu mencapai 30.5 °C (di atas batas 30 °C)", "14:20", "suhu"),
                NotifikasiItem("notif_2", "Kekeruhan Meningkat", "Kekeruhan 35 NTU (di atas batas 30 NTU)", "10:15", "kekeruhan"),
                NotifikasiItem("notif_3", "Level Air Rendah", "Level air di bawah batas aman", "08:30", "level"),
                NotifikasiItem("notif_4", "Pompa Dinyalakan", "Mode otomatis", "08:00", "pompa"),
                NotifikasiItem("notif_5", "Pemberian Pakan Selesai", "Pakan telah diberikan (Porsi 1x)", "08:00", "feeder")
            )
            notifList.addAll(defaults)
            adapter.notifyDataSetChanged()
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
                } else {
                    snapshot
                }

                val notifNode = dataNode.child("notifikasi")
                    .takeIf { it.exists() }
                    ?: dataNode.child("notifications")

                if (notifNode != null && notifNode.exists()) {
                    val newList = mutableListOf<NotifikasiItem>()
                    for (child in notifNode.children) {
                        val id = child.key ?: continue
                        val title = child.child("title").value?.toString() ?: "Pemberitahuan"
                        val message = child.child("message").value?.toString() ?: child.child("pesan").value?.toString() ?: ""
                        val time = child.child("time").value?.toString() ?: child.child("jam").value?.toString() ?: ""
                        val type = child.child("type").value?.toString() ?: child.child("kategori").value?.toString() ?: "info"

                        newList.add(NotifikasiItem(id, title, message, time, type))
                    }

                    if (newList.isNotEmpty()) {
                        notifList.clear()
                        notifList.addAll(newList)
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotifikasiActivity", "Database error: ${error.message}")
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_beranda
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
