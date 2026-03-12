package com.gymapp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.ai.AiSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val aiSettings: AiSettings,
) : ViewModel() {

    val apiKey: StateFlow<String> = aiSettings.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun saveApiKey(key: String) {
        viewModelScope.launch { aiSettings.setApiKey(key) }
    }
}
