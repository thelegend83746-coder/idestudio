package com.idestudio.app.core.storage;

import android.content.Context;
import android.content.SharedPreferences;

public class SecurePreferences {
    private static final String PREF_NAME = "ide_studio_preferences";

    // Build AI Settings
    public static final String KEY_OLLAMA_API_KEY = "key_ollama_api_key";
    public static final String KEY_OLLAMA_MODEL = "key_ollama_model";
    public static final String KEY_OLLAMA_SYSTEM_PROMPT = "key_ollama_system_prompt";

    // Application Settings
    public static final String KEY_DARK_MODE = "key_dark_mode";
    public static final String KEY_CONFIRM_DELETE = "key_confirm_delete";

    // Editor Settings
    public static final String KEY_FONT_SIZE = "key_font_size";
    public static final String KEY_TAB_SIZE = "key_tab_size";
    public static final String KEY_WORD_WRAP = "key_word_wrap";
    public static final String KEY_SHOW_LINE_NUMBERS = "key_show_line_numbers";
    public static final String KEY_HIGHLIGHT_CURRENT_LINE = "key_highlight_current_line";
    public static final String KEY_AUTO_COMPLETE = "key_auto_complete";
    public static final String KEY_DARK_EDITOR_THEME = "key_dark_editor_theme";

    // Build Settings
    public static final String KEY_DEFAULT_MIN_SDK = "key_default_min_sdk";
    public static final String KEY_DEFAULT_TARGET_SDK = "key_default_target_sdk";
    public static final String KEY_AUTO_SAVE_BEFORE_BUILD = "key_auto_save_before_build";
    public static final String KEY_SHOW_BUILD_LOGS = "key_show_build_logs";

    private final SharedPreferences preferences;

    public SecurePreferences(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    public void setBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    public void setInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
