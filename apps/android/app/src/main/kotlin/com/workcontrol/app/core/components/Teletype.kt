package com.workcontrol.app.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.core.designsystem.rememberReducedMotion

/**
 * Execution output as a teletype print-out: monospace on fresh paper, new text is typed in
 * (~90 chars/s, capped at 1.2 s per update) and a block cursor blinks while the agent is running.
 */
@Composable
fun Teletype(text: String, running: Boolean, modifier: Modifier = Modifier, title: String = "SAÍDA DO AGENTE") {
    val palette = Prelo.colors
    val reduced = rememberReducedMotion()
    var shownFrom by remember { mutableIntStateOf(text.length) }
    val typed = remember { Animatable(text.length.toFloat()) }
    LaunchedEffect(text) {
        if (reduced || text.length < typed.value.toInt()) { typed.snapTo(text.length.toFloat()); shownFrom = text.length; return@LaunchedEffect }
        val from = typed.value
        val chars = text.length - from
        if (chars <= 0f) return@LaunchedEffect
        shownFrom = from.toInt()
        typed.animateTo(text.length.toFloat(), tween((chars / 0.09f).toInt().coerceIn(120, 1200), easing = LinearEasing))
    }
    val cursorOn = if (running && !reduced) {
        rememberInfiniteTransition(label = "cursor")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(530, easing = LinearEasing), RepeatMode.Reverse), label = "blink").value > 0.5f
    } else running
    val visible = text.take(typed.value.toInt().coerceIn(0, text.length))
    Column(
        modifier
            .fillMaxWidth()
            .background(palette.card, RoundedCornerShape(6.dp))
            .border(1.dp, palette.rule.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("▚ $title", style = PreloType.Telemetry, color = palette.faint)
            Spacer(Modifier.weight(1f))
            if (running) Text("RECEBENDO", style = PreloType.Telemetry, color = palette.accentInk)
        }
        Text(
            buildAnnotatedString {
                append(visible.ifEmpty { if (running) "" else "(sem saída)" })
                if (running) withStyle(SpanStyle(color = if (cursorOn) palette.accent else palette.accent.copy(alpha = 0f))) { append("▌") }
            },
            Modifier.padding(top = 8.dp),
            style = PreloType.Teletype, color = palette.ink,
        )
    }
}
