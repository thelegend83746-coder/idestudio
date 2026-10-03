package com.idestudio.app.core;

import com.idestudio.app.core.utils.PathSanitizer;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PathSanitizerTest {

    @Test
    public void testSafePathInsideRoot() {
        File root = new File("/storage/emulated/0/test-folder/ide-studio/projects");
        File child = new File("/storage/emulated/0/test-folder/ide-studio/projects/MyProject/MainActivity.java");
        assertTrue(PathSanitizer.isPathSafe(root, child));
    }

    @Test
    public void testPathTraversalDetected() {
        File root = new File("/storage/emulated/0/test-folder/ide-studio/projects");
        File attack = new File("/storage/emulated/0/test-folder/ide-studio/projects/../../etc/passwd");
        assertFalse(PathSanitizer.isPathSafe(root, attack));
    }

    @Test
    public void testSanitizeFileName() {
        String unsafe = "My/App:Project*Name?<>|";
        String safe = PathSanitizer.sanitizeFileName(unsafe);
        assertEquals("My_App_Project_Name____", safe);
    }
}
