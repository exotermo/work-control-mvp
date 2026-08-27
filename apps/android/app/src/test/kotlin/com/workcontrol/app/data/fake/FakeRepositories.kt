package com.workcontrol.app.data.fake

import com.workcontrol.app.domain.model.Agent
import com.workcontrol.app.domain.model.Approval
import com.workcontrol.app.domain.model.ApprovalDecision
import com.workcontrol.app.domain.model.Machine
import com.workcontrol.app.domain.model.TaskDetail
import com.workcontrol.app.domain.model.TaskItem
import com.workcontrol.app.domain.model.TaskStatus
import com.workcontrol.app.domain.model.Workspace
import com.workcontrol.app.domain.repository.AgentRepository
import com.workcontrol.app.domain.repository.ApprovalRepository
import com.workcontrol.app.domain.repository.MachineRepository
import com.workcontrol.app.domain.repository.TaskRepository
import com.workcontrol.app.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeWorkspaceRepository : WorkspaceRepository {
    override fun observeCurrentWorkspace(): Flow<Workspace> = MutableStateFlow(FakeData.workspace)
}

class FakeTaskRepository : TaskRepository {
    private val tasks = MutableStateFlow(FakeData.tasks)
    override fun observeTasks(): StateFlow<List<TaskItem>> = tasks.asStateFlow()
    override fun observeTaskDetail(taskId: String): Flow<TaskDetail?> = tasks.map { FakeData.taskDetail(taskId) }

    override suspend fun createTask(description: String): TaskItem = TaskItem(
        id = "new-${tasks.value.size + 1}",
        code = "#novo",
        title = description,
        status = TaskStatus.QUEUED,
        activeAgentCount = 0,
        progressPercent = 0,
    ).also { tasks.value = listOf(it) + tasks.value }
}

class FakeAgentRepository : AgentRepository {
    override fun observeActiveAgents(): Flow<List<Agent>> = MutableStateFlow(FakeData.agents)
}

class FakeMachineRepository : MachineRepository {
    override fun observeMachines(): Flow<List<Machine>> = MutableStateFlow(FakeData.machines)
}

class FakeApprovalRepository : ApprovalRepository {
    private val approval = MutableStateFlow<Approval?>(FakeData.pendingApproval)
    override fun observePendingApproval(): Flow<Approval?> = approval.asStateFlow()

    override suspend fun decide(approvalId: String, approved: Boolean) {
        approval.value = approval.value?.takeIf { it.id == approvalId }?.copy(
            decision = if (approved) ApprovalDecision.APPROVED else ApprovalDecision.REJECTED,
        )
    }
}
