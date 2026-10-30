package com.frameforge.fps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var view: OverlayView
    private lateinit var lp: WindowManager.LayoutParams
    private lateinit var reader: FpsReader
    private lateinit var worker: HandlerThread
    private lateinit var bg: Handler
    private var running = false

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("ff", "Overlay", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "ff")
            .setContentTitle("FrameForge is running")
            .setContentText("Tap to open settings")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(open).build()
        startForeground(1, n)

        reader = FpsReader(this)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        view = OverlayView(this)
        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = Prefs.posX(this@OverlayService); y = Prefs.posY(this@OverlayService) }

        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0
        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = ox + (e.rawX - sx).toInt(); lp.y = oy + (e.rawY - sy).toInt()
                    wm.updateViewLayout(view, lp)
                }
                MotionEvent.ACTION_UP -> Prefs.setPos(this, lp.x, lp.y)
            }
            true
        }
        wm.addView(view, lp)

        SessionLog.clear()
        worker = HandlerThread("ff-sampler").also { it.start() }
        bg = Handler(worker.looper)
        running = true
        bg.post(tick)
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            try { sampleOnce() } catch (_: Throwable) { }
            bg.postDelayed(this, 1000)
        }
    }

    private fun sampleOnce() {
        val sys = SystemStats.read(this)
        val pkg = reader.foregroundPackage()
        val ready = reader.hasDump() && pkg != null && pkg != packageName
        val s = if (ready) reader.sample(pkg!!) else null

        val lines = mutableListOf<String>()
        if (!reader.hasDump()) lines += "DUMP not granted (see app)"
        else if (pkg == null) lines += "Usage access needed"
        else if (s == null) lines += "Waiting for frames…"
        if (s != null && Prefs.showStats(this)) {
            lines += "Frame %.1f ms".format(s.frameMs)
            val ft = s.recentFrameMs.sorted()
            if (ft.isNotEmpty()) lines += "Worst %.1f ms".format(ft.last())
        }
        if (Prefs.showSystem(this)) {
            lines += "%.1f °C  Bat %d%%  RAM %d%%".format(sys.tempC, sys.battery, sys.ramUsedPct)
            if (sys.throttling) lines += "⚠ THERMAL THROTTLING"
        }

        if (s != null && pkg != null)
            SessionLog.add(SessionLog.Row(System.currentTimeMillis(), pkg, s.fps, s.frameMs, sys.tempC, sys.battery, sys.ramUsedPct), s.newFrameMs)

        view.post {
            view.fps = s?.fps ?: 0f
            view.graph = s?.recentFrameMs ?: emptyList()
            view.lines = lines
            view.hot = sys.throttling
            view.showGraph = Prefs.showGraph(this)
            view.scale = Prefs.scale(this)
            view.bgAlpha = Prefs.opacity(this)
            view.requestLayout(); view.invalidate()
        }
    }

    override fun onDestroy() {
        running = false
        worker.quitSafely()
        runCatching { wm.removeView(view) }
        super.onDestroy()
    }
}
