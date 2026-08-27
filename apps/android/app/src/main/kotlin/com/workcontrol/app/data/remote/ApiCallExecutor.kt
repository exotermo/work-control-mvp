package com.workcontrol.app.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.workcontrol.app.data.remote.dto.ApiErrorDto
import com.workcontrol.app.domain.error.WorkControlException
import java.io.EOFException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

@Singleton
class ApiCallExecutor @Inject constructor(
    private val gson: Gson,
) {
    suspend fun <T> execute(call: suspend () -> T): T = try {
        call()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (http: HttpException) {
        throw http.toDomainFailure()
    } catch (invalidPayload: EOFException) {
        throw WorkControlException.Protocol(invalidPayload)
    } catch (network: IOException) {
        throw WorkControlException.Offline(network)
    } catch (invalidPayload: JsonParseException) {
        throw WorkControlException.Protocol(invalidPayload)
    } catch (invalidPayload: IllegalArgumentException) {
        throw WorkControlException.Protocol(invalidPayload)
    } catch (invalidPayload: NullPointerException) {
        throw WorkControlException.Protocol(invalidPayload)
    }

    private fun HttpException.toDomainFailure(): WorkControlException {
        // A mensagem é lida apenas para validar o envelope. Ela nunca é mostrada nem registrada.
        runCatching {
            response()?.errorBody()?.charStream()?.use { gson.fromJson(it, ApiErrorDto::class.java) }
        }
        return when (code()) {
            401 -> WorkControlException.Authentication(this)
            403 -> WorkControlException.Authorization(this)
            404 -> WorkControlException.NotFound(this)
            409 -> WorkControlException.Conflict(this)
            429 -> WorkControlException.RateLimited(
                retryAfterSeconds = response()?.headers()?.get("Retry-After")?.toLongOrNull(),
                cause = this,
            )
            in 500..599 -> WorkControlException.Server(this)
            else -> WorkControlException.Protocol(this)
        }
    }
}
