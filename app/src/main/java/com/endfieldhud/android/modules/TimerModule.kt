package com.endfieldhud.android.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endfieldhud.android.ChamferSmall
import com.endfieldhud.android.EndfieldAmber
import com.endfieldhud.android.EndfieldAmberDim
import com.endfieldhud.android.EndfieldGreen
import com.endfieldhud.android.EndfieldRed
import com.endfieldhud.android.EndfieldPanelHi
import com.endfieldhud.android.EndfieldText
import com.endfieldhud.android.EndfieldTextDim
import com.endfieldhud.android.HudService
import com.endfieldhud.android.HudState

/** Desktop feature: Calendar — countdown timer + stopwatch. */
@Composable
fun timerModule() {
    var mode by remember { mutableStateOf(0) } // 0 = timer, 1 = stopwatch

    Column {
        ModuleTitle("计时 TIMER", "03") {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = mode == 0,
                    onClick = { mode = 0 },
                    label = { Text("计时器", fontSize = 10.sp) },
                    colors = chipColors()
                )
                FilterChip(
                    selected = mode == 1,
                    onClick = { mode = 1 },
                    label = { Text("秒表", fontSize = 10.sp) },
                    colors = chipColors()
                )
            }
        }
        if (mode == 0) TimerSection() else StopwatchSection()
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = EndfieldPanelHi,
    selectedContainerColor = EndfieldAmber.copy(alpha = 0.2f),
    selectedLabelColor = EndfieldAmber,
    labelColor = EndfieldTextDim
)

@Composable
private fun TimerSection() {
    val context = LocalContext.current
    val remaining by HudState.timerRemainingMs.collectAsState()
    val total by HudState.timerTotalMs.collectAsState()
    val running by HudState.timerRunning.collectAsState()
    var presetMin by remember { mutableIntStateOf(5) }

    val presets = listOf(1, 3, 5, 10, 25)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(EndfieldPanelHi, ChamferSmall)
                .border(1.dp, EndfieldAmberDim, ChamferSmall)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatMs(remaining),
                    color = if (running) EndfieldAmber else EndfieldTextDim,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                if (running) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(3.dp)
                            .background(EndfieldTextDim.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(remaining.toFloat() / total.toFloat().coerceAtLeast(1f))
                                .height(3.dp)
                                .background(EndfieldAmber)
                        )
                    }
                    Text(
                        "剩余 ${"%.0f".format(remaining / 60000f)} 分钟",
                        color = EndfieldTextDim,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { min ->
                FilterChip(
                    selected = presetMin == min,
                    onClick = { presetMin = min },
                    label = { Text("${min}分", fontSize = 10.sp) },
                    colors = chipColors()
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(
                text = if (running) "取消" else "启动计时",
                color = if (running) EndfieldRed else EndfieldGreen,
            ) {
                if (running) {
                    HudService.send(context, HudService.ACTION_TIMER_CANCEL)
                } else {
                    HudService.send(
                        context,
                        HudService.ACTION_TIMER_START,
                        presetMin * 60_000L
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HintText("计时结束将通知 + 震动提醒（可在设置中关闭震动）")
    }
}

@Composable
private fun StopwatchSection() {
    val context = LocalContext.current
    val elapsed by HudState.stopwatchMs.collectAsState()
    val running by HudState.stopwatchRunning.collectAsState()
    val laps by HudState.stopwatchLaps.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(EndfieldPanelHi, ChamferSmall)
                .border(1.dp, EndfieldAmberDim, ChamferSmall)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                formatMs(elapsed, tenths = true),
                color = if (running) EndfieldAmber else EndfieldTextDim,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(
                text = if (running) "暂停" else "开始",
                color = EndfieldGreen
            ) { HudService.send(context, HudService.ACTION_STOPWATCH_TOGGLE) }
            ActionButton("计次", EndfieldAmber) {
                HudService.send(context, HudService.ACTION_STOPWATCH_LAP)
            }
            ActionButton("重置", EndfieldTextDim) {
                HudService.send(context, HudService.ACTION_STOPWATCH_RESET)
            }
        }

        if (laps.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                laps.asReversed().forEachIndexed { i, lap ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "#${laps.size - i}",
                            color = EndfieldTextDim,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatMs(lap, tenths = true),
                            color = EndfieldText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), ChamferSmall)
            .border(1.dp, color, ChamferSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

internal fun formatMs(ms: Long, tenths: Boolean = false): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return if (tenths) {
        val t = (ms % 1000) / 100
        "%02d:%02d.%d".format(m, s, t)
    } else {
        "%02d:%02d".format(m, s)
    }
}
