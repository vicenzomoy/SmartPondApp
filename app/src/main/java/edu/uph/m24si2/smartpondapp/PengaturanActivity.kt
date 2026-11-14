package edu.uph.m24si2.smartpondapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PengaturanActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvAppVersion: TextView
    private lateinit var tvAppDescription: TextView
    private lateinit var tvDeveloperInfo: TextView

    private lateinit var btnChangePassword: ConstraintLayout
    private lateinit var btnLogout: ConstraintLayout

    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pengaturan)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPengaturan)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        auth = FirebaseAuth.getInstance()

        initViews()
        setupListeners()
        setupBottomNavigation()
        listenRealtimeDatabase()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvAppVersion = findViewById(R.id.tvAppVersion)
        tvAppDescription = findViewById(R.id.tvAppDescription)
        tvDeveloperInfo = findViewById(R.id.tvDeveloperInfo)

        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnLogout = findViewById(R.id.btnLogout)

        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val user = auth.currentUser
        if (user == null || user.email == null) {
            Toast.makeText(this, "Sesi login tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val etOldPassword = dialogView.findViewById<EditText>(R.id.etOldPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val oldPass = etOldPassword.text.toString().trim()
                val newPass = etNewPassword.text.toString().trim()
                val confirmPass = etConfirmPassword.text.toString().trim()

                if (oldPass.isEmpty()) {
                    Toast.makeText(this, "Password lama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPass.length < 6) {
                    Toast.makeText(this, "Password baru minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPass != confirmPass) {
                    Toast.makeText(this, "Konfirmasi password tidak cocok!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)
                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        user.updatePassword(newPass)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Password berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { error ->
                                Toast.makeText(this, "Gagal memperbarui password: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Password lama tidak sesuai!", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Keluar")
            .setMessage("Apakah Anda yakin ingin keluar dari akun SmartPond?")
            .setPositiveButton("Ya, Keluar") { _, _ ->
                auth.signOut()
                Toast.makeText(this, "Berhasil keluar", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun listenRealtimeDatabase() {
        try {
            val database = FirebaseDatabase.getInstance("https://smartpondapp-default-rtdb.asia-southeast1.firebasedatabase.app/")
            val ref = database.getReference("SmartPond")

            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val appInfoNode = snapshot.child("app_info").takeIf { it.exists() }
                        ?: snapshot.child("info")

                    if (appInfoNode.exists()) {
                        val version = appInfoNode.child("version").value?.toString()
                        val description = appInfoNode.child("description").value?.toString()
                        val developer = appInfoNode.child("developer").value?.toString()

                        if (!version.isNullOrEmpty()) {
                            tvAppVersion.text = if (version.lowercase().startsWith("v")) version else "Versi $version"
                        }
                        if (!description.isNullOrEmpty()) {
                            tvAppDescription.text = description
                        }
                        if (!developer.isNullOrEmpty()) {
                            tvDeveloperInfo.text = developer
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PengaturanActivity", "Database error: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("PengaturanActivity", "Firebase init error: ${e.message}")
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(null)
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
                else -> false
            }
        }
    }
}
