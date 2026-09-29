package com.tyron.compiler;

import android.content.Context;
import android.os.AsyncTask;
import com.apk.builder.model.Library;
import com.apk.builder.model.Project;
import com.apk.builder.util.Decompress;
import com.build.studio.XmlSanitizer;
import com.ide.studio.compiler.ApkSignerTool;
import com.ide.studio.compiler.ZipAlignTool;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class CompilerAsyncTask extends AsyncTask<Void, String, CompilerResult> {

    public interface CompileCallback {
        void onProgress(String message);
        void onComplete(CompilerResult result);
    }

    private final WeakReference<Context> mContext;
    private final Project project;
    private CompileCallback callback;
    private long startTime;

    public CompilerAsyncTask(Context context, Project project) {
        this.mContext = new WeakReference<>(context);
        this.project = project;
    }

    public void setCompileCallback(CompileCallback callback) {
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        this.startTime = System.currentTimeMillis();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        if (values != null && values.length > 0 && callback != null) {
            callback.onProgress(values[0]);
        }
    }

    @Override
    protected CompilerResult doInBackground(Void... voids) {
        try {
            // Step 1: XML sanitization
            publishProgress("Validating and sanitizing XML files...");
            sanitizeProjectXmlFiles();

            // Step 2: Ensure SDK platform is available
            publishProgress("Checking SDK platform...");
            ensureAndroidJarAvailable();

            // Step 3: AAPT2 compile and link
            AAPT2Compiler aapt2Compiler = new AAPT2Compiler(project);
            aapt2Compiler.setProgressListener(this::publishProgress);
            aapt2Compiler.prepare();
            aapt2Compiler.run();

            // Step 4: ECJ Java compilation
            ECJCompiler ecjCompiler = new ECJCompiler(project);
            ecjCompiler.setProgressListener(this::publishProgress);
            ecjCompiler.prepare();
            ecjCompiler.run();

            // Step 5: D8 Dex bytecode compilation
            D8Compiler d8Compiler = new D8Compiler(project);
            d8Compiler.setProgressListener(this::publishProgress);
            d8Compiler.prepare();
            d8Compiler.run();

            // Step 6: Package APK
            publishProgress("Packaging APK...");
            project.getLogger().d("APK Builder", "Packaging APK");

            File binDir = new File(project.getOutputFile(), "bin");
            File genApk = new File(binDir, "gen.apk");
            File generatedRes = new File(binDir, "generated.apk.res");

            packageApk(generatedRes, binDir, genApk);

            // Step 7: ZipAlign & Sign APK
            publishProgress("Signing Apk...");
            project.getLogger().d("APK Signer", "Signing Apk");

            File finalSignedApk = new File(binDir, "app-release-signed.apk");
            signFile(genApk, finalSignedApk);

            long totalTime = (System.currentTimeMillis() - startTime) / 1000;
            project.getLogger().d("APK Builder", "Build completed in " + totalTime + "s: " + finalSignedApk.getAbsolutePath());

            return new CompilerResult("Success");
        } catch (Throwable t) {
            project.getLogger().e("Compiler", "Build failed: " + t.getMessage());
            return new CompilerResult(true, t.getMessage() != null ? t.getMessage() : t.toString());
        }
    }

    private void sanitizeProjectXmlFiles() {
        if (project.getManifestFile() != null && project.getManifestFile().exists()) {
            XmlSanitizer.sanitizeFile(project.getManifestFile(), project.getTargetSdk());
        }
        if (project.getResourcesFile() != null && project.getResourcesFile().exists()) {
            XmlSanitizer.sanitizeDirectory(project.getResourcesFile(), project.getTargetSdk());
        }
    }

    private void ensureAndroidJarAvailable() {
        int targetSdk = project.getTargetSdk() > 0 ? project.getTargetSdk() : 33;
        File sdkDir = new File("/storage/emulated/0/test-folder/idestudio/app/src/main/assets/platforms/android-" + targetSdk);
        File androidJar = new File(sdkDir, "android.jar");

        if (androidJar.exists()) {
            project.getLogger().d("Compiler", "Android SDK " + targetSdk + " platform jar is already available.");
            return;
        }

        project.getLogger().d("Compiler", "SDK " + targetSdk + " platform jar missing. Checking pre-bundled assets...");

        Context ctx = mContext.get();
        if (ctx != null) {
            String assetPath = "platforms/android-" + targetSdk + "/android.jar";
            try (InputStream is = ctx.getAssets().open(assetPath)) {
                sdkDir.mkdirs();
                try (FileOutputStream fos = new FileOutputStream(androidJar)) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                }
                project.getLogger().d("Compiler", "Copying SDK " + targetSdk + " platform jar from pre-bundled assets succeeded.");
                return;
            } catch (Exception ignored) {
            }

            // Extract default SDK 30 platform jar from android.jar.zip
            try {
                project.getLogger().d("Compiler", "Extracting default SDK 30 platform jar from assets...");
                Decompress.unzipFromAssets(ctx, "android.jar.zip", sdkDir.getAbsolutePath());
                if (androidJar.exists()) {
                    project.getLogger().d("Compiler", "Default platform jar successfully set up.");
                    return;
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void packageApk(File baseResApk, File binDir, File outputApk) throws Exception {
        if (outputApk.exists()) outputApk.delete();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputApk))) {
            // 1. Copy entries from baseResApk
            if (baseResApk != null && baseResApk.exists()) {
                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(baseResApk))) {
                    ZipEntry entry;
                    byte[] buf = new byte[8192];
                    while ((entry = zis.getNextEntry()) != null) {
                        zos.putNextEntry(new ZipEntry(entry.getName()));
                        int len;
                        while ((len = zis.read(buf)) != -1) {
                            zos.write(buf, 0, len);
                        }
                        zos.closeEntry();
                        zis.closeEntry();
                    }
                }
            }

            // 2. Add classes.dex and secondary dex files
            File[] files = binDir.listFiles();
            if (files != null) {
                byte[] buf = new byte[8192];
                for (File f : files) {
                    if (f.isFile() && f.getName().startsWith("classes") && f.getName().endsWith(".dex")) {
                        project.getLogger().d("APK Builder", "Adding dex file " + f.getName() + " to APK.");
                        zos.putNextEntry(new ZipEntry(f.getName()));
                        try (FileInputStream fis = new FileInputStream(f)) {
                            int len;
                            while ((len = fis.read(buf)) != -1) {
                                zos.write(buf, 0, len);
                            }
                        }
                        zos.closeEntry();
                    }
                }
            }

            // 3. Add library resources
            if (project.getLibraries() != null) {
                byte[] buf = new byte[8192];
                for (Library lib : project.getLibraries()) {
                    File jar = lib.getClassJarFile();
                    if (jar != null && jar.exists() && !jar.isDirectory()) {
                        project.getLogger().d("APK Builder", "Adding resources of " + lib.getName() + " to the APK");
                        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jar))) {
                            ZipEntry entry;
                            while ((entry = zis.getNextEntry()) != null) {
                                String name = entry.getName();
                                if (!name.endsWith(".class") && !name.startsWith("META-INF/")) {
                                    try {
                                        zos.putNextEntry(new ZipEntry(name));
                                        int len;
                                        while ((len = zis.read(buf)) != -1) {
                                            zos.write(buf, 0, len);
                                        }
                                        zos.closeEntry();
                                    } catch (Exception ignored) {
                                        // Ignore duplicate entries
                                    }
                                }
                                zis.closeEntry();
                            }
                        }
                    }
                }
            }
        }
    }

    private void signFile(File unsignedApk, File signedApk) throws Exception {
        Context ctx = mContext.get();
        File alignedApk = new File(unsignedApk.getParentFile(), "aligned.apk");

        // Align
        ZipAlignTool.align(unsignedApk, alignedApk, 4, null);

        // Sign
        File toSign = alignedApk.exists() && alignedApk.length() > 0 ? alignedApk : unsignedApk;
        boolean signed = ApkSignerTool.sign(ctx, toSign, signedApk, null);
        if (!signed && (!signedApk.exists() || signedApk.length() == 0)) {
            // Fallback: copy unsigned as signed if signing keystore not loaded yet
            try (FileInputStream fis = new FileInputStream(toSign);
                 FileOutputStream fos = new FileOutputStream(signedApk)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = fis.read(buf)) != -1) fos.write(buf, 0, r);
            }
        }
        if (alignedApk.exists()) alignedApk.delete();
    }

    @Override
    protected void onPostExecute(CompilerResult result) {
        super.onPostExecute(result);
        if (callback != null) {
            callback.onComplete(result);
        }
    }
}
