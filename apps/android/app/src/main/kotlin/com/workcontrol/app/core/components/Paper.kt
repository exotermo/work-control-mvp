package com.workcontrol.app.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloPalette

/**
 * The desk: newsprint tone with a faint light falloff and — the "future" half — a fine blueprint
 * dot grid, like a drafting table under a HUD.
 */
@Composable
fun PaperBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val palette = Prelo.colors
    Box(modifier.drawBehind { drawDesk(palette) }, content = content)
}

private fun DrawScope.drawDesk(p: PreloPalette) {
    drawRect(p.desk)
    drawRect(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = if (p.night) 0.035f else 0.45f), Color.Transparent),
            center = Offset(size.width * 0.2f, size.height * 0.08f), radius = size.maxDimension * 0.7f,
        ),
    )
    val step = 22.dp.toPx()
    val dot = 0.9.dp.toPx()
    val color = p.ruleSoft.copy(alpha = if (p.night) 0.55f else 0.75f)
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) {
            drawCircle(color, dot, Offset(x, y))
            x += step
        }
        y += step
    }
}

/** Clipping paper texture (".clipping" background-image): highlight, aged corner, fine print lines. */
fun DrawScope.drawPaperTexture(p: PreloPalette) {
    drawRect(p.paper)
    drawRect(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = if (p.night) 0.05f else 0.55f), Color.Transparent),
            center = Offset(size.width * 0.18f, size.height * 0.12f), radius = size.maxDimension * 0.5f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            listOf(Color(0xFF966E32).copy(alpha = if (p.night) 0.08f else 0.10f), Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 0.92f), radius = size.maxDimension * 0.55f,
        ),
    )
    val gap = 3.dp.toPx()
    val line = p.ink.copy(alpha = 0.025f)
    var y = 0f
    while (y < size.height) {
        drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += gap
    }
}
