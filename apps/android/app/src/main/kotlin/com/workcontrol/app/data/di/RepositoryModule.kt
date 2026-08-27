package com.workcontrol.app.data.di

import com.workcontrol.app.data.remote.RemoteAgentRepository
import com.workcontrol.app.data.remote.RemoteApprovalRepository
import com.workcontrol.app.data.remote.RemoteMachineRepository
import com.workcontrol.app.data.remote.RemoteTaskRepository
import com.workcontrol.app.data.remote.RemoteWorkspaceRepository
import com.workcontrol.app.domain.repository.AgentRepository
import com.workcontrol.app.domain.repository.ApprovalRepository
import com.workcontrol.app.domain.repository.MachineRepository
import com.workcontrol.app.domain.repository.TaskRepository
import com.workcontrol.app.domain.repository.WorkspaceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWorkspaceRepository(impl: RemoteWorkspaceRepository): WorkspaceRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: RemoteTaskRepository): TaskRepository

    @Binds
    @Singleton
    abstract fun bindAgentRepository(impl: RemoteAgentRepository): AgentRepository

    @Binds
    @Singleton
    abstract fun bindMachineRepository(impl: RemoteMachineRepository): MachineRepository

    @Binds
    @Singleton
    abstract fun bindApprovalRepository(impl: RemoteApprovalRepository): ApprovalRepository
}
