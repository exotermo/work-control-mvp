package com.workcontrol.app.feature.tasks

import com.workcontrol.app.domain.model.TaskItem
import com.workcontrol.app.domain.model.TaskStatus

enum class TaskFilter(val label: String) {
    ALL("Todas"),
    ACTIVE("Ativas"),
    QUEUED("Em fila"),
    COMPLETED("Concluídas"),
}

fun TaskFilter.matches(task: TaskItem): Boolean = when (this) {
    TaskFilter.ALL -> true
    TaskFilter.ACTIVE -> task.status == TaskStatus.IN_PROGRESS
    TaskFilter.QUEUED -> task.status == TaskStatus.QUEUED
    TaskFilter.COMPLETED -> task.status == TaskStatus.COMPLETED
}

data class TaskListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val allTaskCount: Int = 0,
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val visibleTasks: List<TaskItem> = emptyList(),
)

sealed interface TaskListAction {
    data class SelectFilter(val filter: TaskFilter) : TaskListAction
    data object Retry : TaskListAction
}
