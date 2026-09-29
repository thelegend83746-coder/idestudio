package com.apk.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class BinaryExecutor {
    private ProcessBuilder mProcess;
    private final StringWriter mWriter = new StringWriter();
    private final List<String> mCommands = new ArrayList<>();

    public BinaryExecutor() {
        this.mProcess = new ProcessBuilder();
    }

    public BinaryExecutor(List<String> commands) {
        this();
        setCommands(commands);
    }

    public void setCommands(List<String> commands) {
        this.mCommands.clear();
        if (commands != null) {
            this.mCommands.addAll(commands);
        }
        this.mProcess.command(this.mCommands);
    }

    public void setDirectory(File dir) {
        if (dir != null && dir.exists()) {
            this.mProcess.directory(dir);
        }
    }

    public int execute() {
        mWriter.getBuffer().setLength(0);
        try {
            mProcess.redirectErrorStream(true);
            Process process = mProcess.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    mWriter.write(line);
                    mWriter.write("\n");
                }
            }
            return process.waitFor();
        } catch (Exception e) {
            PrintWriter pw = new PrintWriter(mWriter);
            e.printStackTrace(pw);
            return -1;
        }
    }

    public String getLog() {
        return mWriter.toString();
    }
}
