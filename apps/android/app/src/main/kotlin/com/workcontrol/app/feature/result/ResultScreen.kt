package com.workcontrol.app.feature.result

import androidx.compose.runtime.Composable
import com.workcontrol.app.core.components.PlaceholderScreen

@Composable
fun ResultScreen(taskId: String, onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Tarefa Concluída",
        subtitle = taskId,
        note = "Tela 14 do Diário de Bordo — pendente de port.",
        onBack = onBack,
    )
}
