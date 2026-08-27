package com.workcontrol.app.domain.usecase

import com.workcontrol.app.domain.model.HomeSnapshot
import com.workcontrol.app.domain.model.TaskItem
import com.workcontrol.app.domain.repository.AgentRepository
import com.workcontrol.app.domain.repository.ApprovalRepository
import com.workcontrol.app.domain.repository.MachineRepository
import com.workcontrol.app.domain.repository.TaskRepository
import com.workcontrol.app.domain.repository.WorkspaceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private const val HOME_RECENT_TASK_LIMIT = 2

/** Orquestra os 5 repositórios que alimentam o dashboard. */
class ObserveHomeSnapshotUseCase @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
    private val taskRepository: TaskRepository,
    private val agentRepository: AgentRepository,
    private val machineRepository: MachineRepository,
    private val approvalRepository: ApprovalRepository,
) {
    operator fun invoke(): Flow<HomeSnapshot> = combine(
        workspaceRepository.observeCurrentWorkspace(),
        taskRepository.observeTasks(),
        agentRepository.observeActiveAgents(),
        machineRepository.observeMachines(),
        approvalRepository.observePendingApproval(),
    ) { workspace, tasks, agents, machines, approval ->
        HomeSnapshot(
            workspace = workspace,
            pendingApproval = approval,
            activeAgents = agents,
            machines = machines,
            recentTasks = tasks.take(HOME_RECENT_TASK_LIMIT),
        )
    }
}

class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(description: String): Result<TaskItem> {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Descrição da tarefa não pode ser vazia"))
        }
        return runCatching { taskRepository.createTask(trimmed) }
    }
}

class DecideApprovalUseCase @Inject constructor(
    private val approvalRepository: ApprovalRepository,
) {
    suspend operator fun invoke(approvalId: String, approved: Boolean) {
        approvalRepository.decide(approvalId, approved)
    }
}
