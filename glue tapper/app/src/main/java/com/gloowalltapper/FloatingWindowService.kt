package com.gloowalltapper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FloatingWindowService : Service() {

    companion object {
        private const val CHANNEL_ID = "floating_service_channel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "FloatingWindowService"
        var instance: FloatingWindowService? = null
            private set
    }

    private lateinit var windowManager: WindowManager
    private var btn1: View? = null
    private var btn2: View? = null
    private var btn1Params: WindowManager.LayoutParams? = null
    private var btn2Params: WindowManager.LayoutParams? = null
    private var tapJob: Job? = null
    private var isBtn1Held = false
    private var isBtn2Held = false
    private var btn1StartX = 0f
    private var btn1StartY = 0f
    private var continuousStarted = false
    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        instance = this
        val prefs = getSharedPreferences("gloowalltapper_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("service_running", true).apply()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        removeViews()
        addFloatingBubbles()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopTapping()
        removeViews()
        longPressRunnable = null
        val prefs = getSharedPreferences("gloowalltapper_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("service_running", false).apply()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_started))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun addFloatingBubbles() {
        val prefs = getSharedPreferences("gloowalltapper_prefs", Context.MODE_PRIVATE)
        val btn1Color = prefs.getInt("btn1_color", 0xFFFF5252.toInt())
        val btn2Color = prefs.getInt("btn2_color", 0xFF2196F3.toInt())
        val btn1Size = prefs.getInt("btn1_size", 80)
        val btn2Size = prefs.getInt("btn2_size", 80)

        btn1 = createBubble(btn1Color, btn1Size, "1")
        btn2 = createBubble(btn2Color, btn2Size, "2")

        btn1?.let { windowManager.addView(it, btn1Params) }
        btn2?.let { windowManager.addView(it, btn2Params) }

        val display = windowManager.defaultDisplay
        val size = android.graphics.Point()
        display.getSize(size)
        btn2Params?.x = (size.x - btn2Size) - 80
        btn2Params?.y = 300

        btn1?.setOnTouchListener(BubbleTouchListener(btn1Params!!, "1") { x, y, isTap ->
            if (isTap && !isBtn2Held && !continuousStarted) {
                TapEngine.performSingleTap(x, y)
            }
            if (!isBtn1Held) {
                continuousStarted = false
                stopTapping()
            }
        })

        btn2?.setOnTouchListener(BubbleTouchListener(btn2Params!!, "2",
            { _, _, _ ->
                isBtn2Held = true
                stopTapping()
                continuousStarted = false
                cancelLongPressStart()
            },
            onUp = {
                if (isBtn1Held && !continuousStarted) {
                    continuousStarted = true
                    startContinuousTap(btn1StartX, btn1StartY)
                }
            }
        ))
    }

    private fun createBubble(color: Int, size: Int, tag: String): View {
        val safeSize = size.coerceIn(40, 150)
        val view = ImageView(this)
        view.setBackgroundColor(color)
        view.tag = tag
        val layoutParams = WindowManager.LayoutParams(
            safeSize,
            safeSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 100
        layoutParams.y = 400

        if (tag == "1") {
            btn1Params = layoutParams
        } else {
            btn2Params = layoutParams
        }

        return view
    }

    private fun startContinuousTap(x: Float, y: Float) {
        stopTapping()
        tapJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && isBtn1Held && !isBtn2Held) {
                try {
                    TapEngine.performSingleTap(x, y)
                } catch (e: Exception) {
                    Log.e(TAG, "Continuous tap error", e)
                }
                delay(getTapInterval())
            }
        }
    }

    private fun getTapInterval(): Long {
        val prefs = getSharedPreferences("gloowalltapper_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("tap_interval", 50).toLong()
    }

    private fun stopTapping() {
        tapJob?.cancel()
        tapJob = null
    }

    private fun removeViews() {
        stopTapping()
        btn1?.let { windowManager.removeView(it) }
        btn2?.let { windowManager.removeView(it) }
        btn1 = null
        btn2 = null
        btn1Params = null
        btn2Params = null
    }

    private fun postLongPressStart(x: Float, y: Float) {
        longPressRunnable = Runnable {
            if (isBtn1Held && !isBtn2Held && !continuousStarted) {
                continuousStarted = true
                startContinuousTap(x, y)
            }
        }
        handler.postDelayed(longPressRunnable!!, 300)
    }

    private fun cancelLongPressStart() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
    }

    private inner class BubbleTouchListener(
        private val params: WindowManager.LayoutParams,
        private val tag: String,
        private val onAction: (Float, Float, Boolean) -> Unit,
        private val onUp: (() -> Unit)? = null
    ) : View.OnTouchListener {

        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var isDragging = false
        private val touchSlop = ViewConfiguration.get(this@FloatingWindowService).scaledTouchSlop.toFloat()
        private var lastRawX = 0f
        private var lastRawY = 0f

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    if (tag == "1") {
                        btn1StartX = event.rawX
                        btn1StartY = event.rawY
                        isBtn1Held = true
                        continuousStarted = false
                        postLongPressStart(btn1StartX, btn1StartY)
                    } else {
                        isBtn2Held = true
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                        isDragging = true
                        cancelLongPressStart()
                    }
                    if (isDragging) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(v, params)
                    }
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    onAction(event.rawX, event.rawY, false)
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val wasTap = !isDragging
                    cancelLongPressStart()
                    if (tag == "1") {
                        isBtn1Held = false
                        stopTapping()
                        onAction(lastRawX, lastRawY, wasTap)
                    } else {
                        isBtn2Held = false
                        onAction(lastRawX, lastRawY, wasTap)
                        onUp?.invoke()
                    }
                    return true
                }
            }
            return false
        }
    }
}
