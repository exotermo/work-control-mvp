package com.workcontrol.app.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * "Jornal do futuro": the Prelo dashboard's paper (prelo-dashboard/src/index.css) plus a night
 * edition. Every text pair is >= 4.5:1 (WCAG AA) on paper, desk and card in both editions.
 */
@Immutable
data class PreloPalette(
    val desk: Color,          // page background (the table the clippings lie on)
    val paper: Color,         // newspaper clipping
    val card: Color,          // fresh paper: inputs, sheets, bars
    val ink: Color,           // headlines, strong text, rules
    val text: Color,          // body text
    val faint: Color,         // datelines, hints
    val rule: Color,          // strong rules and borders
    val ruleSoft: Color,      // dashed separators, grid
    val accent: Color,        // signal orange (primary button, live, counters)
    val onAccent: Color,
    val accentInk: Color,     // orange-ish text and links
    val accentWash: Color,    // pressed rows, highlights
    val kicker: Color,
    val ok: Color,
    val bad: Color,
    val wait: Color,
    val tape: Color,          // translucent adhesive tape
    val shadow: Color,        // hard offset shadow under clippings
    val night: Boolean,
)

val PaperPalette = PreloPalette(
    desk = Color(0xFFF7F1E8),
    paper = Color(0xFFFBF4E2),
    card = Color(0xFFFFFBF4),
    ink = Color(0xFF1E1B16),
    text = Color(0xFF5A5347),
    faint = Color(0xFF6E6456),
    rule = Color(0xFF1E1B16),
    ruleSoft = Color(0xFFD9CDB8),
    accent = Color(0xFFFF5A36),
    onAccent = Color(0xFF1E1B16),
    accentInk = Color(0xFFA8481A),
    accentWash = Color(0xFFFFE9D9),
    kicker = Color(0xFF8A3D12),
    ok = Color(0xFF1F6A40),
    bad = Color(0xFF9E2F1F),
    wait = Color(0xFF8A3D12),
    tape = Color(0x80E8DCC0),
    shadow = Color(0x381E1B16),
    night = false,
)

val CarbonPalette = PreloPalette(
    desk = Color(0xFF14110D),
    paper = Color(0xFF211C16),
    card = Color(0xFF2A241C),
    ink = Color(0xFFF3EADB),
    text = Color(0xFFCFC3AE),
    faint = Color(0xFFA39882),
    rule = Color(0xB3F3EADB),
    ruleSoft = Color(0xFF4A4036),
    accent = Color(0xFFFF7A57),
    onAccent = Color(0xFF14110D),
    accentInk = Color(0xFFFFB48F),
    accentWash = Color(0xFF3B2116),
    kicker = Color(0xFFFFB27A),
    ok = Color(0xFF7FD3A0),
    bad = Color(0xFFFF8A73),
    wait = Color(0xFFFFB27A),
    tape = Color(0x40CFC3AE),
    shadow = Color(0x80000000),
    night = true,
)

val LocalPalette = staticCompositionLocalOf { PaperPalette }
