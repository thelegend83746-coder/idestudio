package com.ide.studio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Settings hub activity routing to application, editor, build/run, and Ollama settings.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView btnBack = findViewById(R.id.btn_back);
        LinearLayout itemAppSettings = findViewById(R.id.item_app_settings);
        LinearLayout itemEditorSettings = findViewById(R.id.item_editor_settings);
        LinearLayout itemBuildSettings = findViewById(R.id.item_build_settings);
        LinearLayout itemOllamaSettings = findViewById(R.id.item_ollama_settings);
        LinearLayout itemAboutPage = findViewById(R.id.item_about_page);

        btnBack.setOnClickListener(v -> finish());

        itemAppSettings.setOnClickListener(v -> startActivity(new Intent(this, ApplicationSettingsActivity.class)));
        itemEditorSettings.setOnClickListener(v -> startActivity(new Intent(this, EditorSettingsActivity.class)));
        itemBuildSettings.setOnClickListener(v -> startActivity(new Intent(this, BuildRunSettingsActivity.class)));
        itemOllamaSettings.setOnClickListener(v -> startActivity(new Intent(this, OllamaSettingsActivity.class)));
        itemAboutPage.setOnClickListener(v -> startActivity(new Intent(this, AboutPageActivity.class)));
    }
}
