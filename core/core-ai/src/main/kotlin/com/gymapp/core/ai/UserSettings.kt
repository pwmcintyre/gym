package com.gymapp.core.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class UserSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_BODY_WEIGHT = floatPreferencesKey("body_weight_kg")
        private val KEY_REST_TIMER_SECONDS = intPreferencesKey("rest_timer_seconds")
        const val DEFAULT_REST_TIMER_SECONDS = 90
    }

    val bodyWeightKg: Flow<Float?> = context.userDataStore.data
        .map { prefs -> prefs[KEY_BODY_WEIGHT] }

    val restTimerSeconds: Flow<Int> = context.userDataStore.data
        .map { prefs -> prefs[KEY_REST_TIMER_SECONDS] ?: DEFAULT_REST_TIMER_SECONDS }

    suspend fun setBodyWeightKg(kg: Float) {
        context.userDataStore.edit { prefs -> prefs[KEY_BODY_WEIGHT] = kg }
    }

    suspend fun setRestTimerSeconds(seconds: Int) {
        context.userDataStore.edit { prefs -> prefs[KEY_REST_TIMER_SECONDS] = seconds }
    }
}
