package com.frameforge.fps

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.util.Collections

/** Records every second of the session; computes avg/min/max/1% low; exports CSV. */
object SessionLog {
    data class Row(val t: Long, val pkg: String, val fps: Float, val frameMs: Float,
                   val tempC: Float, val battery: Int, val ramPct: Int)

    private val rows = Collections.synchronizedList(mutableListOf<Row>())
    private val frameTimes = Collections.synchronizedList(mutableListOf<Float>())

    fun clear() { rows.clear(); frameTimes.clear() }
    fun size() = rows.size

    fun add(r: Row, newFrames: List<Float>) {
        rows.add(r)
        frameTimes.addAll(newFrames)
        while (frameTimes.size > 60_000) frameTimes.removeAt(0)
    }

    fun summary(): String {
        val f = synchronized(rows) { rows.map { it.fps }.filter { it > 0 } }
        if (f.isEmpty()) return "No data yet. Start the overlay and open a game."
        val ft = synchronized(frameTimes) { frameTimes.sorted() }
        val low1 = if (ft.isNotEmpty()) 1000f / ft[(ft.size * 0.99).toInt().coerceAtMost(ft.size - 1)] else 0f
        val maxT = synchronized(rows) { rows.maxOf { it.tempC } }
        return "Avg %.1f  |  Min %.0f  |  Max %.0f  |  1%% low %.1f\nSamples: %d  |  Peak temp: %.1f °C"
            .format(f.average(), f.min(), f.max(), low1, f.size, maxT)
    }

    fun exportAndShare(c: Context) {
        val dir = File(c.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "frameforge_${System.currentTimeMillis()}.csv")
        file.bufferedWriter().use { w ->
            w.write("time_ms,package,fps,frame_ms,temp_c,battery_pct,ram_pct\n")
            synchronized(rows) { rows.forEach {
                w.write("${it.t},${it.pkg},${"%.1f".format(it.fps)},${"%.2f".format(it.frameMs)},${it.tempC},${it.battery},${it.ramPct}\n")
            } }
        }
        val uri = FileProvider.getUriForFile(c, "${c.packageName}.files", file)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        c.startActivity(Intent.createChooser(i, "Export session").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
