package com.endfieldhud.android.modules

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endfieldhud.android.ChamferSmall
import com.endfieldhud.android.ChamferTiny
import com.endfieldhud.android.EndfieldAmber
import com.endfieldhud.android.EndfieldAmberDim
import com.endfieldhud.android.EndfieldPanelHi
import com.endfieldhud.android.EndfieldRed
import com.endfieldhud.android.EndfieldText
import com.endfieldhud.android.EndfieldTextDim
import com.endfieldhud.android.HudService
import com.endfieldhud.android.HudSettings
import com.endfieldhud.android.HudState

/** Desktop feature: Settings — HUD size, bubble opacity, vibration, about. */
@Composable
fun settingsModule() {
    val context = LocalContext.current
    val settings by HudState.settings.collectAsState()
    val overlayGranted = Settings.canDrawOverlays(context)

    Column {
        ModuleTitle("设置 SETTINGS", "09")

        SettingLabel("HUD 面板尺寸")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("小 S" to 0, "中 M" to 1, "大 L" to 2).forEach { (label, idx) ->
                FilterChip(
                    selected = settings.scale == idx,
                    onClick = {
                        HudState.setSettings(HudSettings(idx, settings.bubbleAlpha, settings.vibrate))
                    },
                    label = { Text(label, fontSize = 10.sp) },
                    colors = FilterChipDefaults2()
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingLabel("气泡透明度 ${(settings.bubbleAlpha * 100).toInt()}%")
        Slider(
            value = settings.bubbleAlpha,
            onValueChange = {
                HudState.setSettings(HudSettings(settings.scale, it.coerceIn(0.4f, 1f), settings.vibrate))
            },
            valueRange = 0.4f..1f,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EndfieldPanelHi, ChamferSmall)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "计时结束震动",
                color = EndfieldText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = settings.vibrate,
                onCheckedChange = {
                    HudState.setSettings(HudSettings(settings.scale, settings.bubbleAlpha, it))
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = EndfieldAmber,
                    checkedThumbColor = EndfieldText,
                    uncheckedTrackColor = EndfieldPanelHi,
                    uncheckedThumbColor = EndfieldTextDim
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        if (!overlayGranted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EndfieldRed.copy(alpha = 0.12f), ChamferSmall)
                    .border(1.dp, EndfieldRed, ChamferSmall)
                    .clickable { openOverlaySettings(context) }
                    .padding(10.dp)
            ) {
                Text(
                    "悬浮窗权限未授予 · 点击前往系统设置开启",
                    color = EndfieldRed,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(EndfieldRed.copy(alpha = 0.1f), ChamferSmall)
                .border(1.dp, EndfieldRed, ChamferSmall)
                .clickable {
                    HudState.logEvent("用户结束 HUD 服务", 1)
                    HudService.stop(context)
                }
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "结束 HUD 服务（同时清空剪贴板缓存）",
                color = EndfieldRed,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "关于 ABOUT",
            color = EndfieldTextDim,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "EndfieldHUD Android v0.1\n移植自桌面版 EndfieldHUD (DDDuoDuo)\nMIT License · 非官方同人项目\n游戏素材与品牌归鹰角网络所有",
            color = EndfieldTextDim,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 13.sp,
        )
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text,
        color = EndfieldTextDim,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

private fun openOverlaySettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
private fun FilterChipDefaults2() = androidx.compose.material3.FilterChipDefaults.filterChipColors(
    containerColor = EndfieldPanelHi,
    selectedContainerColor = EndfieldAmber.copy(alpha = 0.2f),
    selectedLabelColor = EndfieldAmber,
    labelColor = EndfieldTextDim
)
