package com.idestudio.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.idestudio.app.data.model.SettingsModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ide_studio_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SettingsModel> = _settings.asStateFlow()

    private fun loadSettings(): SettingsModel {
        return SettingsModel(
            themeMode = prefs.getString("themeMode", "light") ?: "light",
            fontSize = prefs.getInt("fontSize", 14),
            showLineNumbers = prefs.getBoolean("showLineNumbers", true),
            wordWrap = prefs.getBoolean("wordWrap", false),
            autoIndent = prefs.getBoolean("autoIndent", true),
            bracketMatching = prefs.getBoolean("bracketMatching", true),
            syntaxHighlighting = prefs.getBoolean("syntaxHighlighting", true),
            tabSize = prefs.getInt("tabSize", 4),
            autoSave = prefs.getBoolean("autoSave", true),
            hapticFeedback = prefs.getBoolean("hapticFeedback", true),
            cleanBuildBeforeRun = prefs.getBoolean("cleanBuildBeforeRun", false),
            autoInstallApk = prefs.getBoolean("autoInstallApk", true),
            aiProvider = prefs.getString("aiProvider", "gemini") ?: "gemini",
            aiApiKey = prefs.getString("aiApiKey", "") ?: "",
            aiModel = prefs.getString("aiModel", "gemini-1.5-pro") ?: "gemini-1.5-pro",
            customAiEndpoint = prefs.getString("customAiEndpoint", "") ?: ""
        )
    }

    fun updateSettings(newSettings: SettingsModel) {
        prefs.edit().apply {
            putString("themeMode", newSettings.themeMode)
            putInt("fontSize", newSettings.fontSize)
            putBoolean("showLineNumbers", newSettings.showLineNumbers)
            putBoolean("wordWrap", newSettings.wordWrap)
            putBoolean("autoIndent", newSettings.autoIndent)
            putBoolean("bracketMatching", newSettings.bracketMatching)
            putBoolean("syntaxHighlighting", newSettings.syntaxHighlighting)
            putInt("tabSize", newSettings.tabSize)
            putBoolean("autoSave", newSettings.autoSave)
            putBoolean("hapticFeedback", newSettings.hapticFeedback)
            putBoolean("cleanBuildBeforeRun", newSettings.cleanBuildBeforeRun)
            putBoolean("autoInstallApk", newSettings.autoInstallApk)
            putString("aiProvider", newSettings.aiProvider)
            putString("aiApiKey", newSettings.aiApiKey)
            putString("aiModel", newSettings.aiModel)
            putString("customAiEndpoint", newSettings.customAiEndpoint)
            apply()
        }
        _settings.value = newSettings
    }
}
