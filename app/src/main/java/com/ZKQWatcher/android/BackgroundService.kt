package com.ZKQWatcher.android

import android.app.*
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.ZKQWatcher.android.data.SettingsRepository
import com.ZKQWatcher.android.model.Account
import androidx.lifecycle.LifecycleService
import com.ZKQWatcher.android.scheduler.ScheduleEngine
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalTime

class BackgroundService : LifecycleService() {

    /* ───────── 状态供 UI 观察 ───────── */
    companion object {
        var running by mutableStateOf(false)
            private set

        private const val CHANNEL_ID = "ZKQWatcherChannel"
        private const val NOTIF_ID   = 1
    }

    /* ───────── 私有字段 ───────── */
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repo: SettingsRepository
    private var schedule: List<ScheduleEngine.Segment> = emptyList()
    private var currentDate: java.time.LocalDate = java.time.LocalDate.now()
    /* 每分钟执行一次 */
    private val ticker = object : Runnable {
        override fun run() {
            try {
                tick()
            } finally {
                handler.postDelayed(this, 60_000L)
            }
        }
    }

    /* ───────── 生命周期 ───────── */
    override fun onCreate() {
        super.onCreate()
        running = true

        repo = SettingsRepository(applicationContext)
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("服务运行中"))

        /* 生成今天的排班表（协程异步完成后开始 tick） */
        lifecycleScope.launch {
            schedule = makeTodaySchedule()
            handler.post(ticker)                       // 第一次 tick
        }

        Toast.makeText(this, "后台服务已启动", Toast.LENGTH_SHORT).show()
    }

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        running = false
        Toast.makeText(this, "后台服务已停止", Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }



    /* ───────── 核心逻辑 ───────── */
    private suspend fun makeTodaySchedule(): List<ScheduleEngine.Segment> {
        val st = repo.load()
        val day = st.accounts.filter { it.group == Account.Group.DAY }
        val night = st.accounts.filter { it.group == Account.Group.NIGHT }
        return ScheduleEngine.buildDaily(day, night, st.segmentsPerAccount)
    }

    private fun tick() = try {
        val today = java.time.LocalDate.now()
        if (today != currentDate) {
            currentDate = today
            lifecycleScope.launch {
                schedule = makeTodaySchedule()
            }
        }
        val now = LocalTime.now()
        val nowMin = now.hour * 60 + now.minute
        val activeId = schedule.firstOrNull { seg ->
            if (seg.startMin <= seg.endMin)
                nowMin in seg.startMin until seg.endMin
            else            // 夜间跨 0 点
                nowMin >= seg.startMin || nowMin < seg.endMin
        }?.accId

        updateAccountFiles(activeId)
//        RootPermissionUtil.killZKPProcesses()

    } catch (t: Throwable) {
        Log.e("BackgroundService", "tick error", t)
    }

    /* ───────── 文件+进程操作 ───────── */
    private fun updateAccountFiles(activeId: Int?) {
        val dir = File("/sdcard/紫孔雀配置")
        val mkdirCmd = "mkdir -p /sdcard/紫孔雀配置"
        Runtime.getRuntime().exec(arrayOf("su", "-c", mkdirCmd))

        lifecycleScope.launch {
            val allAccounts = repo.load().accounts

            val allDayIds = allAccounts
                .filter { it.group == Account.Group.ALL_DAY }
                .map { it.id }
                .toSet()

            var changed = false

            for (acc in allAccounts) {
                val shouldBeOn = acc.id == activeId || acc.id in allDayIds
                val targetValue = if (shouldBeOn) "1" else "0"
                val path = "/sdcard/紫孔雀配置/开启状态${acc.id}.txt"

                // 读取当前内容
                val checkCmd = "cat \"$path\" 2>/dev/null"
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", checkCmd))
                val currentValue = process.inputStream.bufferedReader().readText().trim()

                if (currentValue != targetValue) {
                    changed = true
                }
            }

            if (changed) {
                for (acc in allAccounts) {
                    val shouldBeOn = acc.id == activeId || acc.id in allDayIds
                    val value = if (shouldBeOn) "1" else "0"
                    val writeCmd = "echo $value > /sdcard/紫孔雀配置/开启状态${acc.id}.txt"
                    Runtime.getRuntime().exec(arrayOf("su", "-c", writeCmd))
                }
                RootPermissionUtil.killZKPProcesses()
            }
        }
    }

    /* ───────── 通知 ───────── */
    private fun buildNotification(text: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("ZKQWatcher")
            .setContentText(text)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "ZKQWatcher 后台服务",
                        NotificationManager.IMPORTANCE_MIN
                    )
                )
            }
        }
    }
}
