package com.idestudio.app.domain;

import com.idestudio.app.domain.filesystem.SafeFileOperations;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SafeFileOperationsTest {

    private File testRoot;

    @Before
    public void setUp() {
        testRoot = new File("/storage/emulated/0/test-folder/ide-studio/projects/ExplorerUnitTest");
        if (testRoot.exists()) {
            deleteRecursive(testRoot);
        }
        testRoot.mkdirs();
    }

    @After
    public void tearDown() {
        if (testRoot != null && testRoot.exists()) {
            deleteRecursive(testRoot);
        }
    }

    @Test
    public void testCreateFileAndFolder() throws IOException {
        File subFolder = SafeFileOperations.createFolder(testRoot, testRoot, "models");
        assertNotNull(subFolder);
        assertTrue(subFolder.exists());
        assertTrue(subFolder.isDirectory());

        File newFile = SafeFileOperations.createFile(testRoot, subFolder, "User.java");
        assertNotNull(newFile);
        assertTrue(newFile.exists());
        assertTrue(newFile.isFile());
    }

    @Test
    public void testRename() throws IOException {
        File file = SafeFileOperations.createFile(testRoot, testRoot, "OldName.java");
        File renamed = SafeFileOperations.rename(testRoot, file, "NewName.java");

        assertFalse(file.exists());
        assertTrue(renamed.exists());
        assertEquals("NewName.java", renamed.getName());
    }

    @Test(expected = SecurityException.class)
    public void testPathTraversalBlock() throws IOException {
        File outside = new File(testRoot, "../../outside.txt");
        SafeFileOperations.createFile(testRoot, outside.getParentFile(), "outside.txt");
    }

    @Test
    public void testDelete() throws IOException {
        File folder = SafeFileOperations.createFolder(testRoot, testRoot, "to_delete");
        SafeFileOperations.createFile(testRoot, folder, "child.txt");

        assertTrue(folder.exists());
        SafeFileOperations.delete(testRoot, folder);
        assertFalse(folder.exists());
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File child : files) deleteRecursive(child);
            }
        }
        f.delete();
    }
}
