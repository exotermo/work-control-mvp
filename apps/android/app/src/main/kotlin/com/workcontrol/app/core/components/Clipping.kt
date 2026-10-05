package com.workcontrol.app.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloMotion
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.core.designsystem.rememberReducedMotion
import kotlin.math.abs
import kotlin.random.Random

/** A newspaper cut: straight sides, torn top and bottom. The tear is stable per [seed]. */
class TornEdgeShape(private val seed: Int, private val depth: Float = 6f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val d = with(density) { depth.dp.toPx() }
        val rnd = Random(seed)
        val teeth = 24
        val path = Path()
        path.moveTo(0f, rnd.nextFloat() * d)
        for (i in 1..teeth) path.lineTo(size.width * i / teeth, rnd.nextFloat() * d)
        path.lineTo(size.width, size.height - rnd.nextFloat() * d)
        for (i in teeth - 1 downTo 0) path.lineTo(size.width * i / teeth, size.height - rnd.nextFloat() * d)
        path.close()
        return Outline.Generic(path)
    }
}

/** Stable small tilt per item, like the dashboard's nth-child tilts (-0.5°, 0.45°, -0.2°). */
fun tiltFor(seed: Int): Float = listOf(-0.5f, 0.45f, -0.2f, 0.3f, -0.35f)[abs(seed) % 5]

/**
 * A clipping on the desk. Drops in rotated and settles ([index] staggers it), gets a strip of tape,
 * casts a hard offset shadow, and shows HUD corner brackets while pressed (the sci-fi touch).
 * [animateEntrance] = false skips the drop (already seen, or reduced motion).
 */
@Composable
fun Clipping(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    index: Int = 0,
    animateEntrance: Boolean = true,
    tape: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = Prelo.colors
    val reduced = rememberReducedMotion()
    val tilt = tiltFor(seed)
    val shape = remember(seed) { TornEdgeShape(seed) }
    val drop = remember { Animatable(if (animateEntrance && !reduced) 0f else 1f) }
    val tapeIn = remember { Animatable(if (animateEntrance && !reduced) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (drop.value < 1f) {
            kotlinx.coroutines.delay((index.coerceAtMost(8) * PreloMotion.STAGGER_MS).toLong())
            drop.animateTo(1f, PreloMotion.paste())
        }
        if (tapeIn.value < 1f) tapeIn.animateTo(1f, PreloMotion.soft())
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hud by animateFloatAsState(if (pressed) 1f else 0f, tween(if (reduced) 0 else 160), label = "hud")
    val density = LocalDensity.current
    val shadowDx = with(density) { 3.dp.toPx() }
    val shadowDy = with(density) { 5.dp.toPx() }

    Box(
        modifier
            .padding(top = 8.dp)
            .graphicsLayer {
                val p = drop.value
                val q = 1f - p
                translationX = 14.dp.toPx() * q
                translationY = -46.dp.toPx() * q + if (pressed) -2.dp.toPx() else 0f
                rotationZ = if (pressed) 0f else tilt + (tilt * 14f + 4f) * q
                scaleX = 1f + 0.08f * q
                scaleY = 1f + 0.08f * q
                alpha = p.coerceIn(0f, 1f)
            }
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                if (outline is Outline.Generic) translate(shadowDx, shadowDy) { drawPath(outline.path, palette.shadow) }
            }
            .drawWithContent {
                drawContent()
                if (tape) {
                    val t = tapeIn.value
                    val w = 64.dp.toPx() * (1.25f - 0.25f * t)
                    val h = 16.dp.toPx()
                    val cx = size.width * if (seed % 2 == 0) 0.5f else 0.24f
                    rotate(if (seed % 2 == 0) -3f else 4f, pivot = Offset(cx, 0f)) {
                        drawRect(palette.tape.copy(alpha = palette.tape.alpha * t), Offset(cx - w / 2, -h / 2 - 2.dp.toPx() * (1 - t)), Size(w, h))
                    }
                }
                if (hud > 0f) drawHudCorners(palette.accent.copy(alpha = hud), inset = -6.dp.toPx() * hud)
            },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .drawBehind { drawPaperTexture(palette) }
                .then(
                    if (onClick != null) Modifier.clickable(interaction, indication = null, role = Role.Button,
                        onClickLabel = onClickLabel, onClick = onClick) else Modifier,
                )
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp),
            content = content,
        )
    }
}

/** ".clipping-kicker": spaced capitals in rust. */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, style = PreloType.Kicker, color = Prelo.colors.kicker, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** ".clipping-headline". */
@Composable
fun Headline(text: String, modifier: Modifier = Modifier, maxLines: Int = 3) {
    Text(text, modifier.padding(top = 4.dp, bottom = 8.dp), style = PreloType.Headline, color = Prelo.colors.ink,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** ".clipping-dateline": double rule above, thin rule below, typewritten. */
@Composable
fun Dateline(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val palette = Prelo.colors
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val w = size.width
                drawLine(palette.rule, Offset(0f, 0f), Offset(w, 0f), 1.dp.toPx())
                drawLine(palette.rule, Offset(0f, 3.dp.toPx()), Offset(w, 3.dp.toPx()), 1.dp.toPx())
                drawLine(palette.rule, Offset(0f, size.height), Offset(w, size.height), 1.dp.toPx())
            }
            .padding(top = 7.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun DatelineText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = PreloType.Dateline, color = Prelo.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Serif body text of a clipping. */
@Composable
fun ClippingBody(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    Text(text, modifier.padding(top = 8.dp), style = PreloType.Body, color = Prelo.colors.text, maxLines = maxLines,
        overflow = TextOverflow.Ellipsis)
}

/** ".clipping-footer": dashed rule, then actions/stamp in a row. */
@Composable
fun ClippingFooter(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val palette = Prelo.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .drawBehind {
                drawLine(palette.rule.copy(alpha = 0.45f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
            }
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** HUD corner brackets ⌜⌝⌞⌟ around the drawing area ([inset] < 0 draws outside it). */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHudCorners(color: androidx.compose.ui.graphics.Color, inset: Float = 0f) {
    val l = 14.dp.toPx()
    val s = 2.dp.toPx()
    val x0 = inset
    val y0 = inset
    val x1 = size.width - inset
    val y1 = size.height - inset
    drawLine(color, Offset(x0, y0), Offset(x0 + l, y0), s); drawLine(color, Offset(x0, y0), Offset(x0, y0 + l), s)
    drawLine(color, Offset(x1, y0), Offset(x1 - l, y0), s); drawLine(color, Offset(x1, y0), Offset(x1, y0 + l), s)
    drawLine(color, Offset(x0, y1), Offset(x0 + l, y1), s); drawLine(color, Offset(x0, y1), Offset(x0, y1 - l), s)
    drawLine(color, Offset(x1, y1), Offset(x1 - l, y1), s); drawLine(color, Offset(x1, y1), Offset(x1, y1 - l), s)
}
