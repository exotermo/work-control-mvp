package com.workcontrol.app.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.workcontrol.app.R

// Same families as the dashboard: Fraunces (headlines), Work Sans (UI). Plus the "future" half:
// Courier Prime for typewritten details/stamps and JetBrains Mono for telemetry. All OFL, bundled
// in res/font (no Google Play services needed). Fraunces/Work Sans/JetBrains Mono are variable fonts.

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int, style: FontStyle = FontStyle.Normal) = Font(
    res, FontWeight(weight), style,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Fraunces = FontFamily(
    variable(R.font.fraunces, 600), variable(R.font.fraunces, 700), variable(R.font.fraunces, 900),
    variable(R.font.fraunces_italic, 500, FontStyle.Italic),
)
val WorkSans = FontFamily(
    variable(R.font.work_sans, 400), variable(R.font.work_sans, 500),
    variable(R.font.work_sans, 600), variable(R.font.work_sans, 700),
)
val Typewriter = FontFamily(
    Font(R.font.courier_prime, FontWeight.Normal), Font(R.font.courier_prime_bold, FontWeight.Bold),
)
val TelemetryFamily = FontFamily(
    variable(R.font.jetbrains_mono, 400), variable(R.font.jetbrains_mono, 500), variable(R.font.jetbrains_mono, 700),
)

val WorkControlTypography = Typography(
    displaySmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = TelemetryFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

/** Newspaper and telemetry styles Material's Typography doesn't model. */
object PreloType {
    /** Masthead "PRELO CONTROL". */
    val Masthead = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp)
    /** Spaced capitals above a headline (".clipping-kicker"). */
    val Kicker = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
    /** Clipping headline (".clipping-headline"). */
    val Headline = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 25.sp)
    /** Italic deck under a cover title. */
    val Deck = TextStyle(fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp)
    /** Serif body of a clipping (Georgia in the dashboard). */
    val Body = TextStyle(fontFamily = FontFamily.Serif, fontSize = 15.sp, lineHeight = 22.sp)
    /** Dateline / typewritten details. */
    val Dateline = TextStyle(fontFamily = Typewriter, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.4.sp)
    val Stamp = TextStyle(fontFamily = Typewriter, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.2.sp)
    /** Sci-fi telemetry strip and ids. */
    val Telemetry = TextStyle(fontFamily = TelemetryFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.sp)
    /** Teletype output of an execution. */
    val Teletype = TextStyle(fontFamily = TelemetryFamily, fontSize = 13.sp, lineHeight = 19.sp)
    /** Section title with a rule under it (".clipping-section-title"). */
    val Section = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.6.sp)
    /** Big short approval code. */
    val Code = TextStyle(fontFamily = TelemetryFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 6.sp)
}
