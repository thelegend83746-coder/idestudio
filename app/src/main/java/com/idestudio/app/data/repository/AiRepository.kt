package com.idestudio.app.data.repository

import com.idestudio.app.data.model.AiMessage
import com.idestudio.app.data.model.MessageSender
import com.idestudio.app.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AiRepository(
    private val settingsRepository: SettingsRepository
) {

    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    fun addMessage(message: AiMessage) {
        _messages.value = _messages.value + message
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    suspend fun sendMessage(
        userPrompt: String,
        currentProject: Project?,
        activeFile: File?,
        activeFileContent: String?
    ): Result<AiMessage> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.value
        val apiKey = settings.aiApiKey.trim()

        if (apiKey.isEmpty()) {
            val configMsg = AiMessage(
                sender = MessageSender.ASSISTANT,
                text = "⚠️ No AI Provider API key is configured.\n\nPlease go to Settings > AI Builder to enter your API key (Gemini, OpenAI, or compatible endpoint). Once configured, IDE STUDIO can assist you in generating code, fixing compilation errors, and suggesting architectural improvements."
            )
            addMessage(configMsg)
            return@withContext Result.success(configMsg)
        }

        try {
            // Build system prompt and context
            val contextBuilder = StringBuilder()
            contextBuilder.append("You are the AI Assistant embedded in IDE STUDIO, an on-device Android IDE.\n")
            if (currentProject != null) {
                contextBuilder.append("Active Project: ${currentProject.name} (${currentProject.packageName})\n")
                contextBuilder.append("Min SDK: ${currentProject.minSdk}, Target SDK: ${currentProject.targetSdk}\n")
            }
            if (activeFile != null) {
                contextBuilder.append("Active File: ${activeFile.name}\n")
                if (!activeFileContent.isNullOrBlank()) {
                    contextBuilder.append("```\n$activeFileContent\n```\n")
                }
            }
            contextBuilder.append("\nUser Request: $userPrompt\n")
            contextBuilder.append("If proposing file modifications, specify the code clearly.")

            val fullText = callAiService(
                provider = settings.aiProvider,
                apiKey = apiKey,
                model = settings.aiModel,
                prompt = contextBuilder.toString(),
                customEndpoint = settings.customAiEndpoint
            )

            // Parse for proposed code change or diff if present
            val proposedDiff = extractProposedCode(fullText)
            val assistantMessage = AiMessage(
                sender = MessageSender.ASSISTANT,
                text = fullText,
                proposedDiff = proposedDiff,
                targetFilePath = activeFile?.absolutePath
            )
            addMessage(assistantMessage)
            Result.success(assistantMessage)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = AiMessage(
                sender = MessageSender.ASSISTANT,
                text = "Error communicating with AI service: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
            addMessage(errorMsg)
            Result.failure(e)
        }
    }

    suspend fun applyProposedChange(message: AiMessage): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val targetPath = message.targetFilePath ?: return@withContext Result.failure(IllegalStateException("No target file specified."))
            val code = message.proposedDiff ?: return@withContext Result.failure(IllegalStateException("No code diff to apply."))

            val file = File(targetPath)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
            }
            file.writeText(code)

            // Mark message as applied
            _messages.value = _messages.value.map {
                if (it.id == message.id) it.copy(isApplied = true) else it
            }
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun extractProposedCode(text: String): String? {
        val codeBlockRegex = Regex("```(?:[a-zA-Z0-9]+)?\\n([\\s\\S]*?)```")
        val match = codeBlockRegex.find(text)
        return match?.groups?.get(1)?.value?.trim()
    }

    private fun callAiService(
        provider: String,
        apiKey: String,
        model: String,
        prompt: String,
        customEndpoint: String
    ): String {
        return when (provider.lowercase()) {
            "openai" -> callOpenAi(apiKey, model, prompt)
            "custom" -> callCustomEndpoint(customEndpoint, apiKey, prompt)
            else -> callGemini(apiKey, model, prompt)
        }
    }

    private fun callGemini(apiKey: String, model: String, prompt: String): String {
        val modelName = if (model.isNotBlank()) model else "gemini-1.5-flash"
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 30000
        conn.readTimeout = 30000

        val requestJson = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    }
                    put("parts", parts)
                })
            }
            put("contents", contents)
        }

        OutputStreamWriter(conn.outputStream).use { it.write(requestJson.toString()) }

        val responseCode = conn.responseCode
        if (responseCode != 200) {
            val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            throw RuntimeException("Gemini API error ($responseCode): $errorText")
        }

        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(responseText)
        val candidates = json.getJSONArray("candidates")
        if (candidates.length() > 0) {
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            if (parts.length() > 0) {
                return parts.getJSONObject(0).getString("text")
            }
        }
        return "No response generated."
    }

    private fun callOpenAi(apiKey: String, model: String, prompt: String): String {
        val url = URL("https://api.openai.com/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.doOutput = true
        conn.connectTimeout = 30000
        conn.readTimeout = 30000

        val requestJson = JSONObject().apply {
            put("model", if (model.isNotBlank()) model else "gpt-3.5-turbo")
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messages)
        }

        OutputStreamWriter(conn.outputStream).use { it.write(requestJson.toString()) }

        val responseCode = conn.responseCode
        if (responseCode != 200) {
            val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            throw RuntimeException("OpenAI API error ($responseCode): $errorText")
        }

        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(responseText)
        val choices = json.getJSONArray("choices")
        if (choices.length() > 0) {
            return choices.getJSONObject(0).getJSONObject("message").getString("content")
        }
        return "No response generated."
    }

    private fun callCustomEndpoint(endpoint: String, apiKey: String, prompt: String): String {
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        if (apiKey.isNotBlank()) {
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
        }
        conn.doOutput = true

        val requestJson = JSONObject().apply {
            put("prompt", prompt)
        }
        OutputStreamWriter(conn.outputStream).use { it.write(requestJson.toString()) }

        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
        return responseText
    }
}
