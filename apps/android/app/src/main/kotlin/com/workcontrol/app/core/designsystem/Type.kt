package com.workcontrol.app.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// O protótipo usa Inter para título/corpo e JetBrains Mono para todo rótulo/dado técnico.
// Aqui mapeamos para as famílias do sistema até os arquivos de fonte reais serem
// adicionados em res/font/ — a substituição não muda nenhuma outra parte do design system.
private val Sans = FontFamily.SansSerif
private val Mono = FontFamily.Monospace

val WorkControlTypography = Typography(
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 13.sp),
)

/** Estilos "técnicos" adicionais que o Material3 Typography não modela (rótulo eyebrow, dado mono). */
object WcType {
    val Eyebrow = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        letterSpacing = 1.5.sp,
    )
    val MonoData = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val MonoDataLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 28.sp)
}
