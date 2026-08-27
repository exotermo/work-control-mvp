package com.workcontrol.app.feature.files

import androidx.compose.runtime.Composable
import com.workcontrol.app.core.components.PlaceholderScreen

@Composable
fun FilesScreen(machineId: String, onBack: (() -> Unit)?) {
    PlaceholderScreen(
        title = "Arquivos",
        subtitle = machineId,
        note = "Tela 13 do Diário de Bordo — pendente de port.",
        onBack = onBack,
    )
}
