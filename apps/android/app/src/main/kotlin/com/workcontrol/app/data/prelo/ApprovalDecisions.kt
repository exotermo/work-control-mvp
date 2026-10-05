package com.workcontrol.app.data.prelo

import com.google.gson.JsonParser
import javax.inject.Inject
import retrofit2.HttpException

sealed interface ApproveOutcome {
    data object Done : ApproveOutcome
    data object TotpRequired : ApproveOutcome
}

class ApprovalDecisions @Inject constructor(private val api: PreloResourceApi) {
    suspend fun approve(id: String, totpCode: String? = null): ApproveOutcome {
        return try {
            api.approve(id, ApprovalDecision(totpCode))
            ApproveOutcome.Done
        } catch (failure: HttpException) {
            val code = runCatching {
                JsonParser.parseReader(failure.response()?.errorBody()?.charStream()).asJsonObject.get("code")?.asString
            }.getOrNull()
            if (failure.code() == 403 && code == "step_up_required" && totpCode == null) ApproveOutcome.TotpRequired
            else throw failure
        }
    }

    suspend fun deny(id: String) { api.deny(id) }
}
