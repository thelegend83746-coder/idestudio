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
 * BuildPipelineService background service for running full compilation pipelines with live terminal streams.
 */
public class BuildPipelineService {

    public interface PipelineListener {
        void onStepStart(int stepIndex, int totalSteps, String stepTitle);
        void onLogMessage(String logLine);
        void onBuildComplete(File signedApk);
        void onBuildError(String failedStep, String errorLog);
    }

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final StringBuilder logAccumulator = new StringBuilder();

    public BuildPipelineService(Context context) {
        this.context = context.getApplicationContext();
    }

    public void startBuild(File projectRoot, int minSdk, int targetSdk, String javaLevel, PipelineListener listener) {
        executor.execute(() -> {
            logAccumulator.setLength(0);

            try {
                File appDir = new File(projectRoot, "app");
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

                postStep(listener, 1, 6, "Initializing Toolchain & Project Environment");
                postLog(listener, "[INFO] Project: " + projectRoot.getName());
                postLog(listener, "[INFO] Target SDK: " + targetSdk + " | Min SDK: " + minSdk);

                postStep(listener, 2, 6, "Compiling and Linking Resources (AAPT2)");
                File aapt2Bin = AaptManager.getAapt2Binary(context);
                File compiledRes = new File(intermediates, "compiled_res.zip");
                File resourcesApk = new File(intermediates, "resources.ap_");
                File androidJar = new File(context.getFilesDir(), "platforms/android-" + targetSdk + "/android.jar");

                AaptManager.compileResources(aapt2Bin, resDir, compiledRes, line -> postLog(listener, line));
                AaptManager.linkResources(aapt2Bin, androidJar, manifest, compiledRes, genDir, resourcesApk, minSdk, targetSdk, line -> postLog(listener, line));

                postStep(listener, 3, 6, "Compiling Java Sources (ECJ 4.6.1)");
                List<File> srcDirs = new ArrayList<>();
                srcDirs.add(javaDir);
                srcDirs.add(genDir);
                JavaCompiler.compile(context, androidJar, srcDirs, Collections.emptyList(), classesDir, line -> postLog(listener, line));

                postStep(listener, 4, 6, "Converting Bytecode to DEX (Google D8)");
                DexCompiler.dex(context, androidJar, classesDir, Collections.emptyList(), dexDir, minSdk, line -> postLog(listener, line));

                postStep(listener, 5, 6, "Signing APK (Android Debug Keystore)");
                File unalignedApk = new File(binDir, projectRoot.getName() + "-unaligned.apk");
                ApkSignerTool.packageAndSign(context, resourcesApk, dexDir, unalignedApk, line -> postLog(listener, line));

                postStep(listener, 6, 6, "ZipAlign Optimization");
                File finalApk = new File(binDir, projectRoot.getName() + "-debug.apk");
                ZipAlignTool.align(unalignedApk, finalApk, 4, line -> postLog(listener, line));
                if (unalignedApk.exists()) unalignedApk.delete();

                postComplete(listener, finalApk);

            } catch (Exception e) {
                postError(listener, "Build Failure", e.getMessage());
            }
        });
    }

    private void postStep(PipelineListener listener, int step, int total, String title) {
        logAccumulator.append("[").append(step).append("/").append(total).append("] ").append(title).append("\n");
        if (listener != null) {
            mainHandler.post(() -> listener.onStepStart(step, total, title));
        }
    }

    private void postLog(PipelineListener listener, String line) {
        logAccumulator.append(line).append("\n");
        if (listener != null) {
            mainHandler.post(() -> listener.onLogMessage(line));
        }
    }

    private void postComplete(PipelineListener listener, File apk) {
        if (listener != null) {
            mainHandler.post(() -> listener.onBuildComplete(apk));
        }
    }

    private void postError(PipelineListener listener, String step, String msg) {
        if (listener != null) {
            mainHandler.post(() -> listener.onBuildError(step, logAccumulator.toString() + "\n" + msg));
        }
    }
}
