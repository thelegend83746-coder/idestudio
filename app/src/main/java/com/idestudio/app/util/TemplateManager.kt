package com.idestudio.app.util

import com.idestudio.app.data.model.Project
import java.io.File

object TemplateManager {

    fun generateProjectStructure(project: Project) {
        val root = project.rootDir
        if (!root.exists()) {
            root.mkdirs()
        }

        // Directories
        val appDir = project.appDir
        val srcMain = project.srcDir
        val javaDir = project.javaDir
        val resDir = project.resDir
        val buildDir = project.buildDir

        val packageSubPath = project.packageName.replace('.', '/')
        val packageDir = File(javaDir, packageSubPath)
        packageDir.mkdirs()

        val layoutDir = File(resDir, "layout")
        layoutDir.mkdirs()

        val valuesDir = File(resDir, "values")
        valuesDir.mkdirs()

        val drawableDir = File(resDir, "drawable")
        drawableDir.mkdirs()

        val buildBinDir = File(buildDir, "bin")
        buildBinDir.mkdirs()
        val buildGenDir = File(buildDir, "gen")
        buildGenDir.mkdirs()

        // Write Project Metadata JSON
        val metaFile = File(root, "IDE_STUDIO_PROJECT.json")
        metaFile.writeText(project.toJson().toString(2))

        // Write AndroidManifest.xml
        val manifestContent = buildManifest(project)
        project.manifestFile.writeText(manifestContent)

        // Write strings.xml
        val stringsXml = File(valuesDir, "strings.xml")
        stringsXml.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="app_name">${project.appName}</string>
                <string name="hello_world">Hello from ${project.appName}!</string>
                <string name="action_settings">Settings</string>
            </resources>
            """.trimIndent()
        )

        // Write colors.xml
        val colorsXml = File(valuesDir, "colors.xml")
        colorsXml.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="primary">#6200EE</color>
                <color name="primary_dark">#3700B3</color>
                <color name="accent">#03DAC5</color>
                <color name="background">#FFFFFF</color>
                <color name="text_color">#212121</color>
            </resources>
            """.trimIndent()
        )

        // Write styles.xml
        val stylesXml = File(valuesDir, "styles.xml")
        stylesXml.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <style name="AppTheme" parent="android:Theme.Material.Light.DarkActionBar">
                    <item name="android:colorPrimary">@color/primary</item>
                    <item name="android:colorPrimaryDark">@color/primary_dark</item>
                    <item name="android:colorAccent">@color/accent</item>
                </style>
            </resources>
            """.trimIndent()
        )

        // Generate the single Standard App template
        generateStandardApp(packageDir, layoutDir, project)
    }

    private fun buildManifest(project: Project): String {
        return """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            package="${project.packageName}">

            <application
                android:allowBackup="true"
                android:label="@string/app_name"
                android:theme="@style/AppTheme">
                <activity
                    android:name=".${project.mainActivityName}"
                    android:exported="true">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <category android:name="android.intent.category.LAUNCHER" />
                    </intent-filter>
                </activity>
            </application>

        </manifest>
        """.trimIndent()
    }

    private fun generateStandardApp(packageDir: File, layoutDir: File, project: Project) {
        val layoutFile = File(layoutDir, "activity_main.xml")
        layoutFile.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center"
                android:orientation="vertical"
                android:padding="24dp">

                <TextView
                    android:id="@+id/tv_greeting"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/hello_world"
                    android:textSize="20sp"
                    android:textStyle="bold"
                    android:textColor="@color/text_color" />

                <Button
                    android:id="@+id/btn_action"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="20dp"
                    android:text="Click Me" />

            </LinearLayout>
            """.trimIndent()
        )

        val activityFile = File(packageDir, "${project.mainActivityName}.java")
        activityFile.writeText(
            """
            package ${project.packageName};

            import android.app.Activity;
            import android.os.Bundle;
            import android.view.View;
            import android.widget.Button;
            import android.widget.TextView;
            import android.widget.Toast;

            public class ${project.mainActivityName} extends Activity {

                private TextView tvGreeting;
                private Button btnAction;
                private int clickCount = 0;

                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    setContentView(R.layout.activity_main);

                    tvGreeting = (TextView) findViewById(R.id.tv_greeting);
                    btnAction = (Button) findViewById(R.id.btn_action);

                    btnAction.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            clickCount++;
                            tvGreeting.setText("Clicked: " + clickCount + " times!");
                            Toast.makeText(${project.mainActivityName}.this, "Button Clicked #" + clickCount, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            """.trimIndent()
        )
    }
}
