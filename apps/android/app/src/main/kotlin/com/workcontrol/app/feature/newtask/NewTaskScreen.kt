package com.workcontrol.app.feature.newtask

import androidx.compose.runtime.Composable
import com.workcontrol.app.core.components.PlaceholderScreen

@Composable
fun NewTaskScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Nova Tarefa",
        subtitle = "PROJETO ATLAS",
        note = "Tela 12 do Diário de Bordo — pendente de port (já existe CreateTaskUseCase pronto).",
        onBack = onBack,
    )
}
