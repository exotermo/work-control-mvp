package com.workcontrol.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MeResponseDto(
    val user: UserDto,
    val workspaces: List<WorkspaceDto>,
)

data class UserDto(
    val id: String,
    val email: String,
    @SerializedName("display_name") val displayName: String,
)

data class WorkspaceDto(
    val id: String,
    val name: String,
    val slug: String,
    val role: String,
)

data class TaskDto(
    val id: String,
    val code: String,
    val title: String,
    val status: String,
    @SerializedName("active_agent_count") val activeAgentCount: Long = 0,
    @SerializedName("progress_percent") val progressPercent: Int = 0,
)

data class CreateTaskRequestDto(
    val description: String,
    @SerializedName("project_id") val projectId: String? = null,
)

data class TaskDetailDto(
    val task: TaskDto,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("execution_flow") val executionFlow: List<ExecutionNodeDto>,
    val agents: List<AgentDto>,
    @SerializedName("activity_log") val activityLog: List<ActivityLogEntryDto>,
)

data class ExecutionNodeDto(
    val label: String,
    val status: String,
    @SerializedName("agent_id") val agentId: String?,
)

data class AgentDto(
    val id: String,
    val name: String,
    val role: String,
    val model: String,
    @SerializedName("machine_name") val machineName: String?,
    val status: String,
    @SerializedName("progress_percent") val progressPercent: Int?,
    @SerializedName("current_activity") val currentActivity: String,
)

data class ActivityLogEntryDto(
    @SerializedName("occurred_at") val occurredAt: String,
    val message: String,
    val highlighted: Boolean,
)

data class DeviceDto(
    val id: String,
    val name: String,
    @SerializedName("role_label") val roleLabel: String,
    val status: String,
    @SerializedName("cpu_percent") val cpuPercent: Int,
    @SerializedName("ram_percent") val ramPercent: Int,
    @SerializedName("disk_percent") val diskPercent: Int,
    @SerializedName("active_agent_count") val activeAgentCount: Long = 0,
)

data class ApprovalDto(
    val id: String,
    val title: String,
    @SerializedName("requested_by") val requestedBy: String,
    val description: String,
    val payload: ApprovalPayloadDto,
    val decision: String?,
)

data class ApprovalPayloadDto(
    val environment: String = "",
    val branch: String = "",
    @SerializedName("commit_summary") val commitSummary: String = "",
    val image: String = "",
    @SerializedName("rollback_policy") val rollbackPolicy: String = "",
    @SerializedName("tests_passed") val testsPassed: Int = 0,
    @SerializedName("tests_total") val testsTotal: Int = 0,
)

data class ApiErrorDto(val error: String?)
