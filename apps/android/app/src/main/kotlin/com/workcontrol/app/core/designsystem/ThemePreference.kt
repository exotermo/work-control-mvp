package com.workcontrol.app.core.designsystem

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Which edition the app prints: follow the system, always paper, or always the night edition. */
enum class ThemeMode(val label: String) { SYSTEM("Sistema"), PAPER("Claro"), NIGHT("Noturno") }

private val Context.appearance by preferencesDataStore(name = "prelo_appearance")
private val THEME_MODE = stringPreferencesKey("theme_mode")

/** Display preference only — not a secret, kept apart from the session/device store. */
@Singleton
class ThemePreference @Inject constructor(@ApplicationContext private val context: Context) {
    val mode: Flow<ThemeMode> = context.appearance.data.map { prefs ->
        prefs[THEME_MODE]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } } ?: ThemeMode.SYSTEM
    }

    suspend fun set(mode: ThemeMode) {
        context.appearance.edit { it[THEME_MODE] = mode.name }
    }
}
