package com.workcontrol.app.core.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.core.designsystem.rememberReducedMotion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * The front page header: "PRELO CONTROL" set in black Fraunces, slightly crooked as if pasted,
 * a double rule, and the telemetry strip (edition date/time, live link, project).
 */
@Composable
fun Masthead(live: Boolean, modifier: Modifier = Modifier, projectName: String? = null,
    onProjectClick: (() -> Unit)? = null, pollSeconds: Int = 10) {
    val palette = Prelo.colors
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("PRELO", Modifier.rotate(-1.2f), style = PreloType.Masthead, color = palette.ink)
            Text(" CONTROL", Modifier.rotate(-1.2f), style = PreloType.Masthead, color = palette.accent)
            Spacer(Modifier.weight(1f))
            if (projectName != null) ProjectChip(projectName, onProjectClick)
        }
        Spacer(Modifier.padding(top = 6.dp))
        DoubleRule()
        TelemetryStrip(live, pollSeconds)
        Rule()
    }
}

@Composable
private fun ProjectChip(name: String, onClick: (() -> Unit)?) {
    val palette = Prelo.colors
    Text(
        "$name ▾",
        Modifier
            .border(2.dp, palette.ink, RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "Trocar de projeto", onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = 36.dp)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = palette.ink,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}

/** "EDIÇÃO 05.10 · 18:52 · ◉ AO VIVO" — the sci-fi half of the masthead. */
@Composable
fun TelemetryStrip(live: Boolean, pollSeconds: Int = 10) {
    val palette = Prelo.colors
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val edition = remember(now) { SimpleDateFormat("dd.MM · HH:mm", Locale("pt", "BR")).format(Date(now)) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = if (live) "Conectado ao Prelo ao vivo" else "Sem conexão ao vivo; atualizando a cada $pollSeconds segundos"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("EDIÇÃO $edition", style = PreloType.Telemetry, color = palette.text)
        Spacer(Modifier.weight(1f))
        LivePulse(live)
        Text(if (live) "AO VIVO" else "RESERVA ${pollSeconds}s", style = PreloType.Telemetry,
            color = if (live) palette.accentInk else palette.faint)
    }
}

/** Radar rings while the event stream is connected; a hollow dot when on polling fallback. */
@Composable
fun LivePulse(live: Boolean, modifier: Modifier = Modifier) {
    val palette = Prelo.colors
    val reduced = rememberReducedMotion()
    val ring = if (live && !reduced) {
        val t = rememberInfiniteTransition(label = "pulse")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "ring").value
    } else 0f
    Canvas(modifier.size(14.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        if (live) {
            if (ring > 0f) {
                drawCircle(palette.accent.copy(alpha = 0.55f * (1f - ring)), radius = size.minDimension / 2 * ring, center = c,
                    style = Stroke(1.5.dp.toPx()))
                val r2 = (ring + 0.5f) % 1f
                drawCircle(palette.accent.copy(alpha = 0.55f * (1f - r2)), radius = size.minDimension / 2 * r2, center = c,
                    style = Stroke(1.5.dp.toPx()))
            }
            drawCircle(palette.accent, radius = 3.dp.toPx(), center = c)
        } else {
            drawCircle(palette.faint, radius = 3.dp.toPx(), center = c, style = Stroke(1.5.dp.toPx()))
        }
    }
}

/** Newspaper double rule (border-top: 6px double). */
@Composable
fun DoubleRule(modifier: Modifier = Modifier) {
    val palette = Prelo.colors
    Spacer(
        modifier
            .fillMaxWidth()
            .size(height = 6.dp, width = 0.dp)
            .drawBehind {
                drawLine(palette.rule, Offset(0f, 1.dp.toPx()), Offset(size.width, 1.dp.toPx()), 2.dp.toPx())
                drawLine(palette.rule, Offset(0f, 5.dp.toPx()), Offset(size.width, 5.dp.toPx()), 1.dp.toPx())
            },
    )
}

@Composable
fun Rule(modifier: Modifier = Modifier, strong: Boolean = false) {
    val palette = Prelo.colors
    Spacer(
        modifier
            .fillMaxWidth()
            .size(height = 2.dp, width = 0.dp)
            .drawBehind {
                drawLine(if (strong) palette.rule else palette.rule.copy(alpha = 0.6f), Offset(0f, size.height / 2),
                    Offset(size.width, size.height / 2), (if (strong) 2 else 1).dp.toPx())
            },
    )
}
