package com.frameforge.fps

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Measures the FPS of the foreground app by reading SurfaceFlinger's per-layer
 * frame timestamps (`dumpsys SurfaceFlinger --latency <layer>`).
 * Needs the DUMP permission, granted once over ADB (see the app's main screen).
 */
class FpsReader(private val ctx: Context) {

    data class Sample(
        val fps: Float,
        val frameMs: Float,
        val recentFrameMs: List<Float>,   // for the graph
        val newFrameMs: List<Float>       // not yet logged, for 1% lows
    )

    private var layer: String? = null
    private var layerPkg: String? = null
    private var lastTs = 0L

    private fun sh(vararg cmd: String): String = try {
        val p = Runtime.getRuntime().exec(cmd)
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out
    } catch (e: Exception) { "" }

    fun hasDump(): Boolean =
        ctx.checkSelfPermission("android.permission.DUMP") == android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Foreground package via usage events (needs Usage Access). */
    fun foregroundPackage(): String? {
        val um = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val ev = um.queryEvents(now - 15_000, now)
        val e = UsageEvents.Event()
        var pkg: String? = null
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) pkg = e.packageName
            if (e.eventType == UsageEvents.Event.ACTIVITY_PAUSED && e.packageName == pkg) { /* keep last */ }
        }
        return pkg
    }

    private fun pickLayer(pkg: String): String? {
        val layers = sh("dumpsys", "SurfaceFlinger", "--list").lines().filter { it.isNotBlank() }
        val mine = layers.filter { it.contains(pkg) }
        return mine.firstOrNull { it.startsWith("SurfaceView") && it.contains("BLAST") }
            ?: mine.firstOrNull { it.startsWith("SurfaceView") }
            ?: mine.firstOrNull { it.contains("BLAST") }
            ?: mine.firstOrNull()
    }

    /** Returns null when nothing can be measured right now. */
    fun sample(pkg: String): Sample? {
        if (pkg != layerPkg || layer == null) {
            layer = pickLayer(pkg); layerPkg = pkg; lastTs = 0L
        }
        val l = layer ?: return null
        val lines = sh("dumpsys", "SurfaceFlinger", "--latency", l).lines()
        if (lines.size < 3) { layer = null; return null }
        val ts = lines.drop(1).mapNotNull {
            it.trim().split(Regex("\\s+")).getOrNull(1)?.toLongOrNull()
        }.filter { it > 0 && it < Long.MAX_VALUE / 2 }.sorted()
        if (ts.size < 2) return null

        val deltas = ts.zipWithNext { a, b -> (b - a) / 1_000_000f }
        val newest = ts.last()
        val window = ts.filter { it >= newest - 1_000_000_000L }
        // Stale buffer (game paused / static screen) -> 0 fps
        val stale = (System.nanoTime() - newest) > 0 && lines.size > 0 &&
            window.size < 2
        val fps = if (window.size >= 2)
            (window.size - 1) * 1e9f / (window.last() - window.first()) else 0f
        val fresh = ts.indices.filter { ts[it] > lastTs && it > 0 }.map { deltas[it - 1] }
        lastTs = newest
        return Sample(
            fps = if (stale) 0f else fps,
            frameMs = if (fps > 0) 1000f / fps else 0f,
            recentFrameMs = deltas.takeLast(60),
            newFrameMs = fresh
        )
    }
}
