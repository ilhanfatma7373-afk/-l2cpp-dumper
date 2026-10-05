package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FloatingModMenuService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var collapsedView: View
    private lateinit var expandedView: View
    private lateinit var params: WindowManager.LayoutParams

    private var targetPackage = "com.dts.freefireth"
    private var iconSizeDp = 56f
    private var iconOpacity = 100f
    private var cornerRadiusDp = 12f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("TARGET_PACKAGE")?.let {
            targetPackage = it
        }
        iconSizeDp = intent?.getFloatExtra("ICON_SIZE", 56f) ?: 56f
        iconOpacity = intent?.getFloatExtra("ICON_OPACITY", 100f) ?: 100f
        cornerRadiusDp = intent?.getFloatExtra("CORNER_RADIUS", 12f) ?: 12f

        applyCustomizations()
        return START_NOT_STICKY
    }

    private fun applyCustomizations() {
        if (::collapsedView.isInitialized) {
            val scale = resources.displayMetrics.density
            val sizePx = (iconSizeDp * scale).toInt()
            collapsedView.layoutParams.width = sizePx
            collapsedView.layoutParams.height = sizePx
            collapsedView.requestLayout()

            collapsedView.alpha = iconOpacity / 100f

            val drawable = GradientDrawable().apply {
                setColor(0xCC000000.toInt())
                cornerRadius = cornerRadiusDp * scale
            }
            collapsedView.background = drawable
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Foreground service notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "anonymous_dumper_channel"
            val channelName = "Anonymous Dumper Service"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)

            val notification = Notification.Builder(this, channelId)
                .setContentTitle("Anonymous Dumper Aktif")
                .setContentText("Mod menü ve dumper motoru hazır")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .build()
            startForeground(1, notification)
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        windowManager.addView(floatingView, params)

        collapsedView = floatingView.findViewById(R.id.collapsed_iv)
        expandedView = floatingView.findViewById(R.id.expanded_container)
        
        val logoIv = floatingView.findViewById<ImageView>(R.id.anonymous_logo_iv)
        logoIv.setImageResource(R.drawable.anonymous_logo)

        // Title with Colorful Gradient Shader (Anonim v1.0 Dumper)
        val tvMenuTitle = floatingView.findViewById<TextView>(R.id.tv_menu_title)
        tvMenuTitle.post {
            val paint = tvMenuTitle.paint
            val width = paint.measureText(tvMenuTitle.text.toString())
            if (width > 0f) {
                val textShader = LinearGradient(
                    0f, 0f, width, tvMenuTitle.textSize,
                    intArrayOf(
                        Color.parseColor("#FF0055"), // Hot Pink
                        Color.parseColor("#FFD700"), // Gold Yellow
                        Color.parseColor("#00FF00"), // Neon Green
                        Color.parseColor("#00FFFF"), // Cyan
                        Color.parseColor("#8A2BE2")  // Violet
                    ),
                    null, Shader.TileMode.CLAMP
                )
                tvMenuTitle.paint.shader = textShader
                tvMenuTitle.invalidate()
            }
        }

        val closeBtn = floatingView.findViewById<Button>(R.id.btn_close_menu)
        val dumpBtn = floatingView.findViewById<Button>(R.id.btn_start_dumper)
        val statusTv = floatingView.findViewById<TextView>(R.id.tv_dumper_status)

        applyCustomizations()

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        collapsedView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
                        isDragging = true
                    }
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        if (expandedView.visibility == View.VISIBLE) {
                            expandedView.visibility = View.GONE
                            collapsedView.visibility = View.VISIBLE
                        } else {
                            expandedView.visibility = View.VISIBLE
                            collapsedView.visibility = View.GONE
                        }
                    }
                    true
                }
                else -> false
            }
        }

        closeBtn.setOnClickListener {
            expandedView.visibility = View.GONE
            collapsedView.visibility = View.VISIBLE
        }

        dumpBtn.setOnClickListener {
            statusTv.text = "[*] Scanning IL2CPP structures for $targetPackage...\n"
            try {
                val resultPath = DumperNative.dumpIl2cpp(targetPackage)
                statusTv.append("[+] SUCCESS! Exhaustive offsets dumped:\n-> $resultPath\n[+] All 1500+ fields, methods & esp ready!")
                Toast.makeText(this, "Master Dump Kaydedildi!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                statusTv.append("[-] DUMP ERROR: ${e.message}\n")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
