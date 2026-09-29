package com.ide.studio.dependency;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extracts .aar archives into classes.jar and resources directory for compiler consumption.
 */
public class AarExtractor {

    public static boolean extractAar(File aarFile, File destDir) {
        if (!aarFile.exists()) return false;
        destDir.mkdirs();

        try (ZipFile zip = new ZipFile(aarFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            byte[] buf = new byte[8192];

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File target = new File(destDir, entry.getName());

                if (entry.isDirectory()) {
                    target.mkdirs();
                } else {
                    target.getParentFile().mkdirs();
                    try (InputStream is = zip.getInputStream(entry);
                         BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target))) {
                        int len;
                        while ((len = is.read(buf)) > 0) {
                            bos.write(buf, 0, len);
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
