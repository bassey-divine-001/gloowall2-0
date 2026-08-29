package com.gloowalltapper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var toggleSwitch: View
    private lateinit var tvStatus: TextView
    private lateinit var btnColor1: Button
    private lateinit var btnColor2: Button
    private lateinit var seekSize1: SeekBar
    private lateinit var seekSize2: SeekBar
    private lateinit var seekInterval: SeekBar
    private lateinit var btnSaveSettings: Button

    private val prefs by lazy {
        getSharedPreferences("gloowalltapper_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toggleSwitch = findViewById(R.id.toggleSwitch)
        tvStatus = findViewById(R.id.tvStatus)
        btnColor1 = findViewById(R.id.btnColor1)
        btnColor2 = findViewById(R.id.btnColor2)
        seekSize1 = findViewById(R.id.seekSize1)
        seekSize2 = findViewById(R.id.seekSize2)
        seekInterval = findViewById(R.id.seekInterval)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)

        loadSettings()

        toggleSwitch.setOnClickListener {
            if (isServiceOn()) {
                stopFloatingService()
            } else {
                if (checkAndRequestPermissions()) {
                    startFloatingService()
                }
            }
        }

        btnColor1.setOnClickListener {
            val colors = arrayOf("#FFFF5252", "#FFFF9800", "#FFFFEB3B", "#FF4CAF50", "#FF2196F3", "#FF9C27B0")
            val current = prefs.getInt("btn1_color", 0xFFFF5252.toInt())
            val idx = colors.indexOfFirst { android.graphics.Color.parseColor(it) == current }
            val next = colors[(idx + 1) % colors.size]
            val nextColor = android.graphics.Color.parseColor(next)
            btnColor1.setBackgroundColor(nextColor)
            prefs.edit().putInt("btn1_color", nextColor).apply()
        }

        btnColor2.setOnClickListener {
            val colors = arrayOf("#FFFF5252", "#FFFF9800", "#FFFFEB3B", "#FF4CAF50", "#FF2196F3", "#FF9C27B0")
            val current = prefs.getInt("btn2_color", 0xFF2196F3.toInt())
            val idx = colors.indexOfFirst { android.graphics.Color.parseColor(it) == current }
            val next = colors[(idx + 1) % colors.size]
            val nextColor = android.graphics.Color.parseColor(next)
            btnColor2.setBackgroundColor(nextColor)
            prefs.edit().putInt("btn2_color", nextColor).apply()
        }

        btnSaveSettings.setOnClickListener {
            saveSettings()
            android.widget.Toast.makeText(this, "Settings saved", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateToggleState()
    }

    private fun loadSettings() {
        val size1 = prefs.getInt("btn1_size", 80)
        val size2 = prefs.getInt("btn2_size", 80)
        val interval = prefs.getInt("tap_interval", 50)

        seekSize1.progress = size1
        seekSize2.progress = size2
        seekInterval.progress = interval

        val c1 = prefs.getInt("btn1_color", 0xFFFF5252.toInt())
        val c2 = prefs.getInt("btn2_color", 0xFF2196F3.toInt())
        btnColor1.setBackgroundColor(c1)
        btnColor2.setBackgroundColor(c2)
    }

    private fun saveSettings() {
        prefs.edit()
            .putInt("btn1_size", seekSize1.progress)
            .putInt("btn2_size", seekSize2.progress)
            .putInt("tap_interval", seekInterval.progress)
            .apply()
    }

    private fun checkAndRequestPermissions(): Boolean {
        val overlayOk = Settings.canDrawOverlays(this)
        val accessibilityOk = isAccessibilityEnabled()

        if (!overlayOk) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
            return false
        }

        if (!accessibilityOk) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return false
        }

        return true
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains("com.gloowalltapper/.AccessibilityService") == true ||
                enabledServices?.contains("com.gloowalltapper.androidx") == true ||
                enabledServices?.contains("com.gloowalltapper") == true
    }

    private fun isServiceOn(): Boolean {
        return prefs.getBoolean("service_running", false)
    }

    private fun startFloatingService() {
        try {
            val intent = Intent(this, FloatingWindowService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            prefs.edit().putBoolean("service_running", true).apply()
            updateToggleState()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Failed to start service: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopFloatingService() {
        try {
            val intent = Intent(this, FloatingWindowService::class.java)
            stopService(intent)
            prefs.edit().putBoolean("service_running", false).apply()
            updateToggleState()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Failed to stop service", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateToggleState() {
        val running = isServiceOn()
        tvStatus.text = if (running) getString(R.string.main_switch_on) else getString(R.string.main_switch_off)
        toggleSwitch.isSelected = running
        val ivIcon = findViewById<TextView>(R.id.ivToggleIcon)
        ivIcon.text = if (running) "●" else "○"
    }
}
