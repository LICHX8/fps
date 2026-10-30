# FrameForge
Original FPS monitor overlay for Android (Kotlin + Compose).

1. Open this folder in Android Studio (Koala or newer), let Gradle sync, run on a device (Android 8+).
2. In the app: grant "Display over other apps" and "Usage access".
3. One time, over USB debugging:
   adb shell pm grant com.frameforge.fps android.permission.DUMP
4. Tap Start overlay, open a game. Drag the overlay to reposition.

FPS comes from SurfaceFlinger layer timestamps of the foreground app.
Notes: some devices/ROMs restrict `dumpsys SurfaceFlinger --latency`; temps come from the battery sensor.
