package com.workcontrol.app.feature.session

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.components.Clipping
import com.workcontrol.app.core.components.ClippingFooter
import com.workcontrol.app.core.components.DoubleRule
import com.workcontrol.app.core.components.GhostButton
import com.workcontrol.app.core.components.Headline
import com.workcontrol.app.core.components.InkButton
import com.workcontrol.app.core.components.InkChip
import com.workcontrol.app.core.components.InkLink
import com.workcontrol.app.core.components.Kicker
import com.workcontrol.app.core.components.Rule
import com.workcontrol.app.core.components.SectionTitle
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.core.designsystem.ThemeMode
import com.workcontrol.app.data.prelo.MobileSession

/** Front page of the signed-out "access edition". */
@Composable
internal fun AccessMasthead() {
    val palette = Prelo.colors
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text("EDIÇÃO DE ACESSO · CANAL SEGURO", style = PreloType.Telemetry, color = palette.faint)
        Text("PRELO", Modifier.rotate(-1.5f).padding(top = 6.dp), style = androidx.compose.material3.MaterialTheme.typography.displaySmall.copy(
            fontSize = androidx.compose.material3.MaterialTheme.typography.displaySmall.fontSize * 1.5f,
            lineHeight = androidx.compose.material3.MaterialTheme.typography.displaySmall.lineHeight * 1.4f), color = palette.ink)
        Text("CONTROL", Modifier.rotate(-1.5f), style = androidx.compose.material3.MaterialTheme.typography.displaySmall.copy(
            fontSize = androidx.compose.material3.MaterialTheme.typography.displaySmall.fontSize * 1.5f,
            lineHeight = androidx.compose.material3.MaterialTheme.typography.displaySmall.lineHeight * 1.4f), color = palette.accent)
        Text("A central de comando dos seus agentes, edição de bolso.", Modifier.padding(top = 10.dp, bottom = 12.dp),
            style = PreloType.Deck, color = palette.text)
        DoubleRule()
    }
}

/** Banner on the front page asking for the notification permission (Android 13+). */
@Composable
internal fun NotificationNotice(onAllow: () -> Unit) {
    Clipping(seed = 21, animateEntrance = false, modifier = Modifier.padding(top = 8.dp)) {
        Kicker("Aviso")
        Text("Ative notificações para receber alertas de aprovações e deploys.", style = PreloType.Body, color = Prelo.colors.ink)
        ClippingFooter {
            Spacer(Modifier.weight(1f))
            InkButton("Permitir notificações", onClick = onAllow)
        }
    }
}

/** Conta: notifications, edition (theme), connected devices and sign out. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccountContent(
    serverPush: Boolean, pushWanted: Boolean, notificationsAllowed: Boolean,
    onAllowNotifications: () -> Unit, onTogglePush: () -> Unit,
    themeMode: ThemeMode, onThemeMode: (ThemeMode) -> Unit,
    devices: List<MobileSession>, onLoadDevices: () -> Unit, onRevoke: (MobileSession) -> Unit,
    onLogout: () -> Unit, busy: Boolean, error: String?,
) {
    val palette = Prelo.colors
    Column {
        SectionTitle("Notificações")
        Text(
            when {
                !serverPush -> "Notificações ainda não ativadas no servidor."
                pushWanted -> "Você recebe um aviso no celular quando surge uma aprovação ou um deploy termina."
                else -> "Notificações desligadas neste aparelho."
            },
            style = PreloType.Body, color = palette.text,
        )
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(if (pushWanted) "Desligar notificações" else "Ligar notificações", onClick = onTogglePush, enabled = !busy)
            if (!notificationsAllowed) InkButton("Permitir notificações", onClick = onAllowNotifications)
        }

        SectionTitle("Edição")
        Text("Papel claro de dia, papel carbono à noite — ou siga o aparelho.", style = PreloType.Body, color = palette.text)
        FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode -> InkChip(mode.label, themeMode == mode, onClick = { onThemeMode(mode) }) }
        }

        SectionTitle("Aparelhos conectados", trailing = { InkLink(if (devices.isEmpty()) "Ver" else "Atualizar", onClick = onLoadDevices) })
        devices.forEach { item ->
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.deviceName, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = palette.ink)
                    Text(item.platform.uppercase() + if (item.current) " · ESTE APARELHO" else "", style = PreloType.Telemetry,
                        color = if (item.current) palette.accentInk else palette.faint)
                }
                InkLink("Revogar", onClick = { onRevoke(item) })
            }
            Rule()
        }

        error?.let { Text(it, Modifier.padding(top = 16.dp), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = palette.bad) }
        GhostButton("Sair", onClick = onLogout, enabled = !busy, danger = true,
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp))
    }
}
