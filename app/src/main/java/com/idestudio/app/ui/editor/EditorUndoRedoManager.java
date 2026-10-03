package com.idestudio.app.ui.editor;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Robust Undo/Redo history manager for the code editor.
 */
public class EditorUndoRedoManager {

    private static final int MAX_HISTORY = 50;

    private final EditText editor;
    private final Deque<EditHistoryItem> undoStack = new ArrayDeque<>();
    private final Deque<EditHistoryItem> redoStack = new ArrayDeque<>();
    private boolean isUndoingOrRedoing = false;

    private static class EditHistoryItem {
        final String text;
        final int cursorPosition;

        EditHistoryItem(String text, int cursorPosition) {
            this.text = text;
            this.cursorPosition = cursorPosition;
        }
    }

    public EditorUndoRedoManager(EditText editor) {
        this.editor = editor;
        saveInitialState();
        attachTextWatcher();
    }

    private void saveInitialState() {
        String initial = editor.getText() != null ? editor.getText().toString() : "";
        undoStack.push(new EditHistoryItem(initial, editor.getSelectionStart()));
    }

    private void attachTextWatcher() {
        editor.addTextChangedListener(new TextWatcher() {
            private String beforeText;
            private int beforeCursor;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isUndoingOrRedoing && s != null) {
                    beforeText = s.toString();
                    beforeCursor = editor.getSelectionStart();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUndoingOrRedoing || s == null) return;

                String current = s.toString();
                if (!current.equals(beforeText)) {
                    if (undoStack.size() >= MAX_HISTORY) {
                        undoStack.removeLast();
                    }
                    undoStack.push(new EditHistoryItem(current, editor.getSelectionStart()));
                    redoStack.clear();
                }
            }
        });
    }

    public boolean canUndo() {
        return undoStack.size() > 1;
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (!canUndo()) return;

        isUndoingOrRedoing = true;
        try {
            EditHistoryItem current = undoStack.pop();
            redoStack.push(current);

            EditHistoryItem previous = undoStack.peek();
            if (previous != null) {
                editor.setText(previous.text);
                int sel = Math.min(Math.max(0, previous.cursorPosition), editor.getText().length());
                editor.setSelection(sel);
            }
        } finally {
            isUndoingOrRedoing = false;
        }
    }

    public void redo() {
        if (!canRedo()) return;

        isUndoingOrRedoing = true;
        try {
            EditHistoryItem item = redoStack.pop();
            undoStack.push(item);

            editor.setText(item.text);
            int sel = Math.min(Math.max(0, item.cursorPosition), editor.getText().length());
            editor.setSelection(sel);
        } finally {
            isUndoingOrRedoing = false;
        }
    }
}
