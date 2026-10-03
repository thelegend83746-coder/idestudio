package com.idestudio.app.ai.context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ProjectContextBuilder {

    public static final String DEFAULT_SYSTEM_PROMPT =
            "You are Build AI, a senior Android Java developer assistant integrated into IDE Studio.\n"
            + "You help developers write native Java code, layout XML, and resolve build errors directly on Android.\n"
            + "CRITICAL RULES:\n"
            + "1. Only propose valid Android Java and XML code (do not propose Kotlin).\n"
            + "2. Any proposed file or folder modification MUST be wrapped in a <proposal> tag using relative project paths:\n"
            + "   <proposal type=\"create_file\" path=\"app/src/main/java/.../FileName.java\">\n"
            + "   [complete file contents here]\n"
            + "   </proposal>\n"
            + "   Supported types: create_file, update_file, create_folder, rename_folder, delete_file, delete_folder.\n"
            + "3. The user must manually approve each proposal before it is applied to disk. Provide concise explanations.";

    private ProjectContextBuilder() {}

    public static String buildInitialContext(File projectRoot, String buildError) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Project: ").append(projectRoot.getName()).append("\n");
        sb.append("Project Structure:\n");

        List<String> files = new ArrayList<>();
        listProjectFilesRelative(projectRoot, projectRoot, files, 0);
        for (String f : files) {
            sb.append(" - ").append(f).append("\n");
        }

        if (buildError != null && !buildError.isEmpty()) {
            sb.append("\n[COMPILER BUILD ERROR DETECTED]:\n");
            sb.append(buildError).append("\n");
            sb.append("Please diagnose this compilation failure and propose the necessary code fix using <proposal> tags.\n");
        }

        return sb.toString();
    }

    private static void listProjectFilesRelative(File root, File current, List<String> list, int depth) {
        if (depth > 4) return; // Keep context concise
        File[] files = current.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.getName().startsWith(".") || f.getName().equals("build")) continue;
            String rel = root.toURI().relativize(f.toURI()).getPath();
            list.add(rel);
            if (f.isDirectory()) {
                listProjectFilesRelative(root, f, list, depth + 1);
            }
        }
    }
}
