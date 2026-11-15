package com.endfieldhud.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HudState.init(this)
        setContent {
            EndfieldTheme {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val serviceRunning by HudState.serviceRunning.collectAsState()
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val notifGranted = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // refresh overlay permission state when returning from settings
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EndfieldBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        // ---- header ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(EndfieldPanel, ChamferMedium)
                    .border(1.5.dp, EndfieldAmber, ChamferMedium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "E",
                    color = EndfieldAmber,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "ENDFIELD HUD",
                    color = EndfieldAmber,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                )
                Text(
                    "ANDROID EDITION · 管理员终端",
                    color = EndfieldTextDim,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        Spacer(Modifier.padding(24.dp))

        // ---- permission: overlay ----
        StatusCard(
            title = "悬浮窗权限",
            ok = overlayGranted,
        ) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
        }

        Spacer(Modifier.padding(10.dp))

        // ---- permission: notifications ----
        if (Build.VERSION.SDK_INT >= 33 && !notifGranted) {
            StatusCard(title = "通知权限", ok = false) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            Spacer(Modifier.padding(10.dp))
        }

        // ---- service ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(EndfieldPanel, ChamferMedium)
                .border(1.dp, if (serviceRunning) EndfieldAmber else EndfieldAmberDim, ChamferMedium)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(if (serviceRunning) EndfieldGreen else EndfieldTextDim, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (serviceRunning) "HUD 服务运行中" else "HUD 服务未启动",
                        color = EndfieldText,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Spacer(Modifier.padding(12.dp))
                Row {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (overlayGranted) EndfieldAmber else EndfieldTextDim.copy(alpha = 0.3f)
                            )
                            .clickable { if (overlayGranted) HudService.start(context) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (serviceRunning) "已在运行" else "启动 HUD",
                            color = EndfieldBg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, EndfieldRed, ChamferSmall)
                            .clickable { HudService.stop(context) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "停止服务",
                            color = EndfieldRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                if (!overlayGranted) {
                    Spacer(Modifier.padding(8.dp))
                    Text(
                        "请先授予悬浮窗权限，否则 HUD 无法显示",
                        color = EndfieldRed,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        Spacer(Modifier.padding(20.dp))

        // ---- usage ----
        SectionLabel("使用方式")
        Text(
            "· 启动后从快捷设置下拉磁贴「EndfieldHUD 悬浮窗」一键切换\n" +
                "· 或点常驻通知上的「切换 HUD」按钮\n" +
                "· 面板顶部横条可拖动，右上按钮收起为气泡\n" +
                "· 气泡常驻屏幕，点按即展开 · 面板内可直接打字",
            color = EndfieldTextDim,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 18.sp,
        )

        Spacer(Modifier.padding(20.dp))

        // ---- modules ----
        SectionLabel("功能模块 · 对应桌面版")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "01 便笺 · 写字与待办清单" to "Notes",
                "02 剪贴板 · 自动捕获 / 一键回拷 / 退出即清" to "Clipboard Cache",
                "03 计时 · 倒计时(通知+震动) / 秒表计次" to "Calendar",
                "04 监视 · 电池 / 内存 / 存储 / CPU" to "Monitor",
                "05 个人名片 · 自定义身份卡" to "Personal ID",
                "06 事件日志 · 管理员事件时间线" to "Event Log",
                "07 应用快捷方式 · 收藏 / 搜索 / 启动" to "App Shortcuts",
                "08 融合小游戏 · 致敬山团团" to "Closure's Minigame",
                "09 设置 · 尺寸 / 透明度 / 震动" to "Settings",
            ).forEach { (zh, en) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EndfieldPanelHi, ChamferSmall)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        zh,
                        color = EndfieldText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        en,
                        color = EndfieldTextDim.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        Spacer(Modifier.padding(24.dp))

        Text(
            "移植自桌面版 EndfieldHUD（DDDuoDuo/EndfieldHUD）\nMIT License · 非官方同人项目 · 明日方舟：终末地相关素材与品牌归鹰角网络所有",
            color = EndfieldTextDim.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 13.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        "// $text",
        color = EndfieldAmber,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun StatusCard(
    title: String,
    ok: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EndfieldPanel, ChamferSmall)
            .border(1.dp, if (ok) EndfieldAmberDim else EndfieldRed, ChamferSmall)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = EndfieldText,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Text(
            if (ok) "已授予" else "未授予",
            color = if (ok) EndfieldGreen else EndfieldRed,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
        if (!ok) {
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .background(EndfieldAmber)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    "去授权",
                    color = EndfieldBg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
