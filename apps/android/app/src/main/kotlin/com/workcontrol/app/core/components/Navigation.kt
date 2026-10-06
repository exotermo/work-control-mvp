package com.workcontrol.app.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloMotion
import com.workcontrol.app.core.designsystem.rememberReducedMotion

data class BarItem<T>(val key: T, val label: String, val icon: ImageVector, val badge: Int = 0)

/**
 * Bottom bar on fresh paper with a double rule on top. The active section is tilted -1° and
 * underlined in signal orange (".nav-link.active").
 */
@Composable
fun <T> PreloBottomBar(items: List<BarItem<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val palette = Prelo.colors
    Column(modifier.fillMaxWidth().background(palette.card)) {
        DoubleRule()
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceAround) {
            items.forEach { item ->
                val active = item.key == selected
                val tilt by animateFloatAsState(if (active) -1.5f else 0f, PreloMotion.paste(), label = "tilt")
                Column(
                    Modifier
                        .weight(1f)
                        .semantics { this.selected = active }
                        .clickable(role = Role.Tab, onClick = { onSelect(item.key) })
                        .padding(top = 8.dp, bottom = 6.dp)
                        .rotate(tilt),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        Icon(item.icon, contentDescription = null, tint = if (active) palette.ink else palette.text)
                        if (item.badge > 0) Counter(item.badge, Modifier.align(Alignment.TopEnd).padding(start = 18.dp).graphicsLayer {
                            translationX = 10.dp.toPx(); translationY = (-6).dp.toPx()
                        })
                    }
                    Text(item.label, style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = if (active) palette.ink else palette.text, maxLines = 1)
                    Spacer(
                        Modifier
                            .padding(top = 3.dp)
                            .width(28.dp)
                            .height(3.dp)
                            .drawBehind {
                                if (active) drawLine(palette.accent, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 3.dp.toPx())
                            },
                    )
                }
            }
        }
    }
}

/**
 * Turning a magazine page between sections (dashboard ".page-sheet"): the new sheet swings in from
 * the spine — rotationY around the left edge going forward, the right edge going back — a little
 * crooked, lands with a soft spring, and a fold shadow fades off it.
 */
@Composable
fun <T> PageTurn(target: T, modifier: Modifier = Modifier, forward: Boolean = true,
    content: @Composable (T) -> Unit) {
    val reduced = rememberReducedMotion()
    val palette = Prelo.colors
    key(target) {
        val progress = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) { if (progress.value < 1f) progress.animateTo(1f, PreloMotion.soft()) }
        Box(
            modifier
                .sheetSwing(progress.value.coerceAtMost(1.04f), forward)
                .drawWithContent {
                    drawContent()
                    val shade = (1f - progress.value).coerceIn(0f, 1f)
                    if (shade > 0f) drawRect(
                        Brush.horizontalGradient(
                            listOf(palette.ink.copy(alpha = 0.22f * shade), palette.ink.copy(alpha = 0.05f * shade), Color.Transparent),
                            startX = if (forward) 0f else size.width, endX = if (forward) size.width * 0.45f else size.width * 0.55f,
                        ),
                    )
                },
        ) { content(target) }
    }
}

/** graphicsLayer for the sheet swing (used by screens that animate their own page entrance). */
fun Modifier.sheetSwing(progress: Float, forward: Boolean): Modifier = graphicsLayer {
    val q = 1f - progress
    cameraDistance = 22f * density
    transformOrigin = TransformOrigin(if (forward) 0f else 1f, 0.5f)
    rotationY = (if (forward) -24f else 24f) * q
    rotationZ = (if (forward) 1.4f else -1.4f) * q
    translationX = (if (forward) 0.03f else -0.03f) * size.width * q
    alpha = (progress * 2.4f).coerceIn(0f, 1f)
}
