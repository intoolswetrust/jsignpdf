package net.sf.jsignpdf.fx.util;

/**
 * Validation rules for the output file name suffix, shared by the Signature Properties field and the Preferences dialog.
 * The suffix is used verbatim, so it must not contain anything that would turn the derived output name into a different
 * path or an unwritable file name.
 */
public final class OutputSuffixValidation {

    public static final int MAX_LENGTH = 40;

    private static final String ILLEGAL_CHARS = "/\\:*?\"<>|";

    public enum Result {
        OK,
        ILLEGAL_CHAR,
        TOO_LONG,
        SURROUNDING_WHITESPACE
    }

    private OutputSuffixValidation() {
    }

    /**
     * Validates a suffix as typed. {@code null} and empty are both valid: the Signature Properties field maps a blank
     * value to "no suffix of my own", so the configured default applies.
     *
     * @param suffix the suffix as entered by the user
     * @return the validation result
     */
    public static Result validate(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return Result.OK;
        }
        if (suffix.length() > MAX_LENGTH) {
            return Result.TOO_LONG;
        }
        if (!suffix.equals(suffix.trim())) {
            return Result.SURROUNDING_WHITESPACE;
        }
        for (int i = 0; i < suffix.length(); i++) {
            char c = suffix.charAt(i);
            if (c < 0x20 || c == 0x7F || ILLEGAL_CHARS.indexOf(c) >= 0) {
                return Result.ILLEGAL_CHAR;
            }
        }
        return Result.OK;
    }

    /**
     * Returns true when the suffix can be used to derive an output file name.
     */
    public static boolean isValid(String suffix) {
        return validate(suffix) == Result.OK;
    }
}
