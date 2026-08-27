package com.workcontrol.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workcontrol.app.domain.usecase.ObserveHomeSnapshotUseCase
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
class HomeViewModel @Inject constructor(
    observeHomeSnapshot: ObserveHomeSnapshotUseCase,
) : ViewModel() {
    private val refreshVersion = MutableStateFlow(0L)

    val uiState: StateFlow<HomeUiState> = refreshVersion
        .flatMapLatest {
            flow<HomeSnapshotLoadEvent> {
                emit(HomeSnapshotLoadEvent.Loading)
                observeHomeSnapshot().collect { emit(HomeSnapshotLoadEvent.Data(it.toUiState())) }
            }
                .catch { emit(HomeSnapshotLoadEvent.Failure(it.toUserMessage())) }
        }
        .scan(HomeUiState()) { previous, event ->
            when (event) {
                HomeSnapshotLoadEvent.Loading -> previous.copy(isLoading = true, errorMessage = null)
                is HomeSnapshotLoadEvent.Data -> event.state
                is HomeSnapshotLoadEvent.Failure -> previous.copy(isLoading = false, errorMessage = event.message)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState(),
        )

    fun refresh() {
        refreshVersion.value += 1
    }
}

private sealed interface HomeSnapshotLoadEvent {
    data object Loading : HomeSnapshotLoadEvent
    data class Data(val state: HomeUiState) : HomeSnapshotLoadEvent
    data class Failure(val message: String) : HomeSnapshotLoadEvent
}
