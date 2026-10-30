package com.frameforge.fps

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface { Screen(tick) } } }
    }

    override fun onResume() { super.onResume(); tick++ }

    private fun hasUsage(): Boolean {
        val ops = getSystemService(AppOpsManager::class.java)
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    @Composable
    fun Screen(t: Int) {
        val ctx = this
        val overlayOk = remember(t) { Settings.canDrawOverlays(ctx) }
        val usageOk = remember(t) { hasUsage() }
        val dumpOk = remember(t) { checkSelfPermission("android.permission.DUMP") == android.content.pm.PackageManager.PERMISSION_GRANTED }
        var opacity by remember { mutableFloatStateOf(Prefs.opacity(ctx)) }
        var scale by remember { mutableFloatStateOf(Prefs.scale(ctx)) }
        var graph by remember { mutableStateOf(Prefs.showGraph(ctx)) }
        var stats by remember { mutableStateOf(Prefs.showStats(ctx)) }
        var system by remember { mutableStateOf(Prefs.showSystem(ctx)) }
        var summary by remember { mutableStateOf(SessionLog.summary()) }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("FrameForge", style = MaterialTheme.typography.headlineLarge)
            Text("Live FPS, frame-time graph, temperature and session stats over any game.",
                style = MaterialTheme.typography.bodyMedium)

            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Setup", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((if (overlayOk) "✅ " else "❌ ") + "Display over other apps", Modifier.weight(1f))
                    if (!overlayOk) Button(onClick = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }) { Text("Grant") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((if (usageOk) "✅ " else "❌ ") + "Usage access (detect the game)", Modifier.weight(1f))
                    if (!usageOk) Button(onClick = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) { Text("Grant") }
                }
                Text((if (dumpOk) "✅ " else "❌ ") + "Frame stats permission (one-time, via PC)")
                if (!dumpOk) {
                    Text("Enable USB debugging, connect to a PC and run:", style = MaterialTheme.typography.bodySmall)
                    androidx.compose.foundation.text.selection.SelectionContainer { Text("adb shell pm grant $packageName android.permission.DUMP",
                        style = MaterialTheme.typography.bodySmall) }
                }
            } }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(enabled = overlayOk, onClick = {
                    val i = Intent(ctx, OverlayService::class.java)
                    startForegroundService(i)
                }) { Text("Start overlay") }
                OutlinedButton(onClick = { stopService(Intent(ctx, OverlayService::class.java)); summary = SessionLog.summary() }) { Text("Stop") }
            }

            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Overlay", style = MaterialTheme.typography.titleMedium)
                Text("Opacity"); Slider(value = opacity, onValueChange = { opacity = it; Prefs.setOpacity(ctx, it) }, valueRange = 0.2f..1f)
                Text("Size"); Slider(value = scale, onValueChange = { scale = it; Prefs.setScale(ctx, it) }, valueRange = 0.7f..1.8f)
                SwitchRow("Frame-time graph", graph) { graph = it; Prefs.setShowGraph(ctx, it) }
                SwitchRow("Frame time / worst frame", stats) { stats = it; Prefs.setShowStats(ctx, it) }
                SwitchRow("Temp, battery, RAM, throttle warning", system) { system = it; Prefs.setShowSystem(ctx, it) }
                Text("Tip: drag the overlay to move it.", style = MaterialTheme.typography.bodySmall)
            } }

            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Last session", style = MaterialTheme.typography.titleMedium)
                Text(summary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { summary = SessionLog.summary() }) { Text("Refresh") }
                    OutlinedButton(enabled = SessionLog.size() > 0, onClick = { SessionLog.exportAndShare(ctx) }) { Text("Export CSV") }
                }
            } }
        }
    }

    @Composable
    private fun SwitchRow(label: String, v: Boolean, on: (Boolean) -> Unit) =
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f)); Switch(checked = v, onCheckedChange = on)
        }
}
