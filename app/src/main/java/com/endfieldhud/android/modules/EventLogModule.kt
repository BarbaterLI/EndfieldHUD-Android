package com.endfieldhud.android.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endfieldhud.android.ChamferTiny
import com.endfieldhud.android.EndfieldAmber
import com.endfieldhud.android.EndfieldGreen
import com.endfieldhud.android.EndfieldRed
import com.endfieldhud.android.EndfieldText
import com.endfieldhud.android.EndfieldTextDim
import com.endfieldhud.android.HudState
import com.endfieldhud.android.LogEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Desktop feature: Event Log — view events like a true Endministrator. */
@Composable
fun eventLogModule() {
    val events by HudState.events.collectAsState()
    val timeFmt = remember {
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    }

    Column {
        ModuleTitle("事件日志 EVENT LOG", "06") {
            Text(
                "清空",
                color = EndfieldRed,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .background(EndfieldRed.copy(alpha = 0.12f), ChamferTiny)
                    .clickable { HudState.clearEvents() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        if (events.isEmpty()) {
            EmptyHint("暂无事件记录")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                events.forEach { ev ->
                    Row {
                        Text(
                            timeFmt.format(Date(ev.at)),
                            color = EndfieldTextDim.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(78.dp)
                        )
                        Text(
                            when (ev.kind) {
                                LogEvent.OK -> "[OK] "
                                LogEvent.WARN -> "[!!] "
                                else -> "[--] "
                            },
                            color = when (ev.kind) {
                                LogEvent.OK -> EndfieldGreen
                                LogEvent.WARN -> EndfieldRed
                                else -> EndfieldAmber
                            },
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            ev.msg,
                            color = EndfieldText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 13.sp,
                        )
                    }
                }
            }
        }
    }
}
