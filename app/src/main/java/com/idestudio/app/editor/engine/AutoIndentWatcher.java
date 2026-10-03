package com.idestudio.app.editor.engine;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class AutoIndentWatcher implements TextWatcher {

    private final EditText editText;
    private boolean isModifying = false;

    public AutoIndentWatcher(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (isModifying) return;

        // Detect if newline was typed (count == 1, char is '\n')
        if (count == 1 && before == 0 && s.charAt(start) == '\n') {
            String text = s.toString();
            int newlineIndex = start;
            int prevLineStart = text.lastIndexOf('\n', newlineIndex - 1);
            if (prevLineStart < 0) {
                prevLineStart = 0;
            } else {
                prevLineStart += 1;
            }

            String prevLine = text.substring(prevLineStart, newlineIndex);

            // Count leading spaces
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < prevLine.length(); i++) {
                char c = prevLine.charAt(i);
                if (c == ' ' || c == '\t') {
                    indent.append(c);
                } else {
                    break;
                }
            }

            // Block-aware indent: if previous line ended with '{', add 4 spaces
            String trimmedPrev = prevLine.trim();
            if (trimmedPrev.endsWith("{")) {
                indent.append("    ");
            }

            if (indent.length() > 0) {
                isModifying = true;
                Editable editable = editText.getText();
                editable.insert(start + 1, indent.toString());
                editText.setSelection(start + 1 + indent.length());
                isModifying = false;
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {}
}
