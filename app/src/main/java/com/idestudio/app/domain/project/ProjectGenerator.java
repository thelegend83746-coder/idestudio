package com.idestudio.app.domain.project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.idestudio.app.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Generates Android project scaffolding directly inside /storage/emulated/0/idestudio/<ProjectName>
 */
public class ProjectGenerator {

    private static final String TAG = "ProjectGenerator";

    public static File createProject(
            Context context,
            String projectName,
            String packageName,
            String templateType,
            int minSdk,
            int targetSdk,
            Uri customIconUri,
            int presetIconResId
    ) throws Exception {

        File baseDir = LocalProjectStore.getResolvedBaseDir();
        File projectDir = new File(baseDir, projectName);

        if (projectDir.exists()) {
            throw new IllegalArgumentException("A project with name '" + projectName + "' already exists in " + baseDir.getAbsolutePath());
        }

        if (!projectDir.mkdirs()) {
            throw new Exception("Could not create project directory: " + projectDir.getAbsolutePath());
        }

        // Create standard Gradle Android directory hierarchy
        File appDir = new File(projectDir, "app");
        File mainDir = new File(appDir, "src/main");
        File javaDir = new File(mainDir, "java/" + packageName.replace('.', '/'));
        File resDir = new File(mainDir, "res");
        File layoutDir = new File(resDir, "layout");
        File valuesDir = new File(resDir, "values");
        File drawableDir = new File(resDir, "drawable");

        javaDir.mkdirs();
        layoutDir.mkdirs();
        valuesDir.mkdirs();
        drawableDir.mkdirs();

        // 1. root build.gradle
        writeFile(new File(projectDir, "build.gradle"),
                "// Top-level build file\n" +
                "buildscript {\n" +
                "    repositories {\n" +
                "        google()\n" +
                "        mavenCentral()\n" +
                "    }\n" +
                "    dependencies {\n" +
                "        classpath 'com.android.tools.build:gradle:8.2.0'\n" +
                "    }\n" +
                "}\n" +
                "allprojects {\n" +
                "    repositories {\n" +
                "        google()\n" +
                "        mavenCentral()\n" +
                "    }\n" +
                "}\n"
        );

        // 2. settings.gradle
        writeFile(new File(projectDir, "settings.gradle"),
                "rootProject.name = '" + projectName + "'\n" +
                "include ':app'\n"
        );

        // 3. app/build.gradle
        writeFile(new File(appDir, "build.gradle"),
                "plugins {\n" +
                "    id 'com.android.application'\n" +
                "}\n\n" +
                "android {\n" +
                "    namespace '" + packageName + "'\n" +
                "    compileSdk " + targetSdk + "\n\n" +
                "    defaultConfig {\n" +
                "        applicationId '" + packageName + "'\n" +
                "        minSdk " + minSdk + "\n" +
                "        targetSdk " + targetSdk + "\n" +
                "        versionCode 1\n" +
                "        versionName '1.0'\n" +
                "    }\n\n" +
                "    buildTypes {\n" +
                "        release {\n" +
                "            minifyEnabled false\n" +
                "        }\n" +
                "    }\n" +
                "    compileOptions {\n" +
                "        sourceCompatibility JavaVersion.VERSION_1_8\n" +
                "        targetCompatibility JavaVersion.VERSION_1_8\n" +
                "    }\n" +
                "}\n\n" +
                "dependencies {\n" +
                "    implementation 'androidx.appcompat:appcompat:1.6.1'\n" +
                "    implementation 'com.google.android.material:material:1.11.0'\n" +
                "}\n"
        );

        // 4. AndroidManifest.xml
        writeFile(new File(mainDir, "AndroidManifest.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    package=\"" + packageName + "\">\n\n" +
                "    <application\n" +
                "        android:allowBackup=\"true\"\n" +
                "        android:icon=\"@drawable/ic_launcher\"\n" +
                "        android:label=\"@string/app_name\"\n" +
                "        android:theme=\"@style/Theme.MaterialComponents.DayNight.NoActionBar\">\n\n" +
                "        <activity\n" +
                "            android:name=\".MainActivity\"\n" +
                "            android:exported=\"true\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.MAIN\" />\n" +
                "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
                "            </intent-filter>\n" +
                "        </activity>\n" +
                "    </application>\n" +
                "</manifest>\n"
        );

        // 5. res/values/strings.xml
        writeFile(new File(valuesDir, "strings.xml"),
                "<resources>\n" +
                "    <string name=\"app_name\">" + escapeXml(projectName) + "</string>\n" +
                "    <string name=\"hello_world\">Hello from " + escapeXml(projectName) + "!</string>\n" +
                "</resources>\n"
        );

        // 6. res/layout/activity_main.xml
        writeFile(new File(layoutDir, "activity_main.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    android:padding=\"16dp\"\n" +
                "    android:background=\"#FFFFFF\">\n\n" +
                "    <ImageView\n" +
                "        android:id=\"@+id/iv_logo\"\n" +
                "        android:layout_width=\"96dp\"\n" +
                "        android:layout_height=\"96dp\"\n" +
                "        android:layout_centerHorizontal=\"true\"\n" +
                "        android:layout_marginTop=\"120dp\"\n" +
                "        android:src=\"@drawable/ic_launcher\" />\n\n" +
                "    <TextView\n" +
                "        android:id=\"@+id/tv_title\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_below=\"@+id/iv_logo\"\n" +
                "        android:layout_centerHorizontal=\"true\"\n" +
                "        android:layout_marginTop=\"24dp\"\n" +
                "        android:text=\"@string/app_name\"\n" +
                "        android:textSize=\"22sp\"\n" +
                "        android:textStyle=\"bold\"\n" +
                "        android:textColor=\"#212121\" />\n\n" +
                "    <TextView\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_below=\"@+id/tv_title\"\n" +
                "        android:layout_centerHorizontal=\"true\"\n" +
                "        android:layout_marginTop=\"8dp\"\n" +
                "        android:text=\"@string/hello_world\"\n" +
                "        android:textSize=\"15sp\"\n" +
                "        android:textColor=\"#757575\" />\n" +
                "</RelativeLayout>\n"
        );

        // 7. MainActivity.java
        writeFile(new File(javaDir, "MainActivity.java"),
                "package " + packageName + ";\n\n" +
                "import android.os.Bundle;\n" +
                "import androidx.appcompat.app.AppCompatActivity;\n\n" +
                "public class MainActivity extends AppCompatActivity {\n\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n" +
                "    }\n" +
                "}\n"
        );

        // 8. Launcher Icon
        saveLauncherIcon(context, drawableDir, customIconUri, presetIconResId);

        // 9. Save project in LocalProjectStore
        ProjectMeta meta = new ProjectMeta();
        meta.setId("proj_" + System.currentTimeMillis());
        meta.setName(projectName);
        meta.setPackageName(packageName);
        meta.setProjectPath(projectDir.getAbsolutePath());
        meta.setTemplateType(templateType);
        meta.setMinSdkVersion(minSdk);
        meta.setTargetSdkVersion(targetSdk);
        meta.setLastModified(System.currentTimeMillis());
        LocalProjectStore.getInstance().saveProject(meta);

        Log.i(TAG, "Generated project successfully at: " + projectDir.getAbsolutePath());
        return projectDir;
    }

    private static void saveLauncherIcon(Context context, File drawableDir, Uri customIconUri, int presetResId) {
        try {
            File iconFile = new File(drawableDir, "ic_launcher.png");
            Bitmap bitmap = null;

            if (customIconUri != null && context != null) {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), customIconUri);
            } else if (presetResId != 0 && context != null) {
                Drawable d = ContextCompat.getDrawable(context, presetResId);
                if (d != null) {
                    bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    d.draw(canvas);
                }
            }

            if (bitmap == null && context != null) {
                Drawable d = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);
                if (d != null) {
                    bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    d.draw(canvas);
                }
            }

            if (bitmap != null) {
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 192, 192, true);
                try (FileOutputStream fos = new FileOutputStream(iconFile)) {
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed creating launcher icon: " + e.getMessage(), e);
        }
    }

    private static void writeFile(File file, String content) throws Exception {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.flush();
        }
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "\\'");
    }
}
