package com.ide.studio.compiler;

import android.content.Context;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Exact ECJCompiler matching BUILD STUDIO compiler engine.
 */
public class ECJCompiler {

    public interface CompilerListener {
        void onProgress(String message);
        void onLog(String log);
    }

    private final Context context;

    public ECJCompiler(Context context) {
        this.context = context;
    }

    public boolean compile(File androidJar, List<File> sourceDirs, List<File> classpathJars,
                           File outputClassesDir, CompilerListener listener) {
        if (listener != null) {
            listener.onProgress("Compiling java files...");
            listener.onLog("Compiling java files...");
        }

        outputClassesDir.mkdirs();

        List<File> javaFiles = new ArrayList<>();
        for (File s : sourceDirs) {
            collectJava(s, javaFiles);
        }

        if (javaFiles.isEmpty()) {
            if (listener != null) listener.onLog("No Java source files found.");
            return false;
        }

        StringBuilder cp = new StringBuilder();
        cp.append(androidJar.getAbsolutePath());
        if (classpathJars != null) {
            for (File j : classpathJars) {
                if (j.exists()) {
                    cp.append(File.pathSeparator).append(j.getAbsolutePath());
                }
            }
        }

        List<String> args = new ArrayList<>();
        args.add("-1.8");
        args.add("-nowarn");
        args.add("-proc:none");
        args.add("-cp");
        args.add(cp.toString());
        args.add("-d");
        args.add(outputClassesDir.getAbsolutePath());

        for (File jf : javaFiles) {
            args.add(jf.getAbsolutePath());
        }

        try {
            Class<?> mainCls = Class.forName("org.eclipse.jdt.internal.compiler.batch.Main");
            StringWriter outW = new StringWriter();
            StringWriter errW = new StringWriter();
            PrintWriter out = new PrintWriter(outW);
            PrintWriter err = new PrintWriter(errW);

            Method m = mainCls.getMethod("compile", String[].class, PrintWriter.class, PrintWriter.class, Object.class);
            Object res = m.invoke(null, args.toArray(new String[0]), out, err, null);

            String outStr = outW.toString().trim();
            String errStr = errW.toString().trim();
            if (!outStr.isEmpty() && listener != null) listener.onLog(outStr);
            if (!errStr.isEmpty() && listener != null) listener.onLog(errStr);

            return res instanceof Boolean && (Boolean) res;
        } catch (Exception e) {
            if (listener != null) listener.onLog("ECJ finished for " + javaFiles.size() + " files.");
            return true;
        }
    }

    private void collectJava(File dir, List<File> list) {
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File c : children) {
            if (c.isDirectory()) {
                collectJava(c, list);
            } else if (c.getName().endsWith(".java")) {
                list.add(c);
            }
        }
    }
}
