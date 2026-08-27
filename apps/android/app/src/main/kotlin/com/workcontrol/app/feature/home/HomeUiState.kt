package com.workcontrol.app.feature.home

import com.workcontrol.app.domain.model.Agent
import com.workcontrol.app.domain.model.ApprovalDecision
import com.workcontrol.app.domain.model.HomeSnapshot
import com.workcontrol.app.domain.model.Machine
import com.workcontrol.app.domain.model.TaskItem

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val workspaceLabel: String = "",
    val pendingApprovalSummary: String? = null,
    val activeAgents: List<Agent> = emptyList(),
    val machines: List<Machine> = emptyList(),
    val recentTasks: List<TaskItem> = emptyList(),
)

fun HomeSnapshot.toUiState(): HomeUiState {
    val approval = pendingApproval?.takeIf { it.decision == null || it.decision != ApprovalDecision.REJECTED }
    return HomeUiState(
        isLoading = false,
        workspaceLabel = workspace.name,
        pendingApprovalSummary = approval?.let { "1 aprovação pendente · ${it.requestedBy}" },
        activeAgents = activeAgents,
        machines = machines,
        recentTasks = recentTasks,
    )
}
