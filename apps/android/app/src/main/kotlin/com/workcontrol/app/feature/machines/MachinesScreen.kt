package com.workcontrol.app.feature.machines

import androidx.compose.runtime.Composable
import com.workcontrol.app.core.components.PlaceholderScreen

@Composable
fun MachinesScreen(onBack: (() -> Unit)?) {
    PlaceholderScreen(
        title = "Máquinas",
        subtitle = "4 REGISTRADAS · 3 ONLINE",
        note = "Tela 10 do Diário de Bordo — pendente de port.",
        onBack = onBack,
    )
}

@Composable
fun MachineDetailScreen(machineId: String, onBack: () -> Unit) {
    PlaceholderScreen(
        title = machineId,
        subtitle = "ONLINE",
        note = "Tela 11 do Diário de Bordo — pendente de port.",
        onBack = onBack,
    )
}
