package com.workcontrol.app.navigation

import kotlinx.serialization.Serializable

// Rotas type-safe (Navigation Compose 2.8+ / kotlinx.serialization) — uma classe por destino do
// mapa de navegação descrito no Diário de Bordo (capítulo 15).

@Serializable object HomeRoute

@Serializable object TaskListRoute

@Serializable data class TaskDetailRoute(val taskId: String)

@Serializable data class AgentDetailRoute(val agentId: String)

@Serializable data class CodeDiffRoute(val filePath: String = "src/auth/auth.service.ts")

@Serializable data class TerminalRoute(val machineId: String)

@Serializable data class ApprovalRoute(val approvalId: String)

@Serializable object MachineListRoute

@Serializable data class MachineDetailRoute(val machineId: String)

@Serializable object NewTaskRoute

@Serializable data class FilesRoute(val machineId: String)

@Serializable data class ResultRoute(val taskId: String)
