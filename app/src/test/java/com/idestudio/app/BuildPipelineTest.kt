package com.idestudio.app

import com.idestudio.app.data.model.Project
import com.idestudio.app.toolchain.ApkPackager
import com.idestudio.app.toolchain.DexGenerator
import com.idestudio.app.toolchain.JavaCompiler
import com.idestudio.app.toolchain.ResourceGenerator
import com.idestudio.app.util.TemplateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuildPipelineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testResourceGeneratorProducesValidRJava() {
        val root = tempFolder.newFolder("TestProject")
        val project = Project(
            id = "test-proj",
            name = "TestProject",
            appName = "Test App",
            packageName = "com.test.app",
            mainActivityName = "MainActivity",
            rootPath = root.absolutePath
        )

        TemplateManager.generateProjectStructure(project)

        val genDir = File(project.buildDir, "gen")
        val rJava = ResourceGenerator.generateRJava(project, genDir)

        assertTrue(rJava.exists())
        val content = rJava.readText()
        assertTrue(content.contains("package com.test.app;"))
        assertTrue(content.contains("public final class R"))
        assertTrue(content.contains("public static final class layout"))
        assertTrue(content.contains("public static final class string"))
    }

    @Test
    fun testJavaCompilerDetectsSyntaxError() {
        val root = tempFolder.newFolder("ErrorProject")
        val project = Project(
            id = "err-proj",
            name = "ErrorProject",
            appName = "Error App",
            packageName = "com.test.err",
            mainActivityName = "MainActivity",
            rootPath = root.absolutePath
        )

        TemplateManager.generateProjectStructure(project)

        // Introduce deliberate syntax error into MainActivity.java
        val mainJava = File(project.javaDir, "com/test/err/MainActivity.java")
        mainJava.writeText(
            """
            package com.test.err;
            import android.app.Activity
            public class MainActivity extends Activity {}
            """.trimIndent()
        )

        val compiler = JavaCompiler(androidJar = null)
        val result = compiler.compile(project) {}

        assertTrue("Compiler should report failure on missing semicolon in import", !result.success)
        assertTrue(result.totalErrors > 0)
    }

    @Test
    fun testDexGeneratorCreatesValidDex() {
        val root = tempFolder.newFolder("DexProject")
        val project = Project(
            id = "dex-proj",
            name = "DexProject",
            appName = "Dex App",
            packageName = "com.test.dex",
            mainActivityName = "MainActivity",
            rootPath = root.absolutePath
        )

        TemplateManager.generateProjectStructure(project)

        val classesDir = File(project.buildDir, "classes")
        classesDir.mkdirs()
        File(classesDir, "Test.class").writeBytes(byteArrayOf(1, 2, 3, 4))

        val dexGen = DexGenerator()
        val result = dexGen.generateDex(project, classesDir) {}

        assertTrue(result.success)
        assertNotNull(result.outputDexFile)
        assertTrue(result.outputDexFile!!.exists())

        // Verify DEX header magic
        val bytes = result.outputDexFile!!.readBytes()
        assertEquals(0x64.toByte(), bytes[0]) // 'd'
        assertEquals(0x65.toByte(), bytes[1]) // 'e'
        assertEquals(0x78.toByte(), bytes[2]) // 'x'
    }
}
