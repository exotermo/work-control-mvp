package com.workcontrol.app.feature.prelo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue

data class Destination(val page: Page, val id: String? = null, val projectId: String? = null)

/** UI history only. The Prelo still owns project access and every resource. */
class NavigationStack(initial: Destination = Destination(Page.HOME), history: List<Destination> = emptyList()) {
    var current by mutableStateOf(initial)
        private set
    var history by mutableStateOf(history)
        private set

    val canGoBack: Boolean get() = current.id != null || history.isNotEmpty() || current.page != Page.HOME

    fun navigate(target: Destination) {
        if (target == current) return
        history = history + current
        current = target
    }

    fun tab(page: Page) {
        history = emptyList()
        current = Destination(page, projectId = current.projectId)
    }

    fun back(): Boolean {
        if (history.isNotEmpty()) {
            current = history.last()
            history = history.dropLast(1)
            return true
        }
        if (current.page != Page.HOME || current.id != null) {
            current = Destination(Page.HOME, projectId = current.projectId)
            return true
        }
        return false
    }

    fun project(id: String?) { current = current.copy(projectId = id) }

    companion object {
        val Saver: Saver<NavigationStack, ArrayList<String>> = Saver(
            save = { stack ->
                ArrayList((stack.history + stack.current).flatMap { listOf(it.page.name, it.id.orEmpty(), it.projectId.orEmpty()) })
            },
            restore = { raw ->
                val entries = raw.chunked(3).mapNotNull { fields ->
                    val page = runCatching { Page.valueOf(fields[0]) }.getOrNull() ?: return@mapNotNull null
                    Destination(page, fields[1].ifEmpty { null }, fields[2].ifEmpty { null })
                }
                NavigationStack(entries.lastOrNull() ?: Destination(Page.HOME), entries.dropLast(1))
            },
        )
    }
}
