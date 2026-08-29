package com.gloowalltapper

import android.accessibilityservice.AccessibilityService
import android.graphics.Path
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TapEngine(private val service: AccessibilityService) {

    companion object {
        private const val TAG = "TapEngine"
        private var tapJob: Job? = null
        var isTapping = false
            private set
        var isDisabled = false
            private set

        fun enable() { isDisabled = false }
        fun disable() { isDisabled = true }

        fun performSingleTap(x: Float, y: Float) {
            try {
                val svc = AccessibilityService.instance ?: return
                val path = Path().apply { moveTo(x, y) }
                val gesture = android.view.accessibility.GestureDescription.Builder()
                    .addStroke(android.view.accessibility.GestureDescription.StrokeDescription(path, 0, 1))
                    .build()
                svc.dispatchGesture(gesture, null, null)
            } catch (e: Exception) {
                Log.e(TAG, "Single tap failed", e)
            }
        }
    }

    fun startContinuousTap(x: Float, y: Float, intervalMs: Long = 50L) {
        if (isTapping) return
        isTapping = true
        tapJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && isTapping && !isDisabled) {
                try {
                    performSingleTap(x, y)
                } catch (e: Exception) {
                    Log.e(TAG, "Continuous tap error", e)
                }
                delay(intervalMs)
            }
        }
    }

    fun stopContinuousTap() {
        isTapping = false
        tapJob?.cancel()
        tapJob = null
    }

    fun cleanup() {
        stopContinuousTap()
        isDisabled = false
    }
}
