package com.endfieldhud.android.modules

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endfieldhud.android.ChamferSmall
import com.endfieldhud.android.EndfieldAmber
import com.endfieldhud.android.EndfieldGreen
import com.endfieldhud.android.EndfieldPanelHi
import com.endfieldhud.android.EndfieldRed
import com.endfieldhud.android.EndfieldText
import com.endfieldhud.android.EndfieldTextDim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class BatterySnapshot(
    val level: Int = 0,
    val charging: Boolean = false,
    val temp: Float = 0f,
    val voltage: Float = 0f,
)

private data class MonitorSnapshot(
    val battery: BatterySnapshot = BatterySnapshot(),
    val ramUsed: Long = 0,
    val ramTotal: Long = 0,
    val storageUsed: Long = 0,
    val storageTotal: Long = 0,
    val cpu: Int = 0,
)

private object CpuSample {
    var last: Pair<Long, Long>? = null
}

/** Desktop feature: Battery, Storage & Activity Monitor. */
@Composable
fun monitorModule() {
    val context = LocalContext.current
    var snap by remember { mutableStateOf(MonitorSnapshot()) }

    LaunchedEffect(Unit) {
        while (true) {
            val next = withContext(Dispatchers.IO) {
                MonitorSnapshot(
                    battery = readBattery(context),
                    ramUsed = readRam(context).first,
                    ramTotal = readRam(context).second,
                    storageUsed = readStorage().first,
                    storageTotal = readStorage().second,
                    cpu = readCpu(),
                )
            }
            snap = next
            delay(1000)
        }
    }

    Column {
        ModuleTitle("监视 MONITOR", "04")

        MeterRow(
            label = "电池 BATTERY",
            valueText = "${snap.battery.level}%" + if (snap.battery.charging) " ⚡" else "",
            ratio = snap.battery.level / 100f,
            color = if (snap.battery.level <= 20 && !snap.battery.charging) EndfieldRed else EndfieldAmber
        )
        Text(
            "状态 ${if (snap.battery.charging) "充电中" else "放电"} · 温度 ${"%.1f".format(snap.battery.temp)}°C · 电压 ${"%.2f".format(snap.battery.voltage)}V",
            color = EndfieldTextDim,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
        )

        MeterRow(
            label = "内存 RAM",
            valueText = "${fmtBytes(snap.ramUsed)} / ${fmtBytes(snap.ramTotal)}",
            ratio = if (snap.ramTotal > 0) snap.ramUsed.toFloat() / snap.ramTotal else 0f,
            color = EndfieldAmber
        )
        Spacer(Modifier.height(8.dp))

        MeterRow(
            label = "存储 STORAGE",
            valueText = "${fmtBytes(snap.storageUsed)} / ${fmtBytes(snap.storageTotal)}",
            ratio = if (snap.storageTotal > 0) snap.storageUsed.toFloat() / snap.storageTotal else 0f,
            color = EndfieldAmber
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EndfieldPanelHi, ChamferSmall)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CPU 活动",
                color = EndfieldTextDim,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${snap.cpu}%",
                color = if (snap.cpu > 80) EndfieldRed else EndfieldGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }

        Spacer(Modifier.height(8.dp))
        HintText("每秒刷新一次 · 数据来自系统接口")
    }
}

@Composable
private fun MeterRow(label: String, valueText: String, ratio: Float, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = EndfieldTextDim,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Text(
                valueText,
                color = EndfieldText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(EndfieldTextDim.copy(alpha = 0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(color)
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}

private fun readBattery(context: Context): BatterySnapshot {
    return try {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent != null) {
            val lvl = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val level = if (lvl >= 0 && scale > 0) lvl * 100 / scale else 0
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            BatterySnapshot(
                level = level,
                charging = charging,
                temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
                voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f,
            )
        } else BatterySnapshot()
    } catch (_: Exception) {
        BatterySnapshot()
    }
}

private fun readRam(context: Context): Pair<Long, Long> {
    return try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        (info.totalMem - info.availMem) to info.totalMem
    } catch (_: Exception) {
        0L to 0L
    }
}

private fun readStorage(): Pair<Long, Long> {
    return try {
        val stat = StatFs(Environment.getDataDirectory().path)
        (stat.totalBytes - stat.availableBytes) to stat.totalBytes
    } catch (_: Exception) {
        0L to 0L
    }
}

/** CPU usage from /proc/stat deltas between calls. */
private fun readCpu(): Int {
    return try {
        val line = java.io.File("/proc/stat").bufferedReader().readLine() ?: return 0
        val vals = line.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
        if (vals.size < 5) return 0
        val idle = vals[3] + vals[4]
        val total = vals.take(8).sum()
        val prev = CpuSample.last
        CpuSample.last = idle to total
        if (prev == null) return 0
        val dIdle = idle - prev.first
        val dTotal = total - prev.second
        if (dTotal <= 0) 0
        else ((dTotal - dIdle) * 100 / dTotal).toInt().coerceIn(0, 100)
    } catch (_: Exception) {
        0
    }
}

internal fun fmtBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024 * 1024)
    return if (gb >= 1) "%.1fG".format(gb) else "%.0fM".format(bytes / (1024.0 * 1024))
}
