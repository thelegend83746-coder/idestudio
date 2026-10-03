package com.idestudio.app.ui.editor;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.idestudio.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller for Pro Editor features:
 * - Programming Symbol Bar for Mobile
 * - Auto-indentation & Auto-bracket pairing
 * - Synchronized Line Numbers Gutter
 * - Real-time Cursor & Line count status
 * - Find & Replace in file
 */
public class EditorProFeaturesController {

    private final Context context;
    private final EditText editor;
    private final TextView lineNumbers;
    private final LinearLayout symbolsContainer;
    private final TextView statusCursor;
    private final TextView statusLines;

    // Find & Replace Views
    private final View searchReplaceLayout;
    private final EditText etSearchQuery;
    private final EditText etReplaceQuery;
    private final TextView tvSearchCount;

    private final List<Integer> searchMatches = new ArrayList<>();
    private int currentMatchIndex = -1;

    private boolean isAutoPairing = false;
    private int lastLineCount = 0;

    public EditorProFeaturesController(
            Context context,
            EditText editor,
            TextView lineNumbers,
            LinearLayout symbolsContainer,
            TextView statusCursor,
            TextView statusLines,
            View searchReplaceLayout,
            EditText etSearchQuery,
            EditText etReplaceQuery,
            TextView tvSearchCount
    ) {
        this.context = context;
        this.editor = editor;
        this.lineNumbers = lineNumbers;
        this.symbolsContainer = symbolsContainer;
        this.statusCursor = statusCursor;
        this.statusLines = statusLines;
        this.searchReplaceLayout = searchReplaceLayout;
        this.etSearchQuery = etSearchQuery;
        this.etReplaceQuery = etReplaceQuery;
        this.tvSearchCount = tvSearchCount;

        applyPreferences();
        setupSymbolBar();
        setupTextWatchers();
        setupCursorTracking();
        setupSearchReplaceListeners();
    }

    public void applyPreferences() {
        android.content.SharedPreferences prefs = context.getSharedPreferences(com.idestudio.app.ui.settings.SettingsActivity.PREF_EDITOR, Context.MODE_PRIVATE);
        boolean showLineNums = prefs.getBoolean(com.idestudio.app.ui.settings.SettingsActivity.KEY_LINE_NUMBERS, true);
        boolean showSymbols = prefs.getBoolean(com.idestudio.app.ui.settings.SettingsActivity.KEY_SYMBOL_BAR, true);

        if (lineNumbers != null) {
            lineNumbers.setVisibility(showLineNums ? View.VISIBLE : View.GONE);
        }
        if (symbolsContainer != null) {
            symbolsContainer.setVisibility(showSymbols ? View.VISIBLE : View.GONE);
        }
    }

    private void setupSymbolBar() {
        if (symbolsContainer == null) return;
        symbolsContainer.removeAllViews();

        String[] symbols = {
                "TAB", "{", "}", "(", ")", "[", "]", ";", "=", "\"", "'", "<", ">",
                "/", "\\", "+", "-", "*", "&", "|", "!", "?", ":", ".", ",", "_", "$", "//", "->", "=>"
        };

        for (String sym : symbols) {
            Button btn = new Button(context);
            btn.setText(sym);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btn.setBackgroundResource(R.drawable.bg_button_secondary);
            btn.setPadding(dpToPx(10), 0, dpToPx(10), 0);
            btn.setAllCaps(false);
            btn.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpToPx(32)
            );
            lp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
            btn.setLayoutParams(lp);

            btn.setOnClickListener(v -> handleSymbolClick(sym));
            symbolsContainer.addView(btn);
        }
    }

    private void handleSymbolClick(String sym) {
        int start = Math.max(editor.getSelectionStart(), 0);
        int end = Math.max(editor.getSelectionEnd(), 0);
        Editable text = editor.getText();
        if (text == null) return;

        switch (sym) {
            case "TAB":
                text.replace(Math.min(start, end), Math.max(start, end), "    ");
                editor.setSelection(start + 4);
                break;
            case "{":
                text.replace(Math.min(start, end), Math.max(start, end), "{}");
                editor.setSelection(start + 1);
                break;
            case "(":
                text.replace(Math.min(start, end), Math.max(start, end), "()");
                editor.setSelection(start + 1);
                break;
            case "[":
                text.replace(Math.min(start, end), Math.max(start, end), "[]");
                editor.setSelection(start + 1);
                break;
            case "<":
                text.replace(Math.min(start, end), Math.max(start, end), "<>");
                editor.setSelection(start + 1);
                break;
            case "\"":
                text.replace(Math.min(start, end), Math.max(start, end), "\"\"");
                editor.setSelection(start + 1);
                break;
            case "'":
                text.replace(Math.min(start, end), Math.max(start, end), "''");
                editor.setSelection(start + 1);
                break;
            default:
                text.replace(Math.min(start, end), Math.max(start, end), sym);
                editor.setSelection(start + sym.length());
                break;
        }
    }

    private void setupTextWatchers() {
        editor.addTextChangedListener(new TextWatcher() {
            private CharSequence beforeText;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                beforeText = s;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isAutoPairing || s == null) return;

                // Auto-close brackets/quotes on typing
                if (count == 1 && before == 0 && start < s.length()) {
                    char typed = s.charAt(start);
                    char closing = 0;
                    if (typed == '{') closing = '}';
                    else if (typed == '(') closing = ')';
                    else if (typed == '[') closing = ']';
                    else if (typed == '"') closing = '"';
                    else if (typed == '\'') closing = '\'';

                    if (closing != 0) {
                        isAutoPairing = true;
                        editor.getText().insert(start + 1, String.valueOf(closing));
                        editor.setSelection(start + 1);
                        isAutoPairing = false;
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateLineNumbersAndStatus();
            }
        });
    }

    public void updateLineNumbersAndStatus() {
        int lines = editor.getLineCount();
        if (lines < 1) lines = 1;

        if (lines != lastLineCount) {
            lastLineCount = lines;
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= lines; i++) {
                sb.append(i).append("\n");
            }
            if (lineNumbers != null) {
                lineNumbers.setText(sb.toString().trim());
            }
            if (statusLines != null) {
                statusLines.setText(lines + " lines");
            }
        }
    }

    private void setupCursorTracking() {
        editor.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void sendAccessibilityEvent(View host, int eventType) {
                super.sendAccessibilityEvent(host, eventType);
                updateCursorPosition();
            }
        });

        // Also track on touch
        editor.setOnClickListener(v -> updateCursorPosition());
    }

    public void updateCursorPosition() {
        int pos = editor.getSelectionStart();
        Layout layout = editor.getLayout();
        if (layout != null && pos >= 0 && statusCursor != null) {
            int line = layout.getLineForOffset(pos) + 1;
            int col = pos - layout.getLineStart(line - 1) + 1;
            statusCursor.setText(String.format(Locale.US, "Ln %d, Col %d", line, Math.max(1, col)));
        }
    }

    // --- Find & Replace ---

    private void setupSearchReplaceListeners() {
        if (etSearchQuery == null) return;

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public void toggleFindReplace(boolean show) {
        if (searchReplaceLayout != null) {
            searchReplaceLayout.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show && etSearchQuery != null) {
                etSearchQuery.requestFocus();
            }
        }
    }

    public boolean isFindReplaceVisible() {
        return searchReplaceLayout != null && searchReplaceLayout.getVisibility() == View.VISIBLE;
    }

    public void performSearch(String query) {
        searchMatches.clear();
        currentMatchIndex = -1;

        if (query == null || query.isEmpty()) {
            if (tvSearchCount != null) tvSearchCount.setText("0/0");
            return;
        }

        String content = editor.getText() != null ? editor.getText().toString() : "";
        String lowerContent = content.toLowerCase(Locale.US);
        String lowerQuery = query.toLowerCase(Locale.US);

        int index = lowerContent.indexOf(lowerQuery);
        while (index != -1) {
            searchMatches.add(index);
            index = lowerContent.indexOf(lowerQuery, index + query.length());
        }

        if (!searchMatches.isEmpty()) {
            currentMatchIndex = 0;
            highlightMatch(currentMatchIndex, query.length());
            if (tvSearchCount != null) {
                tvSearchCount.setText("1/" + searchMatches.size());
            }
        } else {
            if (tvSearchCount != null) tvSearchCount.setText("0/0");
        }
    }

    public void findNext() {
        if (searchMatches.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size();
        String q = etSearchQuery.getText().toString();
        highlightMatch(currentMatchIndex, q.length());
        if (tvSearchCount != null) {
            tvSearchCount.setText((currentMatchIndex + 1) + "/" + searchMatches.size());
        }
    }

    public void findPrevious() {
        if (searchMatches.isEmpty()) return;
        currentMatchIndex = (currentMatchIndex - 1 + searchMatches.size()) % searchMatches.size();
        String q = etSearchQuery.getText().toString();
        highlightMatch(currentMatchIndex, q.length());
        if (tvSearchCount != null) {
            tvSearchCount.setText((currentMatchIndex + 1) + "/" + searchMatches.size());
        }
    }

    public void replaceCurrent() {
        if (searchMatches.isEmpty() || currentMatchIndex < 0 || currentMatchIndex >= searchMatches.size()) return;
        String query = etSearchQuery.getText().toString();
        String replacement = etReplaceQuery != null ? etReplaceQuery.getText().toString() : "";

        int start = searchMatches.get(currentMatchIndex);
        Editable text = editor.getText();
        if (text != null && start + query.length() <= text.length()) {
            text.replace(start, start + query.length(), replacement);
            performSearch(query);
        }
    }

    public void replaceAll() {
        String query = etSearchQuery.getText().toString();
        if (query.isEmpty()) return;
        String replacement = etReplaceQuery != null ? etReplaceQuery.getText().toString() : "";

        String currentText = editor.getText() != null ? editor.getText().toString() : "";
        String replaced = currentText.replace(query, replacement);
        editor.setText(replaced);
        performSearch(query);
        Toast.makeText(context, "All occurrences replaced", Toast.LENGTH_SHORT).show();
    }

    private void highlightMatch(int matchIdx, int length) {
        if (matchIdx < 0 || matchIdx >= searchMatches.size()) return;
        int start = searchMatches.get(matchIdx);
        editor.setSelection(start, start + length);
        editor.requestFocus();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()
        );
    }
}
