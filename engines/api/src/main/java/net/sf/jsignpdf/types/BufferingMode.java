package net.sf.jsignpdf.types;

import java.util.Locale;

/**
 * Where a signing engine stages the intermediate bytes of the document it is producing.
 *
 * <p>
 * This is a property of the machine and the workload, not of the signature, so it is configured
 * application-wide through {@code buffering.mode} rather than per engine. {@link #MEMORY} is the default and
 * reproduces the historical behaviour of both engines exactly; {@link #TEMP} moves the intermediates to
 * temporary files so that document size stops being bounded by {@code -Xmx}.
 * </p>
 *
 * @author Josef Cacek
 */
public enum BufferingMode {

    /** Everything on the Java heap. Fastest, bounded by {@code -Xmx}. */
    MEMORY,
    /** Intermediates staged in temporary files. Needed for very large documents, costs disk I/O. */
    TEMP;

    /**
     * Parses a case-insensitive configuration token into a {@link BufferingMode}.
     *
     * @param value the token, may be {@code null}
     * @return the matching mode, or {@code null} when {@code value} is {@code null}, empty or unrecognised
     */
    public static BufferingMode fromString(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toUpperCase(Locale.ENGLISH);
        if (v.isEmpty()) {
            return null;
        }
        try {
            return BufferingMode.valueOf(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
