package com.endfieldhud.android.modules

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endfieldhud.android.ChamferSmall
import com.endfieldhud.android.EndfieldAmber
import com.endfieldhud.android.EndfieldAmberDim
import com.endfieldhud.android.EndfieldPanelHi
import com.endfieldhud.android.EndfieldText
import com.endfieldhud.android.EndfieldTextDim
import com.endfieldhud.android.HudService
import com.endfieldhud.android.HudState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Desktop feature: Clipboard Cache — browse recent copied text and copy it
 * back with one tap. History lives only while the HUD service runs and is
 * wiped on exit, exactly like the desktop app.
 */
@Composable
fun clipboardModule() {
    val items by HudState.clipboardItems.collectAsState()
    val context = LocalContext.current
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column {
        ModuleTitle("剪贴板 CLIPBOARD", "02") {
            Text(
                "共 ${items.size} 条",
                color = EndfieldTextDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        HintText("HUD 运行期间自动捕获复制内容 · 点击条目回拷 · 退出服务即清空")

        if (items.isEmpty()) {
            EmptyHint("暂无捕获 · 去别处复制点什么")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EndfieldPanelHi, ChamferSmall)
                            .border(1.dp, EndfieldAmberDim, ChamferSmall)
                            .clickable {
                                (context as? HudService)?.copyClipBack(item.text)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.text,
                                color = EndfieldText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                timeFmt.format(Date(item.at)) + " · 点击回拷",
                                color = EndfieldTextDim,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        Text(
                            "⧉",
                            color = EndfieldAmber,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}
