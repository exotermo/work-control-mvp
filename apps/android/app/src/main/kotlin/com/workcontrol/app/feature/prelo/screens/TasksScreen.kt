package com.workcontrol.app.feature.prelo.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.workcontrol.app.core.components.Clipping
import com.workcontrol.app.core.components.Dateline
import com.workcontrol.app.core.components.DatelineText
import com.workcontrol.app.core.components.EmptySheet
import com.workcontrol.app.core.components.Headline
import com.workcontrol.app.core.components.InkButton
import com.workcontrol.app.core.components.InkChip
import com.workcontrol.app.core.components.InkField
import com.workcontrol.app.core.components.InkLink
import com.workcontrol.app.core.components.Kicker
import com.workcontrol.app.core.components.PaperSheet
import com.workcontrol.app.core.components.SectionCover
import com.workcontrol.app.core.components.SectionTitle
import com.workcontrol.app.core.components.Stamp
import com.workcontrol.app.core.components.Teletype
import com.workcontrol.app.core.components.stampFor
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloType
import com.workcontrol.app.feature.prelo.PreloController
import com.workcontrol.app.feature.prelo.child
import com.workcontrol.app.feature.prelo.pageState
import com.workcontrol.app.feature.prelo.relativeTime
import com.workcontrol.app.feature.prelo.rows
import com.workcontrol.app.feature.prelo.str

/** Agent display name from /api/v1/agents (id → name), falling back to the id. */
internal fun agentName(c: PreloController, agentId: String?): String =
    c.agents.rows().firstOrNull { it.str("id") == agentId || it.str("agentId") == agentId }?.str("name") ?: agentId ?: "Agente"

@Composable
fun TasksScreen(c: PreloController, padding: PaddingValues) {
    if (c.detailId != null) { TaskDetail(c, padding); return }
    val tasks = c.data.rows()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(key = "cover") { SectionCover("${c.projectName ?: "Projeto"} · Tarefas", "Pauta de trabalho", deck = "O que os agentes estão fazendo neste projeto.") }
        pageState(c, c.data != null)
        if (c.data != null && tasks.isEmpty() && c.error == null) item(key = "empty") {
            EmptySheet("Nenhuma tarefa na pauta.", "Toque em “Nova tarefa” para mandar um agente trabalhar.")
        }
        items(tasks.withIndex().toList(), key = { (_, t) -> "t-" + (t.str("id") ?: "") }) { (index, task) ->
            val id = task.str("id") ?: return@items
            TaskClipping(c, task, index) { c.openItem(id) }
        }
    }
}

@Composable
private fun TaskClipping(c: PreloController, task: JsonElement, index: Int, onClick: () -> Unit) {
    val id = task.str("id") ?: ""
    val (label, tone) = stampFor(task.str("status"))
    val first = c.firstSight("task-$id")
    Clipping(seed = id.hashCode(), index = index, animateEntrance = first, onClick = onClick, onClickLabel = "Abrir tarefa") {
        Kicker(agentName(c, task.str("agentId")) + if (task.str("source") == "MESSAGING") " · WhatsApp" else "")
        Headline(task.str("description") ?: "Tarefa")
        Dateline {
            DatelineText(listOfNotNull(relativeTime(task.str("createdAt")), "nº ${id.take(8)}").joinToString(" · "), Modifier.weight(1f))
            Stamp(label, tone, animate = first)
        }
    }
}

@Composable
private fun TaskDetail(c: PreloController, padding: PaddingValues) {
    val task = c.detail
    val execution = c.execution
    val status = execution.str("status") ?: task.str("status")
    val running = status in listOf("RUNNING", "QUEUED", "PENDING", "CREATED")
    val turns = c.turns.rows()
    val transcript = buildString {
        turns.forEach { turn ->
            append("› ").append(turn.str("kind") ?: "TURNO")
            (turn.str("output") ?: turn.str("error"))?.takeIf { it.isNotBlank() }?.let { append("\n").append(it.trim()) }
            append("\n\n")
        }
        (execution.str("result") ?: execution.str("error"))?.takeIf { it.isNotBlank() }?.let { append(it.trim()) }
    }.trim()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "back") { InkLink("← Voltar à pauta", onClick = { c.closeDetail() }) }
        pageState(c, task != null)
        if (task != null) {
            item(key = "head") {
                val (label, tone) = stampFor(task.str("status"))
                Column {
                    Kicker(agentName(c, task.str("agentId")))
                    Text(task.str("description") ?: "Tarefa", Modifier.padding(top = 4.dp, bottom = 10.dp),
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = Prelo.colors.ink)
                    Dateline {
                        DatelineText(listOfNotNull(relativeTime(task.str("createdAt")), "nº ${(task.str("id") ?: "").take(8)}")
                            .joinToString(" · "), Modifier.weight(1f))
                        Stamp(label, tone)
                    }
                }
            }
            item(key = "exec-title") { SectionTitle("Execução") }
            item(key = "exec") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (execution == null) EmptySheet("Ainda não executada.")
                    else {
                        val meta = listOfNotNull(execution.str("provider"), execution.str("model")).joinToString(" · ")
                        if (meta.isNotBlank()) Text("MODELO $meta".uppercase(), style = PreloType.Telemetry, color = Prelo.colors.faint)
                        Teletype(transcript, running = running)
                    }
                }
            }
            val tree = c.tree
            val children = tree.child("children").rows()
            if (children.isNotEmpty()) {
                item(key = "tree-title") { SectionTitle("Subtarefas", count = children.size) }
                item(key = "tree") { TreeRows(children, depth = 0, onOpen = { id -> c.openTask(id) }) }
            }
        }
    }
}

/** Indented tree with ink rules, for subtasks and the pipeline. */
@Composable
internal fun TreeRows(nodes: List<JsonElement>, depth: Int, onOpen: (String) -> Unit, statusField: String = "status") {
    val palette = Prelo.colors
    Column {
        nodes.forEach { node ->
            val (label, tone) = stampFor(node.str(statusField) ?: node.str("status"))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = (depth * 18).dp)
                    .drawBehind {
                        if (depth > 0) drawLine(palette.rule.copy(alpha = 0.5f), Offset(-9.dp.toPx(), 0f), Offset(-9.dp.toPx(), size.height), 1.dp.toPx())
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(if (depth == 0) "■" else "└", style = PreloType.Telemetry, color = palette.accentInk)
                Text(node.str("description") ?: "—", Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = palette.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Stamp(label, tone, rotation = -3f, animate = false)
                node.str("taskId")?.let { id -> InkLink("abrir", onClick = { onOpen(id) }) }
            }
            val kids = node.child("children").rows()
            if (kids.isNotEmpty()) TreeRows(kids, depth + 1, onOpen, statusField)
        }
    }
}

/** "Nova pauta": description + agent chips (from /api/v1/agents), then create and execute. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewTaskSheet(c: PreloController, onDismiss: () -> Unit) {
    var description by remember { mutableStateOf("") }
    var agentId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c.ensureAgents() }
    PaperSheet(onDismiss) {
        Kicker("Nova pauta")
        Text("O que o agente deve fazer?", Modifier.padding(top = 4.dp, bottom = 14.dp),
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = Prelo.colors.ink)
        InkField(description, { description = it }, "Descrição da tarefa", Modifier.fillMaxWidth(), singleLine = false, minLines = 3)
        Text("AGENTE", Modifier.padding(top = 16.dp, bottom = 8.dp), style = PreloType.Kicker, color = Prelo.colors.kicker)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InkChip("Automático", agentId == null, onClick = { agentId = null })
            c.agents.rows().forEach { agent ->
                val id = agent.str("id") ?: agent.str("agentId") ?: return@forEach
                InkChip(agent.str("name") ?: id, agentId == id, onClick = { agentId = id })
            }
        }
        c.error?.let { Text(it, Modifier.padding(top = 12.dp), color = Prelo.colors.bad) }
        Row(Modifier.fillMaxWidth().padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            InkLink("Cancelar", onDismiss)
            Spacer(Modifier.weight(1f))
            InkButton("Criar e executar", onClick = { c.createAndExecute(description.trim(), agentId) { onDismiss() } },
                enabled = !c.busy && description.isNotBlank())
        }
        Spacer(Modifier.width(1.dp))
    }
}
