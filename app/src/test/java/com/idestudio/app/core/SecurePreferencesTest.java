package com.idestudio.app.core;

import com.idestudio.app.core.storage.SecurePreferences;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SecurePreferencesTest {

    @Test
    public void testPreferenceKeysConstants() {
        assertEquals("key_ollama_api_key", SecurePreferences.KEY_OLLAMA_API_KEY);
        assertEquals("key_ollama_model", SecurePreferences.KEY_OLLAMA_MODEL);
        assertEquals("key_dark_mode", SecurePreferences.KEY_DARK_MODE);
        assertEquals("key_font_size", SecurePreferences.KEY_FONT_SIZE);
        assertEquals("key_tab_size", SecurePreferences.KEY_TAB_SIZE);
        assertEquals("key_default_min_sdk", SecurePreferences.KEY_DEFAULT_MIN_SDK);
    }
}
