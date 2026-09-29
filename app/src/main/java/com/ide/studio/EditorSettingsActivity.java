package com.ide.studio;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.ide.studio.core.PreferencesManager;

/**
 * Editor Settings: Configure kinetic scrolling, overscroll physics, font size, tabs, line numbers.
 */
public class EditorSettingsActivity extends AppCompatActivity {

    private PreferencesManager mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor_settings);

        mPrefs = new PreferencesManager(this);

        ImageView btnBack = findViewById(R.id.btn_back);
        CheckBox cbOverscroll = findViewById(R.id.cb_overscroll);
        EditText etFontSize = findViewById(R.id.et_font_size);
        EditText etTabSize = findViewById(R.id.et_tab_size);
        CheckBox cbLineNumbers = findViewById(R.id.cb_line_numbers);
        CheckBox cbWordWrap = findViewById(R.id.cb_word_wrap);
        Button btnSave = findViewById(R.id.btn_save_editor_settings);

        btnBack.setOnClickListener(v -> finish());

        cbOverscroll.setChecked(mPrefs.isKineticOverscrollEnabled());
        etFontSize.setText(String.valueOf(mPrefs.getEditorFontSize()));
        etTabSize.setText(String.valueOf(mPrefs.getEditorTabSize()));
        cbLineNumbers.setChecked(mPrefs.showLineNumbers());
        cbWordWrap.setChecked(mPrefs.isWordWrap());

        btnSave.setOnClickListener(v -> {
            try {
                int font = Integer.parseInt(etFontSize.getText().toString().trim());
                int tab = Integer.parseInt(etTabSize.getText().toString().trim());
                mPrefs.setEditorFontSize(font);
                mPrefs.setEditorTabSize(tab);
                mPrefs.setKineticOverscrollEnabled(cbOverscroll.isChecked());
                mPrefs.setShowLineNumbers(cbLineNumbers.isChecked());
                mPrefs.setWordWrap(cbWordWrap.isChecked());

                Toast.makeText(this, "Editor settings saved!", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid number inputs", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
