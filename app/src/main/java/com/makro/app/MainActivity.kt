package com.makro.app

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btnAdd: Button
    private lateinit var btnToggle: Button
    private lateinit var tvStatus: TextView
    private lateinit var rvTargets: RecyclerView
    private lateinit var adapter: TargetAdapter

    private var isRunning = false

    // MediaProjection için request code
    private val REQ_MEDIA_PROJECTION = 100
    private val REQ_CROP = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnAdd    = findViewById(R.id.btnAdd)
        btnToggle = findViewById(R.id.btnToggle)
        tvStatus  = findViewById(R.id.tvStatus)
        rvTargets = findViewById(R.id.rvTargets)

        // Hedef görselleri listele
        adapter = TargetAdapter(getTargetFiles()) { file ->
            // Uzun basınca sil
            file.delete()
            refreshList()
            Toast.makeText(this, "${file.name} silindi", Toast.LENGTH_SHORT).show()
        }
        rvTargets.layoutManager = LinearLayoutManager(this)
        rvTargets.adapter = adapter

        // Görsel ekle: önce ekran görüntüsü al, sonra kırp
        btnAdd.setOnClickListener {
            checkAccessibilityAndProceed()
        }

        // Başlat / Durdur
        btnToggle.setOnClickListener {
            if (isRunning) stopMacro() else startMacro()
        }
    }

    // Erişilebilirlik servisi açık mı kontrol et
    private fun checkAccessibilityAndProceed() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this,
                "Lütfen Erişilebilirlik ayarlarından 'Makro' servisini aktif edin",
                Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else {
            requestScreenCapture()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = "${packageName}/${ClickService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(service)
    }

    // MediaProjection izni iste
    private fun requestScreenCapture() {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), REQ_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_MEDIA_PROJECTION -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // MediaProjection iznini CropActivity'ye gönder
                    val intent = Intent(this, CropActivity::class.java)
                    intent.putExtra("resultCode", resultCode)
                    intent.putExtra("data", data)
                    startActivityForResult(intent, REQ_CROP)
                }
            }
            REQ_CROP -> {
                if (resultCode == Activity.RESULT_OK) {
                    refreshList()
                    Toast.makeText(this, "Görsel kaydedildi!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startMacro() {
        if (getTargetFiles().isEmpty()) {
            Toast.makeText(this, "Önce en az bir görsel ekle", Toast.LENGTH_SHORT).show()
            return
        }

        // MacroService'i başlat
        val intent = Intent(this, MacroService::class.java)
        startForegroundService(intent)

        isRunning = true
        btnToggle.text = "⏹ Durdur"
        btnToggle.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#F44336"))
        tvStatus.text = "Çalışıyor... Ekranı kapatma!"
    }

    private fun stopMacro() {
        stopService(Intent(this, MacroService::class.java))

        isRunning = false
        btnToggle.text = "▶ Başlat"
        btnToggle.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#4CAF50"))
        tvStatus.text = "Durdu"
    }

    private fun getTargetFiles(): List<File> {
        val dir = File(filesDir, "targets")
        if (!dir.exists()) dir.mkdirs()
        return dir.listFiles()?.filter { it.extension == "png" } ?: emptyList()
    }

    private fun refreshList() {
        adapter.updateList(getTargetFiles())
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }
}
