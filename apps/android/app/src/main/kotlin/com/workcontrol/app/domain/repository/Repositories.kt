package com.workcontrol.app.domain.repository

import com.workcontrol.app.domain.model.Agent
import com.workcontrol.app.domain.model.Approval
import com.workcontrol.app.domain.model.Machine
import com.workcontrol.app.domain.model.TaskDetail
import com.workcontrol.app.domain.model.TaskItem
import com.workcontrol.app.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    fun observeCurrentWorkspace(): Flow<Workspace>
}

interface TaskRepository {
    fun observeTasks(): Flow<List<TaskItem>>
    fun observeTaskDetail(taskId: String): Flow<TaskDetail?>
    suspend fun createTask(description: String): TaskItem
}

interface AgentRepository {
    fun observeActiveAgents(): Flow<List<Agent>>
}

interface MachineRepository {
    fun observeMachines(): Flow<List<Machine>>
}

interface ApprovalRepository {
    fun observePendingApproval(): Flow<Approval?>
    suspend fun decide(approvalId: String, approved: Boolean)
}
