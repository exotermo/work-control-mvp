package com.workcontrol.app.data.push

import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PushDestination(val kind: String, val id: String, val projectId: String?) {
    companion object {
        fun from(values: Map<String, String?>): PushDestination? {
            val kind = values["kind"] ?: return null
            val id = values["id"]?.takeIf { it.isNotBlank() } ?: return null
            if (kind !in setOf("approval", "action")) return null
            return PushDestination(kind, id, values["projectId"]?.takeIf { it.isNotBlank() })
        }
    }
}

@Singleton
class PushRouting @Inject constructor() {
    private val mutableDestination = MutableStateFlow<PushDestination?>(null)
    val destination: StateFlow<PushDestination?> = mutableDestination
    fun accept(intent: Intent?) {
        val extras = intent?.extras ?: return
        mutableDestination.value = PushDestination.from(mapOf(
            "kind" to extras.getString("kind"), "id" to extras.getString("id"),
            "projectId" to extras.getString("projectId")))
    }
    fun consumed(value: PushDestination) {
        if (mutableDestination.value == value) mutableDestination.value = null
    }
}
