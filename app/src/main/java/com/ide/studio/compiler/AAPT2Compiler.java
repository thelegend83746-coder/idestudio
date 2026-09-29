package com.ide.studio.compiler;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Exact AAPT2Compiler matching BUILD STUDIO compiler engine.
 */
public class AAPT2Compiler {

    public interface CompilerListener {
        void onProgress(String message);
        void onLog(String log);
    }

    private final Context context;
    private File aapt2File;

    public AAPT2Compiler(Context context) {
        this.context = context;
        resolveBinary();
    }

    private void resolveBinary() {
        // Native lib directory check
        File nativeLib = new File(context.getApplicationInfo().nativeLibraryDir, "libaapt2.so");
        if (nativeLib.exists()) {
            this.aapt2File = nativeLib;
            return;
        }

        // App internal files check
        File binDir = new File(context.getFilesDir(), "bin");
        binDir.mkdirs();
        File f = new File(binDir, "libaapt2.so");
        if (f.exists()) {
            f.setExecutable(true, false);
            this.aapt2File = f;
        }
    }

    public File getAAPT2File() {
        return aapt2File;
    }

    public boolean compileResources(File resDir, File outputZip, CompilerListener listener) {
        if (aapt2File == null || !aapt2File.exists()) {
            if (listener != null) listener.onLog("AAPT2 binary not found.");
            return false;
        }

        if (listener != null) {
            listener.onProgress("AAPT2 > Compiling resources");
            listener.onLog("AAPT2 > Compiling resources...");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(aapt2File.getAbsolutePath());
        cmd.add("compile");
        cmd.add("--dir");
        cmd.add(resDir.getAbsolutePath());
        cmd.add("-o");
        cmd.add(outputZip.getAbsolutePath());

        return runCommand(cmd, listener);
    }

    public boolean linkResources(File androidJar, File manifestFile, File compiledZip,
                                 File genDir, File outputApk, int minSdk, int targetSdk, CompilerListener listener) {
        if (aapt2File == null || !aapt2File.exists()) {
            if (listener != null) listener.onLog("AAPT2 binary not found.");
            return false;
        }

        if (listener != null) {
            listener.onProgress("AAPT2 > Linking resources");
            listener.onLog("AAPT2 > Linking resources...");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(aapt2File.getAbsolutePath());
        cmd.add("link");
        cmd.add("-I");
        cmd.add(androidJar.getAbsolutePath());
        cmd.add("--manifest");
        cmd.add(manifestFile.getAbsolutePath());
        cmd.add("--java");
        cmd.add(genDir.getAbsolutePath());
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

        return runCommand(cmd, listener);
    }

    private boolean runCommand(List<String> cmd, CompilerListener listener) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (listener != null) listener.onLog("AAPT2: " + line);
                }
            }

            int exit = p.waitFor();
            return exit == 0;
        } catch (Exception e) {
            if (listener != null) listener.onLog("AAPT2 error: " + e.getMessage());
            return false;
        }
    }
}
