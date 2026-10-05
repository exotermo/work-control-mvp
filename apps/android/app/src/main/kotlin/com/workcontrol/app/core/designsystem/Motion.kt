package com.workcontrol.app.core.designsystem

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * "Folhear a revista" (dashboard index.css) in Compose terms. The dashboard samples two damped
 * springs into CSS linear(): --spring-soft (~5% overshoot, page turns, unfolding) and
 * --spring-paste (~12% overshoot, things slapped onto the page). These are the same springs.
 */
object PreloMotion {
    fun <T> soft(): SpringSpec<T> = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessVeryLow * 2.4f)
    fun <T> paste(): SpringSpec<T> = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessLow)
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)

    /** Delay between clippings dropping onto the desk (".clipping" animation-delay: --i * 80ms). */
    const val STAGGER_MS = 80
    /** Stamp hits after its clipping lands. */
    const val STAMP_DELAY_MS = 520
}

/**
 * True when the user turned animations off (Developer options / Accessibility "remove animations").
 * Every decorative animation checks this and renders its final state directly.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}
