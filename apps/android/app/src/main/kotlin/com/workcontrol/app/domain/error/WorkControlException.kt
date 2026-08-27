package com.workcontrol.app.domain.error

/** Falhas esperadas do boundary remoto, sem expor corpo HTTP ou detalhes sensíveis à UI. */
sealed class WorkControlException(
    val userMessage: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    class Authentication(cause: Throwable? = null) : WorkControlException(
        userMessage = "Sua sessão expirou. Entre novamente para continuar.",
        message = "remote authentication failed",
        cause = cause,
    )

    class Authorization(cause: Throwable? = null) : WorkControlException(
        userMessage = "Você não tem acesso a este conteúdo.",
        message = "remote authorization failed",
        cause = cause,
    )

    class NotFound(cause: Throwable? = null) : WorkControlException(
        userMessage = "Este conteúdo não existe mais ou foi removido.",
        message = "remote resource not found",
        cause = cause,
    )

    class Conflict(cause: Throwable? = null) : WorkControlException(
        userMessage = "O estado mudou no servidor. Atualize antes de tentar novamente.",
        message = "remote state conflict",
        cause = cause,
    )

    class RateLimited(
        val retryAfterSeconds: Long?,
        cause: Throwable? = null,
    ) : WorkControlException(
        userMessage = "Muitas solicitações. Aguarde um pouco e tente novamente.",
        message = "remote rate limit exceeded",
        cause = cause,
    )

    class Offline(cause: Throwable? = null) : WorkControlException(
        userMessage = "Não foi possível conectar. Verifique a rede e tente novamente.",
        message = "remote network unavailable",
        cause = cause,
    )

    class Server(cause: Throwable? = null) : WorkControlException(
        userMessage = "O serviço está indisponível no momento. Tente novamente.",
        message = "remote server failed",
        cause = cause,
    )

    class Protocol(cause: Throwable? = null) : WorkControlException(
        userMessage = "A resposta do serviço não pôde ser processada.",
        message = "remote protocol failed",
        cause = cause,
    )
}

fun Throwable.toUserMessage(): String =
    (this as? WorkControlException)?.userMessage
        ?: "Não foi possível concluir a operação. Tente novamente."
