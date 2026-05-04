package net.sf.jsignpdf.fx.preferences;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

/**
 * Pure validation helpers for the Preferences dialog. Kept in their own class so they're easy to unit-test without firing up
 * the JavaFX runtime.
 */
public final class PreferencesValidation {

    private PreferencesValidation() {
    }

    public static boolean validateFontPath(String path) {
        if (StringUtils.isBlank(path)) {
            return true;
        }
        try {
            return Files.isReadable(Paths.get(path));
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean validatePdfLibSelection(boolean jpedal, boolean pdfbox, boolean openpdf) {
        return jpedal || pdfbox || openpdf;
    }
}
