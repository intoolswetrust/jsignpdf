package net.sf.jsignpdf.fx.util;

/**
 * Validation rules for the output file name (base name) field. The value is a single path component joined onto the
 * output directory, so it must not carry a directory separator, a {@code ..} traversal or a home-directory expansion
 * that would move the write outside the chosen directory. This matters most under the sandbox, where the directory
 * grant is scoped to exactly that directory subtree.
 */
public final class OutputBaseNameValidation {

    public static final int MAX_LENGTH = 255;

    private static final String ILLEGAL_CHARS = "/\\:*?\"<>|";

    public enum Result {
        OK,
        ILLEGAL_CHAR,
        TOO_LONG,
        SURROUNDING_WHITESPACE,
        PATH_TRAVERSAL
    }

    private OutputBaseNameValidation() {
    }

    /**
     * Validates a base name as typed. {@code null} and empty are both valid: a blank field means "use the name derived
     * from the input file and the suffix".
     *
     * @param name the base name as entered by the user
     * @return the validation result
     */
    public static Result validate(String name) {
        if (name == null || name.isEmpty()) {
            return Result.OK;
        }
        if (name.length() > MAX_LENGTH) {
            return Result.TOO_LONG;
        }
        if (!name.equals(name.trim())) {
            return Result.SURROUNDING_WHITESPACE;
        }
        if (name.equals(".") || name.equals("..") || name.startsWith("~")) {
            return Result.PATH_TRAVERSAL;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 0x20 || c == 0x7F || ILLEGAL_CHARS.indexOf(c) >= 0) {
                return Result.ILLEGAL_CHAR;
            }
        }
        return Result.OK;
    }

    /**
     * Returns true when the base name can be joined onto the output directory.
     */
    public static boolean isValid(String name) {
        return validate(name) == Result.OK;
    }
}
