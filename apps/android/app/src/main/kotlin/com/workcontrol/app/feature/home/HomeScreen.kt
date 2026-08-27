package com.workcontrol.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workcontrol.app.core.components.SectionLabel
import com.workcontrol.app.core.components.RemoteErrorPanel
import com.workcontrol.app.core.components.RemoteLoadingPanel
import com.workcontrol.app.core.components.StatusDot
import com.workcontrol.app.core.components.StatusTone
import com.workcontrol.app.core.components.SurfaceCard
import com.workcontrol.app.core.components.WcProgressBar
import com.workcontrol.app.core.designsystem.WcColor
import com.workcontrol.app.core.designsystem.WcType
import com.workcontrol.app.domain.model.Agent
import com.workcontrol.app.domain.model.AgentRole
import com.workcontrol.app.domain.model.AgentRuntimeStatus
import com.workcontrol.app.domain.model.DeviceStatus
import com.workcontrol.app.domain.model.Machine
import com.workcontrol.app.domain.model.TaskItem
import com.workcontrol.app.domain.model.TaskStatus

@Composable
fun HomeScreen(
    onAgentClick: (String) -> Unit,
    onMachineClick: (String) -> Unit,
    onTaskClick: (String) -> Unit,
    onSeeAllMachines: () -> Unit,
    onSeeAllTasks: () -> Unit,
    onApprovalClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onAgentClick = onAgentClick,
        onMachineClick = onMachineClick,
        onTaskClick = onTaskClick,
        onSeeAllMachines = onSeeAllMachines,
        onSeeAllTasks = onSeeAllTasks,
        onApprovalClick = onApprovalClick,
        onRefresh = viewModel::refresh,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onAgentClick: (String) -> Unit,
    onMachineClick: (String) -> Unit,
    onTaskClick: (String) -> Unit,
    onSeeAllMachines: () -> Unit,
    onSeeAllTasks: () -> Unit,
    onApprovalClick: () -> Unit,
    onRefresh: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { HomeHeader(workspaceLabel = state.workspaceLabel, onRefresh = onRefresh) }

        val hasContent = state.workspaceLabel.isNotBlank()
        if (state.isLoading && !hasContent) {
            item { RemoteLoadingPanel("Carregando o workspace") }
            return@LazyColumn
        }
        if (state.errorMessage != null) {
            item {
                RemoteErrorPanel(
                    message = state.errorMessage,
                    onRetry = onRefresh,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (!hasContent) return@LazyColumn
        }

        if (state.pendingApprovalSummary != null) {
            item { ApprovalBanner(text = state.pendingApprovalSummary, onClick = onApprovalClick) }
        }

        item {
            SectionLabel("Agentes ativos · ${state.activeAgents.size}", modifier = Modifier.padding(horizontal = 16.dp))
        }
        items(state.activeAgents, key = { it.id }) { agent ->
            AgentSummaryCard(
                agent = agent,
                onClick = { onAgentClick(agent.id) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        item { Spacer(Modifier.height(12.dp)) }
        item {
            SectionHeaderRow(title = "Máquinas", actionLabel = "Ver todas", onAction = onSeeAllMachines)
        }
        item {
            SurfaceCard(modifier = Modifier.padding(horizontal = 16.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                state.machines.take(2).forEachIndexed { index, machine ->
                    if (index > 0) HorizontalDivider(color = WcColor.Border)
                    MachineRow(machine = machine, onClick = { onMachineClick(machine.id) })
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
        item {
            SectionHeaderRow(title = "Tarefas recentes", actionLabel = "Ver todas", onAction = onSeeAllTasks)
        }
        item {
            SurfaceCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                state.recentTasks.forEachIndexed { index, task ->
                    if (index > 0) HorizontalDivider(color = WcColor.Border)
                    RecentTaskRow(task = task, onClick = { onTaskClick(task.id) })
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HomeHeader(workspaceLabel: String, onRefresh: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(tone = StatusTone.ONLINE, size = 6.dp)
                Text(
                    text = workspaceLabel,
                    style = WcType.MonoData,
                    color = WcColor.InkFaint,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = "Work Control",
                style = MaterialTheme.typography.titleLarge,
                color = WcColor.Ink,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar", tint = WcColor.InkFaint)
        }
    }
}

@Composable
private fun ApprovalBanner(text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WcColor.Warn.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = WcColor.Warn, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            color = WcColor.Warn,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = WcColor.Warn.copy(alpha = 0.6f))
    }
}

@Composable
private fun SectionHeaderRow(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SectionLabel(title, modifier = Modifier.weight(1f), padding = androidx.compose.foundation.layout.PaddingValues(0.dp))
        Text(
            text = actionLabel,
            color = WcColor.Accent,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable(onClick = onAction),
        )
    }
}

private fun AgentRole.tint(): Color = when (this) {
    AgentRole.DEV -> WcColor.Accent
    AgentRole.QA -> WcColor.Info
    AgentRole.DEVOPS -> WcColor.Warn
}

private fun AgentRuntimeStatus.toTone(): StatusTone = when (this) {
    AgentRuntimeStatus.RUNNING -> StatusTone.RUNNING
    AgentRuntimeStatus.WARNING -> StatusTone.WARNING
    AgentRuntimeStatus.IDLE -> StatusTone.IDLE
    AgentRuntimeStatus.ERROR -> StatusTone.ERROR
    AgentRuntimeStatus.OFFLINE -> StatusTone.OFFLINE
}

@Composable
private fun AgentSummaryCard(agent: Agent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isWarning = agent.status == AgentRuntimeStatus.WARNING
    SurfaceCard(
        modifier = modifier.fillMaxWidth(),
        accentBorder = if (isWarning) WcColor.Warn.copy(alpha = 0.35f) else WcColor.Border,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            RoleBadge(role = agent.role)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(agent.name, color = WcColor.Ink, style = MaterialTheme.typography.titleSmall)
                Text(agent.machineName, color = WcColor.InkFaint, style = WcType.Eyebrow)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(tone = agent.status.toTone())
                Text(
                    text = agent.progressPercent?.let { "$it%" } ?: "WAIT",
                    color = agent.status.toTone().let { toneColor(it) },
                    style = WcType.MonoData,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        Text(
            text = agent.currentActivity,
            color = if (isWarning) WcColor.Warn.copy(alpha = 0.75f) else WcColor.InkDim,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 10.dp, bottom = if (agent.progressPercent != null) 8.dp else 0.dp),
        )
        if (agent.progressPercent != null) {
            WcProgressBar(percent = agent.progressPercent, color = agent.role.tint())
        }
    }
}

private fun toneColor(tone: StatusTone): Color = when (tone) {
    StatusTone.ONLINE, StatusTone.RUNNING -> WcColor.Ok
    StatusTone.WARNING -> WcColor.Warn
    StatusTone.ERROR -> WcColor.Bad
    StatusTone.IDLE -> WcColor.Info
    StatusTone.OFFLINE -> WcColor.Off
}

@Composable
private fun RoleBadge(role: AgentRole) {
    val tint = role.tint()
    Row(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(tint.copy(alpha = 0.16f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.SmartToy, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun MachineRow(machine: Machine, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        StatusDot(tone = if (machine.status == DeviceStatus.ONLINE) StatusTone.ONLINE else StatusTone.OFFLINE)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(machine.name, color = WcColor.Ink, style = MaterialTheme.typography.titleSmall)
            Text(machine.role, color = WcColor.InkFaint, style = WcType.Eyebrow)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("CPU ${machine.cpuPercent}%", color = WcColor.InkFaint, style = WcType.Eyebrow)
            Text("RAM ${machine.ramPercent}%", color = WcColor.InkFaint, style = WcType.Eyebrow)
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = WcColor.InkFaint,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun RecentTaskRow(task: TaskItem, onClick: () -> Unit) {
    val isDone = task.status == TaskStatus.COMPLETED
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        StatusDot(
            tone = when (task.status) {
                TaskStatus.IN_PROGRESS -> StatusTone.RUNNING
                TaskStatus.FAILED -> StatusTone.ERROR
                else -> StatusTone.OFFLINE
            },
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(task.title, color = WcColor.Ink, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${task.activeAgentCount} agentes · ${task.status.label()}",
                color = WcColor.InkFaint,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (isDone) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = WcColor.Ok)
        } else {
            Text("${task.progressPercent}%", color = WcColor.Accent, style = WcType.MonoData)
        }
    }
}

private fun TaskStatus.label(): String = when (this) {
    TaskStatus.QUEUED -> "Em fila"
    TaskStatus.IN_PROGRESS -> "Em progresso"
    TaskStatus.COMPLETED -> "Concluído"
    TaskStatus.FAILED -> "Falhou"
}
