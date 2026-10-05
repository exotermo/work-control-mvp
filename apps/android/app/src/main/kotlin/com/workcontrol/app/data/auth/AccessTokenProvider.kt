package com.workcontrol.app.data.auth

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** Access token da sessão Prelo, consumido somente pelo cliente HTTP e SSE. */
fun interface AccessTokenProvider {
    fun accessToken(): String?
}

/**
 * Sessão somente em memória: evita persistir bearer token sem a proteção do Android Keystore.
 * O refresh cifrado fica em SecureRefreshStore; o access token nunca é persistido.
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
