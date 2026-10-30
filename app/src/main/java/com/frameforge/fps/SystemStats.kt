package com.frameforge.fps

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

data class SysSnapshot(
    val tempC: Float,
    val battery: Int,
    val ramUsedPct: Int,
    val thermalStatus: Int   // PowerManager.THERMAL_STATUS_*, 0 = none/unknown
) {
    val throttling get() = thermalStatus >= 3 || tempC >= 43f
}

object SystemStats {
    fun read(c: Context): SysSnapshot {
        val b: Intent? = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = (b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val mi = ActivityManager.MemoryInfo()
        (c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        val used = if (mi.totalMem > 0) (100 - mi.availMem * 100 / mi.totalMem).toInt() else 0
        val thermal = if (Build.VERSION.SDK_INT >= 29)
            (c.getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus else 0
        return SysSnapshot(temp, level * 100 / scale, used, thermal)
    }
}
