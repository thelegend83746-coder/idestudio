package com.idestudio.app.domain;

import com.idestudio.app.core.constants.AppConstants;
import com.idestudio.app.data.models.ProjectMeta;
import com.idestudio.app.domain.project.ProjectGenerator;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProjectGeneratorTest {

    @Test
    public void testGenerateProjectStructure() throws IOException {
        String testDir = "/storage/emulated/0/test-folder/ide-studio/projects/UnitTestSample";
        ProjectMeta meta = ProjectGenerator.generateProject(
                "UnitTestSample",
                "com.test.sample",
                21,
                34,
                AppConstants.TEMPLATE_EMPTY_ACTIVITY,
                testDir
        );

        assertNotNull(meta);
        assertEquals("UnitTestSample", meta.getName());
        assertEquals("com.test.sample", meta.getPackageName());

        File root = new File(testDir);
        assertTrue(new File(root, "build.gradle").exists());
        assertTrue(new File(root, "settings.gradle").exists());
        assertTrue(new File(root, "app/build.gradle").exists());
        assertTrue(new File(root, "app/src/main/AndroidManifest.xml").exists());
        assertTrue(new File(root, "app/src/main/res/layout/activity_main.xml").exists());
        assertTrue(new File(root, "app/src/main/java/com/test/sample/MainActivity.java").exists());

        // Cleanup test directory
        deleteRecursive(root);
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
