package com.workcontrol.app.core.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.WcColor

/** Barra fina de progresso — equivalente ao componente `Bar` do protótipo. */
@Composable
fun WcProgressBar(
    percent: Int,
    modifier: Modifier = Modifier,
    color: Color = WcColor.Primary,
) {
    val fraction = (percent.coerceIn(0, 100)) / 100f
    val animatedFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(400), label = "progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedFraction)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}
