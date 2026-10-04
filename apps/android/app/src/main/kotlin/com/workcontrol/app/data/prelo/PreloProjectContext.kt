package com.workcontrol.app.data.prelo

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** UX selection only. The Prelo server validates membership and scopes. */
@Singleton
class PreloProjectContext @Inject constructor() {
    private val selected = AtomicReference<String?>(null)

    fun selectedProjectId(): String? = selected.get()

    fun select(projectId: String?) = selected.set(projectId)

    fun clear() = selected.set(null)
}
