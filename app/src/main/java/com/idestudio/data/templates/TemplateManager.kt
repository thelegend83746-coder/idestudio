package com.idestudio.data.templates

import android.content.Context
import com.idestudio.data.model.Project
import com.idestudio.util.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Unified Template Manager for IDE STUDIO.
 * Uses the single standard "EmptyProject" template (packaged in assets as EmptyProject.zip)
 * for all project creations, while UI displays the requested template options.
 */
object TemplateManager {

    @Throws(IOException::class)
    fun generateProjectFiles(project: Project, context: Context? = null) {
        val rootDir = File(project.rootPath)
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }

        var extractedFromZip = false

        // 1. Try extracting EmptyProject.zip from assets if context is provided
        if (context != null) {
            try {
                context.assets.open("EmptyProject.zip").use { inputStream ->
                    extractAndCustomizeZip(inputStream, project, rootDir)
                    extractedFromZip = true
                }
            } catch (_: Exception) {
                extractedFromZip = false
            }
        }

        // 2. If not extracted from asset zip, generate the standard EmptyProject template directly
        if (!extractedFromZip) {
            generateDirectEmptyProject(project, rootDir)
        }

        // 3. Project Metadata file (idestudio.json) preserving selected UI template identity
        val projectMetaJson = """{
  "id": "${project.id}",
  "name": "${project.name}",
  "packageName": "${project.packageName}",
  "minSdk": ${project.minSdk},
  "targetSdk": ${project.targetSdk},
  "templateId": "${project.templateId}",
  "language": "${project.language}",
  "createdAt": ${project.createdAt}
}""".trimIndent()
        FileUtils.writeFileSafe(File(rootDir, "idestudio.json"), projectMetaJson)
    }

    private fun extractAndCustomizeZip(zipInputStream: InputStream, project: Project, rootDir: File) {
        val zis = ZipInputStream(zipInputStream)
        var entry: ZipEntry? = zis.nextEntry

        val targetPackagePath = "app/src/main/java/" + project.packageName.replace('.', '/')

        while (entry != null) {
            if (!entry.isDirectory) {
                var entryPath = entry.name
                // Handle zip root prefix if zipped with EmptyProject/
                if (entryPath.startsWith("EmptyProject/")) {
                    entryPath = entryPath.removePrefix("EmptyProject/")
                }

                val destFile: File = if (entryPath.contains("app/src/main/java/com/idestudio/emptyproject/")) {
                    val fileName = entryPath.substringAfterLast('/')
                    File(rootDir, "$targetPackagePath/$fileName")
                } else {
                    File(rootDir, entryPath)
                }

                destFile.parentFile?.mkdirs()

                val bytes = zis.readBytes()
                val isTextFile = entryPath.endsWith(".java") ||
                                entryPath.endsWith(".xml") ||
                                entryPath.endsWith(".gradle") ||
                                entryPath.endsWith(".json")

                if (isTextFile) {
                    var content = String(bytes, Charsets.UTF_8)
                    content = content.replace("com.idestudio.emptyproject", project.packageName)
                    content = content.replace("EmptyProject", project.name)
                    FileUtils.writeFileSafe(destFile, content)
                } else {
                    FileOutputStream(destFile).use { fos ->
                        fos.write(bytes)
                    }
                }
            }
            entry = zis.nextEntry
        }
    }

    private fun generateDirectEmptyProject(project: Project, rootDir: File) {
        val appDir = File(rootDir, "app")
        val mainDir = File(appDir, "src/main")
        val javaPackageDir = File(mainDir, "java/${project.packageName.replace('.', '/')}")
        val resDir = File(mainDir, "res")
        val layoutDir = File(resDir, "layout")
        val valuesDir = File(resDir, "values")
        val drawableDir = File(resDir, "drawable")
        val assetsDir = File(mainDir, "assets")

        javaPackageDir.mkdirs()
        layoutDir.mkdirs()
        valuesDir.mkdirs()
        drawableDir.mkdirs()
        assetsDir.mkdirs()

        // 1. Strings Resource
        val stringsXml = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">${project.name}</string>
    <string name="welcome_message">Welcome to ${project.name}!</string>
    <string name="action_click_me">Click Me</string>
    <string name="button_clicked_message">Hello from IDE STUDIO!</string>
</resources>
""".trimIndent()
        FileUtils.writeFileSafe(File(valuesDir, "strings.xml"), stringsXml)

        // 2. Colors Resource
        val colorsXml = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary">#3884FF</color>
    <color name="primary_dark">#1E60D5</color>
    <color name="accent">#00C853</color>
    <color name="bg_light">#F5F7FA</color>
    <color name="text_dark">#1A1A1A</color>
</resources>
""".trimIndent()
        FileUtils.writeFileSafe(File(valuesDir, "colors.xml"), colorsXml)

        // 3. Styles / Themes Resource
        val stylesXml = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:colorPrimary">@color/primary</item>
        <item name="android:colorPrimaryDark">@color/primary_dark</item>
        <item name="android:colorAccent">@color/accent</item>
    </style>
</resources>
""".trimIndent()
        FileUtils.writeFileSafe(File(valuesDir, "styles.xml"), stylesXml)

        // 4. AndroidManifest.xml
        val manifestXml = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="${project.packageName}">

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
""".trimIndent()
        FileUtils.writeFileSafe(File(mainDir, "AndroidManifest.xml"), manifestXml)

        // 5. activity_main.xml
        val layoutXml = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="24dp"
    android:background="@color/bg_light">

    <TextView
        android:id="@+id/textViewTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/welcome_message"
        android:textSize="22sp"
        android:textStyle="bold"
        android:textColor="@color/text_dark" />

    <Button
        android:id="@+id/btnClickMe"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:text="@string/action_click_me" />

</LinearLayout>
""".trimIndent()
        FileUtils.writeFileSafe(File(layoutDir, "activity_main.xml"), layoutXml)

        // 6. MainActivity.java
        val javaSource = """package ${project.packageName};

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView textViewTitle;
    private Button btnClickMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewTitle = findViewById(R.id.textViewTitle);
        btnClickMe = findViewById(R.id.btnClickMe);

        btnClickMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, R.string.button_clicked_message, Toast.LENGTH_SHORT).show();
                textViewTitle.setText("Button Clicked!");
            }
        });
    }
}
""".trimIndent()
        FileUtils.writeFileSafe(File(javaPackageDir, "MainActivity.java"), javaSource)
    }
}
