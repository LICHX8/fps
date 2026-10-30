package com.frameforge.fps

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

class OverlayView(ctx: Context) : View(ctx) {
    var fps = 0f
    var graph: List<Float> = emptyList()
    var lines: List<String> = emptyList()
    var hot = false
    var showGraph = true
    var scale = 1f
    var bgAlpha = 0.8f

    private val d = resources.displayMetrics.density
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.CYAN }

    private fun u(v: Float) = v * d * scale

    override fun onMeasure(w: Int, h: Int) {
        val gh = if (showGraph) u(44f) else 0f
        setMeasuredDimension(u(140f).toInt(), (u(46f) + lines.size * u(15f) + gh + u(8f)).toInt())
    }

    override fun onDraw(c: Canvas) {
        bg.color = Color.argb((bgAlpha * 255).toInt(), 12, 14, 20)
        c.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), u(10f), u(10f), bg)

        big.textSize = u(30f)
        big.color = when {
            hot -> Color.rgb(255, 82, 82)
            fps >= 55 -> Color.rgb(76, 217, 100)
            fps >= 30 -> Color.rgb(255, 204, 0)
            else -> Color.rgb(255, 82, 82)
        }
        c.drawText(if (fps > 0) "%.0f".format(fps) else "--", u(10f), u(34f), big)
        small.textSize = u(11f)
        c.drawText("FPS", u(10f) + big.measureText(if (fps > 0) "%.0f".format(fps) else "--") + u(4f), u(34f), small)

        var y = u(52f)
        small.textSize = u(11f)
        for (s in lines) { c.drawText(s, u(10f), y, small); y += u(15f) }

        if (showGraph && graph.size > 1) {
            val top = y; val h = u(36f); val w = width - u(20f)
            val max = 50f
            line.strokeWidth = u(1.5f)
            val p = Path()
            graph.forEachIndexed { i, ms ->
                val x = u(10f) + w * i / (graph.size - 1)
                val yy = top + h - h * (ms.coerceAtMost(max) / max)
                if (i == 0) p.moveTo(x, yy) else p.lineTo(x, yy)
            }
            c.drawPath(p, line)
        }
    }
}
