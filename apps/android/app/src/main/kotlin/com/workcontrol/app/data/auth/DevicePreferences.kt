package com.workcontrol.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.preloPreferences by preferencesDataStore(name = "prelo_device")
private val DEVICE_ID = stringPreferencesKey("device_id")
private val PROJECT_ID = stringPreferencesKey("project_id")

@Singleton
class DevicePreferences @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun deviceId(): String {
        val existing = context.preloPreferences.data.first()[DEVICE_ID]
        if (existing != null) return existing
        val created = UUID.randomUUID().toString()
        context.preloPreferences.edit { if (it[DEVICE_ID] == null) it[DEVICE_ID] = created }
        return context.preloPreferences.data.first()[DEVICE_ID] ?: created
    }

    suspend fun projectId(): String? = context.preloPreferences.data.first()[PROJECT_ID]
    suspend fun setProjectId(id: String?) {
        context.preloPreferences.edit { if (id == null) it.remove(PROJECT_ID) else it[PROJECT_ID] = id }
    }
}
