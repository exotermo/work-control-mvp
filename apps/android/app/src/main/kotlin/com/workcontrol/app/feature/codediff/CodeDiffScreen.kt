package com.workcontrol.app.feature.codediff

import androidx.compose.runtime.Composable
import com.workcontrol.app.core.components.PlaceholderScreen

@Composable
fun CodeDiffScreen(filePath: String, onBack: () -> Unit) {
    PlaceholderScreen(
        title = filePath.substringAfterLast('/'),
        subtitle = filePath,
        note = "Tela 07 do Diário de Bordo — pendente de port (precisa de parser de diff real).",
        onBack = onBack,
    )
}
