package com.gloowalltapper

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AccessibilityService : AccessibilityService() {

    companion object {
        var instance: AccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Gestures only; no event interception needed.
    }

    override fun onInterrupt() {
        TapEngine.cleanup()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        TapEngine.cleanup()
    }
}
