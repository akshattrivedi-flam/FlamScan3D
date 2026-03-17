package com.yourorg.objectcapture.core

import com.yourorg.objectcapture.model.FeedbackMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FeedbackManager {
    private val _messages = MutableStateFlow<List<FeedbackMessage>>(emptyList())
    val messages: StateFlow<List<FeedbackMessage>> = _messages.asStateFlow()

    fun postInfo(text: String) {
        add(FeedbackMessage(text, FeedbackMessage.Level.INFO))
    }

    fun postWarning(text: String) {
        add(FeedbackMessage(text, FeedbackMessage.Level.WARNING))
    }

    fun postError(text: String) {
        add(FeedbackMessage(text, FeedbackMessage.Level.ERROR))
    }

    fun clear() {
        _messages.value = emptyList()
    }

    private fun add(message: FeedbackMessage) {
        _messages.value = _messages.value + message
    }
}
