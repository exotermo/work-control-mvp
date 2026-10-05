package com.workcontrol.app.feature.prelo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.components.EmptySheet
import com.workcontrol.app.core.components.SectionCover
import com.workcontrol.app.core.components.SectionTitle
import com.workcontrol.app.core.components.Stamp
import com.workcontrol.app.core.components.Tone
import com.workcontrol.app.core.components.tiltFor
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.feature.prelo.Page
import com.workcontrol.app.feature.prelo.PreloController
import com.workcontrol.app.feature.prelo.child
import com.workcontrol.app.feature.prelo.pageState
import com.workcontrol.app.feature.prelo.relativeTime
import com.workcontrol.app.feature.prelo.rows
import com.workcontrol.app.feature.prelo.str

private val pendingStamp = mapOf(
    "APPROVAL" to ("AGUARDANDO APROVAÇÃO" to Tone.WAIT),
    "RUNNING" to ("EM EXECUÇÃO" to Tone.OK),
    "FAILED" to ("FALHOU" to Tone.BAD),
)
private val recentKicker = mapOf("CLIENT" to "Cliente", "PROJECT" to "Projeto", "TASK" to "Tarefa")

/** Front page (dashboard HomePanels): what is waiting on you, and where you left off. */
@Composable
fun HomeScreen(c: PreloController, padding: PaddingValues, notice: (@Composable () -> Unit)?) {
    val pending = c.data.child("pending").rows()
    val recent = c.data.child("recent").rows()
    val name = c.me.email.substringBefore('@').replaceFirstChar { it.uppercase() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item(key = "cover") {
            SectionCover("Primeira página · ${c.me.workspaceName}", "Olá, $name",
                deck = when {
                    c.data == null -> "Montando a edição de hoje…"
                    pending.isEmpty() -> "Nada esperando por você agora."
                    pending.size == 1 -> "Uma pendência esperando por você."
                    else -> "${pending.size} pendências esperando por você."
                })
        }
        if (notice != null) item(key = "notice") { notice() }
        pageState(c, c.data != null)
        if (c.data != null) {
            item(key = "pending-title") { SectionTitle("Pendências", count = pending.size) }
            if (pending.isEmpty()) item(key = "pending-empty") { EmptySheet("Nada esperando por você agora.") }
            items(pending, key = { "p-" + (it.str("kind") ?: "") + (it.str("id") ?: "") }) { item ->
                PendingRow(item.str("kind"), item.str("title") ?: "Pendência",
                    listOfNotNull(item.str("projectName") ?: "sem projeto", item.str("detail")).joinToString(" · "),
                    relativeTime(item.str("at"))) {
                    if (item.str("kind") == "APPROVAL") {
                        item.str("projectId")?.let { c.selectProject(it) }
                        c.open(Page.APPROVALS)
                    } else item.str("taskId")?.let { c.openTask(it) }
                }
            }
            item(key = "recent-title") { SectionTitle("Continuar de onde parou") }
            item(key = "recent") {
                if (recent.isEmpty()) EmptySheet("Os projetos e tarefas que você abrir aparecem aqui.")
                else LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(recent, key = { (it.str("kind") ?: "") + (it.str("id") ?: "") }) { item ->
                        val kind = item.str("kind")
                        RecentCard(recentKicker[kind] ?: "Item", item.str("title") ?: "—", item.str("subtitle"),
                            relativeTime(item.str("viewedAt")), seed = (item.str("id") ?: "").hashCode(),
                            onClick = when (kind) {
                                "TASK" -> ({ item.str("id")?.let { c.openTask(it) } })
                                "PROJECT" -> ({ item.str("id")?.let { c.selectProject(it) } })
                                else -> null
                            })
                    }
                }
            }
        }
    }
}

/** ".pending-row": stamp, title + detail, time — separated by dashed rules. */
@Composable
private fun PendingRow(kind: String?, title: String, detail: String, at: String?, onClick: () -> Unit) {
    val palette = Prelo.colors
    val (label, tone) = pendingStamp[kind] ?: ((kind ?: "—") to Tone.NEUTRAL)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Stamp(label, tone, rotation = -4f, delayMs = 200)
        Column(Modifier.weight(1f)) {
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = palette.ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = palette.text,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (at != null) Text(at, style = PreloType.Dateline, color = palette.faint)
    }
}

/** ".recent-card": small aged card, tilted, serif title, typewritten time. */
@Composable
private fun RecentCard(kicker: String, title: String, subtitle: String?, at: String?, seed: Int, onClick: (() -> Unit)?) {
    val palette = Prelo.colors
    Column(
        Modifier
            .width(196.dp)
            .heightIn(min = 112.dp)
            .rotate(tiltFor(seed) * 1.6f)
            .background(palette.paper, RoundedCornerShape(4.dp))
            .border(1.dp, palette.rule.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(kicker.uppercase(), style = PreloType.Kicker, color = palette.kicker)
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = palette.ink, maxLines = 2,
            overflow = TextOverflow.Ellipsis)
        if (!subtitle.isNullOrBlank()) Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (at != null) Text(at, Modifier.padding(top = 4.dp), style = PreloType.Dateline, color = palette.faint)
    }
}
