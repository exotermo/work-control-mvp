package com.workcontrol.app.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloMotion
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.core.designsystem.rememberReducedMotion
import kotlinx.coroutines.delay

/**
 * Section cover (".project-cover"): kicker, a big crooked Fraunces headline that is pasted onto the
 * page (".page-header h2" headline-paste: drops in rotated and oversized, then settles), italic deck.
 */
@Composable
fun SectionCover(kicker: String, title: String, modifier: Modifier = Modifier, deck: String? = null) {
    val palette = Prelo.colors
    val reduced = rememberReducedMotion()
    val paste = remember(title) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(title) {
        if (paste.value < 1f) { delay(120); paste.animateTo(1f, PreloMotion.paste()) }
    }
    Column(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp)) {
        Kicker(kicker)
        Text(
            title,
            Modifier
                .padding(top = 4.dp)
                .semantics { heading() }
                .graphicsLayer {
                    val q = 1f - paste.value
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    translationX = -10.dp.toPx() * q
                    translationY = -14.dp.toPx() * q
                    rotationZ = -1f - 4f * q
                    scaleX = 1f + 0.22f * q
                    scaleY = 1f + 0.22f * q
                    alpha = (paste.value * 1.5f).coerceIn(0f, 1f)
                },
            style = androidx.compose.material3.MaterialTheme.typography.displaySmall, color = palette.ink,
        )
        if (deck != null) Text(deck, Modifier.padding(top = 8.dp), style = PreloType.Deck, color = palette.text)
    }
}

/**
 * Bottom sheet on fresh paper whose content unfolds like a folded sheet (".wizard" paper-unfold:
 * rotationX from -66° with a soft spring).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val palette = Prelo.colors
    val reduced = rememberReducedMotion()
    val unfold = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { if (unfold.value < 1f) unfold.animateTo(1f, PreloMotion.soft()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.card,
        contentColor = palette.ink,
        scrimColor = palette.ink.copy(alpha = 0.35f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val q = 1f - unfold.value
                    cameraDistance = 16f * density
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    rotationX = -66f * q
                    rotationZ = -2f * q
                    translationY = -28.dp.toPx() * q
                    alpha = (unfold.value * 1.4f).coerceIn(0f, 1f)
                }
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            content = content,
        )
    }
}
