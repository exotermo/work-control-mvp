package com.workcontrol.app.domain.model

enum class TaskStatus { QUEUED, IN_PROGRESS, COMPLETED, FAILED }

enum class AgentRole { DEV, QA, DEVOPS }

enum class AgentRuntimeStatus { RUNNING, WARNING, IDLE, ERROR, OFFLINE }

enum class DeviceStatus { ONLINE, OFFLINE }

enum class ExecutionNodeStatus { DONE, ACTIVE, PENDING }

enum class ApprovalDecision { APPROVED, REJECTED }

data class Workspace(
    val id: String,
    val name: String,
    val slug: String,
    val role: String,
)

data class TaskItem(
    val id: String,
    val code: String,
    val title: String,
    val status: TaskStatus,
    val activeAgentCount: Int,
    val progressPercent: Int,
)

data class Agent(
    val id: String,
    val name: String,
    val role: AgentRole,
    val model: String,
    val machineName: String,
    val status: AgentRuntimeStatus,
    val progressPercent: Int?,
    val currentActivity: String,
)

data class Machine(
    val id: String,
    val name: String,
    val role: String,
    val status: DeviceStatus,
    val cpuPercent: Int,
    val ramPercent: Int,
    val diskPercent: Int,
    val activeAgentCount: Int,
)

data class ExecutionNode(
    val label: String,
    val status: ExecutionNodeStatus,
    val agentId: String? = null,
)

data class ActivityLogEntry(
    val timestamp: String,
    val message: String,
    val highlighted: Boolean = false,
)

data class TaskDetail(
    val task: TaskItem,
    val elapsedLabel: String,
    val executionFlow: List<ExecutionNode>,
    val agents: List<Agent>,
    val activityLog: List<ActivityLogEntry>,
)

data class Approval(
    val id: String,
    val title: String,
    val requestedBy: String,
    val description: String,
    val environment: String,
    val branch: String,
    val commitSummary: String,
    val image: String,
    val rollbackPolicy: String,
    val testsPassed: Int,
    val testsTotal: Int,
    val decision: ApprovalDecision? = null,
)

data class HomeSnapshot(
    val workspace: Workspace,
    val pendingApproval: Approval?,
    val activeAgents: List<Agent>,
    val machines: List<Machine>,
    val recentTasks: List<TaskItem>,
)
