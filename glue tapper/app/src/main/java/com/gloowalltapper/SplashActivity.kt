package com.gloowalltapper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnOverlay = findViewById<Button>(R.id.btnOverlay)
        val btnAccessibility = findViewById<Button>(R.id.btnAccessibility)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        val prefs = getSharedPreferences("gloowalltapper_prefs", Context.MODE_PRIVATE)
        val onboardingComplete = prefs.getBoolean("onboarding_complete", false)

        checkPermissionsAndProceed { overlayOk, accessibilityOk ->
            runOnUiThread {
                if (!overlayOk) {
                    tvStatus.text = getString(R.string.permission_overlay_message)
                    progressBar.visibility = android.view.View.GONE
                    btnOverlay.visibility = android.view.View.VISIBLE
                    btnOverlay.setOnClickListener {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    }
                } else if (!accessibilityOk) {
                    tvStatus.text = getString(R.string.permission_accessibility_message)
                    progressBar.visibility = android.view.View.GONE
                    btnAccessibility.visibility = android.view.View.VISIBLE
                    btnAccessibility.setOnClickListener {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                } else {
                    if (!onboardingComplete) {
                        prefs.edit().putBoolean("onboarding_complete", true).apply()
                    }
                    progressBar.visibility = android.view.View.VISIBLE
                    tvStatus.text = "Loading..."
                    proceedToMain()
                }
            }
        }
    }

    private fun checkPermissionsAndProceed(callback: (Boolean, Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(800)
            val overlayOk = Settings.canDrawOverlays(this@SplashActivity)
            val accessibilityOk = isAccessibilityEnabled()
            callback(overlayOk, accessibilityOk)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val ourService = "com.gloowalltapper/.AccessibilityService"
        val ourServiceFull = "com.gloowalltapper.androidx"
        return enabledServices?.contains(ourService) == true ||
               enabledServices?.contains(ourServiceFull) == true
    }

    private fun proceedToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
