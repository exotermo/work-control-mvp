package com.workcontrol.app.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workcontrol.app.domain.repository.TaskRepository
import com.workcontrol.app.domain.error.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModel @Inject constructor(
    taskRepository: TaskRepository,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(TaskFilter.ALL)
    private val refreshVersion = MutableStateFlow(0L)
    private val taskLoadState = refreshVersion
        .flatMapLatest {
            flow<TaskLoadEvent> {
                emit(TaskLoadEvent.Loading)
                taskRepository.observeTasks().collect { emit(TaskLoadEvent.Data(it)) }
            }
                .catch { emit(TaskLoadEvent.Failure(it.toUserMessage())) }
        }
        .scan(TaskLoadState()) { previous, event ->
            when (event) {
                TaskLoadEvent.Loading -> previous.copy(isLoading = true, errorMessage = null)
                is TaskLoadEvent.Data -> TaskLoadState(tasks = event.tasks, isLoading = false)
                is TaskLoadEvent.Failure -> previous.copy(isLoading = false, errorMessage = event.message)
            }
        }

    val uiState: StateFlow<TaskListUiState> = combine(
        taskLoadState,
        selectedFilter,
    ) { loadState, filter ->
        TaskListUiState(
            isLoading = loadState.isLoading,
            errorMessage = loadState.errorMessage,
            allTaskCount = loadState.tasks.size,
            selectedFilter = filter,
            visibleTasks = loadState.tasks.filter { filter.matches(it) },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TaskListUiState(),
    )

    fun onAction(action: TaskListAction) {
        when (action) {
            is TaskListAction.SelectFilter -> selectedFilter.value = action.filter
            TaskListAction.Retry -> refreshVersion.value += 1
        }
    }
}

private data class TaskLoadState(
    val tasks: List<com.workcontrol.app.domain.model.TaskItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

private sealed interface TaskLoadEvent {
    data object Loading : TaskLoadEvent
    data class Data(val tasks: List<com.workcontrol.app.domain.model.TaskItem>) : TaskLoadEvent
    data class Failure(val message: String) : TaskLoadEvent
}
