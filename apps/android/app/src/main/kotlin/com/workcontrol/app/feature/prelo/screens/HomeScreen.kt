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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import com.workcontrol.app.core.components.InkChip
import com.workcontrol.app.core.components.drawHudCorners
import com.workcontrol.app.core.components.InkLink
import com.workcontrol.app.core.components.stampFor
import com.workcontrol.app.core.designsystem.PreloMotion
import com.workcontrol.app.core.designsystem.rememberReducedMotion
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.workcontrol.app.feature.prelo.recentDestination
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
    var clientFilter by remember { mutableStateOf("ACTIVE") }
    val clients = c.clientList.filter { clientFilter == "ALL" || it.str("status") == clientFilter }.take(6)
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
                        item.str("id")?.let { c.openApproval(it, item.str("projectId")) }
                    } else item.str("taskId")?.let { c.openTask(it, item.str("projectId")) }
                }
            }
            item(key = "projects-title") { SectionTitle("Projetos", count = c.projects.size) }
            if (c.projects.isEmpty()) item(key = "projects-empty") { EmptySheet("Nenhum projeto disponível.") }
            items(c.projects.withIndex().toList(), key = { "project-" + it.value.id }) { (index, project) ->
                ProjectFolder(project.name, project.description,
                    c.clientList.firstOrNull { it.str("id") == project.clientId }?.str("name"),
                    project.memberCount, project.coverColor, project.id == c.selected,
                    index, project.id.hashCode()) { c.openProject(project.id) }
            }
            item(key = "clients-title") {
                Column {
                    SectionTitle("Clientes", count = c.clientList.size)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ACTIVE" to "Ativos", "LEAD" to "Leads", "ALL" to "Todos").forEach { (value, label) ->
                            InkChip(label, clientFilter == value, onClick = { clientFilter = value })
                        }
                    }
                }
            }
            c.clientsError?.let { message -> item(key = "clients-error") { com.workcontrol.app.core.components.ErrorClipping(message, onRetry = { c.reload() }) } }
            if (clients.isEmpty() && c.clientsError == null) item(key = "clients-empty") { EmptySheet("Nenhum cliente nesta seleção.") }
            items(clients, key = { "client-" + it.str("id") }) { client ->
                val id = client.str("id") ?: return@items
                ClientRow(client.str("name") ?: "Cliente", client.str("company"), client.str("status"),
                    client.str("projectCount"), client.str("stage"), client.child("primaryContact").str("value")) { c.openClient(id) }
            }
            item(key = "all-clients") { InkLink("Ver todos os clientes →", onClick = { c.open(Page.CLIENTS) }) }
            item(key = "recent-title") { SectionTitle("Continuar de onde parou") }
            item(key = "recent") {
                if (recent.isEmpty()) EmptySheet("Os projetos e tarefas que você abrir aparecem aqui.")
                else LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(recent, key = { (it.str("kind") ?: "") + (it.str("id") ?: "") }) { item ->
                        val kind = item.str("kind")
                        RecentCard(recentKicker[kind] ?: "Item", item.str("title") ?: "—", item.str("subtitle"),
                            relativeTime(item.str("viewedAt")), item.str("status"), seed = (item.str("id") ?: "").hashCode(),
                            onClick = recentDestination(kind, item.str("id"), item.str("projectId"))?.let { target ->
                                { when (target.page) {
                                    Page.TASKS -> c.openTask(target.id!!, target.projectId)
                                    Page.PROJECT -> c.openProject(target.projectId!!)
                                    Page.CLIENT -> c.openClient(target.id!!)
                                    else -> Unit
                                } }
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
private fun RecentCard(kicker: String, title: String, subtitle: String?, at: String?, status: String?, seed: Int, onClick: (() -> Unit)?) {
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
        if (!status.isNullOrBlank()) {
            val (label, tone) = stampFor(status)
            Stamp(label, tone, animate = false)
        }
    }
}

@Composable
private fun ProjectFolder(name: String, description: String?, clientName: String?, members: Int?, coverColor: String?, current: Boolean,
    index: Int, seed: Int, onClick: () -> Unit) {
    val palette = Prelo.colors
    val coverInk = when (coverColor) {
        "clay" -> palette.kicker
        "moss" -> palette.ok
        "ocean" -> palette.accentInk
        "plum" -> palette.bad
        "mustard" -> palette.wait
        else -> palette.ink
    }
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    val opening = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(index, reduced) {
        if (reduced) progress.snapTo(1f)
        else { delay(index.coerceAtMost(8) * 90L); progress.animateTo(1f, PreloMotion.soft()) }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)
        .graphicsLayer { alpha = progress.value; translationY = (1f - progress.value) * 24.dp.toPx();
            rotationZ = tiltFor(seed) * (1f - progress.value); rotationX = -12f * opening.value;
            scaleX = 1f + 0.035f * opening.value; scaleY = 1f + 0.035f * opening.value }
        .background(palette.paper, RoundedCornerShape(4.dp))
        .border(1.dp, palette.ruleSoft, RoundedCornerShape(4.dp))
        .drawBehind { drawRect(coverInk, size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)) }
        .drawBehind { if (opening.value > 0f) drawHudCorners(palette.accent.copy(alpha = opening.value), inset = -4.dp.toPx()) }
        .semantics { contentDescription = "Abrir projeto $name" }
        .clickable(role = Role.Button) {
            if (reduced) onClick()
            else scope.launch { opening.animateTo(1f, tween(180)); onClick() }
        }.heightIn(min = 96.dp).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(name, Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = palette.ink)
            if (current) Stamp("EM PAUTA", Tone.OK, animate = false)
        }
        if (!description.isNullOrBlank()) Text(description, style = PreloType.Body, color = palette.text, maxLines = 2)
        if (!clientName.isNullOrBlank()) Text(clientName, style = PreloType.Body, color = palette.text)
        Text(members?.let { "$it membros" } ?: "Membros: —", Modifier.padding(top = 8.dp), style = PreloType.Dateline, color = palette.faint)
    }
}

@Composable
internal fun ClientRow(name: String, company: String?, status: String?, projectCount: String?,
    stage: String? = null, primaryContact: String? = null, onClick: () -> Unit) {
    val palette = Prelo.colors
    val (label, tone) = stampFor(status)
    Row(Modifier.fillMaxWidth().semantics { contentDescription = "Abrir cliente $name" }
        .clickable(role = Role.Button, onClick = onClick).heightIn(min = 64.dp)
        .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = palette.ink)
            Text(listOfNotNull(company, projectCount?.let { "$it projetos" }).joinToString(" · "),
                style = PreloType.Dateline, color = palette.text)
            if (stage != null || primaryContact != null) Text(listOfNotNull(stage?.let { stampFor(it).first }, primaryContact).joinToString(" · "),
                style = PreloType.Dateline, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Stamp(label, tone, animate = false)
    }
}
