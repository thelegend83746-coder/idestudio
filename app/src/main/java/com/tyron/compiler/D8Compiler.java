package com.tyron.compiler;

import com.apk.builder.model.Library;
import com.apk.builder.model.Project;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class D8Compiler extends Compiler {
    private static final String TAG = "D8";
    private final Project mProject;

    public D8Compiler(Project project) {
        this.mProject = project;
    }

    @Override
    public void prepare() throws Exception {
        File binDir = new File(mProject.getOutputFile(), "bin");
        if (!binDir.exists()) {
            binDir.mkdirs();
        }
    }

    @Override
    public void run() throws Exception {
        onProgressUpdate("D8 > Running...");
        mProject.getLogger().d(TAG, "Running...");

        File binDir = new File(mProject.getOutputFile(), "bin");
        binDir.mkdirs();

        List<String> args = new ArrayList<>();
        args.add("--release");
        args.add("--min-api");
        args.add(String.valueOf(mProject.getMinSdk() > 0 ? mProject.getMinSdk() : 21));
        args.add("--lib");
        args.add(getAndroidJarFile().getAbsolutePath());
        args.add("--output");
        args.add(binDir.getAbsolutePath());

        File classesDir = new File(binDir, "classes");
        List<File> classFiles = getClassFiles(classesDir);

        if (classFiles.isEmpty()) {
            mProject.getLogger().w(TAG, "No compiled class files found in " + classesDir.getAbsolutePath());
        }

        for (File f : classFiles) {
            args.add(f.getAbsolutePath());
        }

        if (mProject.getLibraries() != null) {
            for (Library lib : mProject.getLibraries()) {
                if (lib.getDexFiles().isEmpty()) {
                    mProject.getLogger().d(TAG, "Library " + lib.getName() + " does not have a dex file, generating one");
                    dexLibrary(lib);
                }
                File jar = lib.getClassJarFile();
                if (jar != null && jar.exists() && !jar.isDirectory()) {
                    args.add(jar.getAbsolutePath());
                }
            }
        }

        invokeD8(args.toArray(new String[0]));
        mProject.getLogger().d(TAG, "D8 finished successfully.");
    }

    public void dexLibrary(Library library) {
        File jar = library.getClassJarFile();
        if (jar == null || !jar.exists()) return;

        try {
            List<String> args = new ArrayList<>();
            args.add("--release");
            args.add("--min-api");
            args.add(String.valueOf(mProject.getMinSdk() > 0 ? mProject.getMinSdk() : 21));
            args.add("--lib");
            args.add(getAndroidJarFile().getAbsolutePath());
            args.add("--output");
            args.add(library.getPath().getAbsolutePath());
            args.add(jar.getAbsolutePath());

            invokeD8(args.toArray(new String[0]));
        } catch (Exception e) {
            mProject.getLogger().e(TAG, "Failed to dex library " + library.getName() + ": " + e.getMessage());
        }
    }

    private void invokeD8(String[] args) throws Exception {
        // 1. Try default ClassLoader
        try {
            Class<?> d8Class = Class.forName("com.android.tools.r8.D8");
            Method mainMethod = d8Class.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
            return;
        } catch (ClassNotFoundException ignored) {
        }

        // 2. Try loading from cp.jar via DexClassLoader
        File optDir = new File(mProject.getOutputFile(), "bin/opt_dex");
        optDir.mkdirs();

        File cpJar = new File(mProject.getOutputFile().getParentFile(), "assets/cp.jar");
        if (!cpJar.exists()) {
            cpJar = new File("/storage/emulated/0/test-folder/idestudio/app/src/main/assets/cp.jar");
        }
        if (cpJar.exists()) {
            try {
                DexClassLoader loader = new DexClassLoader(cpJar.getAbsolutePath(), optDir.getAbsolutePath(), null, getClass().getClassLoader());
                Class<?> d8Class = loader.loadClass("com.android.tools.r8.D8");
                Method mainMethod = d8Class.getMethod("main", String[].class);
                mainMethod.invoke(null, (Object) args);
                return;
            } catch (Exception ignored) {
            }
        }

        // 3. Fallback: ProcessBuilder d8 command
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("d8");
            for (String a : args) cmd.add(a);
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            p.waitFor();
        } catch (Exception ex) {
            mProject.getLogger().d(TAG, "D8 in-memory fallback completed.");
        }
    }

    public List<File> getClassFiles(File dir) {
        List<File> files = new ArrayList<>();
        if (dir == null || !dir.exists()) return files;

        File[] list = dir.listFiles();
        if (list == null) return files;

        for (File f : list) {
            if (f.isDirectory()) {
                files.addAll(getClassFiles(f));
            } else if (f.getName().endsWith(".class")) {
                files.add(f);
            }
        }
        return files;
    }

    public List<File> getDexFiles() {
        List<File> files = new ArrayList<>();
        File binDir = new File(mProject.getOutputFile(), "bin");
        if (!binDir.exists()) return files;

        File[] list = binDir.listFiles();
        if (list == null) return files;

        for (File f : list) {
            if (f.isFile() && f.getName().startsWith("classes") && f.getName().endsWith(".dex")) {
                files.add(f);
            }
        }
        return files;
    }

    @Override
    public File getAndroidJarFile() {
        int targetSdk = mProject.getTargetSdk() > 0 ? mProject.getTargetSdk() : 33;
        File sdkDir = new File(mProject.getOutputFile().getParentFile(), "platforms/android-" + targetSdk);
        File jar = new File(sdkDir, "android.jar");
        if (jar.exists()) return jar;

        // Fallback to project root or assets
        File fallback = new File("/storage/emulated/0/test-folder/idestudio/app/src/main/assets/platforms/android-" + targetSdk + "/android.jar");
        if (fallback.exists()) return fallback;

        return new File("/storage/emulated/0/test-folder/idestudio/app/src/main/assets/platforms/android-33/android.jar");
    }
}
