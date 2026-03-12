package com.gymapp.core.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
    }

    val bodyWeightKg: Flow<Float?> = context.userDataStore.data
        .map { prefs -> prefs[KEY_BODY_WEIGHT] }

    suspend fun setBodyWeightKg(kg: Float) {
        context.userDataStore.edit { prefs -> prefs[KEY_BODY_WEIGHT] = kg }
    }
}
