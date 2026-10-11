package com.idestudio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BuildAiSettings(
    val endpointUrl: String = "https://api.ollama.com",
    val apiKey: String = "",
    val activeModel: String = "qwen2.5-coder:7b",
    val systemInstructions: String = DEFAULT_SYSTEM_INSTRUCTIONS,
    val lastConnectedTime: Long = 0L,
    val isConnected: Boolean = false
) {
    companion object {
        const val DEFAULT_SYSTEM_INSTRUCTIONS =
            "You are the IDE STUDIO Build AI Assistant, an expert Android developer specialized in native Java and XML development. " +
            "Provide clean, production-ready, bug-free Android Java code and XML layouts compatible with API levels 21 to 34. " +
            "When proposing changes, provide explicit file paths and replacement blocks."
    }
}
