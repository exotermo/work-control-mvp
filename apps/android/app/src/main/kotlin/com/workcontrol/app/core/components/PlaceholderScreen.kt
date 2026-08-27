package com.workcontrol.app.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.WcColor

/**
 * Casco padrão para telas ainda não portadas a partir do protótipo (fase 1 do port — ver Diário
 * de Bordo). Mantém TopBar real e navegação funcionando enquanto o conteúdo é implementado.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String?,
    note: String,
    onBack: (() -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        WorkControlTopBar(title = title, subtitle = subtitle, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Em construção", color = WcColor.Ink, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(
                text = note,
                color = WcColor.InkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
