package com.idestudio.app.editor.history;

import java.io.Serializable;
import java.util.Stack;

public class UndoRedoManager implements Serializable {

    public static class TextChange {
        public final int start;
        public final CharSequence beforeText;
        public final CharSequence afterText;

        public TextChange(int start, CharSequence beforeText, CharSequence afterText) {
            this.start = start;
            this.beforeText = beforeText != null ? beforeText.toString() : "";
            this.afterText = afterText != null ? afterText.toString() : "";
        }
    }

    private final Stack<TextChange> undoStack = new Stack<>();
    private final Stack<TextChange> redoStack = new Stack<>();
    private boolean isPerformingUndoOrRedo = false;

    public void pushChange(int start, CharSequence before, CharSequence after) {
        if (isPerformingUndoOrRedo) return;
        undoStack.push(new TextChange(start, before, after));
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public TextChange popUndo() {
        if (!canUndo()) return null;
        TextChange change = undoStack.pop();
        redoStack.push(change);
        return change;
    }

    public TextChange popRedo() {
        if (!canRedo()) return null;
        TextChange change = redoStack.pop();
        undoStack.push(change);
        return change;
    }

    public void setPerformingUndoOrRedo(boolean performing) {
        isPerformingUndoOrRedo = performing;
    }

    public boolean isPerformingUndoOrRedo() {
        return isPerformingUndoOrRedo;
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
