package com.workcontrol.app.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workcontrol.app.core.components.SurfaceCard
import com.workcontrol.app.core.components.RemoteErrorPanel
import com.workcontrol.app.core.components.RemoteLoadingPanel
import com.workcontrol.app.core.components.WcProgressBar
import com.workcontrol.app.core.components.WorkControlTopBar
import com.workcontrol.app.core.designsystem.WcColor
import com.workcontrol.app.core.designsystem.WcType
import com.workcontrol.app.domain.model.TaskItem
import com.workcontrol.app.domain.model.TaskStatus

@Composable
fun TaskListScreen(
    onTaskClick: (String) -> Unit,
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        WorkControlTopBar(
            title = "Tarefas",
            subtitle = "PROJETO ATLAS · ${state.allTaskCount} TOTAL",
        )
        FilterTabs(
            selected = state.selectedFilter,
            onSelect = { viewModel.onAction(TaskListAction.SelectFilter(it)) },
        )
        when {
            state.isLoading && state.allTaskCount == 0 -> {
                RemoteLoadingPanel("Carregando tarefas")
            }
            else -> LazyColumn(
                modifier = Modifier.testTag("task-list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.errorMessage?.let { message ->
                    item {
                        RemoteErrorPanel(
                            message = message,
                            onRetry = { viewModel.onAction(TaskListAction.Retry) },
                        )
                    }
                }
                if (state.errorMessage == null && state.allTaskCount == 0) {
                    item {
                        Text(
                            "Nenhuma tarefa neste projeto.",
                            color = WcColor.InkDim,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                } else if (state.visibleTasks.isEmpty() && state.allTaskCount > 0) {
                    item {
                        Text(
                            "Nenhuma tarefa corresponde a este filtro.",
                            color = WcColor.InkDim,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                items(state.visibleTasks, key = { it.id }) { task ->
                    TaskCard(task = task, onClick = { onTaskClick(task.id) })
                }
            }
        }
    }
}

@Composable
private fun FilterTabs(selected: TaskFilter, onSelect: (TaskFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TaskFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Text(
                text = filter.label,
                color = if (isSelected) WcColor.Ink else WcColor.InkFaint,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) WcColor.Primary else WcColor.SurfaceRaised)
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun TaskCard(task: TaskItem, onClick: () -> Unit) {
    SurfaceCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(task.code, color = WcColor.InkFaint, style = WcType.Eyebrow, modifier = Modifier.padding(end = 8.dp))
            StatusPill(status = task.status)
        }
        Text(
            text = task.title,
            color = WcColor.Ink,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (task.progressPercent in 1..99) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${task.activeAgentCount} agentes ativos", color = WcColor.InkFaint, style = WcType.Eyebrow)
                    Text("${task.progressPercent}%", color = WcColor.Accent, style = WcType.Eyebrow)
                }
                WcProgressBar(percent = task.progressPercent, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun StatusPill(status: TaskStatus) {
    val (bg, fg, label) = when (status) {
        TaskStatus.IN_PROGRESS -> Triple(WcColor.Ok.copy(alpha = 0.12f), WcColor.Ok, "Em progresso")
        TaskStatus.QUEUED -> Triple(WcColor.Info.copy(alpha = 0.12f), WcColor.Info, "Em fila")
        TaskStatus.COMPLETED -> Triple(WcColor.SurfaceRaised, WcColor.InkFaint, "Concluído")
        TaskStatus.FAILED -> Triple(WcColor.Bad.copy(alpha = 0.12f), WcColor.Bad, "Falhou")
    }
    Text(
        text = label,
        color = fg,
        style = WcType.Eyebrow,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
