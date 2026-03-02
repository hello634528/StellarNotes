package com.hellostar.stellarnotes.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferences(private val context: Context) {
    companion object {
        private val INTRO_SHOWN = booleanPreferencesKey("intro_shown")
        private val API_BASE_URL = stringPreferencesKey("api_base_url")
        private val API_KEY = stringPreferencesKey("api_key")
        private val MODEL_NAME = stringPreferencesKey("model_name")
        private val MODEL_LIST = stringPreferencesKey("model_list")
        private val ENABLE_THINKING = booleanPreferencesKey("enable_thinking")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val hasSeenIntro: Flow<Boolean> = context.dataStore.data.map { it[INTRO_SHOWN] ?: false }
    val apiBaseUrl: Flow<String> = context.dataStore.data.map { it[API_BASE_URL] ?: "" }
    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val modelName: Flow<String> = context.dataStore.data.map { it[MODEL_NAME] ?: "" }
    val enableThinking: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_THINKING] ?: true }
    val modelList: Flow<List<String>> = context.dataStore.data.map {
        val raw = it[MODEL_LIST] ?: "[]"
        try { json.decodeFromString<List<String>>(raw) } catch (_: Exception) { emptyList() }
    }

    suspend fun setIntroSeen() { context.dataStore.edit { it[INTRO_SHOWN] = true } }

    suspend fun saveApiSettings(baseUrl: String, key: String, model: String, thinking: Boolean) {
        context.dataStore.edit {
            it[API_BASE_URL] = baseUrl; it[API_KEY] = key; it[MODEL_NAME] = model; it[ENABLE_THINKING] = thinking
        }
    }

    suspend fun saveModelList(models: List<String>) {
        context.dataStore.edit { it[MODEL_LIST] = json.encodeToString(models) }
    }

    suspend fun setDefaultModel(model: String) {
        context.dataStore.edit { it[MODEL_NAME] = model }
    }
}
