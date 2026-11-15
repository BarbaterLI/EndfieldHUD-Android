package com.endfieldhud.android

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endfieldhud.android.modules.appsModule
import com.endfieldhud.android.modules.clipboardModule
import com.endfieldhud.android.modules.eventLogModule
import com.endfieldhud.android.modules.idCardModule
import com.endfieldhud.android.modules.mergeGameModule
import com.endfieldhud.android.modules.monitorModule
import com.endfieldhud.android.modules.notesModule
import com.endfieldhud.android.modules.settingsModule
import com.endfieldhud.android.modules.timerModule

private data class ModuleEntry(
    val module: HudModule,
    val icon: ImageVector?,
    val glyph: String,
)

private val moduleEntries = listOf(
    ModuleEntry(HudModule.NOTES, null, "记"),
    ModuleEntry(HudModule.CLIPBOARD, null, "贴"),
    ModuleEntry(HudModule.TIMER, Icons.Filled.KeyboardArrowUp, "计"),
    ModuleEntry(HudModule.MONITOR, null, "监"),
    ModuleEntry(HudModule.ID, null, "ID"),
    ModuleEntry(HudModule.LOG, null, "志"),
    ModuleEntry(HudModule.APPS, null, "应"),
    ModuleEntry(HudModule.MERGE, null, "合"),
    ModuleEntry(HudModule.SETTINGS, null, "设"),
)

@Composable
fun OverlayRoot(
    onDrag: (Float, Float) -> Unit,
    onCollapse: () -> Unit,
    onExpand: () -> Unit,
) {
    val collapsed by HudState.collapsed.collectAsState()
    if (collapsed) {
        HudBubble(onDrag = onDrag, onExpand = onExpand)
    } else {
        HudPanel(onDrag = onDrag, onCollapse = onCollapse)
    }
}

@Composable
private fun HudBubble(onDrag: (Float, Float) -> Unit, onExpand: () -> Unit) {
    val settings by HudState.settings.collectAsState()
    Box(
        modifier = Modifier
            .size(54.dp)
            .alpha(settings.bubbleAlpha)
            .background(EndfieldPanel, ChamferMedium)
            .border(1.5.dp, EndfieldAmber, ChamferMedium)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDrag(drag.x, drag.y)
                }
            }
            .clickable { onExpand() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "E",
                color = EndfieldAmber,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                "HUD",
                color = EndfieldTextDim,
                fontSize = 8.sp,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun HudPanel(onDrag: (Float, Float) -> Unit, onCollapse: () -> Unit) {
    BackHandler { onCollapse() }

    val settings by HudState.settings.collectAsState()
    val current by HudState.currentModule.collectAsState()
    val widthDp = when (settings.scale) {
        0 -> 300.dp
        2 -> 380.dp
        else -> 340.dp
    }

    Column(
        modifier = Modifier
            .width(widthDp)
            .height(520.dp)
            .background(EndfieldBg.copy(alpha = 0.97f), ChamferLarge)
            .border(1.dp, EndfieldAmberDim, ChamferLarge)
            .padding(10.dp)
    ) {
        PanelHeader(onDrag = onDrag, onCollapse = onCollapse)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.weight(1f)) {
            ModuleSidebar(current = current)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (current) {
                        HudModule.NOTES -> notesModule()
                        HudModule.CLIPBOARD -> clipboardModule()
                        HudModule.TIMER -> timerModule()
                        HudModule.MONITOR -> monitorModule()
                        HudModule.ID -> idCardModule()
                        HudModule.LOG -> eventLogModule()
                        HudModule.APPS -> appsModule()
                        HudModule.MERGE -> mergeGameModule()
                        HudModule.SETTINGS -> settingsModule()
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(onDrag: (Float, Float) -> Unit, onCollapse: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EndfieldPanel, ChamferSmall)
            .border(1.dp, EndfieldAmberDim, ChamferSmall)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDrag(drag.x, drag.y)
                }
            }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(EndfieldGreen, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "ENDMINISTRATOR HUD",
            color = EndfieldAmber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "v0.1",
            color = EndfieldTextDim,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
        )
        IconButton(onClick = onCollapse, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "收起",
                tint = EndfieldTextDim,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ModuleSidebar(current: HudModule) {
    Column(
        modifier = Modifier
            .width(44.dp)
            .fillMaxHeight()
            .background(EndfieldPanel, ChamferSmall)
            .border(1.dp, EndfieldAmberDim, ChamferSmall)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        moduleEntries.forEach { entry ->
            val selected = entry.module == current
            Column(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (selected) EndfieldAmber.copy(alpha = 0.18f) else Color.Transparent,
                        ChamferTiny
                    )
                    .border(
                        1.dp,
                        if (selected) EndfieldAmber else Color.Transparent,
                        ChamferTiny
                    )
                    .clickable {
                        if (current != entry.module) {
                            HudState.currentModule.value = entry.module
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (entry.icon != null) {
                    Icon(
                        entry.icon,
                        contentDescription = entry.module.label,
                        tint = if (selected) EndfieldAmber else EndfieldTextDim,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        entry.glyph,
                        color = if (selected) EndfieldAmber else EndfieldTextDim,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Text(
                    entry.module.code,
                    color = if (selected) EndfieldAmber else EndfieldTextDim.copy(alpha = 0.5f),
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
