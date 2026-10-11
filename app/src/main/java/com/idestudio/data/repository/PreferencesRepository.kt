package com.idestudio.data.repository

import android.content.Context
import com.idestudio.data.model.BuildAiSettings
import com.idestudio.data.model.EditorSettings
import com.idestudio.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class PreferencesRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val settingsFile: File
        get() = File(context.filesDir, "editor_settings.json")

    private val aiSettingsFile: File
        get() = File(context.filesDir, "ai_settings.json")

    private val _editorSettings = MutableStateFlow(EditorSettings())
    val editorSettings: StateFlow<EditorSettings> = _editorSettings.asStateFlow()

    private val _aiSettings = MutableStateFlow(BuildAiSettings())
    val aiSettings: StateFlow<BuildAiSettings> = _aiSettings.asStateFlow()

    init {
        loadSettingsSync()
    }

    private fun loadSettingsSync() {
        if (settingsFile.exists()) {
            try {
                val text = settingsFile.readText(Charsets.UTF_8)
                _editorSettings.value = json.decodeFromString(EditorSettings.serializer(), text)
            } catch (_: Exception) {}
        }
        if (aiSettingsFile.exists()) {
            try {
                val text = aiSettingsFile.readText(Charsets.UTF_8)
                _aiSettings.value = json.decodeFromString(BuildAiSettings.serializer(), text)
            } catch (_: Exception) {}
        }
    }

    suspend fun updateEditorSettings(newSettings: EditorSettings) = withContext(Dispatchers.IO) {
        _editorSettings.value = newSettings
        try {
            val content = json.encodeToString(EditorSettings.serializer(), newSettings)
            FileUtils.writeFileSafe(settingsFile, content)
        } catch (_: Exception) {}
    }

    suspend fun updateAiSettings(newSettings: BuildAiSettings) = withContext(Dispatchers.IO) {
        _aiSettings.value = newSettings
        try {
            val content = json.encodeToString(BuildAiSettings.serializer(), newSettings)
            FileUtils.writeFileSafe(aiSettingsFile, content)
        } catch (_: Exception) {}
    }
}
