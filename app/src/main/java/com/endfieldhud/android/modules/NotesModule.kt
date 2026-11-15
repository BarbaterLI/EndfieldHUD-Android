package com.endfieldhud.android.modules

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.endfieldhud.android.ChamferSmall
import com.endfieldhud.android.EndfieldAmber
import com.endfieldhud.android.EndfieldAmberDim
import com.endfieldhud.android.EndfieldGreen
import com.endfieldhud.android.EndfieldPanelHi
import com.endfieldhud.android.EndfieldText
import com.endfieldhud.android.EndfieldTextDim
import com.endfieldhud.android.HudState

/** Desktop feature: Notes — write text notes and todo checklists. */
@Composable
fun notesModule() {
    val notes by HudState.notes.collectAsState()
    var isTodoMode by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    Column {
        ModuleTitle("便笺 NOTES", "01") {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = !isTodoMode,
                    onClick = { isTodoMode = false },
                    label = { Text("便笺", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = EndfieldPanelHi,
                        selectedContainerColor = EndfieldAmber.copy(alpha = 0.2f),
                        selectedLabelColor = EndfieldAmber,
                        labelColor = EndfieldTextDim
                    )
                )
                FilterChip(
                    selected = isTodoMode,
                    onClick = { isTodoMode = true },
                    label = { Text("待办", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = EndfieldPanelHi,
                        selectedContainerColor = EndfieldAmber.copy(alpha = 0.2f),
                        selectedLabelColor = EndfieldAmber,
                        labelColor = EndfieldTextDim
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EndfieldPanelHi, ChamferSmall)
                .border(1.dp, EndfieldAmberDim, ChamferSmall)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = EndfieldText,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(EndfieldAmber),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (draft.isEmpty()) {
                        Text(
                            if (isTodoMode) "新待办事项…" else "记录一条便笺…",
                            color = EndfieldTextDim.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    inner()
                }
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    val text = draft.trim()
                    if (text.isNotEmpty()) {
                        HudState.addNote(text, isTodoMode)
                        HudState.logEvent(if (isTodoMode) "新增待办：$text" else "新增便笺：$text")
                        draft = ""
                    }
                },
                modifier = Modifier.height(24.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "添加",
                    tint = EndfieldAmber,
                    modifier = Modifier.height(18.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val filtered = notes.filter { it.isTodo == isTodoMode }
        if (filtered.isEmpty()) {
            EmptyHint(if (isTodoMode) "暂无待办 · 管理员请下达指令" else "暂无便笺")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                filtered.forEach { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EndfieldPanelHi, ChamferSmall)
                            .border(
                                1.dp,
                                if (note.done) EndfieldGreen.copy(alpha = 0.4f) else EndfieldAmberDim,
                                ChamferSmall
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTodoMode) {
                            Text(
                                if (note.done) "☑" else "☐",
                                color = if (note.done) EndfieldGreen else EndfieldTextDim,
                                fontSize = 16.sp,
                                modifier = Modifier.clickable { HudState.toggleNote(note.id) }
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            note.text,
                            color = if (note.done) EndfieldTextDim else EndfieldText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { HudState.deleteNote(note.id) },
                            modifier = Modifier.height(22.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "删除",
                                tint = EndfieldTextDim,
                                modifier = Modifier.height(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
