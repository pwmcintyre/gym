package com.gymapp.core.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that holds the last AI error message so any screen can surface it.
 * Cleared when a successful AI call completes.
 */
@Singleton
class AiStatusStore @Inject constructor() {
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun setError(message: String) { _lastError.value = message }
    fun clearError() { _lastError.value = null }
}
