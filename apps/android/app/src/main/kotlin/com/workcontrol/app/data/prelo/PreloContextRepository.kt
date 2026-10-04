package com.workcontrol.app.data.prelo

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Prelo is the only source of identity, project membership and deploy decisions. */
@Singleton
class PreloContextRepository @Inject constructor(
    private val api: PreloApi,
    private val selection: PreloProjectContext,
) {
    suspend fun currentUser(): Me = api.me()

    fun selectProject(projectId: String?) = selection.select(projectId)

    suspend fun deploys(): List<ActionRequest> {
        val projectId = selection.selectedProjectId() ?: return emptyList()
        return api.deployActions(projectId)
    }

    /** Polling is transport only. Every response is server state, never a local decision. */
    fun observeDeploys(intervalMillis: Long = 15_000): Flow<List<ActionRequest>> = flow {
        require(intervalMillis >= 1_000)
        while (true) {
            emit(deploys())
            delay(intervalMillis)
        }
    }

    fun clearSelection() = selection.clear()
}
