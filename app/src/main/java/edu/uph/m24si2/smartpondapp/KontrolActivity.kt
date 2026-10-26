package edu.uph.m24si2.smartpondapp

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class KontrolActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnFeedNow: LinearLayout
    private lateinit var btnStop: LinearLayout
    private lateinit var tvFeederStatusText: TextView
    private lateinit var btnAddJadwal: LinearLayout
    private lateinit var rvJadwalPakan: RecyclerView

    private lateinit var adapter: JadwalAdapter
    private val jadwalList = mutableListOf<JadwalPakan>()

    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_kontrol)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainKontrol)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initFirebase()
        initViews()
        setupRecyclerView()
        setupListeners()
        setupBottomNavigation()
        loadDefaultSchedulesIfEmpty()
        listenRealtimeDatabase()
    }

    private fun initFirebase() {
        val database = FirebaseDatabase.getInstance()
        dbRef = database.reference
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnFeedNow = findViewById(R.id.btnFeedNow)
        btnStop = findViewById(R.id.btnStop)
        tvFeederStatusText = findViewById(R.id.tvFeederStatusText)
        btnAddJadwal = findViewById(R.id.btnAddJadwal)
        rvJadwalPakan = findViewById(R.id.rvJadwalPakan)
    }

    private fun setupRecyclerView() {
        adapter = JadwalAdapter(
            jadwalList = jadwalList,
            onToggleChanged = { item, isChecked ->
                onJadwalToggleChanged(item, isChecked)
            },
            onMenuClicked = { item, view ->
                showJadwalPopupMenu(item, view)
            }
        )
        rvJadwalPakan.layoutManager = LinearLayoutManager(this)
        rvJadwalPakan.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        btnFeedNow.setOnClickListener {
            sendFeedNowCommand()
        }

        btnStop.setOnClickListener {
            sendStopCommand()
        }

        btnAddJadwal.setOnClickListener {
            showAddOrEditJadwalDialog(null)
        }
    }

    private fun sendFeedNowCommand() {
        tvFeederStatusText.text = "Status: Memberi Pakan..."
        Toast.makeText(this, "Perintah Feed Now dikirim ke ESP32", Toast.LENGTH_SHORT).show()

        val updates = mapOf<String, Any>(
            "smartpond/feed_now" to true,
            "smartpond/feeder_status" to "FEEDING",
            "smartpond/last_feed_time" to System.currentTimeMillis()
        )
        dbRef.updateChildren(updates).addOnFailureListener { error ->
            Log.e("KontrolActivity", "Failed to send feed now: ${error.message}")
        }
    }

    private fun sendStopCommand() {
        tvFeederStatusText.text = "Status: Dihentikan"
        Toast.makeText(this, "Pemberian pakan dihentikan", Toast.LENGTH_SHORT).show()

        val updates = mapOf<String, Any>(
            "smartpond/feed_now" to false,
            "smartpond/feeder_status" to "STOPPED"
        )
        dbRef.updateChildren(updates).addOnFailureListener { error ->
            Log.e("KontrolActivity", "Failed to send stop command: ${error.message}")
        }
    }

    private fun loadDefaultSchedulesIfEmpty() {
        // Provide default schedules matching the screenshot requirement if offline or DB is empty
        if (jadwalList.isEmpty()) {
            val defaultList = listOf(
                JadwalPakan("sched_1", "08:00", "Pagi", "Porsi 1x", true),
                JadwalPakan("sched_2", "17:00", "Sore", "Porsi 1x", true),
                JadwalPakan("sched_3", "21:00", "Malam", "Porsi 1x", false)
            )
            jadwalList.addAll(defaultList)
            adapter.notifyDataSetChanged()
        }
    }

    private fun listenRealtimeDatabase() {
        // Listen to "smartpond" node in Firebase
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val dataNode = if (snapshot.hasChild("smartpond")) {
                    snapshot.child("smartpond")
                } else {
                    snapshot
                }

                // 1. Feeder Status & Feed Now flag
                val feedNowVal = dataNode.child("feed_now").value as? Boolean
                val feederStatusVal = dataNode.child("feeder_status").value?.toString()

                if (feederStatusVal != null) {
                    when (feederStatusVal.uppercase()) {
                        "FEEDING" -> tvFeederStatusText.text = "Status: Sedang Memberi Pakan"
                        "STOPPED" -> tvFeederStatusText.text = "Status: Dihentikan"
                        else -> tvFeederStatusText.text = "Status: $feederStatusVal"
                    }
                } else if (feedNowVal == true) {
                    tvFeederStatusText.text = "Status: Sedang Memberi Pakan"
                }

                // 2. Schedules
                val jadwalNode = dataNode.child("jadwal_pakan")
                if (jadwalNode.exists()) {
                    val newList = mutableListOf<JadwalPakan>()
                    for (child in jadwalNode.children) {
                        val id = child.key ?: continue
                        val time = child.child("time").value?.toString() ?: "08:00"
                        val label = child.child("label").value?.toString() ?: "Pakan"
                        val portion = child.child("portion").value?.toString() ?: "Porsi 1x"
                        val enabled = child.child("enabled").value as? Boolean
                            ?: (child.child("active").value as? Boolean) ?: true

                        newList.add(JadwalPakan(id, time, label, portion, enabled))
                    }

                    if (newList.isNotEmpty()) {
                        jadwalList.clear()
                        jadwalList.addAll(newList)
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("KontrolActivity", "Database error: ${error.message}")
            }
        })
    }

    private fun onJadwalToggleChanged(item: JadwalPakan, isChecked: Boolean) {
        val targetPath = "smartpond/jadwal_pakan/${item.id}/enabled"
        dbRef.child(targetPath).setValue(isChecked).addOnFailureListener {
            Toast.makeText(this, "Gagal mengubah status jadwal di Firebase", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showJadwalPopupMenu(item: JadwalPakan, anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menu.add("Edit")
        popup.menu.add("Hapus")
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "Edit" -> {
                    showAddOrEditJadwalDialog(item)
                    true
                }
                "Hapus" -> {
                    deleteJadwal(item)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun deleteJadwal(item: JadwalPakan) {
        jadwalList.remove(item)
        adapter.notifyDataSetChanged()

        val targetPath = "smartpond/jadwal_pakan/${item.id}"
        dbRef.child(targetPath).removeValue()
        Toast.makeText(this, "Jadwal ${item.time} dihapus", Toast.LENGTH_SHORT).show()
    }

    private fun showAddOrEditJadwalDialog(existingItem: JadwalPakan?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_jadwal, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvSelectTime = dialogView.findViewById<TextView>(R.id.tvSelectTime)
        val etJadwalLabel = dialogView.findViewById<EditText>(R.id.etJadwalLabel)
        val etJadwalPortion = dialogView.findViewById<EditText>(R.id.etJadwalPortion)

        val isEdit = existingItem != null
        tvDialogTitle.text = if (isEdit) "Edit Jadwal Pakan" else "Tambah Jadwal Pakan"

        var selectedTime = existingItem?.time ?: "08:00"
        tvSelectTime.text = selectedTime
        etJadwalLabel.setText(existingItem?.label ?: "Pagi")
        etJadwalPortion.setText(existingItem?.portion ?: "Porsi 1x")

        tvSelectTime.setOnClickListener {
            val parts = selectedTime.split(":")
            val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val timePickerDialog = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                    tvSelectTime.text = selectedTime
                },
                initialHour,
                initialMinute,
                true
            )
            timePickerDialog.show()
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val label = etJadwalLabel.text.toString().trim().ifEmpty { "Pagi" }
                val portion = etJadwalPortion.text.toString().trim().ifEmpty { "Porsi 1x" }
                val id = existingItem?.id ?: "sched_${System.currentTimeMillis()}"

                val newItem = JadwalPakan(
                    id = id,
                    time = selectedTime,
                    label = label,
                    portion = portion,
                    enabled = existingItem?.enabled ?: true
                )

                saveJadwalToFirebase(newItem)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveJadwalToFirebase(item: JadwalPakan) {
        val index = jadwalList.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            jadwalList[index] = item
        } else {
            jadwalList.add(item)
        }
        adapter.notifyDataSetChanged()

        val itemMap = mapOf(
            "id" to item.id,
            "time" to item.time,
            "label" to item.label,
            "portion" to item.portion,
            "enabled" to item.enabled
        )

        val targetPath = "smartpond/jadwal_pakan/${item.id}"
        dbRef.child(targetPath).setValue(itemMap).addOnSuccessListener {
            Toast.makeText(this, "Jadwal berhasil disimpan", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { error ->
            Log.e("KontrolActivity", "Failed to save schedule: ${error.message}")
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_kontrol
        bottomNav.setOnItemSelectedListener { item ->
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
                R.id.nav_kontrol -> true
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
