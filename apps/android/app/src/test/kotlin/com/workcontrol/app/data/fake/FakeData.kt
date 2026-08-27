package com.workcontrol.app.data.fake

import com.workcontrol.app.domain.model.ActivityLogEntry
import com.workcontrol.app.domain.model.Agent
import com.workcontrol.app.domain.model.AgentRole
import com.workcontrol.app.domain.model.AgentRuntimeStatus
import com.workcontrol.app.domain.model.Approval
import com.workcontrol.app.domain.model.DeviceStatus
import com.workcontrol.app.domain.model.ExecutionNode
import com.workcontrol.app.domain.model.ExecutionNodeStatus
import com.workcontrol.app.domain.model.Machine
import com.workcontrol.app.domain.model.TaskDetail
import com.workcontrol.app.domain.model.TaskItem
import com.workcontrol.app.domain.model.TaskStatus
import com.workcontrol.app.domain.model.Workspace

object FakeData {
    val workspace = Workspace("workspace-1", "Escritório", "escritorio", "owner")

    val agents = listOf(
        Agent("agent-dev", "Agent Dev", AgentRole.DEV, "Claude Sonnet 4.5", "Workstation-01", AgentRuntimeStatus.RUNNING, 68, "Corrigindo bug #184"),
        Agent("agent-qa", "Agent QA", AgentRole.QA, "GPT-4o", "Server-Atlas", AgentRuntimeStatus.RUNNING, 66, "Executando testes"),
        Agent("agent-devops", "Agent DevOps", AgentRole.DEVOPS, "Claude Haiku 4.5", "Server-CI", AgentRuntimeStatus.WARNING, null, "Aguardando aprovação"),
    )

    val machines = listOf(
        Machine("workstation-01", "Workstation-01", "Dev Machine", DeviceStatus.ONLINE, 45, 62, 38, 1),
        Machine("server-atlas", "Server-Atlas", "App Server", DeviceStatus.ONLINE, 23, 41, 67, 1),
        Machine("server-ci", "Server-CI", "CI/CD", DeviceStatus.ONLINE, 71, 55, 22, 1),
        Machine("workstation-02", "Workstation-02", "Dev Machine", DeviceStatus.OFFLINE, 0, 0, 0, 0),
    )

    val tasks = listOf(
        TaskItem("184", "#184", "Investigar e corrigir bug #184", TaskStatus.IN_PROGRESS, 3, 68),
        TaskItem("183", "#183", "Adicionar rate limiting na API", TaskStatus.QUEUED, 0, 0),
        TaskItem("182", "#182", "Refactor módulo de autenticação", TaskStatus.COMPLETED, 2, 100),
        TaskItem("181", "#181", "Migração PostgreSQL → Supabase", TaskStatus.COMPLETED, 2, 100),
        TaskItem("179", "#179", "Configurar CI/CD pipeline", TaskStatus.FAILED, 1, 45),
    )

    val pendingApproval = Approval(
        "approval-184-staging",
        "Deploy em staging",
        "Agent DevOps",
        "Deploy do fix do bug #184 para staging.",
        "staging.atlas.dev",
        "fix/auth-bug-184",
        "3 novos commits",
        "atlas:1.4.2-staging.7",
        "Automático em falha",
        32,
        32,
    )

    fun taskDetail(taskId: String): TaskDetail? = tasks.find { it.id == taskId }?.let { task ->
        TaskDetail(
            task,
            "Iniciado há 42 min",
            listOf(
                ExecutionNode("Usuário", ExecutionNodeStatus.DONE),
                ExecutionNode("Agent Dev", ExecutionNodeStatus.ACTIVE, "agent-dev"),
                ExecutionNode("Agent QA", ExecutionNodeStatus.PENDING, "agent-qa"),
            ),
            agents,
            listOf(ActivityLogEntry("14:53", "Patch aplicado", highlighted = true)),
        )
    }
}
