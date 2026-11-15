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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endfieldhud.android.ChamferMedium
import com.endfieldhud.android.ChamferSmall
import com.endfieldhud.android.EndfieldAmber
import com.endfieldhud.android.EndfieldAmberDim
import com.endfieldhud.android.EndfieldGreen
import com.endfieldhud.android.EndfieldPanelHi
import com.endfieldhud.android.EndfieldText
import com.endfieldhud.android.EndfieldTextDim
import com.endfieldhud.android.HudState
import com.endfieldhud.android.LogEvent
import com.endfieldhud.android.Profile

/** Desktop feature: Personal ID — customizable operator card. */
@Composable
fun idCardModule() {
    val profile by HudState.profile.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var draft by remember(profile) { mutableStateOf(profile) }

    Column {
        ModuleTitle("个人名片 PERSONAL ID", "05") {
            Text(
                if (editing) "编辑中" else "IDENTITY",
                color = if (editing) EndfieldGreen else EndfieldTextDim,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(EndfieldPanelHi, ChamferMedium)
                .border(1.dp, EndfieldAmber, ChamferMedium)
                .padding(14.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(36.dp)
                            .background(EndfieldAmber)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        if (editing) {
                            DraftField(draft.codename) { draft = draft.copy(codename = it) }
                        } else {
                            Text(
                                profile.codename,
                                color = EndfieldAmber,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                            )
                        }
                        Text(
                            "OPERATOR ${profile.operatorId}",
                            color = EndfieldTextDim,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                val rows = listOf(
                    "编号" to profile.operatorId,
                    "职务" to profile.title,
                    "所属" to profile.division,
                    "状态" to profile.status,
                )
                if (editing) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EditRow("代号", draft.codename) { draft = draft.copy(codename = it) }
                        EditRow("编号", draft.operatorId) { draft = draft.copy(operatorId = it) }
                        EditRow("职务", draft.title) { draft = draft.copy(title = it) }
                        EditRow("所属", draft.division) { draft = draft.copy(division = it) }
                        EditRow("状态", draft.status) { draft = draft.copy(status = it) }
                    }
                } else {
                    rows.forEach { (k, v) ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(
                                k,
                                color = EndfieldTextDim,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(36.dp)
                            )
                            Text(
                                v,
                                color = EndfieldText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (editing) {
                ActionButton2("保存", EndfieldGreen) {
                    HudState.setProfile(draft)
                    HudState.logEvent("个人名片已更新", LogEvent.OK)
                    editing = false
                }
                ActionButton2("取消", EndfieldTextDim) {
                    draft = profile
                    editing = false
                }
            } else {
                ActionButton2("编辑名片", EndfieldAmber) { editing = true }
            }
        }
    }
}

@Composable
private fun DraftField(value: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(
            color = EndfieldAmber,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        ),
        cursorBrush = SolidColor(EndfieldAmber),
    )
}

@Composable
private fun EditRow(label: String, value: String, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = EndfieldTextDim,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(32.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .background(EndfieldPanelHi, ChamferSmall)
                .border(1.dp, EndfieldAmberDim.copy(alpha = 0.6f), ChamferSmall)
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            if (value.isEmpty()) {
                Text("…", color = EndfieldTextDim.copy(alpha = 0.4f), fontSize = 11.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = EndfieldText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                cursorBrush = SolidColor(EndfieldAmber),
            )
        }
    }
}

@Composable
internal fun ActionButton2(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
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
