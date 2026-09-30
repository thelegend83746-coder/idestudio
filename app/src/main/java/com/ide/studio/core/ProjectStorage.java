package com.ide.studio.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import com.ide.studio.model.Project;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Storage manager for creating, loading, and deleting idestudio projects.
 * Supports template generation including standard apps, navigation tabs,
 * and high-performance SurfaceView / Canvas 2D game loops.
 */
public class ProjectStorage {

    private static final String BASE_FOLDER_NAME = "BUILD STUDIO";
    private static final String ALT_FOLDER_NAME = ".BUILD STUDIO";
    private Context mContext;

    public ProjectStorage(Context context) {
        this.mContext = context;
    }

    public static File getProjectsRoot() {
        File dir = new File(Environment.getExternalStorageDirectory(), BASE_FOLDER_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getExportsRoot() {
        File dir = new File(getProjectsRoot(), "Exports");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public List<Project> getAllProjects() {
        return loadProjects();
    }

    public static List<Project> loadProjects() {
        List<Project> list = new ArrayList<>();
        List<String> seenNames = new ArrayList<>();

        // Check primary and alternate roots
        File[] roots = new File[]{
            getProjectsRoot(),
            new File(Environment.getExternalStorageDirectory(), ALT_FOLDER_NAME)
        };

        for (File root : roots) {
            if (root == null || !root.exists()) continue;
            File[] files = root.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory() && !f.getName().equals("Exports") && !f.getName().startsWith(".") && !seenNames.contains(f.getName())) {
                        File manifest = new File(f, "app/src/main/AndroidManifest.xml");
                        String pkg = "com.my.app";
                        int minSdk = 21;
                        int targetSdk = 34;
                        String template = "Empty Activity";

                        // Read app_config.json if available
                        File appConfig = new File(f, "app/app_config.json");
                        if (appConfig.exists()) {
                            try {
                                String cfgStr = FileUtils.readFile(appConfig);
                                org.json.JSONObject cfg = new org.json.JSONObject(cfgStr);
                                pkg = cfg.optString("package", pkg);
                                minSdk = cfg.optInt("minSdkVersion", minSdk);
                                targetSdk = cfg.optInt("targetSdkVersion", targetSdk);
                                template = cfg.optString("template", template);
                            } catch (Exception ignored) {}
                        } else if (manifest.exists()) {
                            try {
                                String content = FileUtils.readFile(manifest);
                                int idx = content.indexOf("package=\"");
                                if (idx != -1) {
                                    int end = content.indexOf("\"", idx + 9);
                                    if (end != -1) {
                                        pkg = content.substring(idx + 9, end);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        seenNames.add(f.getName());
                        list.add(new Project(f.getName(), pkg, f.getAbsolutePath(), minSdk, targetSdk, template));
                    }
                }
            }
        }
        return list;
    }

    public boolean deleteProject(Project project) {
        if (project == null || project.getRootDirectory() == null) return false;
        return FileUtils.deleteDirectory(project.getRootDirectory());
    }

    public static Project createProject(String name, String packageName, String templateType, int minSdk, int targetSdk, Bitmap customIcon) throws IOException {
        File projectDir = new File(getProjectsRoot(), name);
        if (projectDir.exists()) {
            throw new IOException("A project with name '" + name + "' already exists!");
        }
        projectDir.mkdirs();

        // 1. Directory Structure
        File appDir = new File(projectDir, "app");
        String pkgPath = packageName.replace('.', '/');
        File javaDir = new File(appDir, "src/main/java/" + pkgPath);
        File layoutDir = new File(appDir, "src/main/res/layout");
        File valuesDir = new File(appDir, "src/main/res/values");
        File drawableDir = new File(appDir, "src/main/res/drawable");
        File libsDir = new File(appDir, "libs");
        File metaDir = new File(projectDir, ".build_studio");

        javaDir.mkdirs();
        layoutDir.mkdirs();
        valuesDir.mkdirs();
        drawableDir.mkdirs();
        libsDir.mkdirs();
        metaDir.mkdirs();

        // Save custom icon if provided
        if (customIcon != null) {
            File iconFile = new File(drawableDir, "ic_launcher.png");
            try (FileOutputStream out = new FileOutputStream(iconFile)) {
                customIcon.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception ignored) {}
        }

        // 2. AndroidManifest.xml
        boolean hasActivity = !"No Activity".equalsIgnoreCase(templateType);
        StringBuilder manifest = new StringBuilder();
        manifest.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        manifest.append("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n");
        manifest.append("    package=\"").append(packageName).append("\">\n\n");
        manifest.append("    <application\n");
        manifest.append("        android:allowBackup=\"true\"\n");
        manifest.append("        android:label=\"").append(name).append("\"\n");
        manifest.append("        android:icon=\"@drawable/ic_launcher\"\n");
        manifest.append("        android:theme=\"@style/AppTheme\">\n");

        if (hasActivity) {
            manifest.append("        <activity\n");
            manifest.append("            android:name=\".MainActivity\"\n");
            manifest.append("            android:exported=\"true\">\n");
            manifest.append("            <intent-filter>\n");
            manifest.append("                <action android:name=\"android.intent.action.MAIN\" />\n");
            manifest.append("                <category android:name=\"android.intent.category.LAUNCHER\" />\n");
            manifest.append("            </intent-filter>\n");
            manifest.append("        </activity>\n");
        }
        manifest.append("    </application>\n");
        manifest.append("</manifest>\n");
        FileUtils.writeFile(new File(appDir, "src/main/AndroidManifest.xml"), manifest.toString());

        // 3. Layouts & Code based on template
        if ("No Activity".equalsIgnoreCase(templateType)) {
            generateNoActivity(javaDir, layoutDir, packageName, name);
        } else if ("Basic Activity".equalsIgnoreCase(templateType)) {
            generateBasicActivity(javaDir, layoutDir, packageName, name);
        } else if ("Compose Activity".equalsIgnoreCase(templateType)) {
            generateComposeActivity(javaDir, layoutDir, packageName, name);
        } else if ("SurfaceView Game".equalsIgnoreCase(templateType)) {
            generateSurfaceViewGame(javaDir, layoutDir, packageName, name);
        } else if ("Canvas 2D Game".equalsIgnoreCase(templateType)) {
            generateCanvasGame(javaDir, layoutDir, packageName, name);
        } else if ("Bottom Nav".equalsIgnoreCase(templateType)) {
            generateBottomNavApp(javaDir, layoutDir, packageName, name);
        } else {
            generateEmptyApp(javaDir, layoutDir, packageName, name);
        }

        // 4. Values (strings, colors, styles)
        String strings = "<resources>\n    <string name=\"app_name\">" + name + "</string>\n</resources>\n";
        FileUtils.writeFile(new File(valuesDir, "strings.xml"), strings);

        String colors = "<resources>\n" +
                "    <color name=\"colorPrimary\">#5844ED</color>\n" +
                "    <color name=\"colorPrimaryDark\">#4330D0</color>\n" +
                "    <color name=\"colorAccent\">#00BCD4</color>\n" +
                "</resources>\n";
        FileUtils.writeFile(new File(valuesDir, "colors.xml"), colors);

        String styles = "<resources>\n" +
                "    <style name=\"AppTheme\" parent=\"android:Theme.Material.Light.NoActionBar\">\n" +
                "        <item name=\"android:colorPrimary\">@color/colorPrimary</item>\n" +
                "        <item name=\"android:colorPrimaryDark\">@color/colorPrimaryDark</item>\n" +
                "        <item name=\"android:colorAccent\">@color/colorAccent</item>\n" +
                "    </style>\n" +
                "</resources>\n";
        FileUtils.writeFile(new File(valuesDir, "styles.xml"), styles);

        // 5. Build Gradle files
        String rootGradle = "buildscript {\n    repositories {\n        google()\n        mavenCentral()\n    }\n}\nallprojects {\n    repositories {\n        google()\n        mavenCentral()\n    }\n}\n";
        FileUtils.writeFile(new File(projectDir, "build.gradle"), rootGradle);
        FileUtils.writeFile(new File(projectDir, "settings.gradle"), "include ':app'\n");

        // 6. Metadata
        String appConfig = "{\n  \"minSdkVersion\": " + minSdk + ",\n  \"package\": \"" + packageName + "\",\n  \"targetSdkVersion\": " + targetSdk + ",\n  \"template\": \"" + templateType + "\",\n  \"versionName\": \"1.0\",\n  \"versionCode\": 1\n}\n";
        FileUtils.writeFile(new File(appDir, "app_config.json"), appConfig);

        String openedPath = hasActivity
                ? new File(javaDir, "MainActivity.java").getAbsolutePath()
                : new File(appDir, "src/main/AndroidManifest.xml").getAbsolutePath();
        String opened = "[{\"path\": \"" + openedPath + "\"}]\n";
        FileUtils.writeFile(new File(projectDir, "editorOpened.json"), opened);
        FileUtils.writeFile(new File(metaDir, "ai_chat_history.json"), "[]\n");

        return new Project(name, packageName, projectDir.getAbsolutePath(), minSdk, targetSdk, templateType);
    }

    private static void generateNoActivity(File javaDir, File layoutDir, String pkg, String name) {
        // No Activity template: minimal project without an Activity
        String placeholder = "// Minimal project without an Activity\n// Add services, receivers, or application components here.\n";
        FileUtils.writeFile(new File(javaDir, "Placeholder.txt"), placeholder);
    }

    private static void generateBasicActivity(File javaDir, File layoutDir, String pkg, String name) {
        String layoutXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    android:background=\"#F8F9FA\">\n\n" +
                "    <!-- Top Toolbar Header -->\n" +
                "    <LinearLayout\n" +
                "        android:id=\"@+id/app_bar\"\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"56dp\"\n" +
                "        android:background=\"#5844ED\"\n" +
                "        android:gravity=\"center_vertical\"\n" +
                "        android:paddingHorizontal=\"16dp\">\n" +
                "        <TextView\n" +
                "            android:layout_width=\"wrap_content\"\n" +
                "            android:layout_height=\"wrap_content\"\n" +
                "            android:text=\"" + name + "\"\n" +
                "            android:textColor=\"#FFFFFF\"\n" +
                "            android:textSize=\"18sp\"\n" +
                "            android:textStyle=\"bold\" />\n" +
                "    </LinearLayout>\n\n" +
                "    <!-- Main Content -->\n" +
                "    <LinearLayout\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_centerInParent=\"true\"\n" +
                "        android:gravity=\"center\"\n" +
                "        android:orientation=\"vertical\">\n" +
                "        <TextView\n" +
                "            android:layout_width=\"wrap_content\"\n" +
                "            android:layout_height=\"wrap_content\"\n" +
                "            android:text=\"Basic Activity\"\n" +
                "            android:textColor=\"#111827\"\n" +
                "            android:textSize=\"22sp\"\n" +
                "            android:textStyle=\"bold\" />\n" +
                "        <TextView\n" +
                "            android:layout_width=\"wrap_content\"\n" +
                "            android:layout_height=\"wrap_content\"\n" +
                "            android:layout_marginTop=\"8dp\"\n" +
                "            android:text=\"Includes App Bar and Floating Action Button\"\n" +
                "            android:textColor=\"#6B7280\"\n" +
                "            android:textSize=\"14sp\" />\n" +
                "    </LinearLayout>\n\n" +
                "    <!-- Action Button (FAB Style) -->\n" +
                "    <Button\n" +
                "        android:id=\"@+id/btn_action\"\n" +
                "        android:layout_width=\"56dp\"\n" +
                "        android:layout_height=\"56dp\"\n" +
                "        android:layout_alignParentEnd=\"true\"\n" +
                "        android:layout_alignParentBottom=\"true\"\n" +
                "        android:layout_margin=\"24dp\"\n" +
                "        android:background=\"#5844ED\"\n" +
                "        android:text=\"+\"\n" +
                "        android:textColor=\"#FFFFFF\"\n" +
                "        android:textSize=\"24sp\" />\n" +
                "</RelativeLayout>\n";
        FileUtils.writeFile(new File(layoutDir, "activity_main.xml"), layoutXml);

        String javaCode = "package " + pkg + ";\n\n" +
                "import android.app.Activity;\n" +
                "import android.os.Bundle;\n" +
                "import android.view.View;\n" +
                "import android.widget.Button;\n" +
                "import android.widget.Toast;\n\n" +
                "public class MainActivity extends Activity {\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n\n" +
                "        Button fab = findViewById(R.id.btn_action);\n" +
                "        if (fab != null) {\n" +
                "            fab.setOnClickListener(new View.OnClickListener() {\n" +
                "                @Override\n" +
                "                public void onClick(View v) {\n" +
                "                    Toast.makeText(MainActivity.this, \"Floating action clicked!\", Toast.LENGTH_SHORT).show();\n" +
                "                }\n" +
                "            });\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        FileUtils.writeFile(new File(javaDir, "MainActivity.java"), javaCode);
    }

    private static void generateComposeActivity(File javaDir, File layoutDir, String pkg, String name) {
        String layoutXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    android:background=\"#F8F9FA\"\n" +
                "    android:orientation=\"vertical\"\n" +
                "    android:padding=\"20dp\">\n\n" +
                "    <TextView\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:text=\"Compose Equivalent UI\"\n" +
                "        android:textColor=\"#5844ED\"\n" +
                "        android:textSize=\"22sp\"\n" +
                "        android:textStyle=\"bold\" />\n\n" +
                "    <TextView\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_marginTop=\"8dp\"\n" +
                "        android:text=\"Java Material 3 Component Architecture\"\n" +
                "        android:textColor=\"#6B7280\"\n" +
                "        android:textSize=\"14sp\" />\n\n" +
                "    <Button\n" +
                "        android:id=\"@+id/btn_compose_action\"\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"48dp\"\n" +
                "        android:layout_marginTop=\"24dp\"\n" +
                "        android:background=\"#5844ED\"\n" +
                "        android:text=\"Material Action\"\n" +
                "        android:textColor=\"#FFFFFF\" />\n" +
                "</LinearLayout>\n";
        FileUtils.writeFile(new File(layoutDir, "activity_main.xml"), layoutXml);

        String javaCode = "package " + pkg + ";\n\n" +
                "import android.app.Activity;\n" +
                "import android.os.Bundle;\n" +
                "import android.view.View;\n" +
                "import android.widget.Button;\n" +
                "import android.widget.Toast;\n\n" +
                "/**\n" +
                " * Modern Java Material 3 Component Activity.\n" +
                " * (Note: Pure Kotlin Compose is not used here; this project is 100% Java-compatible).\n" +
                " */\n" +
                "public class MainActivity extends Activity {\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n\n" +
                "        Button btn = findViewById(R.id.btn_compose_action);\n" +
                "        if (btn != null) {\n" +
                "            btn.setOnClickListener(new View.OnClickListener() {\n" +
                "                @Override\n" +
                "                public void onClick(View v) {\n" +
                "                    Toast.makeText(MainActivity.this, \"Component tapped!\", Toast.LENGTH_SHORT).show();\n" +
                "                }\n" +
                "            });\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        FileUtils.writeFile(new File(javaDir, "MainActivity.java"), javaCode);
    }

    private static void generateEmptyApp(File javaDir, File layoutDir, String pkg, String name) {
        String layoutXml = "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    android:gravity=\"center\"\n" +
                "    android:orientation=\"vertical\">\n\n" +
                "    <TextView\n" +
                "        android:id=\"@+id/tv_greeting\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:text=\"Hello from " + name + "!\"\n" +
                "        android:textSize=\"20sp\" />\n" +
                "</LinearLayout>\n";
        FileUtils.writeFile(new File(layoutDir, "activity_main.xml"), layoutXml);

        String javaCode = "package " + pkg + ";\n\n" +
                "import android.app.Activity;\n" +
                "import android.os.Bundle;\n" +
                "import android.widget.TextView;\n\n" +
                "public class MainActivity extends Activity {\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n" +
                "    }\n" +
                "}\n";
        FileUtils.writeFile(new File(javaDir, "MainActivity.java"), javaCode);
    }

    private static void generateBottomNavApp(File javaDir, File layoutDir, String pkg, String name) {
        String layoutXml = "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\">\n\n" +
                "    <TextView\n" +
                "        android:id=\"@+id/tv_content\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_centerInParent=\"true\"\n" +
                "        android:text=\"Home Tab\"\n" +
                "        android:textSize=\"22sp\" />\n\n" +
                "    <LinearLayout\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"56dp\"\n" +
                "        android:layout_alignParentBottom=\"true\"\n" +
                "        android:background=\"#EEEEEE\"\n" +
                "        android:orientation=\"horizontal\">\n\n" +
                "        <Button\n" +
                "            android:id=\"@+id/btn_tab1\"\n" +
                "            android:layout_width=\"0dp\"\n" +
                "            android:layout_height=\"match_parent\"\n" +
                "            android:layout_weight=\"1\"\n" +
                "            android:text=\"Home\" />\n\n" +
                "        <Button\n" +
                "            android:id=\"@+id/btn_tab2\"\n" +
                "            android:layout_width=\"0dp\"\n" +
                "            android:layout_height=\"match_parent\"\n" +
                "            android:layout_weight=\"1\"\n" +
                "            android:text=\"Explore\" />\n\n" +
                "        <Button\n" +
                "            android:id=\"@+id/btn_tab3\"\n" +
                "            android:layout_width=\"0dp\"\n" +
                "            android:layout_height=\"match_parent\"\n" +
                "            android:layout_weight=\"1\"\n" +
                "            android:text=\"Profile\" />\n" +
                "    </LinearLayout>\n" +
                "</RelativeLayout>\n";
        FileUtils.writeFile(new File(layoutDir, "activity_main.xml"), layoutXml);

        String javaCode = "package " + pkg + ";\n\n" +
                "import android.app.Activity;\n" +
                "import android.os.Bundle;\n" +
                "import android.widget.Button;\n" +
                "import android.widget.TextView;\n\n" +
                "public class MainActivity extends Activity {\n" +
                "    private TextView tvContent;\n\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n\n" +
                "        tvContent = findViewById(R.id.tv_content);\n" +
                "        findViewById(R.id.btn_tab1).setOnClickListener(v -> tvContent.setText(\"Home Tab\"));\n" +
                "        findViewById(R.id.btn_tab2).setOnClickListener(v -> tvContent.setText(\"Explore Tab\"));\n" +
                "        findViewById(R.id.btn_tab3).setOnClickListener(v -> tvContent.setText(\"Profile Tab\"));\n" +
                "    }\n" +
                "}\n";
        FileUtils.writeFile(new File(javaDir, "MainActivity.java"), javaCode);
    }

    private static void generateSurfaceViewGame(File javaDir, File layoutDir, String pkg, String name) {
        String layoutXml = "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:id=\"@+id/game_container\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\" />\n";
        FileUtils.writeFile(new File(layoutDir, "activity_main.xml"), layoutXml);

        // MainActivity
        String mainJava = "package " + pkg + ";\n\n" +
                "import android.app.Activity;\n" +
                "import android.os.Bundle;\n" +
                "import android.view.Window;\n" +
                "import android.view.WindowManager;\n" +
                "import android.widget.FrameLayout;\n\n" +
                "public class MainActivity extends Activity {\n" +
                "    private GameSurfaceView gameView;\n\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        requestWindowFeature(Window.FEATURE_NO_TITLE);\n" +
                "        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);\n" +
                "        setContentView(R.layout.activity_main);\n\n" +
                "        FrameLayout container = findViewById(R.id.game_container);\n" +
                "        gameView = new GameSurfaceView(this);\n" +
                "        container.addView(gameView);\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    protected void onPause() {\n" +
                "        super.onPause();\n" +
                "        gameView.pause();\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    protected void onResume() {\n" +
                "        super.onResume();\n" +
                "        gameView.resume();\n" +
                "    }\n" +
                "}\n";
        FileUtils.writeFile(new File(javaDir, "MainActivity.java"), mainJava);

        // GameSurfaceView with 60FPS thread loop, touch input, and animated sprite
        String gameViewJava = "package " + pkg + ";\n\n" +
                "import android.content.Context;\n" +
                "import android.graphics.Canvas;\n" +
                "import android.graphics.Color;\n" +
                "import android.graphics.Paint;\n" +
                "import android.view.MotionEvent;\n" +
                "import android.view.SurfaceHolder;\n" +
                "import android.view.SurfaceView;\n\n" +
                "public class GameSurfaceView extends SurfaceView implements Runnable {\n" +
                "    private Thread gameThread;\n" +
                "    private volatile boolean isPlaying = false;\n" +
                "    private SurfaceHolder surfaceHolder;\n" +
                "    private Paint paint;\n" +
                "    private float ballX = 200, ballY = 400;\n" +
                "    private float velX = 12, velY = 12;\n" +
                "    private float radius = 50;\n\n" +
                "    public GameSurfaceView(Context context) {\n" +
                "        super(context);\n" +
                "        surfaceHolder = getHolder();\n" +
                "        paint = new Paint(Paint.ANTI_ALIAS_FLAG);\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void run() {\n" +
                "        while (isPlaying) {\n" +
                "            update();\n" +
                "            draw();\n" +
                "            sleep();\n" +
                "        }\n" +
                "    }\n\n" +
                "    private void update() {\n" +
                "        ballX += velX;\n" +
                "        ballY += velY;\n" +
                "        if (ballX - radius < 0 || ballX + radius > getWidth()) velX = -velX;\n" +
                "        if (ballY - radius < 0 || ballY + radius > getHeight()) velY = -velY;\n" +
                "    }\n\n" +
                "    private void draw() {\n" +
                "        if (surfaceHolder.getSurface().isValid()) {\n" +
                "            Canvas canvas = surfaceHolder.lockCanvas();\n" +
                "            canvas.drawColor(Color.parseColor(\"#121218\"));\n" +
                "            paint.setColor(Color.parseColor(\"#5844ED\"));\n" +
                "            canvas.drawCircle(ballX, ballY, radius, paint);\n" +
                "            paint.setColor(Color.WHITE);\n" +
                "            paint.setTextSize(40);\n" +
                "            canvas.drawText(\"SurfaceView 60FPS Game Loop\", 50, 100, paint);\n" +
                "            surfaceHolder.unlockCanvasAndPost(canvas);\n" +
                "        }\n" +
                "    }\n\n" +
                "    private void sleep() {\n" +
                "        try { Thread.sleep(16); } catch (InterruptedException ignored) {}\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public boolean onTouchEvent(MotionEvent event) {\n" +
                "        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {\n" +
                "            ballX = event.getX();\n" +
                "            ballY = event.getY();\n" +
                "        }\n" +
                "        return true;\n" +
                "    }\n\n" +
                "    public void resume() {\n" +
                "        isPlaying = true;\n" +
                "        gameThread = new Thread(this);\n" +
                "        gameThread.start();\n" +
                "    }\n\n" +
                "    public void pause() {\n" +
                "        isPlaying = false;\n" +
                "        try { gameThread.join(); } catch (InterruptedException ignored) {}\n" +
                "    }\n" +
                "}\n";
        FileUtils.writeFile(new File(javaDir, "GameSurfaceView.java"), gameViewJava);
    }

    private static void generateCanvasGame(File javaDir, File layoutDir, String pkg, String name) {
        String layoutXml = "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    android:orientation=\"vertical\">\n\n" +
                "    <view class=\"" + pkg + ".GameCanvasView\"\n" +
                "        android:id=\"@+id/canvas_view\"\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"match_parent\" />\n" +
                "</LinearLayout>\n";
        FileUtils.writeFile(new File(layoutDir, "activity_main.xml"), layoutXml);

        String mainJava = "package " + pkg + ";\n\n" +
                "import android.app.Activity;\n" +
                "import android.os.Bundle;\n\n" +
                "public class MainActivity extends Activity {\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n" +
                "    }\n" +
                "}\n";
        FileUtils.writeFile(new File(javaDir, "MainActivity.java"), mainJava);

        String canvasViewJava = "package " + pkg + ";\n\n" +
                "import android.content.Context;\n" +
                "import android.graphics.Canvas;\n" +
                "import android.graphics.Color;\n" +
                "import android.graphics.Paint;\n" +
                "import android.util.AttributeSet;\n" +
                "import android.view.MotionEvent;\n" +
                "import android.view.View;\n\n" +
                "public class GameCanvasView extends View {\n" +
                "    private Paint paint;\n" +
                "    private float touchX = 300, touchY = 400;\n\n" +
                "    public GameCanvasView(Context context, AttributeSet attrs) {\n" +
                "        super(context, attrs);\n" +
                "        paint = new Paint(Paint.ANTI_ALIAS_FLAG);\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    protected void onDraw(Canvas canvas) {\n" +
                "        super.onDraw(canvas);\n" +
                "        canvas.drawColor(Color.parseColor(\"#1E1E2E\"));\n" +
                "        paint.setColor(Color.parseColor(\"#00BCD4\"));\n" +
                "        canvas.drawCircle(touchX, touchY, 60, paint);\n" +
                "        paint.setColor(Color.WHITE);\n" +
                "        paint.setTextSize(36);\n" +
                "        canvas.drawText(\"Canvas 2D Game: Tap to Move\", 60, 120, paint);\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public boolean onTouchEvent(MotionEvent event) {\n" +
                "        touchX = event.getX();\n" +
                "        touchY = event.getY();\n" +
                "        invalidate();\n" +
                "        return true;\n" +
                "    }\n" +
                "}\n";
        FileUtils.writeFile(new File(javaDir, "GameCanvasView.java"), canvasViewJava);
    }
}
