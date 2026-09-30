package com.ide.studio;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.ide.studio.core.PreferencesManager;

/**
 * Editor Settings:
 * - Font Size & Tab Size
 * - Show Line Numbers
 * - Highlight Current Line
 * - Code Completion
 * - Word Wrap
 * - Kinetic Overscroll
 * - Dark Editor Theme
 */
public class EditorSettingsActivity extends AppCompatActivity {

    private PreferencesManager mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor_settings);

        mPrefs = new PreferencesManager(this);

        ImageView btnBack = findViewById(R.id.btn_back);
        EditText etFontSize = findViewById(R.id.et_font_size);
        EditText etTabSize = findViewById(R.id.et_tab_size);
        MaterialSwitch switchLineNumbers = findViewById(R.id.switch_line_numbers);
        MaterialSwitch switchHighlightLine = findViewById(R.id.switch_highlight_line);
        MaterialSwitch switchAutoComplete = findViewById(R.id.switch_auto_complete);
        MaterialSwitch switchWordWrap = findViewById(R.id.switch_word_wrap);
        MaterialSwitch switchOverscroll = findViewById(R.id.switch_overscroll);
        MaterialSwitch switchDarkEditor = findViewById(R.id.switch_dark_editor);
        Button btnSave = findViewById(R.id.btn_save_editor_settings);

        btnBack.setOnClickListener(v -> finish());

        // Load values
        etFontSize.setText(String.valueOf(mPrefs.getEditorFontSize()));
        etTabSize.setText(String.valueOf(mPrefs.getEditorTabSize()));
        switchLineNumbers.setChecked(mPrefs.showLineNumbers());
        switchHighlightLine.setChecked(mPrefs.isHighlightCurrentLine());
        switchAutoComplete.setChecked(mPrefs.isAutoCompleteEnabled());
        switchWordWrap.setChecked(mPrefs.isWordWrap());
        switchOverscroll.setChecked(mPrefs.isKineticOverscrollEnabled());
        switchDarkEditor.setChecked(mPrefs.isDarkEditorTheme());

        btnSave.setOnClickListener(v -> {
            try {
                int font = Integer.parseInt(etFontSize.getText().toString().trim());
                int tab = Integer.parseInt(etTabSize.getText().toString().trim());
                if (font < 8 || font > 36) {
                    Toast.makeText(this, "Font size must be between 8 and 36", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (tab < 1 || tab > 8) {
                    Toast.makeText(this, "Tab size must be between 1 and 8", Toast.LENGTH_SHORT).show();
                    return;
                }
                mPrefs.setEditorFontSize(font);
                mPrefs.setEditorTabSize(tab);
                mPrefs.setShowLineNumbers(switchLineNumbers.isChecked());
                mPrefs.setHighlightCurrentLine(switchHighlightLine.isChecked());
                mPrefs.setAutoCompleteEnabled(switchAutoComplete.isChecked());
                mPrefs.setWordWrap(switchWordWrap.isChecked());
                mPrefs.setKineticOverscrollEnabled(switchOverscroll.isChecked());
                mPrefs.setDarkEditorTheme(switchDarkEditor.isChecked());

                Toast.makeText(this, "Editor settings saved", Toast.LENGTH_SHORT).show();
                finish();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numeric values", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
