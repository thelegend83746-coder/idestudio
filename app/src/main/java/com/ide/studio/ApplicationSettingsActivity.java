package com.ide.studio;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.ide.studio.core.PreferencesManager;
import com.ide.studio.core.ProjectStorage;

/**
 * Application Settings: Theme selection, project directory, auto-save preference.
 */
public class ApplicationSettingsActivity extends AppCompatActivity {

    private PreferencesManager mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_settings);

        mPrefs = new PreferencesManager(this);

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvProjectDir = findViewById(R.id.tv_project_dir);
        Spinner spinnerTheme = findViewById(R.id.spinner_theme);
        CheckBox cbAutoSave = findViewById(R.id.cb_auto_save);

        btnBack.setOnClickListener(v -> finish());

        tvProjectDir.setText(ProjectStorage.getProjectsRoot().getAbsolutePath());

        String[] themes = new String[]{"Dark (Darcula Default)", "Light"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, themes);
        spinnerTheme.setAdapter(adapter);
        spinnerTheme.setSelection(mPrefs.isDarkMode() ? 0 : 1);

        cbAutoSave.setChecked(mPrefs.isAutoSaveBeforeBuild());
        cbAutoSave.setOnCheckedChangeListener((buttonView, isChecked) -> mPrefs.setAutoSaveBeforeBuild(isChecked));
    }
}
