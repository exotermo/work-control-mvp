package com.workcontrol.app.feature.approval

import androidx.compose.runtime.Composable
import com.workcontrol.app.core.components.PlaceholderScreen

@Composable
fun ApprovalScreen(approvalId: String, onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Aprovação Necessária",
        subtitle = approvalId,
        note = "Tela 09 do Diário de Bordo — o gate de segurança mais crítico do produto, pendente de port.",
        onBack = onBack,
    )
}
