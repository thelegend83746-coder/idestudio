package com.idestudio.app.data.model

data class AiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val proposedDiff: String? = null,
    val targetFilePath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isApplied: Boolean = false
)

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}
