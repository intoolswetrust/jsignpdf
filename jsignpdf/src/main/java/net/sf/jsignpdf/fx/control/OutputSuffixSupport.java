package net.sf.jsignpdf.fx.control;

import java.io.File;
import java.util.Locale;
import net.sf.jsignpdf.utils.AppConfig;

/** Session-only output filename suffix state shared by the UI field and signing flow. */
public final class OutputSuffixSupport {
    private static volatile String token = "";
    private static volatile boolean touched = false;
    private static volatile String lastInputPath;

    private OutputSuffixSupport() {}

    public static void setUserValue(String value) {
        token = normalizeToken(value);
        touched = true;
    }

    public static String suggestedFor(File inputFile) {
        if (inputFile == null) {
            lastInputPath = null;
            return null;
        }
        lastInputPath = inputFile.getAbsolutePath();
        return appendCurrentSuffix(lastInputPath);
    }

    public static String resolveForSign(String inputPath, String currentOutPath) {
        if (!touched && currentOutPath != null && !currentOutPath.isBlank()) {
            return currentOutPath;
        }
        if (inputPath == null || inputPath.isBlank()) {
            return currentOutPath;
        }
        lastInputPath = inputPath;
        return appendCurrentSuffix(inputPath);
    }

    public static String suggestedForLastInput() {
        String input = lastInputPath;
        return input == null || input.isBlank() ? null : appendCurrentSuffix(input);
    }

    private static String appendCurrentSuffix(String inputPath) {
        String ext = ".pdf";
        String base = inputPath;
        if (inputPath.toLowerCase(Locale.ROOT).endsWith(ext)) {
            base = inputPath.substring(0, inputPath.length() - ext.length());
            ext = inputPath.substring(inputPath.length() - ext.length());
        }
        String suffix = token.isEmpty() ? AppConfig.defaultOutSuffix() : "_" + token;
        return base + suffix + ext;
    }

    private static String normalizeToken(String value) {
        if (value == null) return "";
        String s = value.trim();
        if (s.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            s = s.substring(0, s.length() - 4).trim();
        }
        while (s.startsWith("_")) s = s.substring(1);
        s = s.trim().replaceAll("\\s+", "_");
        while (s.contains("__")) s = s.replace("__", "_");
        return s;
    }
}
