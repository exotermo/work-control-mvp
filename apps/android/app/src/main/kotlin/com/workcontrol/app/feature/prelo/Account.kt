package com.workcontrol.app.feature.prelo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.components.InkLink
import com.workcontrol.app.core.components.SectionCover
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloType

/** Conta: cover + the session-owned content (notifications, devices, theme, logout). */
@Composable
internal fun AccountPage(c: PreloController, padding: PaddingValues, content: @Composable () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item(key = "back") { InkLink("← Índice", onClick = { c.open(Page.MORE) }) }
        item(key = "cover") {
            SectionCover("Seção · Conta", "Sua conta",
                deck = "${c.me.email} · ${if (c.me.role == "ADMIN") "administrador" else "operador"}")
        }
        item(key = "content") { content() }
    }
}

/** One project in the project picker: serif name, ✓ on the current one. */
@Composable
internal fun ProjectRow(name: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val palette = Prelo.colors
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (selected) "✓" else "·", style = PreloType.Telemetry, color = palette.accentInk)
        Text(name, Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = if (selected) palette.ink else palette.text)
    }
}
