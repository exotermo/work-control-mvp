package com.workcontrol.app.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.core.designsystem.rememberReducedMotion

/** ".clipping-section-title": spaced serif capitals over a 2dp rule, with an optional counter. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, count: Int? = null,
    trailing: (@Composable () -> Unit)? = null) {
    val palette = Prelo.colors
    Column(modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text.uppercase(), Modifier.semantics { heading() }, style = PreloType.Section, color = palette.ink)
            if (count != null && count > 0) Counter(count, Modifier.padding(start = 8.dp))
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(6.dp))
        Rule(strong = true)
    }
}

/** ".pending-count": orange pill counter. */
@Composable
fun Counter(count: Int, modifier: Modifier = Modifier) {
    val palette = Prelo.colors
    Text(
        if (count > 99) "99+" else count.toString(),
        modifier.background(palette.accent, RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 1.dp),
        style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = palette.onAccent,
    )
}

/** ".empty-sheet": dashed outline, centered serif title and hint. */
@Composable
fun EmptySheet(title: String, hint: String? = null, modifier: Modifier = Modifier) {
    val palette = Prelo.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .drawBehind {
                drawRoundRect(palette.ruleSoft, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(8.dp.toPx(), 6.dp.toPx()))), cornerRadius = CornerRadius(14.dp.toPx()))
            }
            .padding(horizontal = 16.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = palette.ink, textAlign = TextAlign.Center)
        if (hint != null) Text(hint, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = palette.text,
            textAlign = TextAlign.Center)
    }
}

/** Error as a clipping with a "FALHA" stamp; the message text is kept verbatim ("Sem acesso."). */
@Composable
fun ErrorClipping(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Clipping(modifier, seed = message.hashCode(), animateEntrance = false, tape = false) {
        Kicker("Aviso da redação")
        Text(message, Modifier.padding(top = 6.dp), style = PreloType.Headline, color = Prelo.colors.bad)
        if (onRetry != null) ClippingFooter {
            Spacer(Modifier.weight(1f))
            InkLink("Tentar de novo", onRetry)
        }
    }
}

/** Loading: grey type bars being printed, with a scanline running down (the press is running). */
@Composable
fun PressSkeleton(modifier: Modifier = Modifier, rows: Int = 3) {
    val palette = Prelo.colors
    val reduced = rememberReducedMotion()
    val sweep = if (reduced) 0f else rememberInfiniteTransition(label = "press")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "sweep").value
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(rows) { i ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                        if (!reduced) {
                            val y = size.height * ((sweep + i * 0.3f) % 1f)
                            drawRect(Brush.verticalGradient(listOf(Color.Transparent, palette.accent.copy(alpha = 0.18f), Color.Transparent),
                                startY = y - 12.dp.toPx(), endY = y + 12.dp.toPx()), Offset(0f, y - 12.dp.toPx()), Size(size.width, 24.dp.toPx()))
                        }
                    }
                    .background(palette.paper, RoundedCornerShape(4.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Bar(0.3f, 10.dp); Bar(0.85f, 18.dp); Bar(0.6f, 12.dp)
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, h: androidx.compose.ui.unit.Dp) {
    Box(Modifier.fillMaxWidth(fraction).height(h).background(Prelo.colors.ruleSoft, RoundedCornerShape(2.dp)))
}

/** A thin scanline sweeps across once whenever [trigger] changes (a live event refreshed the page). */
@Composable
fun ScanlineSweep(trigger: Int, modifier: Modifier = Modifier) {
    val palette = Prelo.colors
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger > 0 && !reduced) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(700, easing = LinearEasing))
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .drawBehind {
                val p = progress.value
                if (p < 1f) {
                    val x = size.width * p
                    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, palette.accent, Color.Transparent),
                        startX = x - 80.dp.toPx(), endX = x + 20.dp.toPx()), size = size)
                }
            },
    )
}
