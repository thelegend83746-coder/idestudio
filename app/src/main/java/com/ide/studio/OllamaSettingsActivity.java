package com.ide.studio;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.ide.studio.core.PreferencesManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Ollama AI Build Settings:
 * Configures local/remote endpoint, optional API Key management,
 * test connection with live status verification, model discovery from /api/tags,
 * and system persona instructions.
 */
public class OllamaSettingsActivity extends AppCompatActivity {

    private PreferencesManager mPrefs;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private EditText mEtEndpoint;
    private EditText mEtApiKey;
    private Button mBtnDeleteKey;
    private EditText mEtActiveModel;
    private Button mBtnTestConnection;
    private TextView mTvConnectionStatus;
    private Button mBtnRefreshModels;
    private LinearLayout mLayoutModelsContainer;
    private TextView mTvModelsEmpty;
    private EditText mEtSystemInstructions;
    private Button mBtnResetPrompt;
    private Button mBtnSaveAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ollama_settings);

        mPrefs = new PreferencesManager(this);

        initViews();
        loadStoredSettings();
        setupActions();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        mEtEndpoint = findViewById(R.id.et_ollama_endpoint);
        mEtApiKey = findViewById(R.id.et_ollama_api_key);
        mBtnDeleteKey = findViewById(R.id.btn_delete_key);
        mEtActiveModel = findViewById(R.id.et_active_model);
        mBtnTestConnection = findViewById(R.id.btn_test_connection);
        mTvConnectionStatus = findViewById(R.id.tv_connection_status);
        mBtnRefreshModels = findViewById(R.id.btn_refresh_models);
        mLayoutModelsContainer = findViewById(R.id.layout_models_container);
        mTvModelsEmpty = findViewById(R.id.tv_models_empty);
        mEtSystemInstructions = findViewById(R.id.et_system_instructions);
        mBtnResetPrompt = findViewById(R.id.btn_reset_system_prompt);
        mBtnSaveAll = findViewById(R.id.btn_save_all_ai_settings);
    }

    private void loadStoredSettings() {
        String host = mPrefs.getOllamaHost();
        int port = mPrefs.getOllamaPort();
        if (host.contains(":") && !host.endsWith("://") && host.lastIndexOf(':') > 6) {
            mEtEndpoint.setText(host);
        } else {
            mEtEndpoint.setText(host + ":" + port);
        }

        String apiKey = mPrefs.getOllamaApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            mEtApiKey.setText(apiKey);
            mBtnDeleteKey.setVisibility(View.VISIBLE);
        } else {
            mBtnDeleteKey.setVisibility(View.GONE);
        }

        mEtActiveModel.setText(mPrefs.getActiveAiModel());
        mEtSystemInstructions.setText(mPrefs.getSystemPrompt());
    }

    private void setupActions() {
        mBtnDeleteKey.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete API Key?")
                    .setMessage("Are you sure you want to remove the saved API key?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        mPrefs.setOllamaApiKey("");
                        mEtApiKey.setText("");
                        mBtnDeleteKey.setVisibility(View.GONE);
                        Toast.makeText(this, "API Key deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        mBtnTestConnection.setOnClickListener(v -> testConnectionAndDiscoverModels());
        mBtnRefreshModels.setOnClickListener(v -> testConnectionAndDiscoverModels());

        mBtnResetPrompt.setOnClickListener(v -> {
            mEtSystemInstructions.setText(PreferencesManager.DEFAULT_SYSTEM_PROMPT);
            Toast.makeText(this, "Reset to default system prompt", Toast.LENGTH_SHORT).show();
        });

        mBtnSaveAll.setOnClickListener(v -> saveAllSettings());
    }

    private void testConnectionAndDiscoverModels() {
        String endpoint = mEtEndpoint.getText().toString().trim();
        if (endpoint.isEmpty()) {
            mTvConnectionStatus.setTextColor(Color.parseColor("#EF4444"));
            mTvConnectionStatus.setText("Invalid Endpoint");
            return;
        }

        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            endpoint = "http://" + endpoint;
        }

        final String finalBaseUrl = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        final String apiKey = mEtApiKey.getText().toString().trim();

        mTvConnectionStatus.setTextColor(Color.parseColor("#6B7280"));
        mTvConnectionStatus.setText("Testing...");

        new Thread(() -> {
            try {
                String tagsUrl = finalBaseUrl + "/api/tags";
                URL url = new URL(tagsUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (!apiKey.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                }
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray models = json.optJSONArray("models");

                    mMainHandler.post(() -> {
                        mTvConnectionStatus.setTextColor(Color.parseColor("#10B981"));
                        mTvConnectionStatus.setText("Connected");
                        populateDiscoveredModels(models);
                    });
                } else if (code == 401 || code == 403) {
                    mMainHandler.post(() -> {
                        mTvConnectionStatus.setTextColor(Color.parseColor("#EF4444"));
                        mTvConnectionStatus.setText("Authentication Failed");
                    });
                } else {
                    mMainHandler.post(() -> {
                        mTvConnectionStatus.setTextColor(Color.parseColor("#EF4444"));
                        mTvConnectionStatus.setText("Unsupported API (HTTP " + code + ")");
                    });
                }
            } catch (java.net.MalformedURLException e) {
                mMainHandler.post(() -> {
                    mTvConnectionStatus.setTextColor(Color.parseColor("#EF4444"));
                    mTvConnectionStatus.setText("Invalid Endpoint");
                });
            } catch (Exception e) {
                mMainHandler.post(() -> {
                    mTvConnectionStatus.setTextColor(Color.parseColor("#EF4444"));
                    mTvConnectionStatus.setText("Connection Failed");
                });
            }
        }).start();
    }

    private void populateDiscoveredModels(JSONArray models) {
        mLayoutModelsContainer.removeAllViews();
        if (models == null || models.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No models available on this Ollama server.");
            empty.setTextColor(Color.parseColor("#6B7280"));
            empty.setTextSize(13);
            mLayoutModelsContainer.addView(empty);
            return;
        }

        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < models.length(); i++) {
            JSONObject m = models.optJSONObject(i);
            if (m == null) continue;
            String name = m.optString("name", "Unknown");
            long sizeBytes = m.optLong("size", 0);
            String sizeStr = formatBytes(sizeBytes);

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding((int) (12 * density), (int) (10 * density), (int) (12 * density), (int) (10 * density));
            item.setBackgroundResource(R.drawable.bg_card_rounded);
            item.setClickable(true);
            item.setFocusable(true);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, (int) (4 * density), 0, (int) (4 * density));
            item.setLayoutParams(lp);

            TextView tvName = new TextView(this);
            tvName.setText(name);
            tvName.setTextColor(Color.parseColor("#111827"));
            tvName.setTextSize(14);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            item.addView(tvName);

            TextView tvMeta = new TextView(this);
            tvMeta.setText("Size: " + sizeStr + " • Available through connected Ollama server");
            tvMeta.setTextColor(Color.parseColor("#6B7280"));
            tvMeta.setTextSize(11);
            tvMeta.setPadding(0, (int) (2 * density), 0, 0);
            item.addView(tvMeta);

            item.setOnClickListener(v -> {
                mEtActiveModel.setText(name);
                Toast.makeText(this, "Selected model: " + name, Toast.LENGTH_SHORT).show();
            });

            mLayoutModelsContainer.addView(item);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "Unknown";
        if (bytes >= 1024 * 1024 * 1024) {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        } else if (bytes >= 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return (bytes / 1024) + " KB";
    }

    private void saveAllSettings() {
        String endpoint = mEtEndpoint.getText().toString().trim();
        String apiKey = mEtApiKey.getText().toString().trim();
        String model = mEtActiveModel.getText().toString().trim();
        String prompt = mEtSystemInstructions.getText().toString().trim();

        if (endpoint.isEmpty()) {
            endpoint = "http://127.0.0.1:11434";
        }

        mPrefs.setOllamaHost(endpoint);
        mPrefs.setOllamaApiKey(apiKey);
        if (!model.isEmpty()) {
            mPrefs.setActiveAiModel(model);
        }
        if (!prompt.isEmpty()) {
            mPrefs.setSystemPrompt(prompt);
        }

        Toast.makeText(this, "AI Build settings saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
