package com.yourorg.objectcapture.model

data class FeedbackMessage(
    val text: String,
    val level: Level = Level.INFO
) {
    enum class Level {
        INFO,
        WARNING,
        ERROR
    }
}
