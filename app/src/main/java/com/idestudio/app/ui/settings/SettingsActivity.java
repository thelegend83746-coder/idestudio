package com.idestudio.app.ui.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.idestudio.app.R;
import com.idestudio.app.ai.client.OllamaCloudClient;
import com.idestudio.app.domain.project.LocalProjectStore;

/**
 * Settings screen for configuring workspace location, Ollama / Cloud AI API parameters,
 * and testing real API connections.
 */
public class SettingsActivity extends AppCompatActivity {

    private TextView tvWorkspace;
    private EditText etBaseUrl;
    private EditText etApiKey;
    private EditText etModel;
    private Button btnTestConn;
    private TextView tvTestResult;
    private Button btnSave;

    private OllamaCloudClient aiClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        aiClient = OllamaCloudClient.getInstance(this);

        initViews();
        loadValues();
        setupListeners();
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
        etBaseUrl = findViewById(R.id.et_settings_base_url);
        etApiKey = findViewById(R.id.et_settings_api_key);
        etModel = findViewById(R.id.et_settings_model);
        btnTestConn = findViewById(R.id.btn_settings_test_conn);
        tvTestResult = findViewById(R.id.tv_settings_test_result);
        btnSave = findViewById(R.id.btn_settings_save);

        tvWorkspace.setText(LocalProjectStore.getBaseDirPath());
    }

    private void loadValues() {
        etBaseUrl.setText(aiClient.getBaseUrl());
        etApiKey.setText(aiClient.getApiKey());
        etModel.setText(aiClient.getModel());
    }

    private void setupListeners() {
        // Quick Presets
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
                tvTestResult.setTextColor(0xFF2E7D32); // Green
                tvTestResult.setText("✓ " + message);
            } else {
                tvTestResult.setTextColor(0xFFD32F2F); // Red
                tvTestResult.setText("✗ " + message);
            }
        });
    }

    private void saveSettings() {
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
