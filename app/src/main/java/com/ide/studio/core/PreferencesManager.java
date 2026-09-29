package com.ide.studio.core;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "idestudio_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_CONFIRM_DELETE = "confirm_delete";
    private static final String KEY_DEFAULT_MIN_SDK = "default_min_sdk";
    private static final String KEY_DEFAULT_TARGET_SDK = "default_target_sdk";
    private static final String KEY_AUTO_SAVE = "auto_save";
    private static final String KEY_SHOW_LOGS = "show_logs";
    private static final String KEY_OLLAMA_API_KEY = "ollama_api_key";
    private static final String KEY_ACTIVE_AI_MODEL = "active_ai_model";
    private static final String KEY_SYSTEM_PROMPT = "system_prompt";

    public static final String DEFAULT_SYSTEM_PROMPT =
            "You are Build AI, an expert Android app and game developer and coding assistant inside the idestudio app.\n" +
            "You can create entire Android applications and 2D/3D games (using Canvas, SurfaceView, game loops, thread-safe updates, touch interaction listeners, sprite movement, collision detection, game physics, audio effects, score tracking).\n" +
            "You can create files, folders, write Java and XML code, fix compilation errors, and answer questions.\n\n" +
            "FORMATTING RULES:\n" +
            "1. Always start your response with a high-level plan:\n" +
            "ANALYSIS / PLAN:\n" +
            "<your detailed architecture/step-by-step game or app plan here>\n\n" +
            "2. For each file you need to create, modify, rename, or delete, output an action block:\n" +
            "[CREATE FILE: relative/path/to/File.java]\n" +
            "```java\n" +
            "// complete file code\n" +
            "```\n" +
            "[/CREATE FILE]\n\n" +
            "[WRITE FILE: relative/path/to/ExistingFile.java]\n" +
            "```java\n" +
            "// updated file code\n" +
            "```\n" +
            "[/WRITE FILE]\n\n" +
            "[RENAME FILE: old/path/to/File.java -> new/path/to/File.java]\n\n" +
            "[DELETE FILE: relative/path/to/File.java]\n\n" +
            "Never propose Kotlin code for generated user projects; always use pure Java and standard Android XML layouts.";

    private SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isDarkMode() { return prefs.getBoolean(KEY_DARK_MODE, false); }
    public void setDarkMode(boolean value) { prefs.edit().putBoolean(KEY_DARK_MODE, value).apply(); }

    public boolean isConfirmBeforeDelete() { return prefs.getBoolean(KEY_CONFIRM_DELETE, true); }
    public void setConfirmBeforeDelete(boolean value) { prefs.edit().putBoolean(KEY_CONFIRM_DELETE, value).apply(); }

    public int getDefaultMinSdk() { return prefs.getInt(KEY_DEFAULT_MIN_SDK, 21); }
    public void setDefaultMinSdk(int value) { prefs.edit().putInt(KEY_DEFAULT_MIN_SDK, value).apply(); }

    public int getDefaultTargetSdk() { return prefs.getInt(KEY_DEFAULT_TARGET_SDK, 30); }
    public void setDefaultTargetSdk(int value) { prefs.edit().putInt(KEY_DEFAULT_TARGET_SDK, value).apply(); }

    public boolean isAutoSaveBeforeBuild() { return prefs.getBoolean(KEY_AUTO_SAVE, true); }
    public void setAutoSaveBeforeBuild(boolean value) { prefs.edit().putBoolean(KEY_AUTO_SAVE, value).apply(); }

    public boolean isShowBuildLogs() { return prefs.getBoolean(KEY_SHOW_LOGS, true); }
    public void setShowBuildLogs(boolean value) { prefs.edit().putBoolean(KEY_SHOW_LOGS, value).apply(); }

    public String getOllamaApiKey() { return prefs.getString(KEY_OLLAMA_API_KEY, ""); }
    public void setOllamaApiKey(String value) { prefs.edit().putString(KEY_OLLAMA_API_KEY, value).apply(); }

    public String getActiveAiModel() { return prefs.getString(KEY_ACTIVE_AI_MODEL, "glm-4.6"); }
    public void setActiveAiModel(String value) { prefs.edit().putString(KEY_ACTIVE_AI_MODEL, value).apply(); }

    public String getOllamaHost() { return prefs.getString("ollama_host", "http://127.0.0.1"); }
    public void setOllamaHost(String value) { prefs.edit().putString("ollama_host", value).apply(); }

    public int getOllamaPort() { return prefs.getInt("ollama_port", 11434); }
    public void setOllamaPort(int value) { prefs.edit().putInt("ollama_port", value).apply(); }

    public int getEditorFontSize() { return prefs.getInt("editor_font_size", 14); }
    public void setEditorFontSize(int value) { prefs.edit().putInt("editor_font_size", value).apply(); }

    public int getEditorTabSize() { return prefs.getInt("editor_tab_size", 4); }
    public void setEditorTabSize(int value) { prefs.edit().putInt("editor_tab_size", value).apply(); }

    public boolean showLineNumbers() { return prefs.getBoolean("show_line_numbers", true); }
    public void setShowLineNumbers(boolean value) { prefs.edit().putBoolean("show_line_numbers", value).apply(); }

    public boolean isKineticOverscrollEnabled() { return prefs.getBoolean("kinetic_overscroll", true); }
    public void setKineticOverscrollEnabled(boolean value) { prefs.edit().putBoolean("kinetic_overscroll", value).apply(); }

    public boolean isWordWrap() { return prefs.getBoolean("word_wrap", false); }
    public void setWordWrap(boolean value) { prefs.edit().putBoolean("word_wrap", value).apply(); }
}
