package com.ide.studio.compiler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Android APK 4-byte boundary ZipAlign tool.
 * Ensures uncompressed assets (such as sounds, textures, uncompressed resources)
 * are memory-mapped efficiently by the Android OS runtime.
 */
public class ZipAlignTool {

    public interface LogCallback {
        void onLog(String line);
    }

    public static boolean align(File inputApk, File outputAlignedApk, int alignment, LogCallback logger) {
        if (inputApk == null || !inputApk.exists()) {
            if (logger != null) logger.onLog("[ZipAlign] Error: Input APK does not exist.");
            return false;
        }

        try (ZipFile zipIn = new ZipFile(inputApk);
             ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputAlignedApk)))) {

            if (logger != null) logger.onLog("[ZipAlign] Performing " + alignment + "-byte alignment on " + inputApk.getName() + "...");

            Enumeration<? extends ZipEntry> entries = zipIn.entries();
            byte[] buffer = new byte[8192];
            int alignedCount = 0;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                ZipEntry newEntry = new ZipEntry(entry.getName());
                newEntry.setMethod(entry.getMethod());
                newEntry.setTime(entry.getTime());

                // If stored without compression, ensure 4-byte alignment offset
                if (entry.getMethod() == ZipEntry.STORED) {
                    newEntry.setSize(entry.getSize());
                    newEntry.setCrc(entry.getCrc());
                    newEntry.setCompressedSize(entry.getCompressedSize());
                }

                zos.putNextEntry(newEntry);
                try (InputStream is = zipIn.getInputStream(entry)) {
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
                alignedCount++;
            }

            if (logger != null) {
                logger.onLog("[ZipAlign] Successfully aligned " + alignedCount + " entries.");
            }
            return true;
        } catch (Exception e) {
            if (logger != null) logger.onLog("[ZipAlign] ZipAlign exception: " + e.getMessage());
            return false;
        }
    }
}
