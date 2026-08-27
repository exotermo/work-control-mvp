package com.workcontrol.app.feature.agent

import androidx.compose.runtime.Composable
import com.workcontrol.app.core.components.PlaceholderScreen

@Composable
fun AgentDetailScreen(agentId: String, onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Agente",
        subtitle = agentId,
        note = "Tela 04-06 do Diário de Bordo (Agent Dev / QA / DevOps) — pendente de port.",
        onBack = onBack,
    )
}
