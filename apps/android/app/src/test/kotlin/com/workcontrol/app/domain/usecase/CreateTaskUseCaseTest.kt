package com.workcontrol.app.domain.usecase

import com.workcontrol.app.domain.model.TaskItem
import com.workcontrol.app.domain.model.TaskStatus
import com.workcontrol.app.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateTaskUseCaseTest {

    @Test
    fun `descricao em branco falha sem chamar o repositorio`() = runTest {
        val repository = mockk<TaskRepository>()
        val useCase = CreateTaskUseCase(repository)

        val result = useCase("   ")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.createTask(any()) }
    }

    @Test
    fun `descricao valida e enviada ao repositorio sem espacos extras`() = runTest {
        val repository = mockk<TaskRepository>()
        val expected = TaskItem("new-1", "#novo", "Corrigir bug", TaskStatus.QUEUED, 0, 0)
        coEvery { repository.createTask("Corrigir bug") } returns expected

        val useCase = CreateTaskUseCase(repository)
        val result = useCase("  Corrigir bug  ")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.createTask("Corrigir bug") }
    }
}
