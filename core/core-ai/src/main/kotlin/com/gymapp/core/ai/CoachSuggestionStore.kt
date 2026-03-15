package com.gymapp.core.ai

import javax.inject.Inject
import javax.inject.Singleton

data class PendingCoachSuggestion(
    val sessionId: String,
    val prompt: String,
)

@Singleton
class CoachSuggestionStore @Inject constructor() {
    private var pending: PendingCoachSuggestion? = null

    fun set(sessionId: String, prompt: String) {
        pending = PendingCoachSuggestion(sessionId = sessionId, prompt = prompt)
    }

    fun consume(sessionId: String): String? {
        val current = pending ?: return null
        if (current.sessionId != sessionId) return null
        pending = null
        return current.prompt
    }
}
