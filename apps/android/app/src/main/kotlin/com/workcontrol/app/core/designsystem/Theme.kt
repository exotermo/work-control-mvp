package com.workcontrol.app.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val WorkControlColorScheme = darkColorScheme(
    primary = WcColor.Primary,
    onPrimary = WcColor.Ink,
    secondary = WcColor.Accent,
    onSecondary = WcColor.Ink,
    background = WcColor.Background,
    onBackground = WcColor.Ink,
    surface = WcColor.Surface,
    onSurface = WcColor.Ink,
    surfaceVariant = WcColor.SurfaceRaised,
    onSurfaceVariant = WcColor.InkDim,
    outline = WcColor.Border,
    outlineVariant = WcColor.BorderStrong,
    error = WcColor.Bad,
    onError = WcColor.Ink,
)

/**
 * Tema raiz do app. Dark-only por decisão de produto (cabine de comando pensada para
 * ficar sempre aberta ao lado de terminal/logs) — não há branch de light theme.
 */
@Composable
fun WorkControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WorkControlColorScheme,
        typography = WorkControlTypography,
        shapes = WorkControlShapes,
        content = content,
    )
}
