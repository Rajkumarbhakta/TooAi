package com.rkbapps.tooai.ui.composabels

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp

// The pattern that stands for transparency behind a cut-out.
fun DrawScope.drawCheckerboard(topLeft: Offset, area: Size, cell: Float, light: Color, dark: Color) {
    drawRect(light, topLeft = topLeft, size = area)
    val columns = (area.width / cell).toInt() + 1
    val rows = (area.height / cell).toInt() + 1
    clipRect(topLeft.x, topLeft.y, topLeft.x + area.width, topLeft.y + area.height) {
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                if ((row + column) % 2 == 1) {
                    drawRect(dark, Offset(topLeft.x + column * cell, topLeft.y + row * cell), Size(cell, cell))
                }
            }
        }
    }
}

// Theme tones by default, so the pattern stays quiet in dark mode instead of black and white.
@Composable
fun Modifier.checkerboard(
    cell: Dp,
    light: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    dark: Color = MaterialTheme.colorScheme.surfaceContainerHigh
): Modifier = drawBehind { drawCheckerboard(Offset.Zero, size, cell.toPx(), light, dark) }
