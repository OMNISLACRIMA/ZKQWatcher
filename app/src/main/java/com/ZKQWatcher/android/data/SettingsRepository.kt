// data/SettingsRepository.kt
package com.ZKQWatcher.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.ZKQWatcher.android.model.Settings

private val Context.dataStore by preferencesDataStore("zkq_settings")
private const val KEY_JSON = "settings_json"

class SettingsRepository(private val ctx: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    /** 读取（若第一次则返回默认值） */
    suspend fun load(): Settings {
        val prefs = ctx.dataStore.data.first()
        val str = prefs[androidx.datastore.preferences.core.stringPreferencesKey(KEY_JSON)]
        return if (str.isNullOrEmpty()) Settings() else json.decodeFromString(str)
    }

    /** 覆写保存 */
    suspend fun save(s: Settings) {
        ctx.dataStore.edit { it[androidx.datastore.preferences.core.stringPreferencesKey(KEY_JSON)] = json.encodeToString(s) }
    }
}
