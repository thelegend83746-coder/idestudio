package com.ide.studio.compiler;

import android.content.Context;
import com.ide.studio.model.Project;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Strict validator for compiler toolchain binaries, platform JARs,
 * and project source integrity. Reports exact missing files and tools.
 */
public class ToolchainValidator {

    public static class ValidationResult {
        public boolean isValid;
        public List<String> errors;

        public ValidationResult(boolean isValid, List<String> errors) {
            this.isValid = isValid;
            this.errors = errors;
        }
    }

    public static ValidationResult validate(Context context) {
        return validateToolchain(context, 33);
    }

    public static ValidationResult validateProject(Project project) {
        List<String> errors = new ArrayList<>();

        if (project == null || project.getProjectDir() == null) {
            errors.add("[Compiler] Error: Project handle is null.");
            return new ValidationResult(false, errors);
        }

        if (!project.getProjectDir().exists()) {
            errors.add("[Compiler] Error: Project directory does not exist at: " + project.getPath());
            return new ValidationResult(false, errors);
        }

        if (!project.getManifestFile().exists()) {
            errors.add("[Compiler] Error: Missing AndroidManifest.xml at: app/src/main/AndroidManifest.xml");
        }

        File srcDir = new File(project.getProjectDir(), "app/src/main/java");
        if (!srcDir.exists() || !hasJavaFiles(srcDir)) {
            errors.add("[Compiler] Error: No Java source files found in app/src/main/java/");
        }

        File resDir = new File(project.getProjectDir(), "app/src/main/res");
        if (!resDir.exists()) {
            errors.add("[Compiler] Error: Resources directory 'res' not found in app/src/main/");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public static ValidationResult validateToolchain(Context context, int targetSdk) {
        List<String> errors = new ArrayList<>();

        // 1. AAPT2 validation
        File nativeAapt2 = new File(context.getApplicationInfo().nativeLibraryDir, "libaapt2.so");
        boolean aaptFound = nativeAapt2.exists();
        if (!aaptFound) {
            // Also check internal files dir
            File binAapt2 = new File(context.getFilesDir(), "bin/aapt2");
            aaptFound = binAapt2.exists();
        }
        if (!aaptFound) {
            errors.add("[AAPT2] Error: libaapt2.so missing for current architecture (" + android.os.Build.CPU_ABI + "). Cannot compile Android XML resources.");
        }

        // 2. ECJ & D8 Compiler Jar (cp.jar) validation
        File cpJar = new File(context.getFilesDir(), "bin/cp.jar");
        boolean cpJarFound = cpJar.exists();
        if (!cpJarFound) {
            try (InputStream is = context.getAssets().open("cp.jar")) {
                cpJarFound = true;
            } catch (Exception e) {
                cpJarFound = false;
            }
        }
        if (!cpJarFound) {
            errors.add("[Compiler] Error: cp.jar (ECJ 4.6.1 + D8 Dexer + ApkSigner) is missing. Java source compilation and dexing unavailable.");
        }

        // 3. Android SDK platform jar (android.jar) validation
        File platformsDir = new File(context.getFilesDir(), "platforms/android-" + targetSdk);
        File androidJar = new File(platformsDir, "android.jar");
        boolean jarFound = androidJar.exists();
        if (!jarFound) {
            try (InputStream is = context.getAssets().open("platforms/android-" + targetSdk + "/android.jar")) {
                jarFound = true;
            } catch (Exception e) {
                jarFound = false;
            }
        }
        if (!jarFound) {
            errors.add("[Compiler] Warning/Error: android.jar for API " + targetSdk + " missing locally. Will require network download from Google SDK repository.");
        }

        // 4. Signing Keys validation
        boolean keyFound = false;
        try (InputStream is = context.getAssets().open("keys/testkey.pk8")) {
            keyFound = true;
        } catch (Exception ignored) {}
        if (!keyFound) {
            File keyFile = new File(context.getFilesDir(), "keys/testkey.pk8");
            if (!keyFile.exists()) {
                errors.add("[ApkSigner] Error: Default testkey.pk8 is missing. APK cannot be signed.");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public static boolean isSdkAvailable(Context context, int targetSdk) {
        File platformsDir = new File(context.getFilesDir(), "platforms/android-" + targetSdk);
        File androidJar = new File(platformsDir, "android.jar");
        if (androidJar.exists()) return true;
        try (InputStream is = context.getAssets().open("platforms/android-" + targetSdk + "/android.jar")) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getSdkStatus(Context context, int targetSdk) {
        if (isSdkAvailable(context, targetSdk)) {
            return "Installed & Ready (API " + targetSdk + ")";
        } else {
            return "SDK not installed / unavailable";
        }
    }


    private static boolean hasJavaFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (f.isDirectory() && hasJavaFiles(f)) return true;
            if (f.getName().endsWith(".java")) return true;
        }
        return false;
    }
}
