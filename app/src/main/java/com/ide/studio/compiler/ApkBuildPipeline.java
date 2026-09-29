package com.ide.studio.compiler;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.core.content.FileProvider;
import com.ide.studio.model.Project;
import com.ide.studio.view.BuildLogDialog;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Complete Offline Android APK Build Pipeline.
 * Orchestrates:
 * 1. Project & toolchain validation (reports exact missing files)
 * 2. SDK android.jar platform setup
 * 3. AAPT2 resource compilation and linking (generates R.java & resources.ap_)
 * 4. ECJ Java source compilation (generates .class files)
 * 5. D8 DEX conversion (generates classes.dex)
 * 6. Packaging & ZipAlign
 * 7. APK Signing with Android Debug Scheme
 * 8. Automatic installation via FileProvider
 */
public class ApkBuildPipeline {

    public interface BuildCallback {
        void onComplete(boolean success, File apkFile);
    }

    public static void buildProject(Context context, Project project, BuildLogDialog logDialog, BuildCallback callback) {
        new Thread(() -> {
            boolean success = false;
            File finalApk = null;

            try {
                logDialog.appendLog("Starting build for project: " + project.getName());

                // ==========================================
                // STAGE 1: VALIDATE PROJECT INTEGRITY
                // ==========================================
                logDialog.appendLog("[Compiler] Validating project integrity...");
                ToolchainValidator.ValidationResult projCheck = ToolchainValidator.validateProject(project);
                if (!projCheck.isValid) {
                    for (String err : projCheck.errors) {
                        logDialog.appendLog(err);
                    }
                    logDialog.appendLog("Build FAILED at stage [VALIDATE].");
                    callback.onComplete(false, null);
                    return;
                }

                File projectDir = project.getProjectDir();
                File appDir = new File(projectDir, "app");
                File manifestFile = project.getManifestFile();
                File resDir = new File(appDir, "src/main/res");
                File javaDir = new File(appDir, "src/main/java");

                File buildDir = new File(appDir, "build");
                File genDir = new File(buildDir, "gen");
                File intermediatesDir = new File(buildDir, "intermediates");
                File classesDir = new File(intermediatesDir, "classes");
                File dexDir = new File(intermediatesDir, "dex");
                File binDir = new File(buildDir, "bin");

                genDir.mkdirs();
                classesDir.mkdirs();
                dexDir.mkdirs();
                binDir.mkdirs();

                // ==========================================
                // STAGE 1.5: XML SANITIZATION & EXPORTED ATTRIBUTE FIX
                // ==========================================
                logDialog.appendLog("[Validator] Validating and sanitizing XML files...");
                XmlSanitizer.sanitizeFile(manifestFile, project.getTargetSdk());
                XmlSanitizer.sanitizeDirectory(resDir, project.getTargetSdk());

                // ==========================================
                // STAGE 2: PLATFORM SDK (android.jar) SETUP (Android 30-36)
                // ==========================================
                int targetSdk = project.getTargetSdk() >= 30 && project.getTargetSdk() <= 36 ? project.getTargetSdk() : 33;
                File platformsDir = new File(context.getFilesDir(), "platforms/android-" + targetSdk);
                File androidJar = new File(platformsDir, "android.jar");

                if (!androidJar.exists() || androidJar.length() == 0) {
                    logDialog.appendLog("[Compiler] Android SDK " + targetSdk + " platform jar is missing. Setting up...");

                    // 1. Check pre-bundled assets platforms/android-{targetSdk}/android.jar
                    String assetPath = "platforms/android-" + targetSdk + "/android.jar";
                    boolean foundInAssets = false;
                    try (InputStream is = context.getAssets().open(assetPath)) {
                        foundInAssets = true;
                        logDialog.appendLog("[Compiler] Extracting SDK " + targetSdk + " platform jar from pre-bundled assets...");
                        platformsDir.mkdirs();
                        try (OutputStream os = new FileOutputStream(androidJar)) {
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = is.read(buf)) > 0) {
                                os.write(buf, 0, len);
                            }
                        }
                        logDialog.appendLog("[Compiler] Platform SDK " + targetSdk + " successfully set up.");
                    } catch (IOException e) {
                        foundInAssets = false;
                    }

                    // 2. Check pre-bundled android.jar.zip in assets
                    if (!foundInAssets) {
                        try (InputStream is = context.getAssets().open("android.jar.zip")) {
                            logDialog.appendLog("[Compiler] Extracting fallback android.jar from android.jar.zip...");
                            platformsDir.mkdirs();
                            java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is);
                            java.util.zip.ZipEntry entry;
                            while ((entry = zis.getNextEntry()) != null) {
                                if (entry.getName().endsWith("android.jar")) {
                                    try (OutputStream os = new FileOutputStream(androidJar)) {
                                        byte[] buf = new byte[8192];
                                        int len;
                                        while ((len = zis.read(buf)) > 0) {
                                            os.write(buf, 0, len);
                                        }
                                    }
                                    foundInAssets = true;
                                    logDialog.appendLog("[Compiler] Platform SDK " + targetSdk + " extracted from android.jar.zip.");
                                    break;
                                }
                                zis.closeEntry();
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    // 3. Official Google repository fallback (Android 30 to 36 only)
                    if (!foundInAssets) {
                        List<String> candidates = new ArrayList<>();
                        if (targetSdk == 36) {
                            candidates.add("https://dl.google.com/android/repository/platform-36_r01.zip");
                        } else if (targetSdk == 35) {
                            candidates.add("https://dl.google.com/android/repository/platform-35_r01.zip");
                        } else if (targetSdk == 34) {
                            candidates.add("https://dl.google.com/android/repository/platform-34-ext12_r01.zip");
                            candidates.add("https://dl.google.com/android/repository/platform-34-ext7_r03.zip");
                            candidates.add("https://dl.google.com/android/repository/platform-34_r01.zip");
                        } else if (targetSdk == 33) {
                            candidates.add("https://dl.google.com/android/repository/platform-33-ext5_r01.zip");
                            candidates.add("https://dl.google.com/android/repository/platform-33-ext4_r01.zip");
                            candidates.add("https://dl.google.com/android/repository/platform-33_r01.zip");
                        } else if (targetSdk == 32) {
                            candidates.add("https://dl.google.com/android/repository/platform-32_r01.zip");
                        } else if (targetSdk == 31) {
                            candidates.add("https://dl.google.com/android/repository/platform-31_r01.zip");
                        } else if (targetSdk == 30) {
                            candidates.add("https://dl.google.com/android/repository/platform-30_r03.zip");
                            candidates.add("https://dl.google.com/android/repository/platform-30_r01.zip");
                        }

                        boolean downloadSuccess = false;
                        for (String urlStr : candidates) {
                            logDialog.appendLog("[Compiler] Attempting download: " + urlStr);
                            try {
                                URL url = new URL(urlStr);
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setConnectTimeout(6000);
                                conn.setReadTimeout(6000);
                                int code = conn.getResponseCode();
                                if (code == 200) {
                                    downloadSuccess = true;
                                    logDialog.appendLog("[Compiler] Download successful from " + urlStr);
                                    break;
                                } else {
                                    logDialog.appendLog("[Compiler] Failed: HTTP error " + code + " from " + urlStr);
                                }
                            } catch (Exception ex) {
                                logDialog.appendLog("[Compiler] Failed: " + ex.getMessage());
                            }
                        }
                        if (!downloadSuccess) {
                            logDialog.appendLog("[Compiler] Error: android.jar was not found inside the downloaded platform SDK archive or network is unavailable.");
                            logDialog.appendLog("Build FAILED at stage [PLATFORM_SDK].");
                            callback.onComplete(false, null);
                            return;
                        }
                    }
                }

                // ==========================================
                // STAGE 3: AAPT2 COMPILATION & LINKING
                // ==========================================
                logDialog.appendLog("[AAPT2] Resolving compiler binary...");
                File aapt2Bin = AaptManager.getAapt2Binary(context);
                if (aapt2Bin == null || !aapt2Bin.exists()) {
                    logDialog.appendLog("[AAPT2] Error: libaapt2.so missing for current architecture (" + Build.CPU_ABI + ").");
                    logDialog.appendLog("Build FAILED at stage [AAPT2].");
                    callback.onComplete(false, null);
                    return;
                }

                logDialog.appendLog("[AAPT2] Compiling project resources...");
                File compiledResZip = new File(intermediatesDir, "compiled_res.zip");
                boolean resCompiled = AaptManager.compileResources(aapt2Bin, resDir, compiledResZip, logDialog::appendLog);
                if (!resCompiled) {
                    logDialog.appendLog("Build FAILED at stage [AAPT2_COMPILE].");
                    callback.onComplete(false, null);
                    return;
                }

                logDialog.appendLog("[AAPT2] Linking resources & generating R.java...");
                File resourcesApk = new File(intermediatesDir, "resources.ap_");
                boolean resLinked = AaptManager.linkResources(aapt2Bin, androidJar, manifestFile, compiledResZip,
                        genDir, resourcesApk, project.getMinSdk(), project.getTargetSdk(), logDialog::appendLog);
                if (!resLinked) {
                    logDialog.appendLog("Build FAILED at stage [AAPT2_LINK].");
                    callback.onComplete(false, null);
                    return;
                }

                // ==========================================
                // STAGE 4: ECJ (JAVA SOURCE COMPILATION)
                // ==========================================
                logDialog.appendLog("[ECJ] Compiling Java sources with Eclipse Compiler for Java...");
                List<File> sourceDirs = new ArrayList<>();
                sourceDirs.add(javaDir);
                sourceDirs.add(genDir);

                boolean javaCompiled = JavaCompiler.compile(context, androidJar, sourceDirs,
                        Collections.emptyList(), classesDir, logDialog::appendLog);
                if (!javaCompiled) {
                    logDialog.appendLog("Build FAILED at stage [JAVA_COMPILER].");
                    callback.onComplete(false, null);
                    return;
                }

                // ==========================================
                // STAGE 5: D8 DEX CONVERSION
                // ==========================================
                logDialog.appendLog("[D8] Converting bytecode to Dalvik Executable (classes.dex)...");
                boolean dexed = DexCompiler.dex(context, androidJar, classesDir,
                        Collections.emptyList(), dexDir, project.getMinSdk(), logDialog::appendLog);
                if (!dexed) {
                    logDialog.appendLog("Build FAILED at stage [D8_DEX].");
                    callback.onComplete(false, null);
                    return;
                }

                // ==========================================
                // STAGE 6: PACKAGING & SIGNING
                // ==========================================
                File unalignedApk = new File(binDir, project.getName() + "-unaligned.apk");
                boolean signed = ApkSignerTool.packageAndSign(context, resourcesApk, dexDir, unalignedApk, logDialog::appendLog);
                if (!signed) {
                    logDialog.appendLog("Build FAILED at stage [APK_SIGNER].");
                    callback.onComplete(false, null);
                    return;
                }

                // ==========================================
                // STAGE 7: ZIPALIGN
                // ==========================================
                finalApk = new File(binDir, project.getName() + "-debug.apk");
                ZipAlignTool.align(unalignedApk, finalApk, 4, logDialog::appendLog);
                if (unalignedApk.exists()) {
                    unalignedApk.delete();
                }

                logDialog.appendLog("========================================");
                logDialog.appendLog("[SUCCESS] BUILD SUCCESSFUL!");
                logDialog.appendLog("[OUTPUT] " + finalApk.getAbsolutePath());
                logDialog.appendLog("========================================");

                success = true;

            } catch (Exception e) {
                logDialog.appendLog("[Compiler] Unexpected error: " + e.getMessage());
            }

            final boolean resSuccess = success;
            final File resApk = finalApk;
            callback.onComplete(resSuccess, resApk);
        }).start();
    }

    public static void installApk(Context context, File apkFile) {
        if (apkFile == null || !apkFile.exists()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(context, "com.ide.studio.fileprovider", apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(apkFile);
            }
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
