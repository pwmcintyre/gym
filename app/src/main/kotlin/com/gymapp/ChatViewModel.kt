package com.gymapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.ai.AiAssistant
import com.gymapp.core.ai.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiChatMessage(
    val content: String,
    val isUser: Boolean,
    val isError: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiAssistant: AiAssistant,
) : ViewModel() {

    private val _messages = mutableStateListOf<UiChatMessage>()
    val messages: List<UiChatMessage> get() = _messages

    var isLoading by mutableStateOf(false)
        private set

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        _messages.add(UiChatMessage(trimmed, isUser = true))
        isLoading = true

        viewModelScope.launch {
            val history = _messages
                .filter { !it.isError }
                .map {
                    ChatMessage(
                        role = if (it.isUser) ChatMessage.Role.USER else ChatMessage.Role.ASSISTANT,
                        content = it.content,
                    )
                }

            val result = aiAssistant.chat(SYSTEM_PROMPT, history)
            isLoading = false

            result.fold(
                onSuccess = { reply ->
                    _messages.add(UiChatMessage(reply, isUser = false))
                },
                onFailure = {
                    _messages.add(
                        UiChatMessage(
                            content = "Couldn't reach the AI, try again",
                            isUser = false,
                            isError = true,
                        ),
                    )
                },
            )
        }
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You are a personal training coach. Be present, concise, and direct.
            No filler, no disclaimers, no "As an AI..." hedging.
            Talk like a coach, not a chatbot. Get to the point.
        """.trimIndent()
    }
}
