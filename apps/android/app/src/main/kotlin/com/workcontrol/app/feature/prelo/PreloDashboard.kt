package com.workcontrol.app.feature.prelo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.workcontrol.app.data.auth.DevicePreferences
import com.workcontrol.app.data.prelo.ApprovalDecision
import com.workcontrol.app.data.prelo.ApprovalDecisions
import com.workcontrol.app.data.prelo.ApproveOutcome
import com.workcontrol.app.data.prelo.CreateTask
import com.workcontrol.app.data.prelo.Me
import com.workcontrol.app.data.prelo.PreloResourceApi
import com.workcontrol.app.data.prelo.PreloEvents
import com.workcontrol.app.data.prelo.Project
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collect
import retrofit2.HttpException

private enum class Page(val title: String) {
    HOME("Início"), TASKS("Tarefas"), APPROVALS("Aprovações"),
    SERVERS("Máquinas"), PIPELINE("Pipeline"), DEPLOYS("Deploys"), FILES("Arquivos")
}

/** Every displayed item comes from the Prelo API; local state only selects a view. */
@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
fun PreloDashboard(me: Me, api: PreloResourceApi, preferences: DevicePreferences,
    decisions: ApprovalDecisions, events: PreloEvents, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(Page.HOME) }
    var selected by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<JsonElement?>(null) }
    var detail by remember { mutableStateOf<JsonElement?>(null) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var execution by remember { mutableStateOf<JsonElement?>(null) }
    var tree by remember { mutableStateOf<JsonElement?>(null) }
    var turns by remember { mutableStateOf<JsonElement?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var description by remember { mutableStateOf("") }
    var agentId by remember { mutableStateOf("") }
    var agents by remember { mutableStateOf<JsonElement?>(null) }
    var stepUpId by remember { mutableStateOf<String?>(null) }
    var totp by remember { mutableStateOf("") }
    var adminProjects by remember { mutableStateOf<List<Project>>(emptyList()) }
    val connected by events.connected.collectAsState()

    LaunchedEffect(page, selected, detailId) {
        events.events.filter { event ->
            (event.projectId == null || event.projectId == selected) && when (event.kind) {
                "resync" -> true
                "task", "execution" -> page in listOf(Page.HOME, Page.TASKS, Page.PIPELINE) &&
                    (page != Page.TASKS || detailId == null || event.id == detailId ||
                        event.taskId == detailId || event.parentId == detailId)
                "approval" -> page in listOf(Page.HOME, Page.APPROVALS) &&
                    (page != Page.APPROVALS || detailId == null || event.id == detailId)
                "action" -> page == Page.DEPLOYS
                else -> false
            }
        }.debounce(150).collect { refresh++ }
    }
    LaunchedEffect(page, selected, connected) {
        while (true) { delay(if (connected) 45_000 else 10_000); refresh++ }
    }

    LaunchedEffect(me.userId) {
        val remembered = preferences.projectId()
        selected = remembered?.takeIf { id -> me.projects.any { it.id == id } } ?: me.projects.firstOrNull()?.id
        if (me.role == "ADMIN") {
            runCatching { api.projects() }.getOrNull()?.let { result ->
                adminProjects = result.rows().mapNotNull { item ->
                    val id = item.str("id") ?: return@mapNotNull null
                    Project(id, item.str("name") ?: id, item.str("clientId"))
                }
                if (remembered != null && adminProjects.any { it.id == remembered }) selected = remembered
            }
        }
        if (selected != remembered) preferences.setProjectId(selected)
    }
    LaunchedEffect(page, selected) {
        detail = null; detailId = null; execution = null; tree = null; turns = null
    }
    LaunchedEffect(page, selected, refresh) {
        data = null; error = null
        try {
            data = when (page) {
                Page.HOME -> api.home()
                Page.TASKS -> api.tasks()
                Page.APPROVALS -> api.approvals()
                Page.SERVERS -> api.servers()
                Page.PIPELINE -> api.pipeline()
                Page.DEPLOYS -> selected?.let { api.deploys(it) }
                Page.FILES -> selected?.let { api.files(it) }
            }
            if (page == Page.TASKS) agents = runCatching { api.agents() }.getOrNull()
            detailId?.let { id ->
                detail = when (page) {
                    Page.TASKS -> api.task(id)
                    Page.APPROVALS -> api.approval(id)
                    Page.SERVERS -> api.server(id)
                    else -> null
                }
                if (page == Page.TASKS) {
                    execution = runCatching { api.latest(id) }.getOrNull()
                    val executionId = execution?.str("executionId")
                    if (executionId != null) {
                        execution = runCatching { api.execution(id, executionId) }.getOrNull() ?: execution
                        turns = runCatching { api.turns(id, executionId) }.getOrNull()
                    }
                }
            }
        } catch (failure: Exception) { error = displayError(failure) }
    }
    fun action(block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true; error = null
            try { block() } catch (failure: Exception) { error = displayError(failure) }
            finally { busy = false }
        }
    }
    fun openItem(id: String) = action {
        detailId = id
        detail = when (page) {
            Page.TASKS -> api.task(id)
            Page.APPROVALS -> api.approval(id)
            Page.SERVERS -> api.server(id)
            else -> null
        }
        if (page == Page.TASKS) {
            tree = runCatching { api.tree(id) }.getOrNull()
            execution = runCatching { api.latest(id) }.getOrNull()
            val executionId = execution?.str("executionId")
            if (executionId != null) {
                execution = runCatching { api.execution(id, executionId) }.getOrNull() ?: execution
                turns = runCatching { api.turns(id, executionId) }.getOrNull()
            }
        }
    }

    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${me.workspaceName} · ${me.email}")
        val choices = if (me.role == "ADMIN" && adminProjects.isNotEmpty()) adminProjects else me.projects
        if (choices.isNotEmpty()) {
            Text("Projeto")
            choices.forEach { project ->
                Button(onClick = { action { selected = project.id; preferences.setProjectId(project.id) } }, enabled = !busy) {
                    Text("${if (selected == project.id) "✓ " else ""}${project.name}")
                }
            }
        }
        Page.entries.forEach { option ->
            Button(onClick = { page = option }, enabled = page != option) { Text(option.title) }
        }
        Row {
            Button(onClick = { refresh++ }, enabled = !busy) { Text("Atualizar") }
        }
        if (busy || data == null && error == null) Text("Carregando…")
        error?.let { Text(it) }

        if (page == Page.TASKS && detailId == null) {
            OutlinedTextField(description, { description = it }, label = { Text("Nova tarefa") }, modifier = Modifier.fillMaxWidth())
            Text("Agentes disponíveis: ${agents.rows().joinToString { it.str("name") ?: it.str("agentId") ?: "Agente" }}")
            OutlinedTextField(agentId, { agentId = it }, label = { Text("ID do agente (opcional)") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { action {
                val created = api.createTask(CreateTask(description, agentId.ifBlank { null }))
                val id = requireNotNull(created.str("id")) { "Resposta sem ID de tarefa" }
                api.execute(id)
                description = ""; refresh++
            } }, enabled = !busy && description.isNotBlank()) { Text("Criar e executar") }
        }

        if (detailId == null) {
            val items = if (page == Page.HOME) data?.obj()?.get("recent").rows() + data?.obj()?.get("pending").rows()
                else data.rows()
            items.forEach { item ->
                val id = item.str("id") ?: item.str("taskId")
                Text(item.line(page), modifier = Modifier.padding(vertical = 4.dp))
                if (id != null && page in listOf(Page.TASKS, Page.APPROVALS, Page.SERVERS)) {
                    Button(onClick = { openItem(id) }, enabled = !busy) { Text("Abrir") }
                }
            }
            if (items.isEmpty() && data != null) Text("Nenhum item.")
        } else {
            Button(onClick = { detailId = null; detail = null }, enabled = !busy) { Text("Voltar") }
            detail?.let { Text(it.line(page)) }
            if (page == Page.TASKS) {
                execution?.let { Text("Execução: ${it.str("status") ?: "—"}\n${it.str("result") ?: it.str("error") ?: ""}") }
                tree.rows().forEach { Text("Subtarefa: ${it.str("description") ?: it.str("id") ?: "—"}") }
                turns.rows().forEach { Text("${it.str("kind") ?: it.str("type") ?: "Turno"}: ${it.str("output") ?: ""}") }
            }
            if (page == Page.APPROVALS) {
                val id = detailId!!
                Button(onClick = { action {
                    when (decisions.approve(id)) {
                        ApproveOutcome.Done -> refresh++
                        ApproveOutcome.TotpRequired -> stepUpId = id
                    }
                } }, enabled = !busy) { Text("Aprovar") }
                Button(onClick = { action { decisions.deny(id); refresh++ } }, enabled = !busy) { Text("Negar") }
                if (stepUpId == id) {
                    Text("Confirme esta aprovação com o código TOTP.")
                    OutlinedTextField(totp, { totp = it }, label = { Text("Código TOTP") })
                    Button(onClick = { action { decisions.approve(id, totp); totp = ""; stepUpId = null; refresh++ } },
                        enabled = !busy && totp.length == 6) { Text("Confirmar aprovação") }
                }
            }
            if (page == Page.SERVERS) {
                Button(onClick = { action { detail = api.healthCheck(detailId!!); } }, enabled = !busy) {
                    Text("Verificar saúde")
                }
            }
        }
    }
}

private fun JsonElement?.obj(): JsonObject? = this?.takeIf { it.isJsonObject }?.asJsonObject
private fun JsonElement?.str(name: String): String? = this.obj()?.get(name)?.takeIf { !it.isJsonNull }?.let {
    if (it.isJsonPrimitive) it.asString else null
}
private fun JsonElement?.rows(): List<JsonElement> = when {
    this == null || isJsonNull -> emptyList()
    isJsonArray -> asJsonArray.toList()
    isJsonObject -> obj()?.get("items").rows()
    else -> emptyList()
}
private fun JsonElement.line(page: Page): String = when (page) {
    Page.TASKS -> "${str("description") ?: "Tarefa"} · ${str("status") ?: "—"}"
    Page.APPROVALS -> "${str("scope") ?: "Aprovação"} · ${str("status") ?: "—"}"
    Page.SERVERS -> "${str("name") ?: "Máquina"} · ${str("lastStatus") ?: "—"}"
    Page.DEPLOYS -> {
        val payload = obj()?.get("payload")
        val result = obj()?.get("result")
        "${str("impact") ?: "Deploy"} · ${str("status") ?: "—"}\n" +
            "${payload.str("repository") ?: ""}@${payload.str("commitSha")?.take(8) ?: ""} · " +
            "${payload.str("environment") ?: ""} → ${payload.str("target") ?: ""}\n" +
            "${result.str("status") ?: ""} ${result.str("url") ?: ""}"
    }
    Page.FILES -> "${str("name") ?: "Arquivo"} · ${str("id") ?: ""}"
    Page.PIPELINE -> "${str("description") ?: "Pipeline"} · ${str("pipelineStatus") ?: "—"}"
    Page.HOME -> "${str("title") ?: "Atividade"} · ${str("status") ?: str("detail") ?: ""}"
}
private fun displayError(failure: Exception): String = when (failure) {
    is HttpException -> when (failure.code()) {
        401 -> "Entre novamente para continuar."
        403 -> "Sem acesso."
        404 -> "Recurso não encontrado."
        else -> "Erro do servidor (${failure.code()})."
    }
    else -> "Sem conexão com o Prelo."
}
