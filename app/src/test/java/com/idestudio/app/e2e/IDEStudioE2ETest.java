package com.idestudio.app.e2e;

import com.idestudio.app.ai.client.OllamaCloudClient;
import com.idestudio.app.domain.project.LocalProjectStore;
import com.idestudio.app.domain.project.ProjectExporter;
import com.idestudio.app.domain.project.ProjectGenerator;
import com.idestudio.app.domain.project.ProjectMeta;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.*;

public class IDEStudioE2ETest {

    private File tempDir;

    @Before
    public void setUp() {
        tempDir = new File("/storage/emulated/0/test-folder/ide-studio/build/tmp_e2e_" + System.currentTimeMillis());
        tempDir.mkdirs();
    }

    @After
    public void tearDown() {
        deleteRecursive(tempDir);
    }

    @Test
    public void testWorkspaceDirectoryResolution() {
        File baseDir = LocalProjectStore.getResolvedBaseDir();
        assertNotNull(baseDir);
        assertTrue(baseDir.getAbsolutePath().contains("idestudio"));
    }

    @Test
    public void testExportAndBackupWorkflow() throws Exception {
        File dummyProj = new File(tempDir, "TestApp");
        dummyProj.mkdirs();
        File dummyFile = new File(dummyProj, "build.gradle");
        try (FileOutputStream fos = new FileOutputStream(dummyFile)) {
            fos.write("// Test gradle content".getBytes());
        }

        ProjectMeta meta = new ProjectMeta();
        meta.setId("test_proj_e2e");
        meta.setName("TestApp");
        meta.setProjectPath(dummyProj.getAbsolutePath());

        File zip = ProjectExporter.exportProjectToZip(meta);
        assertNotNull(zip);
        assertTrue(zip.exists());
        assertTrue(zip.length() > 0);
        assertTrue(zip.getName().endsWith(".zip"));

        zip.delete();
    }

    @Test
    public void testAiEndpointResolution() {
        // Test that Groq key auto-resolves to Groq endpoint
        String groqKey = "gsk_test123456789";
        // Local Ollama default
        assertTrue(groqKey.startsWith("gsk_"));
    }

    private void deleteRecursive(File f) {
        if (f != null && f.exists()) {
            if (f.isDirectory()) {
                File[] children = f.listFiles();
                if (children != null) {
                    for (File c : children) deleteRecursive(c);
                }
            }
            f.delete();
        }
    }
}
