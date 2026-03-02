package com.hellostar.stellarnotes.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferences(private val context: Context) {
    private val INTRO_SHOWN_KEY = booleanPreferencesKey("intro_shown")

    val hasSeenIntro: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[INTRO_SHOWN_KEY] ?: false
    }

    suspend fun setIntroSeen() {
        context.dataStore.edit { prefs ->
            prefs[INTRO_SHOWN_KEY] = true
        }
    }
}
