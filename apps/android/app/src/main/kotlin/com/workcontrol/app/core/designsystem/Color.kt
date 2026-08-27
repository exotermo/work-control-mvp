package com.workcontrol.app.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Tokens 1:1 com o protótipo Figma (ver docs/architecture e o Diário de Bordo do design).
 * O app é dark-only por decisão de produto — não existe variante clara.
 */
object WcColor {
    val Background = Color(0xFF09090E)
    val Surface = Color(0xFF0F0F18)
    val SurfaceRaised = Color(0xFF15151F)
    val Border = Color(0x12FFFFFF)
    val BorderStrong = Color(0x24FFFFFF)

    val Ink = Color(0xFFE4E4F2)
    val InkDim = Color(0xFF9797B8)
    val InkFaint = Color(0xFF5F5F7D)

    val Primary = Color(0xFF7C3AED)
    val Accent = Color(0xFF8B5CF6)
    val AccentWash = Color(0x1F7C3AED)

    val Ok = Color(0xFF22C55E)
    val Warn = Color(0xFFEAB308)
    val Bad = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)
    val Off = Color(0xFF52525B)
}
