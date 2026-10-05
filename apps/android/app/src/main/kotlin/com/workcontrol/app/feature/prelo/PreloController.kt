package com.workcontrol.app.feature.prelo

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.google.gson.JsonElement
import com.workcontrol.app.data.auth.DevicePreferences
import com.workcontrol.app.data.prelo.ApprovalDecisions
import com.workcontrol.app.data.prelo.ApproveOutcome
import com.workcontrol.app.data.prelo.CreateTask
import com.workcontrol.app.data.prelo.EventFeed
import com.workcontrol.app.data.prelo.Me
import com.workcontrol.app.data.prelo.PreloResourceApi
import com.workcontrol.app.data.prelo.Project
import com.workcontrol.app.data.push.PushRouting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/** Sections of the app, in page-turn order. MORE/ACCOUNT have no server read of their own. */
enum class Page(val title: String) {
    HOME("Início"), TASKS("Tarefas"), APPROVALS("Aprovações"), DEPLOYS("Deploys"), MORE("Mais"),
    SERVERS("Máquinas"), PIPELINE("Pipeline"), FILES("Arquivos"), ACCOUNT("Conta"),
}

/**
 * Everything the signed-in screens show comes from the Prelo API; this holds what is on screen
 * and the user's actions. The behaviour is the one Codex wired in PreloDashboard (WC-2/WC-3/WC-4):
 * project choice, re-fetch on SSE events (debounced 150 ms), polling fallback 45 s / 10 s, push
 * destinations, file download and HIGH-risk step-up — only moved out of the UI.
 */
class PreloController internal constructor(
    val me: Me,
    private val api: PreloResourceApi,
    private val preferences: DevicePreferences,
    private val decisions: ApprovalDecisions,
    private val scope: CoroutineScope,
) {
    var page by mutableStateOf(Page.HOME)
        private set
    var selected by mutableStateOf<String?>(null)
        internal set
    var data by mutableStateOf<JsonElement?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var detail by mutableStateOf<JsonElement?>(null)
        private set
    var detailId by mutableStateOf<String?>(null)
        private set
    var execution by mutableStateOf<JsonElement?>(null)
        private set
    var tree by mutableStateOf<JsonElement?>(null)
        private set
    var turns by mutableStateOf<JsonElement?>(null)
        private set
    var agents by mutableStateOf<JsonElement?>(null)
        private set
    /** Result of "Verificar saúde" for the open machine. */
    var health by mutableStateOf<JsonElement?>(null)
        private set
    var error by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
        private set
    var refresh by mutableIntStateOf(0)
        private set
    /** Bumped when a live event (not the timer) triggered a refresh — drives the scanline sweep. */
    var liveTick by mutableIntStateOf(0)
        private set
    var stepUpId by mutableStateOf<String?>(null)
        private set
    var adminProjects by mutableStateOf<List<Project>>(emptyList())
        private set
    var pendingApprovals by mutableIntStateOf(0)
        private set
    internal var pendingApprovalId by mutableStateOf<String?>(null)
    private val seen = mutableSetOf<String>()

    /** True the first time a clipping is shown in this session: only then does it drop onto the desk. */
    fun firstSight(key: String): Boolean = seen.add(key)
    internal var downloadId by mutableStateOf<String?>(null)

    val projects: List<Project>
        get() = if (me.role == "ADMIN" && adminProjects.isNotEmpty()) adminProjects else me.projects
    val projectName: String?
        get() = projects.firstOrNull { it.id == selected }?.name

    fun open(target: Page) {
        page = target
    }

    fun reload() { refresh++ }

    internal fun liveReload() { liveTick++; refresh++ }

    fun selectProject(id: String) = action { preferences.setProjectId(id); selected = id }

    fun closeDetail() { detailId = null; detail = null; tree = null; execution = null; turns = null; stepUpId = null; health = null }

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
            loadExecution(id)
        }
    }

    /** Opens a task from another section (home pending/recent, pipeline). */
    fun openTask(id: String) {
        page = Page.TASKS
        pendingTaskId = id
        refresh++
    }

    private var pendingTaskId: String? = null

    fun createAndExecute(description: String, agentId: String?, done: () -> Unit) = action {
        val created = api.createTask(CreateTask(description, agentId))
        val id = requireNotNull(created.str("id")) { "Resposta sem ID de tarefa" }
        api.execute(id)
        done()
        refresh++
    }

    /** Agents for the "Nova tarefa" sheet, when it is opened outside the Tarefas page. */
    fun ensureAgents() {
        if (agents == null) scope.launch { agents = runCatching { api.agents() }.getOrNull() }
    }

    fun approve(id: String) = action {
        when (decisions.approve(id)) {
            ApproveOutcome.Done -> refresh++
            ApproveOutcome.TotpRequired -> stepUpId = id
        }
    }

    fun confirmStepUp(id: String, totp: String, done: () -> Unit) = action {
        decisions.approve(id, totp); done(); stepUpId = null; refresh++
    }

    fun cancelStepUp() { stepUpId = null }

    fun deny(id: String) = action { decisions.deny(id); refresh++ }

    fun healthCheck(id: String) = action { health = api.healthCheck(id) }

    fun requestDownload(id: String) { downloadId = id }

    internal fun saveDownload(context: Context, uri: Uri?) {
        val id = downloadId
        val projectId = selected
        downloadId = null
        if (uri != null && id != null && projectId != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    api.fileContent(projectId, id).use { body ->
                        val output = requireNotNull(context.contentResolver.openOutputStream(uri))
                        output.use { body.byteStream().use { input -> input.copyTo(it) } }
                    }
                }
            } catch (failure: Exception) { error = displayError(failure) }
        }
    }

    private suspend fun loadExecution(id: String) {
        execution = runCatching { api.latest(id) }.getOrNull()
        val executionId = execution?.str("executionId")
        if (executionId != null) {
            execution = runCatching { api.execution(id, executionId) }.getOrNull() ?: execution
            turns = runCatching { api.turns(id, executionId) }.getOrNull()
        }
    }

    internal suspend fun initProjects() {
        val remembered = preferences.projectId()
        val initial = remembered?.takeIf { id -> me.projects.any { it.id == id } } ?: me.projects.firstOrNull()?.id
        preferences.setProjectId(initial)
        selected = initial
        if (me.role == "ADMIN") {
            runCatching { api.projects() }.getOrNull()?.let { result ->
                adminProjects = result.rows().mapNotNull { item ->
                    val id = item.str("id") ?: return@mapNotNull null
                    Project(id, item.str("name") ?: id, item.str("clientId"))
                }
                if (remembered != null && adminProjects.any { it.id == remembered }) {
                    preferences.setProjectId(remembered)
                    selected = remembered
                }
            }
        }
    }

    internal fun resetDetail() {
        detail = null; detailId = null; execution = null; tree = null; turns = null; health = null
    }

    private var loadedKey: Pair<Page, String?>? = null

    internal suspend fun load() {
        // A refresh (live event, polling) keeps the current page on screen; only a new section or
        // project starts from an empty sheet.
        val key = page to selected
        if (key != loadedKey) data = null
        error = null; loading = true
        try {
            data = when (page) {
                Page.HOME -> api.home()
                Page.TASKS -> api.tasks()
                Page.APPROVALS -> api.approvals()
                Page.SERVERS -> api.servers()
                Page.PIPELINE -> api.pipeline()
                Page.DEPLOYS -> selected?.let { api.deploys(it) }
                Page.FILES -> selected?.let { api.files(it) }
                Page.MORE, Page.ACCOUNT -> null
            }
            loadedKey = key
            if (page == Page.TASKS) agents = runCatching { api.agents() }.getOrNull()
            if (page == Page.TASKS && pendingTaskId != null) { detailId = pendingTaskId; pendingTaskId = null }
            (detailId ?: pendingApprovalId)?.let { id ->
                detail = when (page) {
                    Page.TASKS -> api.task(id)
                    Page.APPROVALS -> api.approval(id)
                    Page.SERVERS -> api.server(id)
                    else -> null
                }
                if (page == Page.APPROVALS) { detailId = id; pendingApprovalId = null }
                if (page == Page.TASKS) {
                    if (tree == null) tree = runCatching { api.tree(id) }.getOrNull()
                    loadExecution(id)
                }
            }
        } catch (failure: Exception) {
            error = if (pendingApprovalId != null && failure is HttpException &&
                failure.code() in listOf(403, 404)) "Sem acesso." else displayError(failure)
            pendingApprovalId = null
        } finally {
            loading = false
        }
        // The Aprovações tab badge; a failure here never hides the page itself.
        val approvals = if (page == Page.APPROVALS) data else runCatching { api.approvals() }.getOrNull()
        pendingApprovals = approvals.rows().count { it.str("status") == "PENDING" }
    }
}

/** Creates the controller and runs its effects (events, polling, push, loading) for this session. */
@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
fun rememberPreloController(me: Me, api: PreloResourceApi, preferences: DevicePreferences,
    decisions: ApprovalDecisions, events: EventFeed, pushRouting: PushRouting): PreloController {
    val scope = rememberCoroutineScope()
    val controller = remember(me.userId) { PreloController(me, api, preferences, decisions, scope) }
    val destination by pushRouting.destination.collectAsState()
    val connected by events.connected.collectAsState()
    val page = controller.page
    val selected = controller.selected
    val detailId = controller.detailId

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
        }.debounce(150).collect { controller.liveReload() }
    }
    LaunchedEffect(page, selected, connected) {
        while (true) { delay(if (connected) 45_000 else 10_000); controller.reload() }
    }
    LaunchedEffect(me.userId) { controller.initProjects() }
    LaunchedEffect(page, selected) { controller.resetDetail() }
    LaunchedEffect(page, selected, controller.refresh) { controller.load() }
    LaunchedEffect(destination) {
        destination?.let { target ->
            if (target.projectId != null) {
                preferences.setProjectId(target.projectId)
                controller.selected = target.projectId
            }
            if (target.kind == "approval") {
                controller.open(Page.APPROVALS)
                controller.pendingApprovalId = target.id
            } else controller.open(Page.DEPLOYS)
            controller.reload()
            pushRouting.consumed(target)
        }
    }
    return controller
}
