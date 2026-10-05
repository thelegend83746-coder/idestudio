package com.idestudio.app

import com.idestudio.app.data.model.Project
import com.idestudio.app.util.TemplateManager
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TemplateManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testGenerateStandardApp() {
        val root = tempFolder.newFolder("TestStandardApp")
        val project = Project(
            id = "test-1",
            name = "TestStandardApp",
            appName = "Test App",
            packageName = "com.test.standard",
            mainActivityName = "MainActivity",
            rootPath = root.absolutePath,
            templateId = "simple_app"
        )

        TemplateManager.generateProjectStructure(project)

        assertTrue(File(root, "IDE_STUDIO_PROJECT.json").exists())
        assertTrue(project.manifestFile.exists())
        assertTrue(File(project.resDir, "layout/activity_main.xml").exists())
        assertTrue(File(project.resDir, "values/strings.xml").exists())
        assertTrue(File(project.javaDir, "com/test/standard/MainActivity.java").exists())
    }
}
