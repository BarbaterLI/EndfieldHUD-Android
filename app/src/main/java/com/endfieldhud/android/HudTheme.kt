package com.endfieldhud.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// Endfield industrial palette
val EndfieldBg = Color(0xFF16130E)
val EndfieldBgHi = Color(0xFF1C1811)
val EndfieldPanel = Color(0xFF211C13)
val EndfieldPanelHi = Color(0xFF2C251A)
val EndfieldAmber = Color(0xFFF2A83B)
val EndfieldAmberDim = Color(0xFF8A6A2F)
val EndfieldText = Color(0xFFEFE7D3)
val EndfieldTextDim = Color(0xFF9C917C)
val EndfieldGreen = Color(0xFF8FBF6F)
val EndfieldRed = Color(0xFFE25B45)
val EndfieldBlue = Color(0xFF6FA8BF)

private val endfieldScheme = darkColorScheme(
    primary = EndfieldAmber,
    onPrimary = EndfieldBg,
    secondary = EndfieldAmberDim,
    onSecondary = EndfieldText,
    background = EndfieldBg,
    onBackground = EndfieldText,
    surface = EndfieldPanel,
    onSurface = EndfieldText,
    surfaceVariant = EndfieldPanelHi,
    onSurfaceVariant = EndfieldTextDim,
    outline = EndfieldAmberDim,
    error = EndfieldRed,
    onError = EndfieldBg,
)

/** Chamfered (cut-corner) rectangle, the signature Endfield panel shape. */
class ChamferShape(private val cut: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val c = with(density) { cut.toPx() }.coerceAtMost(size.minDimension / 2f)
        return Outline.Generic(
            Path().apply {
                moveTo(0f, c)
                lineTo(c, 0f)
                lineTo(size.width - c, 0f)
                lineTo(size.width, c)
                lineTo(size.width, size.height - c)
                lineTo(size.width - c, size.height)
                lineTo(c, size.height)
                lineTo(0f, size.height - c)
                close()
            }
        )
    }
}

val ChamferTiny = ChamferShape(4.dp)
val ChamferSmall = ChamferShape(7.dp)
val ChamferMedium = ChamferShape(10.dp)
val ChamferLarge = ChamferShape(14.dp)

@Composable
fun EndfieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = endfieldScheme,
        typography = Typography(),
        content = content,
    )
}
