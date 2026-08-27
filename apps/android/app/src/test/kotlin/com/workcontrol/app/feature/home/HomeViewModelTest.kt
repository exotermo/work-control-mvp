package com.workcontrol.app.feature.home

import app.cash.turbine.test
import com.workcontrol.app.MainDispatcherRule
import com.workcontrol.app.data.fake.FakeAgentRepository
import com.workcontrol.app.data.fake.FakeApprovalRepository
import com.workcontrol.app.data.fake.FakeMachineRepository
import com.workcontrol.app.data.fake.FakeTaskRepository
import com.workcontrol.app.data.fake.FakeWorkspaceRepository
import com.workcontrol.app.domain.usecase.ObserveHomeSnapshotUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(): HomeViewModel = HomeViewModel(
        observeHomeSnapshot = ObserveHomeSnapshotUseCase(
            workspaceRepository = FakeWorkspaceRepository(),
            taskRepository = FakeTaskRepository(),
            agentRepository = FakeAgentRepository(),
            machineRepository = FakeMachineRepository(),
            approvalRepository = FakeApprovalRepository(),
        ),
    )

    @Test
    fun `snapshot combina os 5 repositorios fake em um unico uiState`() = runTest {
        buildViewModel().uiState.test {
            val state = awaitItem().takeUnless { it.isLoading } ?: awaitItem()
            assertEquals(3, state.activeAgents.size)
            assertEquals(4, state.machines.size)
            assertEquals(2, state.recentTasks.size)
            assertTrue(state.pendingApprovalSummary.orEmpty().contains("Agent DevOps"))
        }
    }
}
