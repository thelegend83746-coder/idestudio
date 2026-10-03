package com.idestudio.app.editor.tabs;

import com.idestudio.app.editor.history.UndoRedoManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class OpenFileDocument {

    private final File file;
    private final String fileName;
    private String content;
    private boolean isDirty;
    private final UndoRedoManager undoRedoManager;

    public OpenFileDocument(File file) {
        this.file = file;
        this.fileName = file.getName();
        this.undoRedoManager = new UndoRedoManager();
        this.isDirty = false;
        loadContent();
    }

    public void loadContent() {
        if (!file.exists()) {
            this.content = "";
            return;
        }
        try (FileInputStream fis = new FileInputStream(file);
             Reader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[2048];
            int r;
            while ((r = reader.read(buf)) != -1) {
                sb.append(buf, 0, r);
            }
            this.content = sb.toString();
            this.isDirty = false;
        } catch (IOException e) {
            e.printStackTrace();
            this.content = "";
        }
    }

    public void saveContent(String newContent) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             Writer writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(newContent);
            writer.flush();
            this.content = newContent;
            this.isDirty = false;
        }
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.isDirty = true;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public UndoRedoManager getUndoRedoManager() {
        return undoRedoManager;
    }
}
