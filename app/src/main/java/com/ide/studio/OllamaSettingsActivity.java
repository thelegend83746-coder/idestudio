package com.ide.studio;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ide.studio.core.PreferencesManager;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Ollama Settings: Configure local/remote Ollama AI host, port, model name, and verify connectivity.
 */
public class OllamaSettingsActivity extends AppCompatActivity {

    private PreferencesManager mPrefs;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ollama_settings);

        mPrefs = new PreferencesManager(this);

        ImageView btnBack = findViewById(R.id.btn_back);
        EditText etHost = findViewById(R.id.et_ollama_host);
        EditText etPort = findViewById(R.id.et_ollama_port);
        EditText etModel = findViewById(R.id.et_ollama_model);
        Button btnTest = findViewById(R.id.btn_test_connection);
        TextView tvResult = findViewById(R.id.tv_connection_result);
        Button btnSave = findViewById(R.id.btn_save_ollama);

        btnBack.setOnClickListener(v -> finish());

        etHost.setText(mPrefs.getOllamaHost());
        etPort.setText(String.valueOf(mPrefs.getOllamaPort()));
        etModel.setText(mPrefs.getActiveAiModel());

        btnTest.setOnClickListener(v -> {
            tvResult.setVisibility(View.VISIBLE);
            tvResult.setTextColor(Color.parseColor("#9CA3AF"));
            tvResult.setText("Pinging Ollama server...");

            String host = etHost.getText().toString().trim();
            String portStr = etPort.getText().toString().trim();

            new Thread(() -> {
                try {
                    String testUrl = host + ":" + portStr + "/api/tags";
                    URL url = new URL(testUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);
                    conn.setRequestMethod("GET");
                    int responseCode = conn.getResponseCode();

                    mMainHandler.post(() -> {
                        if (responseCode == 200) {
                            tvResult.setTextColor(Color.parseColor("#10B981"));
                            tvResult.setText("Success! Connected to Ollama (HTTP 200 OK)");
                        } else {
                            tvResult.setTextColor(Color.parseColor("#F59E0B"));
                            tvResult.setText("Server reachable with status: " + responseCode);
                        }
                    });
                } catch (Exception e) {
                    mMainHandler.post(() -> {
                        tvResult.setTextColor(Color.parseColor("#EF4444"));
                        tvResult.setText("Connection failed: " + e.getMessage());
                    });
                }
            }).start();
        });

        btnSave.setOnClickListener(v -> {
            String host = etHost.getText().toString().trim();
            String portStr = etPort.getText().toString().trim();
            String model = etModel.getText().toString().trim();

            if (host.isEmpty()) host = "http://127.0.0.1";
            int port = 11434;
            try {
                port = Integer.parseInt(portStr);
            } catch (Exception ignored) {}

            mPrefs.setOllamaHost(host);
            mPrefs.setOllamaPort(port);
            if (!model.isEmpty()) {
                mPrefs.setActiveAiModel(model);
            }

            Toast.makeText(this, "Ollama configuration saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
