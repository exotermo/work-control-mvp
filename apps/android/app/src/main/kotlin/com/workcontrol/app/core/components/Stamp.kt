package com.workcontrol.app.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloMotion
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.core.designsystem.rememberReducedMotion
import kotlinx.coroutines.delay

enum class Tone { OK, BAD, WAIT, NEUTRAL }

/** Prelo status → stamp text in pt-BR and tone. Unknown statuses are stamped as they come. */
fun stampFor(status: String?): Pair<String, Tone> = when (status?.uppercase()) {
    null, "" -> "—" to Tone.NEUTRAL
    "PENDING" -> "AGUARDANDO" to Tone.WAIT
    "QUEUED" -> "NA FILA" to Tone.WAIT
    "RUNNING", "IN_PROGRESS" -> "EM EXECUÇÃO" to Tone.WAIT
    "WAITING_APPROVAL", "AWAITING_APPROVAL" -> "AGUARDA APROVAÇÃO" to Tone.WAIT
    "AWAITING_SUBTASK", "WAITING_SUBTASK" -> "AGUARDA SUBTAREFA" to Tone.WAIT
    "CREATED" -> "CRIADA" to Tone.NEUTRAL
    "APPROVED" -> "APROVADO" to Tone.OK
    "DENIED", "REJECTED" -> "NEGADO" to Tone.BAD
    "EXPIRED" -> "EXPIRADO" to Tone.BAD
    "COMPLETED", "SUCCEEDED", "SUCCESS", "DONE" -> "CONCLUÍDO" to Tone.OK
    "FAILED", "ERROR" -> "FALHOU" to Tone.BAD
    "CANCELLED", "CANCELED" -> "CANCELADO" to Tone.NEUTRAL
    "ROLLED_BACK" -> "REVERTIDO" to Tone.BAD
    "ONLINE", "HEALTHY", "OK", "UP" -> "NO AR" to Tone.OK
    "OFFLINE", "UNHEALTHY", "DOWN", "UNREACHABLE" -> "FORA DO AR" to Tone.BAD
    "UNKNOWN" -> "SEM SINAL" to Tone.NEUTRAL
    else -> status.uppercase().replace('_', ' ') to Tone.NEUTRAL
}

@Composable
fun toneColor(tone: Tone): Color = when (tone) {
    Tone.OK -> Prelo.colors.ok
    Tone.BAD -> Prelo.colors.bad
    Tone.WAIT -> Prelo.colors.wait
    Tone.NEUTRAL -> Prelo.colors.text
}

/**
 * Rubber stamp (".clipping-stamp"): hit onto the paper after the clipping lands — big and rotated,
 * then settles at -6°. The sci-fi beat: a two-frame chromatic glitch as it lands.
 */
@Composable
fun Stamp(text: String, tone: Tone, modifier: Modifier = Modifier, delayMs: Int = PreloMotion.STAMP_DELAY_MS,
    animate: Boolean = true, rotation: Float = -6f) {
    val color = toneColor(tone)
    val palette = Prelo.colors
    val reduced = rememberReducedMotion()
    val hit = remember { Animatable(if (animate && !reduced) 0f else 1f) }
    val glitch = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (hit.value < 1f) {
            delay(delayMs.toLong())
            hit.animateTo(1f, PreloMotion.paste())
            glitch.snapTo(1f)
            glitch.animateTo(0f, tween(180))
        }
    }
    Box(modifier.clearAndSetSemantics { contentDescription = "Situação: $text" }) {
        if (glitch.value > 0f) {
            StampFace(text, palette.accent.copy(alpha = 0.7f * glitch.value), Modifier.graphicsLayer {
                translationX = -2.dp.toPx() * glitch.value; rotationZ = rotation
            })
            StampFace(text, palette.ok.copy(alpha = 0.6f * glitch.value), Modifier.graphicsLayer {
                translationX = 2.dp.toPx() * glitch.value; translationY = 1.dp.toPx() * glitch.value; rotationZ = rotation
            })
        }
        StampFace(text, color, Modifier.graphicsLayer {
            val p = hit.value
            val q = 1f - p
            alpha = (0.9f * p).coerceIn(0f, 0.9f)
            scaleX = 1f + 1.2f * q
            scaleY = 1f + 1.2f * q
            rotationZ = rotation - 8f * q
        })
    }
}

@Composable
private fun StampFace(text: String, color: Color, modifier: Modifier) {
    Text(
        text, modifier.border(2.dp, color, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
        style = PreloType.Stamp, color = color, maxLines = 1,
    )
}

/** Small status dot for telemetry rows (machines, live). */
@Composable
fun StatusDot(tone: Tone, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).background(toneColor(tone), CircleShape))
}
