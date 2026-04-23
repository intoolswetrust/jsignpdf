package net.sf.jsignpdf.fx.preset;

import java.util.function.Predicate;

/**
 * Validation rules for preset display names. These are UI-level rules — the filename is decoupled from the display name and
 * always safe (see {@link PresetManager}).
 */
public final class PresetValidation {

    public static final int MAX_DISPLAY_NAME_LENGTH = 60;

    public enum Result {
        OK,
        EMPTY,
        ILLEGAL_CHAR,
        TOO_LONG,
        DUPLICATE
    }

    private PresetValidation() {
    }

    /**
     * Trims the input and returns a validation result. Call {@link #trim(String)} separately if you need the normalised form.
     *
     * @param rawName the display name as entered by the user
     * @param isDuplicate predicate that returns true if another preset already uses the (trimmed) name (case-insensitively)
     */
    public static Result validate(String rawName, Predicate<String> isDuplicate) {
        String trimmed = trim(rawName);
        if (trimmed.isEmpty()) {
            return Result.EMPTY;
        }
        if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
            return Result.TOO_LONG;
        }
        if (containsIllegalChars(trimmed)) {
            return Result.ILLEGAL_CHAR;
        }
        if (isDuplicate.test(trimmed)) {
            return Result.DUPLICATE;
        }
        return Result.OK;
    }

    /**
     * Returns the trimmed form of the name. {@code null} input becomes {@code ""}.
     */
    public static String trim(String rawName) {
        return rawName == null ? "" : rawName.trim();
    }

    /**
     * Checks for control characters (below 0x20 or 0x7F).
     */
    private static boolean containsIllegalChars(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return true;
            }
        }
        return false;
    }
}
