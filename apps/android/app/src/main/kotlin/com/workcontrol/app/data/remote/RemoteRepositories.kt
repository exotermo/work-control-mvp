package com.workcontrol.app.data.remote

import com.workcontrol.app.data.remote.dto.ActivityLogEntryDto
import com.workcontrol.app.data.remote.dto.AgentDto
import com.workcontrol.app.data.remote.dto.ApprovalDto
import com.workcontrol.app.data.remote.dto.CreateTaskRequestDto
import com.workcontrol.app.data.remote.dto.DeviceDto
import com.workcontrol.app.data.remote.dto.ExecutionNodeDto
import com.workcontrol.app.data.remote.dto.TaskDetailDto
import com.workcontrol.app.data.remote.dto.TaskDto
import com.workcontrol.app.data.remote.dto.WorkspaceDto
import com.workcontrol.app.domain.error.WorkControlException
import com.workcontrol.app.domain.model.ActivityLogEntry
import com.workcontrol.app.domain.model.Agent
import com.workcontrol.app.domain.model.AgentRole
import com.workcontrol.app.domain.model.AgentRuntimeStatus
import com.workcontrol.app.domain.model.Approval
import com.workcontrol.app.domain.model.ApprovalDecision
import com.workcontrol.app.domain.model.DeviceStatus
import com.workcontrol.app.domain.model.ExecutionNode
import com.workcontrol.app.domain.model.ExecutionNodeStatus
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@Singleton
class RemoteWorkspaceRepository @Inject constructor(
    private val api: WorkControlApi,
    private val executor: ApiCallExecutor,
) : WorkspaceRepository {
    override fun observeCurrentWorkspace(): Flow<Workspace> = flow {
        emit(
            executor.execute {
                api.me().workspaces.firstOrNull()?.toDomain()
                    ?: throw WorkControlException.Authorization()
            },
        )
    }
}

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteTaskRepository @Inject constructor(
    private val api: WorkControlApi,
    private val executor: ApiCallExecutor,
    private val taskEvents: TaskEventSource,
) : TaskRepository {
    private val refreshVersion = MutableStateFlow(0L)

    override fun observeTasks(): Flow<List<TaskItem>> = refreshVersion.flatMapLatest {
        flow { emit(executor.execute { api.tasks().map(TaskDto::toDomain) }) }
    }

    override fun observeTaskDetail(taskId: String): Flow<TaskDetail?> = flow {
        emit(loadTaskDetail(taskId))
        taskEvents.observe(taskId).collect {
            // O evento é só um invalidador; PostgreSQL/REST continua sendo o snapshot de verdade.
            emit(loadTaskDetail(taskId))
        }
    }

    override suspend fun createTask(description: String): TaskItem {
        val created = executor.execute {
            api.createTask(CreateTaskRequestDto(description = description)).toDomain()
        }
        refreshVersion.value += 1
        return created
    }

    private suspend fun loadTaskDetail(taskId: String): TaskDetail =
        executor.execute { api.task(taskId).toDomain() }
}

@Singleton
class RemoteAgentRepository @Inject constructor(
    private val api: WorkControlApi,
    private val executor: ApiCallExecutor,
) : AgentRepository {
    override fun observeActiveAgents(): Flow<List<Agent>> = flow {
        emit(executor.execute { api.activeAgents().map(AgentDto::toDomain) })
    }
}

@Singleton
class RemoteMachineRepository @Inject constructor(
    private val api: WorkControlApi,
    private val executor: ApiCallExecutor,
) : MachineRepository {
    override fun observeMachines(): Flow<List<Machine>> = flow {
        emit(executor.execute { api.devices().map(DeviceDto::toDomain) })
    }
}

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteApprovalRepository @Inject constructor(
    private val api: WorkControlApi,
    private val executor: ApiCallExecutor,
) : ApprovalRepository {
    private val refreshVersion = MutableStateFlow(0L)

    override fun observePendingApproval(): Flow<Approval?> = refreshVersion.flatMapLatest {
        flow {
            emit(executor.execute { api.pendingApprovals().firstOrNull()?.toDomain() })
        }
    }

    override suspend fun decide(approvalId: String, approved: Boolean) {
        executor.execute {
            if (approved) api.approve(approvalId) else api.reject(approvalId)
        }
        refreshVersion.value += 1
    }
}

private fun WorkspaceDto.toDomain() = Workspace(
    id = id,
    name = name,
    slug = slug,
    role = role,
)

private fun TaskDto.toDomain() = TaskItem(
    id = id,
    code = code,
    title = title,
    status = enumValueOf<TaskStatus>(status),
    activeAgentCount = activeAgentCount.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
    progressPercent = progressPercent.coerceIn(0, 100),
)

private fun TaskDetailDto.toDomain() = TaskDetail(
    task = task.toDomain(),
    elapsedLabel = Instant.parse(startedAt).toElapsedLabel(),
    executionFlow = executionFlow.map(ExecutionNodeDto::toDomain),
    agents = agents.map(AgentDto::toDomain),
    activityLog = activityLog.map(ActivityLogEntryDto::toDomain),
)

private fun ExecutionNodeDto.toDomain() = ExecutionNode(
    label = label,
    status = enumValueOf<ExecutionNodeStatus>(status),
    agentId = agentId,
)

private fun AgentDto.toDomain() = Agent(
    id = id,
    name = name,
    role = enumValueOf<AgentRole>(role),
    model = model,
    machineName = machineName ?: "Sem máquina",
    status = enumValueOf<AgentRuntimeStatus>(status),
    progressPercent = progressPercent?.coerceIn(0, 100),
    currentActivity = currentActivity,
)

private fun ActivityLogEntryDto.toDomain() = ActivityLogEntry(
    timestamp = activityTimeFormatter.format(Instant.parse(occurredAt)),
    message = message,
    highlighted = highlighted,
)

private fun DeviceDto.toDomain() = Machine(
    id = id,
    name = name,
    role = roleLabel,
    status = enumValueOf<DeviceStatus>(status),
    cpuPercent = cpuPercent.coerceIn(0, 100),
    ramPercent = ramPercent.coerceIn(0, 100),
    diskPercent = diskPercent.coerceIn(0, 100),
    activeAgentCount = activeAgentCount.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
)

private fun ApprovalDto.toDomain() = Approval(
    id = id,
    title = title,
    requestedBy = requestedBy,
    description = description,
    environment = payload.environment,
    branch = payload.branch,
    commitSummary = payload.commitSummary,
    image = payload.image,
    rollbackPolicy = payload.rollbackPolicy,
    testsPassed = payload.testsPassed,
    testsTotal = payload.testsTotal,
    decision = decision?.let { enumValueOf<ApprovalDecision>(it) },
)

private fun Instant.toElapsedLabel(): String {
    val elapsed = Duration.between(this, Instant.now()).coerceAtLeast(Duration.ZERO)
    return when {
        elapsed.toMinutes() < 1 -> "Iniciado agora"
        elapsed.toHours() < 1 -> "Iniciado há ${elapsed.toMinutes()} min"
        elapsed.toDays() < 1 -> "Iniciado há ${elapsed.toHours()} h"
        else -> "Iniciado há ${elapsed.toDays()} d"
    }
}

private val activityTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
