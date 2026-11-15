package com.endfieldhud.android.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endfieldhud.android.ChamferSmall
import com.endfieldhud.android.ChamferTiny
import com.endfieldhud.android.EndfieldAmber
import com.endfieldhud.android.EndfieldAmberDim
import com.endfieldhud.android.EndfieldBg
import com.endfieldhud.android.EndfieldPanelHi
import com.endfieldhud.android.EndfieldRed
import com.endfieldhud.android.EndfieldText
import com.endfieldhud.android.EndfieldTextDim
import com.endfieldhud.android.HudState
import com.endfieldhud.android.LogEvent
import kotlin.random.Random

private class MergeGame {
    var board: Array<IntArray> = Array(4) { IntArray(4) }
    var score = 0
    var over = false

    init {
        spawn(); spawn()
    }

    fun spawn() {
        val empties = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until 4) for (c in 0 until 4) if (board[r][c] == 0) empties += r to c
        if (empties.isEmpty()) return
        val (r, c) = empties[Random.nextInt(empties.size)]
        board[r][c] = if (Random.nextFloat() < 0.9f) 2 else 4
    }

    fun move(dir: Int) {
        if (over) return
        var gained = 0
        var moved = false
        val new = Array(4) { IntArray(4) }
        for (i in 0 until 4) {
            val line = when (dir) {
                0 -> board[i].copyOf()
                1 -> board[i].copyOf().also { it.reverse() }
                2 -> IntArray(4) { board[it][i] }
                else -> IntArray(4) { board[3 - it][i] }
            }
            val res = moveLine(line)
            gained += res.second
            for (j in 0 until 4) {
                if (line[j] != res.first[j]) moved = true
                when (dir) {
                    0 -> new[i][j] = res.first[j]
                    1 -> new[i][3 - j] = res.first[j]
                    2 -> new[j][i] = res.first[j]
                    else -> new[3 - j][i] = res.first[j]
                }
            }
        }
        if (moved) {
            board = new
            score += gained
            spawn()
            if (!canMove()) over = true
        }
    }

    private fun moveLine(line: IntArray): Pair<IntArray, Int> {
        val vals = line.filter { it != 0 }
        val out = IntArray(4)
        var gained = 0
        var i = 0
        var o = 0
        while (i < vals.size) {
            if (i + 1 < vals.size && vals[i] == vals[i + 1]) {
                val v = vals[i] * 2
                out[o++] = v
                gained += v
                i += 2
            } else {
                out[o++] = vals[i]
                i++
            }
        }
        return out to gained
    }

    private fun canMove(): Boolean {
        for (r in 0 until 4) for (c in 0 until 4) {
            if (board[r][c] == 0) return true
            if (c < 3 && board[r][c] == board[r][c + 1]) return true
            if (r < 3 && board[r][c] == board[r + 1][c]) return true
        }
        return false
    }
}

/**
 * Desktop feature: Closure's minigame — our homage to "OrbiPom! MERGE!".
 * A 2048-style merge game, swipe or use the arrow pad.
 */
@Composable
fun mergeGameModule() {
    val best by HudState.bestScore.collectAsState()
    val game = remember { MergeGame() }
    var redraw by remember { mutableStateOf(0) }
    var dragTotal by remember { mutableStateOf(Offset.Zero) }

    fun doMove(dir: Int) {
        game.move(dir)
        HudState.setBestScore(game.score)
        if (game.over) HudState.logEvent("融合小游戏结束 · 得分 ${game.score}", LogEvent.WARN)
        redraw++
    }

    Column {
        ModuleTitle("融合! MERGE!", "08") {
            Text(
                "重新开始",
                color = EndfieldAmber,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .background(EndfieldAmber.copy(alpha = 0.12f), ChamferTiny)
                    .clickable {
                        game.board = Array(4) { IntArray(4) }
                        game.score = 0
                        game.over = false
                        game.spawn(); game.spawn()
                        redraw++
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatBox("得分 SCORE", "${game.score}", Modifier.weight(1f), redraw)
            Spacer(Modifier.size(8.dp))
            StatBox("最佳 BEST", "$best", Modifier.weight(1f), redraw)
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(EndfieldBg.copy(alpha = 0.6f), ChamferSmall)
                .border(1.dp, EndfieldAmberDim, ChamferSmall)
                .padding(6.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, amount ->
                            change.consume()
                            dragTotal += amount
                        },
                        onDragEnd = {
                            val dx = dragTotal.x
                            val dy = dragTotal.y
                            dragTotal = Offset.Zero
                            if (kotlin.math.abs(dx) > 36 || kotlin.math.abs(dy) > 36) {
                                if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                                    doMove(if (dx > 0) 1 else 0)
                                } else {
                                    doMove(if (dy > 0) 3 else 2)
                                }
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (r in 0 until 4) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0 until 4) {
                            val v = game.board[r][c]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(tileColor(v), ChamferTiny)
                                    .border(
                                        1.dp,
                                        if (v > 0) EndfieldAmberDim else Color.Transparent,
                                        ChamferTiny
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (v > 0) {
                                    Text(
                                        "$v",
                                        color = if (v >= 64) EndfieldBg else EndfieldText,
                                        fontSize = if (v < 100) 18.sp else 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (game.over) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp)
                        .background(EndfieldBg.copy(alpha = 0.88f), ChamferSmall)
                        .border(1.dp, EndfieldRed, ChamferSmall)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "没有可融合的了\n最终得分 ${game.score}",
                        color = EndfieldRed,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "◀",
                color = EndfieldTextDim,
                fontSize = 20.sp,
                modifier = Modifier
                    .background(EndfieldPanelHi, ChamferTiny)
                    .clickable { doMove(0) }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
            Spacer(Modifier.size(6.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "▲",
                    color = EndfieldTextDim,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .background(EndfieldPanelHi, ChamferTiny)
                        .clickable { doMove(2) }
                        .padding(horizontal = 22.dp, vertical = 2.dp)
                )
                Text(
                    "▼",
                    color = EndfieldTextDim,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .background(EndfieldPanelHi, ChamferTiny)
                        .clickable { doMove(3) }
                        .padding(horizontal = 22.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.size(6.dp))
            Text(
                "▶",
                color = EndfieldTextDim,
                fontSize = 20.sp,
                modifier = Modifier
                    .background(EndfieldPanelHi, ChamferTiny)
                    .clickable { doMove(1) }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(4.dp))
        HintText("滑动棋盘或用方向键 · 相同数字融合翻倍")
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier, redraw: Int) {
    // redraw param forces recomposition when the game mutates
    key(redraw) {
        Box(
            modifier = modifier
                .background(EndfieldPanelHi, ChamferSmall)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    label,
                    color = EndfieldTextDim,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    value,
                    color = EndfieldAmber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

private fun tileColor(v: Int): Color = when (v) {
    0 -> EndfieldPanelHi.copy(alpha = 0.35f)
    2 -> Color(0xFF5A4A2E)
    4 -> Color(0xFF6E5834)
    8 -> Color(0xFF82643A)
    16 -> Color(0xFF96703E)
    32 -> Color(0xFFAA7C42)
    64 -> Color(0xFFBE8846)
    128 -> Color(0xFFD2944A)
    256 -> Color(0xFFE2A04E)
    512 -> Color(0xFFECAE5E)
    1024 -> Color(0xFFF4BC72)
    else -> Color(0xFFFBD08E)
}
