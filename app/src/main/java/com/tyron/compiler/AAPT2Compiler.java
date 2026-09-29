package com.tyron.compiler;

import com.apk.builder.BinaryExecutor;
import com.apk.builder.model.Library;
import com.apk.builder.model.Project;
import com.build.studio.XmlSanitizer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AAPT2Compiler extends Compiler {
    private static final String TAG = "AAPT2";
    private final Project mProject;
    private final List<Library> mLibraries;
    private File resPath;
    private File outputPath;
    private File genDir;
    private File binDir;
    private File mFilesDir;
    private BinaryExecutor executor;
    private File aapt2Binary;

    public AAPT2Compiler(Project project) {
        this.mProject = project;
        this.mLibraries = project.getLibraries() != null ? project.getLibraries() : new ArrayList<>();
        this.executor = new BinaryExecutor();
    }

    public File getAAPT2File() {
        return aapt2Binary;
    }

    private void resolveAapt2() {
        // 1. Check common native library directory
        File appLibDir = new File("/data/data/com.ide.studio/lib");
        if (appLibDir.exists()) {
            File aapt2 = new File(appLibDir, "libaapt2.so");
            if (aapt2.exists()) {
                aapt2.setExecutable(true, false);
                this.aapt2Binary = aapt2;
                return;
            }
        }

        // 2. Check bundled jniLibs in test-folder
        String[] abis = {"arm64-v8a", "armeabi-v7a", "x86_64", "x86", "armeabi"};
        for (String abi : abis) {
            File jniBin = new File("/storage/emulated/0/test-folder/idestudio/app/src/main/jniLibs/" + abi + "/libaapt2.so");
            if (jniBin.exists()) {
                jniBin.setExecutable(true, false);
                this.aapt2Binary = jniBin;
                return;
            }
        }

        // 3. Fallback to bin dir
        File f = new File(binDir, "libaapt2.so");
        if (f.exists()) {
            f.setExecutable(true, false);
            this.aapt2Binary = f;
        }
    }

    public void ensureManifestExportedDeclared() {
        if (mProject.getManifestFile() != null && mProject.getManifestFile().exists()) {
            boolean changed = XmlSanitizer.sanitizeFile(mProject.getManifestFile(), mProject.getTargetSdk());
            if (changed) {
                mProject.getLogger().d(TAG, "Automatically declared android:exported=\"true\" for components with intent-filters in AndroidManifest.xml");
            }
        }
    }

    @Override
    public void prepare() throws Exception {
        onProgressUpdate("Preparing AAPT2...");
        mProject.getLogger().d(TAG, "Preparing AAPT2...");

        outputPath = mProject.getOutputFile();
        binDir = new File(outputPath, "bin");
        binDir.mkdirs();

        genDir = new File(outputPath, "gen");
        genDir.mkdirs();

        resPath = mProject.getResourcesFile();

        resolveAapt2();
        ensureManifestExportedDeclared();
    }

    public void compileLibraries() throws Exception {
        if (mLibraries == null || mLibraries.isEmpty()) return;

        onProgressUpdate("AAPT2 > Compiling libraries");
        for (Library lib : mLibraries) {
            if (lib.requiresResourceFile()) {
                mProject.getLogger().d(TAG, "Compiling library: " + lib.getName());
                File libRes = lib.getResourcesFile();
                File compiledRes = new File(lib.getPath(), "compiled.zip");

                List<String> cmd = new ArrayList<>();
                cmd.add(aapt2Binary != null ? aapt2Binary.getAbsolutePath() : "aapt2");
                cmd.add("compile");
                cmd.add("--dir");
                cmd.add(libRes.getAbsolutePath());
                cmd.add("-o");
                cmd.add(compiledRes.getAbsolutePath());

                executor.setCommands(cmd);
                int code = executor.execute();
                if (code != 0) {
                    mProject.getLogger().w(TAG, "Library compile output: " + executor.getLog());
                }
            }
        }
    }

    @Override
    public void run() throws Exception {
        compileLibraries();

        onProgressUpdate("AAPT2 > Compiling resources");
        mProject.getLogger().d(TAG, "Compiling project resources");

        File compiledZip = new File(binDir, "resources.zip");
        if (compiledZip.exists()) compiledZip.delete();

        List<String> compileCmd = new ArrayList<>();
        compileCmd.add(aapt2Binary != null ? aapt2Binary.getAbsolutePath() : "aapt2");
        compileCmd.add("compile");
        compileCmd.add("--dir");
        compileCmd.add(resPath.getAbsolutePath());
        compileCmd.add("-o");
        compileCmd.add(compiledZip.getAbsolutePath());

        executor.setCommands(compileCmd);
        int compileCode = executor.execute();
        mProject.getLogger().d(TAG, "AAPT2 compile: " + executor.getLog());
        if (compileCode != 0) {
            throw new Exception("AAPT2 compilation failed:\n" + executor.getLog());
        }

        onProgressUpdate("AAPT2 > Linking resources");
        mProject.getLogger().d(TAG, "Linking resources");

        File generatedResApk = new File(binDir, "generated.apk.res");
        if (generatedResApk.exists()) generatedResApk.delete();

        List<String> linkCmd = new ArrayList<>();
        linkCmd.add(aapt2Binary != null ? aapt2Binary.getAbsolutePath() : "aapt2");
        linkCmd.add("link");
        linkCmd.add("-I");
        linkCmd.add(getAndroidJarFile().getAbsolutePath());
        linkCmd.add("--manifest");
        linkCmd.add(mProject.getManifestFile().getAbsolutePath());
        linkCmd.add("--java");
        linkCmd.add(genDir.getAbsolutePath());
        linkCmd.add("-o");
        linkCmd.add(generatedResApk.getAbsolutePath());
        linkCmd.add("--auto-add-overlay");

        if (mProject.getMinSdk() > 0) {
            linkCmd.add("--min-sdk-version");
            linkCmd.add(String.valueOf(mProject.getMinSdk()));
        }
        if (mProject.getTargetSdk() > 0) {
            linkCmd.add("--target-sdk-version");
            linkCmd.add(String.valueOf(mProject.getTargetSdk()));
        }
        if (mProject.getVersionCode() > 0) {
            linkCmd.add("--version-code");
            linkCmd.add(String.valueOf(mProject.getVersionCode()));
        }
        if (mProject.getVersionName() != null && !mProject.getVersionName().isEmpty()) {
            linkCmd.add("--version-name");
            linkCmd.add(mProject.getVersionName());
        }

        // Add compiled project resources
        linkCmd.add(compiledZip.getAbsolutePath());

        // Add compiled library resources
        for (Library lib : mLibraries) {
            File libResZip = new File(lib.getPath(), "compiled.zip");
            if (libResZip.exists()) {
                linkCmd.add("-R");
                linkCmd.add(libResZip.getAbsolutePath());
            }
        }

        executor.setCommands(linkCmd);
        int linkCode = executor.execute();
        mProject.getLogger().d(TAG, "AAPT2 link: " + executor.getLog());
        if (linkCode != 0) {
            throw new Exception("AAPT2 linking failed:\n" + executor.getLog());
        }
    }

    @Override
    public File getAndroidJarFile() {
        int targetSdk = mProject.getTargetSdk() > 0 ? mProject.getTargetSdk() : 33;
        File sdkDir = new File(mProject.getOutputFile().getParentFile(), "platforms/android-" + targetSdk);
        File jar = new File(sdkDir, "android.jar");
        if (jar.exists()) return jar;

        File fallback = new File("/storage/emulated/0/test-folder/idestudio/app/src/main/assets/platforms/android-" + targetSdk + "/android.jar");
        if (fallback.exists()) return fallback;

        return new File("/storage/emulated/0/test-folder/idestudio/app/src/main/assets/platforms/android-33/android.jar");
    }
}
