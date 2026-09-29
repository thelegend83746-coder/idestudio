package com.ide.studio.compiler;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-level compiler task manager for building Android APKs with fine-grained callbacks.
 */
public class CompilerManager {

    public interface BuildListener {
        void onStepChanged(String stepName, int currentStep, int totalSteps);
        void onLog(String logLine);
        void onBuildSuccess(File signedApk);
        void onBuildFailed(String failedStep, String fullLog);
    }

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final StringBuilder logAccumulator = new StringBuilder();

    public CompilerManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void buildProject(File projectRootDir, int minSdk, int targetSdk, BuildListener listener) {
        executor.execute(() -> {
            logAccumulator.setLength(0);

            try {
                File appDir = new File(projectRootDir, "app");
                File mainDir = new File(appDir, "src/main");
                File resDir = new File(mainDir, "res");
                File manifest = new File(mainDir, "AndroidManifest.xml");
                File javaDir = new File(mainDir, "java");

                File buildDir = new File(appDir, "build");
                File genDir = new File(buildDir, "gen");
                File intermediates = new File(buildDir, "intermediates");
                File classesDir = new File(intermediates, "classes");
                File dexDir = new File(intermediates, "dex");
                File binDir = new File(buildDir, "bin");

                genDir.mkdirs();
                classesDir.mkdirs();
                dexDir.mkdirs();
                binDir.mkdirs();

                // Step 1: Pre-validation
                postStep(listener, "Validating Project", 1, 6);
                if (!manifest.exists()) {
                    throw new IllegalStateException("AndroidManifest.xml not found at " + manifest.getAbsolutePath());
                }

                // Step 2: AAPT2
                postStep(listener, "Compiling Resources (AAPT2)", 2, 6);
                File aapt2Bin = AaptManager.getAapt2Binary(context);
                File compiledRes = new File(intermediates, "compiled_res.zip");
                File resourcesApk = new File(intermediates, "resources.ap_");

                File androidJar = new File(context.getFilesDir(), "platforms/android-" + targetSdk + "/android.jar");

                AaptManager.compileResources(aapt2Bin, resDir, compiledRes, line -> postLog(listener, line));
                AaptManager.linkResources(aapt2Bin, androidJar, manifest, compiledRes, genDir, resourcesApk, minSdk, targetSdk, line -> postLog(listener, line));

                // Step 3: ECJ Java
                postStep(listener, "Compiling Java Sources (ECJ)", 3, 6);
                List<File> srcDirs = new ArrayList<>();
                srcDirs.add(javaDir);
                srcDirs.add(genDir);
                JavaCompiler.compile(context, androidJar, srcDirs, Collections.emptyList(), classesDir, line -> postLog(listener, line));

                // Step 4: D8 Dex
                postStep(listener, "Dexing Bytecode (D8)", 4, 6);
                DexCompiler.dex(context, androidJar, classesDir, Collections.emptyList(), dexDir, minSdk, line -> postLog(listener, line));

                // Step 5: Package & Sign
                postStep(listener, "Signing APK", 5, 6);
                File unalignedApk = new File(binDir, projectRootDir.getName() + "-unaligned.apk");
                ApkSignerTool.packageAndSign(context, resourcesApk, dexDir, unalignedApk, line -> postLog(listener, line));

                // Step 6: ZipAlign
                postStep(listener, "Aligning APK (ZipAlign)", 6, 6);
                File finalApk = new File(binDir, projectRootDir.getName() + "-debug.apk");
                ZipAlignTool.align(unalignedApk, finalApk, 4, line -> postLog(listener, line));
                if (unalignedApk.exists()) unalignedApk.delete();

                postSuccess(listener, finalApk);

            } catch (Exception e) {
                postFailed(listener, "Build Error", e.getMessage());
            }
        });
    }

    private void postStep(BuildListener listener, String name, int step, int total) {
        logAccumulator.append("[").append(step).append("/").append(total).append("] ").append(name).append("\n");
        if (listener != null) {
            mainHandler.post(() -> listener.onStepChanged(name, step, total));
        }
    }

    private void postLog(BuildListener listener, String line) {
        logAccumulator.append(line).append("\n");
        if (listener != null) {
            mainHandler.post(() -> listener.onLog(line));
        }
    }

    private void postSuccess(BuildListener listener, File apk) {
        if (listener != null) {
            mainHandler.post(() -> listener.onBuildSuccess(apk));
        }
    }

    private void postFailed(BuildListener listener, String step, String msg) {
        if (listener != null) {
            mainHandler.post(() -> listener.onBuildFailed(step, logAccumulator.toString() + "\n" + msg));
        }
    }
}
