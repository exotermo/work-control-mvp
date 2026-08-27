package com.workcontrol.app.feature.tasks

import app.cash.turbine.test
import com.workcontrol.app.MainDispatcherRule
import com.workcontrol.app.data.fake.FakeTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TaskListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `filtro Todas mostra as 5 tarefas fake`() = runTest {
        val viewModel = TaskListViewModel(FakeTaskRepository())
        viewModel.uiState.test {
            val state = awaitItem().takeUnless { it.isLoading } ?: awaitItem()
            assertEquals(5, state.allTaskCount)
            assertEquals(5, state.visibleTasks.size)
        }
    }

    @Test
    fun `selecionar filtro Ativas restringe a tarefas em progresso`() = runTest {
        val viewModel = TaskListViewModel(FakeTaskRepository())
        viewModel.uiState.test {
            if (awaitItem().isLoading) awaitItem()

            viewModel.onAction(TaskListAction.SelectFilter(TaskFilter.ACTIVE))

            val filtered = awaitItem()
            assertEquals(TaskFilter.ACTIVE, filtered.selectedFilter)
            assertTrue(filtered.visibleTasks.isNotEmpty())
            assertTrue(filtered.visibleTasks.all { it.status.name == "IN_PROGRESS" })
        }
    }
}
