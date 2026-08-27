package com.workcontrol.app.feature.taskdetail

import com.workcontrol.app.domain.model.ActivityLogEntry
import com.workcontrol.app.domain.model.Agent
import com.workcontrol.app.domain.model.ExecutionNode
import com.workcontrol.app.domain.model.TaskDetail
import com.workcontrol.app.domain.model.TaskStatus

data class TaskDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val found: Boolean = true,
    val title: String = "",
    val statusLabel: String = "",
    val elapsedLabel: String = "",
    val progressPercent: Int = 0,
    val executionFlow: List<ExecutionNode> = emptyList(),
    val agents: List<Agent> = emptyList(),
    val activityLog: List<ActivityLogEntry> = emptyList(),
)

fun TaskDetail.toUiState(): TaskDetailUiState = TaskDetailUiState(
    isLoading = false,
    found = true,
    title = task.title,
    statusLabel = task.status.detailLabel(),
    elapsedLabel = elapsedLabel,
    progressPercent = task.progressPercent,
    executionFlow = executionFlow,
    agents = agents,
    activityLog = activityLog,
)

private fun TaskStatus.detailLabel(): String = when (this) {
    TaskStatus.QUEUED -> "Em fila"
    TaskStatus.IN_PROGRESS -> "Em progresso"
    TaskStatus.COMPLETED -> "Concluído"
    TaskStatus.FAILED -> "Falhou"
}
