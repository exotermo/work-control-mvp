package com.workcontrol.app.core.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.WcColor

/** Tom visual de status — mapeia 1:1 com o `StatusType` do protótipo. */
enum class StatusTone { ONLINE, RUNNING, WARNING, ERROR, IDLE, OFFLINE }

fun StatusTone.toColor(): Color = when (this) {
    StatusTone.ONLINE, StatusTone.RUNNING -> WcColor.Ok
    StatusTone.WARNING -> WcColor.Warn
    StatusTone.ERROR -> WcColor.Bad
    StatusTone.IDLE -> WcColor.Info
    StatusTone.OFFLINE -> WcColor.Off
}

private fun StatusTone.isPulsing(): Boolean = this == StatusTone.RUNNING || this == StatusTone.WARNING

@Composable
fun StatusDot(tone: StatusTone, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    val alpha = if (tone.isPulsing()) {
        val transition = rememberInfiniteTransition(label = "status-pulse")
        val animated = transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "status-pulse-alpha",
        )
        animated.value
    } else {
        1f
    }
    Box(
        modifier = modifier
            .size(size)
            .alpha(alpha)
            .background(tone.toColor(), CircleShape),
    )
}
