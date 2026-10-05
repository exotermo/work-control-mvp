package com.workcontrol.app.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun scheme(p: PreloPalette) = if (p.night) darkColorScheme(
    primary = p.accent, onPrimary = p.onAccent, secondary = p.accentInk, onSecondary = p.desk,
    background = p.desk, onBackground = p.ink, surface = p.card, onSurface = p.ink,
    surfaceVariant = p.paper, onSurfaceVariant = p.text, surfaceContainer = p.card, surfaceContainerHigh = p.card,
    surfaceContainerLow = p.paper, outline = p.rule, outlineVariant = p.ruleSoft, error = p.bad, onError = p.desk,
) else lightColorScheme(
    primary = p.accent, onPrimary = p.onAccent, secondary = p.accentInk, onSecondary = p.card,
    background = p.desk, onBackground = p.ink, surface = p.card, onSurface = p.ink,
    surfaceVariant = p.paper, onSurfaceVariant = p.text, surfaceContainer = p.card, surfaceContainerHigh = p.card,
    surfaceContainerLow = p.paper, outline = p.rule, outlineVariant = p.ruleSoft, error = p.bad, onError = p.card,
)

/** Root theme: the paper edition or the night ("carbon") edition. */
@Composable
fun WorkControlTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val night = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.PAPER -> false
        ThemeMode.NIGHT -> true
    }
    val palette = if (night) CarbonPalette else PaperPalette
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !night
                isAppearanceLightNavigationBars = !night
            }
        }
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme(palette), typography = WorkControlTypography, shapes = WorkControlShapes, content = content)
    }
}

/** Shorthand: `Prelo.colors.ink`. */
object Prelo {
    val colors: PreloPalette
        @Composable get() = LocalPalette.current
}
