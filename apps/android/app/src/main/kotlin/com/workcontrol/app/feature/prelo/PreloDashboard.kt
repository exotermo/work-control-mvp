package com.workcontrol.app.feature.prelo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.components.BarItem
import com.workcontrol.app.core.components.ErrorClipping
import com.workcontrol.app.core.components.InkButton
import com.workcontrol.app.core.components.Kicker
import com.workcontrol.app.core.components.Masthead
import com.workcontrol.app.core.components.PageTurn
import com.workcontrol.app.core.components.PaperBackground
import com.workcontrol.app.core.components.PaperSheet
import com.workcontrol.app.core.components.PreloBottomBar
import com.workcontrol.app.core.components.PressSkeleton
import com.workcontrol.app.core.components.ScanlineSweep
import com.workcontrol.app.core.components.Rule
import com.workcontrol.app.core.components.Stamp
import com.workcontrol.app.core.components.Tone
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.data.auth.DevicePreferences
import com.workcontrol.app.data.prelo.ApprovalDecisions
import com.workcontrol.app.data.prelo.EventFeed
import com.workcontrol.app.data.prelo.Me
import com.workcontrol.app.data.prelo.PreloResourceApi
import com.workcontrol.app.data.push.PushRouting
import com.workcontrol.app.feature.prelo.screens.ApprovalsScreen
import com.workcontrol.app.feature.prelo.screens.DeploysScreen
import com.workcontrol.app.feature.prelo.screens.FilesScreen
import com.workcontrol.app.feature.prelo.screens.HomeScreen
import com.workcontrol.app.feature.prelo.screens.MoreScreen
import com.workcontrol.app.feature.prelo.screens.NewTaskSheet
import com.workcontrol.app.feature.prelo.screens.PipelineScreen
import com.workcontrol.app.feature.prelo.screens.ServersScreen
import com.workcontrol.app.feature.prelo.screens.TasksScreen
import com.workcontrol.app.feature.prelo.screens.ProjectScreen
import com.workcontrol.app.feature.prelo.screens.ClientsScreen
import com.workcontrol.app.feature.prelo.screens.ClientScreen
import kotlinx.coroutines.delay

/**
 * The signed-in app: a front page with the masthead and telemetry strip, sections turned like
 * magazine pages, a bottom bar and the "Nova tarefa" button. Every item comes from the Prelo API
 * through [PreloController]; [account] is the Conta page (owned by the session screen) and
 * [homeNotice] an optional banner on the front page (notification permission).
 */
@Composable
fun PreloDashboard(me: Me, api: PreloResourceApi, preferences: DevicePreferences,
    decisions: ApprovalDecisions, events: EventFeed, pushRouting: PushRouting, modifier: Modifier = Modifier,
    homeNotice: (@Composable () -> Unit)? = null, account: @Composable () -> Unit = {}) {
    val c = rememberPreloController(me, api, preferences, decisions, events, pushRouting)
    val context = LocalContext.current
    val connected by events.connected.collectAsState()
    var newTask by remember { mutableStateOf(false) }
    var choosingProject by remember { mutableStateOf(false) }
    var projectStamp by remember { mutableStateOf(false) }
    var previousProject by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(c.selected) {
        val next = c.selected
        if (previousProject != null && previousProject != next) {
            projectStamp = true
            delay(900)
            projectStamp = false
        }
        previousProject = next
    }
    BackHandler(newTask || choosingProject || c.stepUpId != null || c.canGoBack) {
        when {
            newTask -> newTask = false
            choosingProject -> choosingProject = false
            c.stepUpId != null -> c.cancelStepUp()
            else -> c.back()
        }
    }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        c.saveDownload(context, uri)
    }
    var pendingDownloadName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingDownloadName) { pendingDownloadName?.let { saveFile.launch(it); pendingDownloadName = null } }

    val tabs = listOf(
        BarItem(Page.HOME, "Início", Icons.Outlined.Home),
        BarItem(Page.TASKS, "Tarefas", Icons.Outlined.Assignment),
        BarItem(Page.APPROVALS, "Aprovações", Icons.Outlined.Gavel, badge = c.pendingApprovals),
        BarItem(Page.DEPLOYS, "Deploys", Icons.Outlined.RocketLaunch),
        BarItem(Page.MORE, "Mais", Icons.Outlined.MenuBook),
    )
    val barSelection = if (c.page in listOf(Page.SERVERS, Page.PIPELINE, Page.FILES, Page.ACCOUNT, Page.CLIENTS, Page.CLIENT)) Page.MORE
        else if (c.page == Page.PROJECT) Page.HOME else c.page

    PaperBackground(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Masthead(
                live = connected,
                modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp).padding(top = 10.dp),
                projectName = c.projectName ?: if (c.projects.isEmpty()) null else "Projeto",
                onProjectClick = if (c.projects.size > 1) ({ choosingProject = true }) else null,
                pollSeconds = 10,
            )
            if (projectStamp) Stamp("PROJETO TROCADO", Tone.OK,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp), delayMs = 0)
            ScanlineSweep(c.liveTick)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                PageTurn(c.page to c.detailId, Modifier.fillMaxSize(), forward = c.forward) { (page, _) ->
                    val pad = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)
                    when (page) {
                        Page.HOME -> HomeScreen(c, pad, homeNotice)
                        Page.TASKS -> TasksScreen(c, pad)
                        Page.APPROVALS -> ApprovalsScreen(c, pad)
                        Page.DEPLOYS -> DeploysScreen(c, pad)
                        Page.MORE -> MoreScreen(c, pad)
                        Page.SERVERS -> ServersScreen(c, pad)
                        Page.PIPELINE -> PipelineScreen(c, pad)
                        Page.FILES -> FilesScreen(c, pad) { id, name -> c.requestDownload(id); pendingDownloadName = name }
                        Page.ACCOUNT -> AccountPage(c, pad, account)
                        Page.PROJECT -> ProjectScreen(c, pad)
                        Page.CLIENTS -> ClientsScreen(c, pad)
                        Page.CLIENT -> ClientScreen(c, pad)
                    }
                }
                if (c.page in listOf(Page.HOME, Page.TASKS) && c.detailId == null) {
                    InkButton("＋ Nova tarefa", onClick = { newTask = true },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp))
                }
            }
            PreloBottomBar(tabs, barSelection, onSelect = { target ->
                c.openTab(target)
            })
        }
    }

    if (newTask) NewTaskSheet(c, onDismiss = { newTask = false })
    if (choosingProject) PaperSheet(onDismiss = { choosingProject = false }) {
        Kicker("Trocar de projeto")
        Text("Em qual projeto você vai trabalhar?", Modifier.padding(top = 4.dp, bottom = 12.dp),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = Prelo.colors.ink)
        Rule(strong = true)
        c.projects.forEach { project ->
            ProjectRow(project.name, selected = project.id == c.selected, enabled = !c.busy) {
                c.selectProject(project.id); choosingProject = false
            }
        }
    }
}

/** Shared top of every section list: global error / loading, then the section's own items. */
internal fun LazyListScope.pageState(c: PreloController, hasData: Boolean) {
    c.error?.let { message -> item(key = "error") { ErrorClipping(message, Modifier.padding(top = 12.dp), onRetry = { c.reload() }) } }
    if (c.loading && !hasData && c.error == null) item(key = "loading") { PressSkeleton(Modifier.padding(top = 8.dp)) }
}
