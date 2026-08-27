package com.workcontrol.app.feature.terminal

import androidx.compose.runtime.Composable
import com.workcontrol.app.core.components.PlaceholderScreen

@Composable
fun TerminalScreen(machineId: String, onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Terminal",
        subtitle = machineId,
        note = "Tela 08 do Diário de Bordo — pendente de port (canal SSH real via device-agent-go).",
        onBack = onBack,
    )
}
