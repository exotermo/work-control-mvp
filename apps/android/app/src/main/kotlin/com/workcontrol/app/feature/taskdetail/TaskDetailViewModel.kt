package com.workcontrol.app.feature.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workcontrol.app.domain.repository.TaskRepository
import com.workcontrol.app.domain.error.WorkControlException
import com.workcontrol.app.domain.error.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    taskRepository: TaskRepository,
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"]) {
        "TaskDetailScreen requer o argumento de rota 'taskId'"
    }

    private val refreshVersion = MutableStateFlow(0L)

    val uiState: StateFlow<TaskDetailUiState> = refreshVersion
        .flatMapLatest {
            flow<TaskDetailLoadEvent> {
                emit(TaskDetailLoadEvent.Loading)
                taskRepository.observeTaskDetail(taskId).collect { detail ->
                    emit(
                        TaskDetailLoadEvent.Data(
                            detail?.toUiState() ?: TaskDetailUiState(isLoading = false, found = false),
                        ),
                    )
                }
            }
                .catch { failure ->
                    if (failure is WorkControlException.NotFound) {
                        emit(TaskDetailLoadEvent.NotFound)
                    } else {
                        emit(TaskDetailLoadEvent.Failure(failure.toUserMessage()))
                    }
                }
        }
        .scan(TaskDetailUiState()) { previous, event ->
            when (event) {
                TaskDetailLoadEvent.Loading -> previous.copy(isLoading = true, errorMessage = null)
                TaskDetailLoadEvent.NotFound -> TaskDetailUiState(isLoading = false, found = false)
                is TaskDetailLoadEvent.Data -> event.state
                is TaskDetailLoadEvent.Failure -> previous.copy(isLoading = false, errorMessage = event.message)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TaskDetailUiState(),
        )

    fun retry() {
        refreshVersion.value += 1
    }
}

private sealed interface TaskDetailLoadEvent {
    data object Loading : TaskDetailLoadEvent
    data object NotFound : TaskDetailLoadEvent
    data class Data(val state: TaskDetailUiState) : TaskDetailLoadEvent
    data class Failure(val message: String) : TaskDetailLoadEvent
}
