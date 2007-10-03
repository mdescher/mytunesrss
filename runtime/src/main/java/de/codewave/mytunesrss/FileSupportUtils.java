/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss;

import org.apache.commons.io.*;
import org.apache.commons.lang.*;

import java.io.*;
import java.util.*;

/**
 * de.codewave.mytunesrss.FileSupportUtils
 */
public class FileSupportUtils {
    public static boolean isSupported(String filename) {
        return isSuffixSupported(getFileSuffix(filename));
    }

    private static boolean isSuffixSupported(String suffix) {
        if (FileSuffixInfo.getSuffixes().contains(suffix)) {
            if (StringUtils.isEmpty(MyTunesRss.CONFIG.getFileTypes())) {
                return true;
            }
            for (StringTokenizer tokenizer = new StringTokenizer(MyTunesRss.CONFIG.getFileTypes().toLowerCase(), ","); tokenizer.hasMoreTokens();) {
                if (suffix.equalsIgnoreCase(tokenizer.nextToken())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static FileSuffixInfo getFileSuffixInfo(String filename) {
        if (isSupported(filename)) {
            return FileSuffixInfo.getForSuffix(getFileSuffix(filename));
        }
        return null;
    }

    public static String getFileSuffix(String filename) {
        int i = filename.lastIndexOf(".");
        if (i != -1 && i + 1 < filename.length()) {
            return filename.substring(i + 1).trim().toLowerCase();
        }
        return null;
    }

    public static String getContentType(String filename, boolean video) {
        FileSuffixInfo fileSuffixInfo = getFileSuffixInfo(filename);
        if (fileSuffixInfo != null) {
            return fileSuffixInfo.getMimeType(video);
        }
        return "application/octet-stream";
    }

    public static boolean isProtected(String filename) {
        FileSuffixInfo fileSuffixInfo = getFileSuffixInfo(filename);
        return fileSuffixInfo != null && fileSuffixInfo.isProtected();
    }

    public static boolean isVideo(String filename) {
        FileSuffixInfo fileSuffixInfo = getFileSuffixInfo(filename);
        return fileSuffixInfo != null && fileSuffixInfo.isVideo();
    }

    public static boolean isMp3(File file) {
        return file != null && file.isFile() && "mp3".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()));
    }

    public static boolean isMp4(File file) {
        if (file != null && file.isFile()) {
            String extension = FilenameUtils.getExtension(file.getName());
            return "mp4".equalsIgnoreCase(extension) || "m4a".equalsIgnoreCase(extension) || "m4v".equalsIgnoreCase(extension) ||
                    "m4p".equalsIgnoreCase(extension);
        }
        return false;
    }
}
