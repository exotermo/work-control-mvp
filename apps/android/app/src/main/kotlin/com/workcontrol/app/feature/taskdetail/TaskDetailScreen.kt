package com.workcontrol.app.feature.taskdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.workcontrol.app.core.components.WorkControlTopBar
import com.workcontrol.app.core.designsystem.WcColor
import com.workcontrol.app.core.designsystem.WcType
import com.workcontrol.app.domain.model.ActivityLogEntry
import com.workcontrol.app.domain.model.Agent
import com.workcontrol.app.domain.model.AgentRuntimeStatus
import com.workcontrol.app.domain.model.ExecutionNode
import com.workcontrol.app.domain.model.ExecutionNodeStatus

@Composable
fun TaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    onAgentClick: (String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        WorkControlTopBar(title = state.title.ifBlank { "Tarefa $taskId" }, subtitle = state.statusLabel, onBack = onBack)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            val hasContent = state.title.isNotBlank()
            if (state.isLoading && !hasContent) {
                item { RemoteLoadingPanel("Carregando detalhes da tarefa") }
                return@LazyColumn
            }
            if (!state.found) {
                item {
                    SurfaceCard(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Esta tarefa não existe mais ou foi removida.",
                            color = WcColor.InkDim,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                return@LazyColumn
            }
            state.errorMessage?.let { message ->
                item {
                    RemoteErrorPanel(
                        message = message,
                        onRetry = viewModel::retry,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (!hasContent) return@LazyColumn
            }
            item {
                ProgressCard(
                    statusLabel = state.statusLabel,
                    elapsedLabel = state.elapsedLabel,
                    progressPercent = state.progressPercent,
                )
            }
            item { SectionLabel("Fluxo de execução", modifier = Modifier.padding(horizontal = 16.dp)) }
            item { ExecutionFlowRow(nodes = state.executionFlow, onNodeClick = onAgentClick) }
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionLabel("Agentes", modifier = Modifier.padding(horizontal = 16.dp)) }
            items(state.agents, key = { it.id }) { agent ->
                AgentRow(agent = agent, onClick = { onAgentClick(agent.id) })
            }
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionLabel("Atividade recente", modifier = Modifier.padding(horizontal = 16.dp)) }
            item {
                SurfaceCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    state.activityLog.forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(color = WcColor.Border)
                        ActivityLogRow(entry = entry)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ProgressCard(statusLabel: String, elapsedLabel: String, progressPercent: Int) {
    SurfaceCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(statusLabel, color = WcColor.Ink, style = MaterialTheme.typography.titleMedium)
                Text(elapsedLabel, color = WcColor.InkFaint, style = WcType.Eyebrow, modifier = Modifier.padding(top = 4.dp))
            }
            Text("$progressPercent%", color = WcColor.Accent, style = WcType.MonoDataLarge)
        }
        WcProgressBar(percent = progressPercent, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ExecutionFlowRow(nodes: List<ExecutionNode>, onNodeClick: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        nodes.forEachIndexed { index, node ->
            if (index > 0) {
                Spacer(
                    modifier = Modifier
                        .width(18.dp)
                        .height(1.dp)
                        .background(if (node.status != ExecutionNodeStatus.PENDING) WcColor.Primary else WcColor.SurfaceRaised),
                )
            }
            ExecutionNodeChip(node = node, onClick = { node.agentId?.let(onNodeClick) })
        }
    }
}

@Composable
private fun ExecutionNodeChip(node: ExecutionNode, onClick: () -> Unit) {
    val isClickable = node.agentId != null
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(if (isClickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        val bg = when (node.status) {
            ExecutionNodeStatus.ACTIVE -> WcColor.Primary
            ExecutionNodeStatus.DONE -> WcColor.Ok.copy(alpha = 0.2f)
            ExecutionNodeStatus.PENDING -> WcColor.SurfaceRaised
        }
        Row(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(bg),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (node.status == ExecutionNodeStatus.DONE) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = WcColor.Ok, modifier = Modifier.size(16.dp))
            } else {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = if (node.status == ExecutionNodeStatus.ACTIVE) WcColor.Ink else WcColor.InkFaint,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = node.label,
            style = WcType.Eyebrow,
            color = if (node.status == ExecutionNodeStatus.PENDING) WcColor.InkFaint else WcColor.InkDim,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun AgentRuntimeStatus.toTone(): StatusTone = when (this) {
    AgentRuntimeStatus.RUNNING -> StatusTone.RUNNING
    AgentRuntimeStatus.WARNING -> StatusTone.WARNING
    AgentRuntimeStatus.IDLE -> StatusTone.IDLE
    AgentRuntimeStatus.ERROR -> StatusTone.ERROR
    AgentRuntimeStatus.OFFLINE -> StatusTone.OFFLINE
}

@Composable
private fun AgentRow(agent: Agent, onClick: () -> Unit) {
    SurfaceCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            StatusDot(tone = agent.status.toTone())
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(agent.name, color = WcColor.Ink, style = MaterialTheme.typography.titleSmall)
                Text(agent.currentActivity, color = WcColor.InkFaint, style = MaterialTheme.typography.bodySmall)
            }
            if (agent.progressPercent != null) {
                Text("${agent.progressPercent}%", color = WcColor.Accent, style = WcType.MonoData)
            }
        }
    }
}

@Composable
private fun ActivityLogRow(entry: ActivityLogEntry) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            entry.timestamp,
            color = WcColor.InkFaint,
            style = WcType.Eyebrow,
            modifier = Modifier.padding(end = 12.dp).width(40.dp),
        )
        Text(
            entry.message,
            color = if (entry.highlighted) WcColor.Ok else WcColor.InkDim,
            style = WcType.MonoData,
        )
    }
}
