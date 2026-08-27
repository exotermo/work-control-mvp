package com.workcontrol.app.data.auth

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** Boundary usado por HTTP e WebSocket. A implementação OIDC do Marco 2 alimentará este store. */
fun interface AccessTokenProvider {
    fun accessToken(): String?
}

/**
 * Sessão somente em memória: evita persistir bearer token sem a proteção do Android Keystore.
 * O armazenamento durável será adicionado junto ao login OIDC, não como preferência em texto claro.
 */
@Singleton
class InMemoryAccessTokenStore @Inject constructor() : AccessTokenProvider {
    private val token = AtomicReference<String?>(null)

    override fun accessToken(): String? = token.get()

    fun replace(accessToken: String) {
        require(accessToken.isNotBlank()) { "access token não pode ser vazio" }
        token.set(accessToken)
    }

    fun clear() {
        token.set(null)
    }
}
