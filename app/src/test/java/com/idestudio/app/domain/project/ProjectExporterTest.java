package com.idestudio.app.domain.project;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

public class ProjectExporterTest {

    private File tempProjectDir;
    private File tempBackupDir;
    private ProjectMeta meta;

    @Before
    public void setUp() throws Exception {
        tempProjectDir = new File("/storage/emulated/0/test-folder/ide-studio/build/tmp_test_project_" + System.currentTimeMillis());
        tempProjectDir.mkdirs();

        File appDir = new File(tempProjectDir, "app");
        appDir.mkdirs();

        File stringsFile = new File(appDir, "strings.xml");
        try (FileOutputStream fos = new FileOutputStream(stringsFile)) {
            fos.write("<resources><string name=\"app_name\">TestApp</string></resources>".getBytes());
        }

        meta = new ProjectMeta();
        meta.setId("test_id");
        meta.setName("TestApp");
        meta.setPackageName("com.test.app");
        meta.setProjectPath(tempProjectDir.getAbsolutePath());
    }

    @After
    public void tearDown() {
        deleteRecursive(tempProjectDir);
    }

    @Test
    public void testExportProjectToZipCreatesValidZip() throws IOException {
        File zipFile = ProjectExporter.exportProjectToZip(meta);
        assertNotNull(zipFile);
        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);
        assertTrue(zipFile.getName().contains("TestApp_backup_"));
        assertTrue(zipFile.getName().endsWith(".zip"));

        // Verify it can be opened as a ZipFile
        try (ZipFile zf = new ZipFile(zipFile)) {
            assertNotNull(zf.getEntry("app/strings.xml"));
        } finally {
            zipFile.delete();
        }
    }

    private void deleteRecursive(File file) {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File c : children) {
                        deleteRecursive(c);
                    }
                }
            }
            file.delete();
        }
    }
}
