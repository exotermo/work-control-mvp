package com.workcontrol.app.feature.prelo.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.components.Clipping
import com.workcontrol.app.core.components.EmptySheet
import com.workcontrol.app.core.components.InkChip
import com.workcontrol.app.core.components.InkLink
import com.workcontrol.app.core.components.Kicker
import com.workcontrol.app.core.components.SectionCover
import com.workcontrol.app.core.components.SectionTitle
import com.workcontrol.app.core.components.Stamp
import com.workcontrol.app.core.components.stampFor
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.feature.prelo.Page
import com.workcontrol.app.feature.prelo.PreloController
import com.workcontrol.app.feature.prelo.child
import com.workcontrol.app.feature.prelo.pageState
import com.workcontrol.app.feature.prelo.relativeTime
import com.workcontrol.app.feature.prelo.rows
import com.workcontrol.app.feature.prelo.str

@Composable
fun ProjectScreen(c: PreloController, padding: PaddingValues) {
    val project = c.projects.firstOrNull { it.id == c.selected }
    val sections = listOf(
        Page.TASKS to "Tarefas", Page.APPROVALS to "Aprovações", Page.DEPLOYS to "Deploys",
        Page.SERVERS to "Máquinas", Page.FILES to "Arquivos", Page.PIPELINE to "Pipeline",
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { InkLink("← Voltar", onClick = { c.back() }) }
        item { SectionCover("Dossiê · ${c.me.workspaceName}", project?.name ?: "Projeto",
            deck = project?.description ?: "Projeto em pauta no Prelo.") }
        project?.clientId?.let { clientId ->
            item { InkLink("Abrir cliente →", onClick = { c.openClient(clientId) }) }
        }
        item {
            Clipping(seed = (project?.id ?: "").hashCode(), animateEntrance = false) {
                Kicker("Números da edição")
                Text("${c.projectTasks ?: "—"} tarefas em execução · ${c.projectApprovals ?: "—"} aprovações pendentes",
                    style = PreloType.Body, color = Prelo.colors.ink)
                Text("Último deploy: ${c.projectLastDeploy ?: "—"}", style = PreloType.Dateline, color = Prelo.colors.text)
            }
        }
        item { SectionTitle("Sumário") }
        items(sections, key = { it.first.name }) { (page, title) ->
            Row(Modifier.fillMaxWidth().semantics { contentDescription = "Abrir seção $title" }
                .clickable(role = Role.Button, onClick = { c.open(page) })
                .heightIn(min = 56.dp).padding(vertical = 12.dp)) {
                Text(title, Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = Prelo.colors.ink)
                Text("→", style = PreloType.Telemetry, color = Prelo.colors.accentInk)
            }
        }
    }
}

@Composable
fun ClientsScreen(c: PreloController, padding: PaddingValues) {
    var filter by remember { mutableStateOf("ALL") }
    val clients = c.data.rows().filter { filter == "ALL" || it.str("status") == filter }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { InkLink("← Voltar", onClick = { c.back() }) }
        item { SectionCover("Índice · Clientes", "Carteira de clientes", deck = "Contatos e projetos sob os cuidados do Prelo.") }
        pageState(c, c.data != null)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ACTIVE" to "Ativos", "LEAD" to "Leads", "ALL" to "Todos").forEach { (key, label) ->
                    InkChip(label, filter == key, onClick = { filter = key })
                }
            }
        }
        if (clients.isEmpty() && c.error == null && !c.loading) item { EmptySheet("Nenhum cliente nesta seleção.") }
        items(clients, key = { it.str("id") ?: it.hashCode().toString() }) { client ->
            val id = client.str("id") ?: return@items
            ClientRow(client.str("name") ?: "Cliente", client.str("company"), client.str("status"),
                client.str("projectCount"), client.str("stage"), client.child("primaryContact").str("value")) { c.openClient(id) }
        }
    }
}

@Composable
fun ClientScreen(c: PreloController, padding: PaddingValues) {
    val client = c.data
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val optedOut = client.str("optedOutAt") != null
    val timeline = c.timeline
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { InkLink("← Voltar", onClick = { c.back() }) }
        item { SectionCover("Dossiê · Cliente", client.str("name") ?: "Cliente",
            deck = listOfNotNull(client.str("company"), client.str("city")).joinToString(" · ")) }
        pageState(c, client != null)
        if (client != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val (status, statusTone) = stampFor(client.str("status"))
                    val (stage, stageTone) = stampFor(client.str("stage"))
                    Stamp(status, statusTone, animate = false)
                    Stamp(stage, stageTone, animate = false)
                }
            }
            if (optedOut) item { Text("Pediu para não ser contatado", style = PreloType.Body, color = Prelo.colors.bad) }
            client.str("website")?.takeIf { it.startsWith("https://") }?.let { site ->
                item { InkLink("Abrir site →", onClick = { runCatching { uriHandler.openUri(site) } }) }
            }
            client.str("notes")?.takeIf { it.isNotBlank() }?.let { notes ->
                item { Clipping(seed = notes.hashCode(), animateEntrance = false) { Kicker("Notas"); Text(notes, style = PreloType.Body, color = Prelo.colors.text) } }
            }
            item { SectionTitle("Contatos") }
            val contacts = client.child("contacts").rows()
            if (contacts.isEmpty()) item { EmptySheet("Nenhum contato cadastrado.") }
            items(contacts, key = { it.str("id") ?: it.hashCode().toString() }) { contact ->
                val kind = contact.str("kind")
                val value = contact.str("value")
                val target = contactUri(kind, value, optedOut)
                if (target != null) InkLink("${kind ?: "Contato"}: ${value ?: "—"}", onClick = {
                    if (kind == "PHONE") context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(target)))
                    else runCatching { uriHandler.openUri(target) }
                })
                else Text("${kind ?: "Contato"}: ${value ?: "—"}", style = PreloType.Body, color = Prelo.colors.text)
            }
            item { SectionTitle("Projetos") }
            val projects = client.child("projects").rows()
            if (projects.isEmpty()) item { EmptySheet("Nenhum projeto vinculado.") }
            items(projects, key = { it.str("id") ?: it.hashCode().toString() }) { project ->
                val id = project.str("id") ?: return@items
                InkLink("${project.str("name") ?: "Projeto"} →", onClick = { c.openProject(id) })
            }
            item { SectionTitle("Linha do tempo") }
            if (timeline.isEmpty() && !c.timelineLoading) item { EmptySheet("Nenhum acontecimento registrado.") }
            items(timeline, key = { "${it.str("kind")}-${it.str("id")}-${it.str("at")}" }) { event ->
                val kind = event.str("kind")
                val id = event.str("id")
                Clipping(seed = (kind + id).hashCode(), animateEntrance = false,
                    onClick = if (id != null && kind in listOf("TASK", "PROJECT")) ({
                        if (kind == "TASK") c.openTask(id, event.str("projectId")) else c.openProject(id)
                    }) else null, onClickLabel = "Abrir acontecimento") {
                    Kicker(event.str("at")?.let { at -> runCatching {
                        java.time.OffsetDateTime.parse(at).format(java.time.format.DateTimeFormatter.ofPattern(
                            "dd MMM yyyy · HH:mm", java.util.Locale("pt", "BR")))
                    }.getOrNull() } ?: relativeTime(event.str("at")) ?: "Linha do tempo")
                    Text(event.str("title") ?: "Acontecimento", style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = Prelo.colors.ink)
                    event.str("detail")?.let { Text(it, style = PreloType.Body, color = Prelo.colors.text) }
                    event.str("status")?.let { val (label, tone) = stampFor(it); Stamp(label, tone, animate = false) }
                }
            }
            if (!c.timelineDone && timeline.isNotEmpty()) item {
                InkLink(if (c.timelineLoading) "Carregando…" else "Carregar mais acontecimentos", onClick = { c.loadMoreTimeline() })
                LaunchedEffect(timeline.size) { c.loadMoreTimeline() }
            }
        }
    }
}
