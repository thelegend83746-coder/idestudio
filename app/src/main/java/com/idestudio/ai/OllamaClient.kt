package com.idestudio.ai

import com.idestudio.data.model.BuildAiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ConnectionTestResult {
    data class Success(val message: String, val latencyMs: Long, val availableModels: List<String>) : ConnectionTestResult()
    data class Failure(val statusCode: Int?, val message: String) : ConnectionTestResult()
}

data class AiResponse(
    val success: Boolean,
    val text: String,
    val suggestedChanges: List<FileChangeProposal> = emptyList(),
    val errorMessage: String? = null
)

data class FileChangeProposal(
    val relativeFilePath: String,
    val modifiedContent: String,
    val explanation: String
)

class OllamaClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(settings: BuildAiSettings): ConnectionTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val url = normalizeUrl(settings.endpointUrl) + "/api/tags"

        val requestBuilder = Request.Builder().url(url).get()
        if (settings.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${settings.apiKey}")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val models = mutableListOf<String>()
                    try {
                        val json = JSONObject(body)
                        val modelsArray = json.optJSONArray("models") ?: JSONArray()
                        for (i in 0 until modelsArray.length()) {
                            val item = modelsArray.getJSONObject(i)
                            val name = item.optString("name")
                            if (name.isNotBlank()) models.add(name)
                        }
                    } catch (_: Exception) {}

                    ConnectionTestResult.Success(
                        message = "Connected successfully (${response.code})",
                        latencyMs = latency,
                        availableModels = models
                    )
                } else {
                    val errorMsg = when (response.code) {
                        401 -> "Unauthorized: API key is invalid or expired."
                        403 -> "Forbidden: Access denied to provider endpoint."
                        404 -> "Endpoint not found: URL does not serve Ollama API."
                        429 -> "Rate limit exceeded: Provider request quota exhausted."
                        500, 502, 503 -> "Provider server error (${response.code})."
                        else -> "HTTP error ${response.code}: ${response.message}"
                    }
                    ConnectionTestResult.Failure(statusCode = response.code, message = errorMsg)
                }
            }
        } catch (e: IOException) {
            ConnectionTestResult.Failure(
                statusCode = null,
                message = "Network connection failed: ${e.message ?: "Unable to reach endpoint"}"
            )
        } catch (e: Exception) {
            ConnectionTestResult.Failure(
                statusCode = null,
                message = "Unexpected error: ${e.message}"
            )
        }
    }

    suspend fun requestAssistance(
        userPrompt: String,
        contextFiles: Map<String, String>,
        settings: BuildAiSettings
    ): AiResponse = withContext(Dispatchers.IO) {
        val url = normalizeUrl(settings.endpointUrl) + "/api/generate"

        val promptBuilder = StringBuilder()
        promptBuilder.append("Here is the relevant project source code:\n\n")
        for ((path, content) in contextFiles) {
            promptBuilder.append("--- FILE: ").append(path).append(" ---\n")
            promptBuilder.append(content).append("\n\n")
        }
        promptBuilder.append("User Request: ").append(userPrompt).append("\n\n")
        promptBuilder.append("Please provide the modified code or implementation in the format:\n")
        promptBuilder.append("```path/to/file\n<code>\n```")

        val payload = JSONObject().apply {
            put("model", settings.activeModel)
            put("system", settings.systemInstructions)
            put("prompt", promptBuilder.toString())
            put("stream", false)
        }

        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val requestBuilder = Request.Builder().url(url).post(requestBody)
        if (settings.apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${settings.apiKey}")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val rawText = json.optString("response", "")
                    val proposals = parseFileProposals(rawText)
                    AiResponse(success = true, text = rawText, suggestedChanges = proposals)
                } else {
                    AiResponse(success = false, text = "", errorMessage = "Provider error (${response.code}): ${response.message}")
                }
            }
        } catch (e: Exception) {
            AiResponse(success = false, text = "", errorMessage = "Connection failed: ${e.message}")
        }
    }

    private fun parseFileProposals(text: String): List<FileChangeProposal> {
        val proposals = mutableListOf<FileChangeProposal>()
        val codeBlockPattern = Regex("```(?:[a-zA-Z0-9_.-]+/)+[a-zA-Z0-9_.-]+\\.[a-zA-Z0-9]+\\n([\\s\\S]*?)```")
        // Also match ```path\n<code>```
        val matches = Regex("```([a-zA-Z0-9_./\\\\-]+)\\n([\\s\\S]*?)```").findAll(text)
        for (m in matches) {
            val path = m.groupValues[1].trim()
            val code = m.groupValues[2]
            if (path.contains(".") && !path.equals("java", true) && !path.equals("xml", true)) {
                proposals.add(
                    FileChangeProposal(
                        relativeFilePath = path,
                        modifiedContent = code,
                        explanation = "Proposed change for $path"
                    )
                )
            }
        }
        return proposals
    }

    private fun normalizeUrl(url: String): String {
        var trimmed = url.trim()
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        return trimmed
    }
}
