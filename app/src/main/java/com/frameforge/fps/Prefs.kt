package com.frameforge.fps

import android.content.Context

/** Overlay settings, persisted in SharedPreferences. */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("frameforge", Context.MODE_PRIVATE)

    fun opacity(c: Context) = sp(c).getFloat("opacity", 0.8f)
    fun scale(c: Context) = sp(c).getFloat("scale", 1.0f)
    fun showGraph(c: Context) = sp(c).getBoolean("graph", true)
    fun showStats(c: Context) = sp(c).getBoolean("stats", true)
    fun showSystem(c: Context) = sp(c).getBoolean("system", true)
    fun posX(c: Context) = sp(c).getInt("x", 20)
    fun posY(c: Context) = sp(c).getInt("y", 120)

    fun setOpacity(c: Context, v: Float) = sp(c).edit().putFloat("opacity", v).apply()
    fun setScale(c: Context, v: Float) = sp(c).edit().putFloat("scale", v).apply()
    fun setShowGraph(c: Context, v: Boolean) = sp(c).edit().putBoolean("graph", v).apply()
    fun setShowStats(c: Context, v: Boolean) = sp(c).edit().putBoolean("stats", v).apply()
    fun setShowSystem(c: Context, v: Boolean) = sp(c).edit().putBoolean("system", v).apply()
    fun setPos(c: Context, x: Int, y: Int) = sp(c).edit().putInt("x", x).putInt("y", y).apply()
}
