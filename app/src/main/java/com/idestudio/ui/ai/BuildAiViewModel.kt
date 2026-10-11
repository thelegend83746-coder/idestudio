package com.idestudio.ui.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idestudio.ai.ConnectionTestResult
import com.idestudio.ai.OllamaClient
import com.idestudio.data.model.BuildAiSettings
import com.idestudio.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BuildAiUiState(
    val endpointUrl: String = "https://api.ollama.com",
    val apiKey: String = "",
    val activeModel: String = "qwen2.5-coder:7b",
    val systemInstructions: String = BuildAiSettings.DEFAULT_SYSTEM_INSTRUCTIONS,
    val isTesting: Boolean = false,
    val testResult: ConnectionTestResult? = null,
    val discoveredModels: List<String> = emptyList(),
    val isSavedSuccess: Boolean = false
)

class BuildAiViewModel(application: Application) : AndroidViewModel(application) {

    private val prefRepo = PreferencesRepository(application)
    private val ollamaClient = OllamaClient()

    private val _uiState = MutableStateFlow(BuildAiUiState())
    val uiState: StateFlow<BuildAiUiState> = _uiState.asStateFlow()

    init {
        val currentSettings = prefRepo.aiSettings.value
        _uiState.update {
            it.copy(
                endpointUrl = currentSettings.endpointUrl,
                apiKey = currentSettings.apiKey,
                activeModel = currentSettings.activeModel,
                systemInstructions = currentSettings.systemInstructions
            )
        }
    }

    fun updateEndpoint(url: String) {
        _uiState.update { it.copy(endpointUrl = url, testResult = null, isSavedSuccess = false) }
    }

    fun updateApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key, testResult = null, isSavedSuccess = false) }
    }

    fun updateActiveModel(model: String) {
        _uiState.update { it.copy(activeModel = model, isSavedSuccess = false) }
    }

    fun updateSystemInstructions(instructions: String) {
        _uiState.update { it.copy(systemInstructions = instructions, isSavedSuccess = false) }
    }

    fun testConnection() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }
            val tempSettings = BuildAiSettings(
                endpointUrl = current.endpointUrl,
                apiKey = current.apiKey,
                activeModel = current.activeModel,
                systemInstructions = current.systemInstructions
            )
            val result = ollamaClient.testConnection(tempSettings)

            val models = if (result is ConnectionTestResult.Success && result.availableModels.isNotEmpty()) {
                result.availableModels
            } else {
                current.discoveredModels
            }

            _uiState.update {
                it.copy(
                    isTesting = false,
                    testResult = result,
                    discoveredModels = models
                )
            }
        }
    }

    fun saveSettings() {
        val current = _uiState.value
        val updated = BuildAiSettings(
            endpointUrl = current.endpointUrl.trim(),
            apiKey = current.apiKey.trim(),
            activeModel = current.activeModel.trim(),
            systemInstructions = current.systemInstructions.trim(),
            isConnected = current.testResult is ConnectionTestResult.Success,
            lastConnectedTime = if (current.testResult is ConnectionTestResult.Success) System.currentTimeMillis() else 0L
        )
        viewModelScope.launch {
            prefRepo.updateAiSettings(updated)
            _uiState.update { it.copy(isSavedSuccess = true) }
        }
    }
}
