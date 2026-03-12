package com.gymapp.core.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

@Singleton
class AiSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_API_KEY = stringPreferencesKey("openai_api_key")
    }

    val apiKey: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_API_KEY] ?: "" }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[KEY_API_KEY] = key.trim() }
    }
}
