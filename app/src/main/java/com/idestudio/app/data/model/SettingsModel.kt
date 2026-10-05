package com.idestudio.app.data.model

data class SettingsModel(
    val themeMode: String = "light", // light, dark, system
    val fontSize: Int = 14,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val autoIndent: Boolean = true,
    val bracketMatching: Boolean = true,
    val syntaxHighlighting: Boolean = true,
    val tabSize: Int = 4,
    val autoSave: Boolean = true,
    val hapticFeedback: Boolean = true,
    val cleanBuildBeforeRun: Boolean = false,
    val autoInstallApk: Boolean = true,
    val aiProvider: String = "gemini", // gemini, openai, custom
    val aiApiKey: String = "",
    val aiModel: String = "gemini-1.5-pro",
    val customAiEndpoint: String = ""
)
