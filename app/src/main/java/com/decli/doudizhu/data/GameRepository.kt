package com.decli.doudizhu.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.decli.doudizhu.model.GameSession
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.gameDataStore by preferencesDataStore(name = "doudizhu_state")

class GameRepository(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun loadSession(): GameSession? {
        val preferences = context.gameDataStore.data.first()
        val raw = preferences[SESSION_KEY] ?: return null
        return runCatching { json.decodeFromString<GameSession>(raw) }.getOrNull()
    }

    suspend fun saveSession(session: GameSession) {
        context.gameDataStore.edit { prefs ->
            prefs[SESSION_KEY] = json.encodeToString(session)
        }
    }

    suspend fun clearSession() {
        context.gameDataStore.edit { prefs ->
            prefs.remove(SESSION_KEY)
        }
    }

    private companion object {
        val SESSION_KEY = stringPreferencesKey("session_json")
    }
}

