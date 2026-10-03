package com.idestudio.app.editor;

import com.idestudio.app.editor.history.UndoRedoManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UndoRedoManagerTest {

    @Test
    public void testUndoRedoStackOperations() {
        UndoRedoManager manager = new UndoRedoManager();
        assertFalse(manager.canUndo());
        assertFalse(manager.canRedo());

        // Push edit 1
        manager.pushChange(0, "", "public class Main ");
        assertTrue(manager.canUndo());
        assertFalse(manager.canRedo());

        // Undo edit 1
        UndoRedoManager.TextChange undo1 = manager.popUndo();
        assertNotNull(undo1);
        assertEquals("", undo1.beforeText.toString());
        assertEquals("public class Main ", undo1.afterText.toString());
        assertFalse(manager.canUndo());
        assertTrue(manager.canRedo());

        // Redo edit 1
        UndoRedoManager.TextChange redo1 = manager.popRedo();
        assertNotNull(redo1);
        assertEquals("public class Main ", redo1.afterText.toString());
        assertTrue(manager.canUndo());
        assertFalse(manager.canRedo());
    }
}
