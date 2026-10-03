package com.idestudio.app.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.idestudio.app.R;
import com.idestudio.app.ai.client.OllamaCloudClient;
import com.idestudio.app.domain.project.LocalProjectStore;

/**
 * Settings screen for configuring:
 * - Storage & Workspace permissions
 * - Editor preferences (font size, line numbers, symbol bar, auto brackets, word wrap)
 * - AI Assistant providers & real API credentials
 */
public class SettingsActivity extends AppCompatActivity {

    public static final String PREF_EDITOR = "ide_editor_prefs";
    public static final String KEY_LINE_NUMBERS = "pref_line_numbers";
    public static final String KEY_SYMBOL_BAR = "pref_symbol_bar";
    public static final String KEY_AUTO_BRACKETS = "pref_auto_brackets";
    public static final String KEY_WORD_WRAP = "pref_word_wrap";

    private static final String PREF_ZOOM = "ide_editor_zoom_prefs";
    private static final String KEY_FONT_SIZE = "editor_font_size_sp";

    private TextView tvWorkspace;
    private TextView tvPermissionStatus;
    private MaterialButton btnGrantStoragePerm;

    private TextView tvFontSize;
    private MaterialButton btnResetZoom;
    private SwitchMaterial switchLineNumbers;
    private SwitchMaterial switchSymbolBar;
    private SwitchMaterial switchAutoBrackets;
    private SwitchMaterial switchWordWrap;

    private EditText etBaseUrl;
    private EditText etApiKey;
    private EditText etModel;
    private MaterialButton btnTestConn;
    private TextView tvTestResult;
    private MaterialButton btnSave;

    private OllamaCloudClient aiClient;
    private SharedPreferences editorPrefs;
    private SharedPreferences zoomPrefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        aiClient = OllamaCloudClient.getInstance(this);
        editorPrefs = getSharedPreferences(PREF_EDITOR, Context.MODE_PRIVATE);
        zoomPrefs = getSharedPreferences(PREF_ZOOM, Context.MODE_PRIVATE);

        initViews();
        loadValues();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStoragePermissionUI();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvWorkspace = findViewById(R.id.tv_settings_workspace);
        tvPermissionStatus = findViewById(R.id.tv_permission_status);
        btnGrantStoragePerm = findViewById(R.id.btn_grant_storage_perm);

        tvFontSize = findViewById(R.id.tv_settings_font_size);
        btnResetZoom = findViewById(R.id.btn_reset_zoom);
        switchLineNumbers = findViewById(R.id.switch_line_numbers);
        switchSymbolBar = findViewById(R.id.switch_symbol_bar);
        switchAutoBrackets = findViewById(R.id.switch_auto_brackets);
        switchWordWrap = findViewById(R.id.switch_word_wrap);

        etBaseUrl = findViewById(R.id.et_settings_base_url);
        etApiKey = findViewById(R.id.et_settings_api_key);
        etModel = findViewById(R.id.et_settings_model);
        btnTestConn = findViewById(R.id.btn_settings_test_conn);
        tvTestResult = findViewById(R.id.tv_settings_test_result);
        btnSave = findViewById(R.id.btn_settings_save);

        if (tvWorkspace != null) {
            tvWorkspace.setText(LocalProjectStore.getBaseDirPath());
        }
    }

    private void loadValues() {
        // Storage permission status
        updateStoragePermissionUI();

        // Editor preferences
        float fontSize = zoomPrefs.getFloat(KEY_FONT_SIZE, 13.0f);
        tvFontSize.setText(String.format("%.1fsp (Use two fingers to zoom in editor)", fontSize));

        switchLineNumbers.setChecked(editorPrefs.getBoolean(KEY_LINE_NUMBERS, true));
        switchSymbolBar.setChecked(editorPrefs.getBoolean(KEY_SYMBOL_BAR, true));
        switchAutoBrackets.setChecked(editorPrefs.getBoolean(KEY_AUTO_BRACKETS, true));
        switchWordWrap.setChecked(editorPrefs.getBoolean(KEY_WORD_WRAP, false));

        // AI values
        etBaseUrl.setText(aiClient.getBaseUrl());
        etApiKey.setText(aiClient.getApiKey());
        etModel.setText(aiClient.getModel());
    }

    private void updateStoragePermissionUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean hasManage = Environment.isExternalStorageManager();
            if (hasManage) {
                tvPermissionStatus.setText("All Files Access: Granted ✓");
                tvPermissionStatus.setTextColor(0xFF10B981);
                btnGrantStoragePerm.setVisibility(View.GONE);
            } else {
                tvPermissionStatus.setText("All Files Access: Not Granted");
                tvPermissionStatus.setTextColor(0xFFEF4444);
                btnGrantStoragePerm.setVisibility(View.VISIBLE);
            }
        } else {
            tvPermissionStatus.setText("Storage Access: Normal");
            tvPermissionStatus.setTextColor(0xFF10B981);
            btnGrantStoragePerm.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        // Permission button
        btnGrantStoragePerm.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        });

        // Reset Zoom
        btnResetZoom.setOnClickListener(v -> {
            zoomPrefs.edit().putFloat(KEY_FONT_SIZE, 13.0f).apply();
            tvFontSize.setText("13.0sp (Reset to 100%)");
            Toast.makeText(this, "Editor zoom reset to default (13sp)", Toast.LENGTH_SHORT).show();
        });

        // Quick AI Presets
        findViewById(R.id.btn_preset_local).setOnClickListener(v -> {
            etBaseUrl.setText("http://localhost:11434");
            etModel.setText("llama3");
            Toast.makeText(this, "Preset applied: Local Ollama", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_preset_groq).setOnClickListener(v -> {
            etBaseUrl.setText("https://api.groq.com/openai/v1");
            etModel.setText("llama-3.1-70b-versatile");
            Toast.makeText(this, "Preset applied: Groq Cloud", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_preset_openrouter).setOnClickListener(v -> {
            etBaseUrl.setText("https://openrouter.ai/api/v1");
            etModel.setText("meta-llama/llama-3-8b-instruct:free");
            Toast.makeText(this, "Preset applied: OpenRouter", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_preset_gemini).setOnClickListener(v -> {
            etBaseUrl.setText("https://generativelanguage.googleapis.com/v1beta/openai");
            etModel.setText("gemini-1.5-flash");
            Toast.makeText(this, "Preset applied: Google Gemini", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_preset_openai).setOnClickListener(v -> {
            etBaseUrl.setText("https://api.openai.com/v1");
            etModel.setText("gpt-4o-mini");
            Toast.makeText(this, "Preset applied: OpenAI", Toast.LENGTH_SHORT).show();
        });

        // Test Connection
        btnTestConn.setOnClickListener(v -> testApiConnection());

        // Save
        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void testApiConnection() {
        String url = etBaseUrl.getText().toString().trim();
        String key = etApiKey.getText().toString().trim();
        String model = etModel.getText().toString().trim();

        tvTestResult.setVisibility(View.VISIBLE);
        tvTestResult.setTextColor(0xFF757575);
        tvTestResult.setText("Testing API connection, please wait...");
        btnTestConn.setEnabled(false);

        aiClient.testConnection(url, key, model, (success, message) -> {
            btnTestConn.setEnabled(true);
            tvTestResult.setVisibility(View.VISIBLE);
            if (success) {
                tvTestResult.setTextColor(0xFF10B981); // Green
                tvTestResult.setText("✓ " + message);
            } else {
                tvTestResult.setTextColor(0xFFEF4444); // Red
                tvTestResult.setText("✗ " + message);
            }
        });
    }

    private void saveSettings() {
        // Save Editor Preferences
        editorPrefs.edit()
                .putBoolean(KEY_LINE_NUMBERS, switchLineNumbers.isChecked())
                .putBoolean(KEY_SYMBOL_BAR, switchSymbolBar.isChecked())
                .putBoolean(KEY_AUTO_BRACKETS, switchAutoBrackets.isChecked())
                .putBoolean(KEY_WORD_WRAP, switchWordWrap.isChecked())
                .apply();

        // Save AI Settings
        String url = etBaseUrl.getText().toString().trim();
        String key = etApiKey.getText().toString().trim();
        String model = etModel.getText().toString().trim();

        aiClient.setBaseUrl(url);
        aiClient.setApiKey(key);
        aiClient.setModel(!model.isEmpty() ? model : "llama3");

        Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
