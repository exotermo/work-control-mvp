package com.workcontrol.app.data.remote

import com.workcontrol.app.data.remote.dto.AgentDto
import com.workcontrol.app.data.remote.dto.ApprovalDto
import com.workcontrol.app.data.remote.dto.CreateTaskRequestDto
import com.workcontrol.app.data.remote.dto.DeviceDto
import com.workcontrol.app.data.remote.dto.MeResponseDto
import com.workcontrol.app.data.remote.dto.TaskDetailDto
import com.workcontrol.app.data.remote.dto.TaskDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface WorkControlApi {
    @GET("v1/me")
    suspend fun me(): MeResponseDto

    @GET("v1/tasks")
    suspend fun tasks(): List<TaskDto>

    @GET("v1/tasks/{id}")
    suspend fun task(@Path("id") id: String): TaskDetailDto

    @POST("v1/tasks")
    suspend fun createTask(@Body request: CreateTaskRequestDto): TaskDto

    @GET("v1/agents")
    suspend fun activeAgents(): List<AgentDto>

    @GET("v1/devices")
    suspend fun devices(): List<DeviceDto>

    @GET("v1/approvals")
    suspend fun pendingApprovals(): List<ApprovalDto>

    @POST("v1/approvals/{id}/approve")
    suspend fun approve(@Path("id") id: String): ApprovalDto

    @POST("v1/approvals/{id}/reject")
    suspend fun reject(@Path("id") id: String): ApprovalDto
}
