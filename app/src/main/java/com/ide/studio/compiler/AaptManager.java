package com.ide.studio.compiler;

import android.content.Context;
import android.os.Build;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * AAPT2 Resource Compilation and Linking Manager.
 * Extracts architecture-specific libaapt2.so, compiles raw XML/drawables
 * into flat zip archives, and links them against android.jar to generate R.java
 * and packaged resources (resources.ap_).
 */
public class AaptManager {

    public interface LogCallback {
        void onLog(String line);
    }

    /**
     * Resolves the executable path of AAPT2.
     * Checks native library directory, then app files directory.
     */
    public static File getAapt2Binary(Context context) {
        // 1. Check native library directory (libaapt2.so installed by APK)
        File nativeLib = new File(context.getApplicationInfo().nativeLibraryDir, "libaapt2.so");
        if (nativeLib.exists() && nativeLib.canExecute()) {
            return nativeLib;
        }

        // 2. Check internal files directory (bin/aapt2)
        File binDir = new File(context.getFilesDir(), "bin");
        binDir.mkdirs();
        File aapt2File = new File(binDir, "aapt2");

        if (!aapt2File.exists() || aapt2File.length() == 0) {
            extractAapt2FromAssets(context, aapt2File);
        }

        if (aapt2File.exists()) {
            aapt2File.setExecutable(true, false);
            aapt2File.setReadable(true, false);
            return aapt2File;
        }

        return nativeLib.exists() ? nativeLib : null;
    }

    private static boolean extractAapt2FromAssets(Context context, File target) {
        String abi = Build.SUPPORTED_ABIS[0];
        String assetPath = "jniLibs/" + abi + "/libaapt2.so";
        try (InputStream is = context.getAssets().open(assetPath);
             OutputStream os = new FileOutputStream(target)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            target.setExecutable(true, false);
            return true;
        } catch (Exception ignored) {
            // Fallback to arm64-v8a or armeabi-v7a
            try (InputStream is = context.getAssets().open("jniLibs/arm64-v8a/libaapt2.so");
                 OutputStream os = new FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) > 0) {
                    os.write(buf, 0, len);
                }
                target.setExecutable(true, false);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    /**
     * Stage 1: Compiles resources in res/ to compiled_res.zip
     */
    public static boolean compileResources(File aapt2Bin, File resDir, File outputZip, LogCallback logger) {
        try {
            outputZip.getParentFile().mkdirs();
            List<String> cmd = new ArrayList<>();
            cmd.add(aapt2Bin.getAbsolutePath());
            cmd.add("compile");
            cmd.add("--dir");
            cmd.add(resDir.getAbsolutePath());
            cmd.add("-o");
            cmd.add(outputZip.getAbsolutePath());

            if (logger != null) {
                logger.onLog("[AAPT2] $ " + String.join(" ", cmd));
            }

            return runProcess(cmd, logger);
        } catch (Exception e) {
            if (logger != null) logger.onLog("[AAPT2] Error compiling resources: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stage 2: Links compiled resources with android.jar to generate R.java and packaged resources.ap_
     */
    public static boolean linkResources(File aapt2Bin, File androidJar, File manifestFile, File compiledZip,
                                         File genJavaDir, File outputApk, int minSdk, int targetSdk, LogCallback logger) {
        try {
            genJavaDir.mkdirs();
            outputApk.getParentFile().mkdirs();

            List<String> cmd = new ArrayList<>();
            cmd.add(aapt2Bin.getAbsolutePath());
            cmd.add("link");
            cmd.add("-I");
            cmd.add(androidJar.getAbsolutePath());
            cmd.add("--manifest");
            cmd.add(manifestFile.getAbsolutePath());
            cmd.add("--java");
            cmd.add(genJavaDir.getAbsolutePath());
            cmd.add("-o");
            cmd.add(outputApk.getAbsolutePath());
            cmd.add("--auto-add-overlay");
            if (minSdk > 0) {
                cmd.add("--min-sdk-version");
                cmd.add(String.valueOf(minSdk));
            }
            if (targetSdk > 0) {
                cmd.add("--target-sdk-version");
                cmd.add(String.valueOf(targetSdk));
            }
            cmd.add(compiledZip.getAbsolutePath());

            if (logger != null) {
                logger.onLog("[AAPT2] $ " + String.join(" ", cmd));
            }

            return runProcess(cmd, logger);
        } catch (Exception e) {
            if (logger != null) logger.onLog("[AAPT2] Error linking resources: " + e.getMessage());
            return false;
        }
    }

    private static boolean runProcess(List<String> cmd, LogCallback logger) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (logger != null) logger.onLog("[AAPT2] " + line);
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            if (logger != null) logger.onLog("[AAPT2] Execution exception: " + e.getMessage());
            return false;
        }
    }
}
