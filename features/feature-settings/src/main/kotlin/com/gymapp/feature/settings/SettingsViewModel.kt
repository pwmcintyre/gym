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

    val proxyUrl: StateFlow<String> = aiSettings.proxyUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun saveProxyUrl(url: String) {
        viewModelScope.launch { aiSettings.setProxyUrl(url) }
    }
}
