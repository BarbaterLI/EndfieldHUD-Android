package com.endfieldhud.android.modules

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endfieldhud.android.ChamferSmall
import com.endfieldhud.android.ChamferTiny
import com.endfieldhud.android.EndfieldAmber
import com.endfieldhud.android.EndfieldAmberDim
import com.endfieldhud.android.EndfieldPanelHi
import com.endfieldhud.android.EndfieldText
import com.endfieldhud.android.EndfieldTextDim
import com.endfieldhud.android.HudState
import com.endfieldhud.android.LogEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppEntry(val label: String, val pkg: String)

/** Desktop feature: App shortcuts — launch and favorite apps from the HUD. */
@Composable
fun appsModule() {
    val context = LocalContext.current
    val favs by HudState.favApps.collectAsState()
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadApps(context) }
    }

    Column {
        ModuleTitle("应用快捷方式 APPS", "07") {
            Text(
                if (apps == null) "载入中…" else "${apps!!.size} 个应用",
                color = EndfieldTextDim,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EndfieldPanelHi, ChamferSmall)
                .border(1.dp, EndfieldAmberDim, ChamferSmall)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "搜索",
                tint = EndfieldTextDim,
                modifier = Modifier.height(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = EndfieldText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(EndfieldAmber),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "搜索应用…",
                            color = EndfieldTextDim.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    inner()
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        val list = apps ?: return
        val q = query.trim()
        val filtered = if (q.isEmpty()) list else list.filter { it.label.contains(q, true) || it.pkg.contains(q, true) }
        val favEntries = list.filter { it.pkg in favs }
        val rest = filtered.filter { it.pkg !in favs }

        if (favEntries.isNotEmpty()) {
            SectionLabel("收藏 FAVORITES")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                favEntries.forEach { app -> AppRow(app, starred = true, context) }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (rest.isEmpty() && favEntries.isEmpty()) {
            EmptyHint("无匹配应用")
        } else {
            SectionLabel("全部 ALL")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rest.take(60).forEach { app -> AppRow(app, starred = false, context) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = EndfieldTextDim,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 4.dp, top = 2.dp)
    )
}

@Composable
private fun AppRow(app: AppEntry, starred: Boolean, context: android.content.Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EndfieldPanelHi, ChamferSmall)
            .border(
                1.dp,
                if (starred) EndfieldAmberDim else androidx.compose.ui.graphics.Color.Transparent,
                ChamferSmall
            )
            .clickable { launchApp(context, app) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            app.label,
            color = EndfieldText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.Star,
            contentDescription = if (starred) "取消收藏" else "收藏",
            tint = if (starred) EndfieldAmber else EndfieldTextDim.copy(alpha = 0.3f),
            modifier = Modifier
                .height(20.dp)
                .clickable { HudState.toggleFav(app.pkg) }
        )
    }
}

private fun launchApp(context: android.content.Context, app: AppEntry) {
    runCatching {
        val intent = context.packageManager.getLaunchIntentForPackage(app.pkg)
        if (intent != null) {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            HudState.logEvent("启动应用 ${app.label}")
        }
    }
}

private fun loadApps(context: android.content.Context): List<AppEntry> {
    return runCatching {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> =
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, 0)
            }
        resolved
            .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.pkg }
            .sortedBy { it.label }
    }.getOrDefault(emptyList())
}
