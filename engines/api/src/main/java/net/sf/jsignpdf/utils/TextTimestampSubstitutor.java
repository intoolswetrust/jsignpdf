package net.sf.jsignpdf.utils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import net.sf.jsignpdf.Constants;

/**
 * Expands normal JSignPdf placeholders and optional formatted timestamp
 * placeholders such as ${timestamp:yyyy.MM.dd}.
 */
public final class TextTimestampSubstitutor {

    private TextTimestampSubstitutor() {
    }

    public static String replace(Object template, Map<String, String> replacements) {
        return replace(template, replacements, null);
    }

    /**
     * Expands the placeholders in {@code template}. Formatted timestamp
     * placeholders (${timestamp:pattern}) are rendered from {@code signDate}, or
     * left untouched when it is null. Everything is resolved in a single
     * {@link StrSubstitutor} pass, so $$ escaping, ${var:-default} and cycle
     * detection apply to the timestamp placeholders as well and the result never
     * depends on map iteration order.
     */
    public static String replace(Object template, Map<String, String> replacements, Date signDate) {
        if (template == null) {
            return null;
        }
        final Map<String, String> values = replacements == null ? Collections.emptyMap() : replacements;
        return new StrSubstitutor(new TimestampLookup(values, signDate)).replace(String.valueOf(template));
    }

    /**
     * Resolves {@code timestamp:<pattern>} against the sign date and every other name against the
     * replacement map. Returning {@code null} leaves the placeholder in the text verbatim, which is
     * what an unknown name, an invalid pattern or a missing sign date should look like.
     */
    private static final class TimestampLookup extends StrLookup<String> {
        private static final String TIMESTAMP_PREFIX = Constants.L2TEXT_PLACEHOLDER_TIMESTAMP + ":";

        private final Map<String, String> replacements;
        private final Date signDate;

        TimestampLookup(Map<String, String> replacements, Date signDate) {
            this.replacements = replacements;
            this.signDate = signDate;
        }

        @Override
        public String lookup(String key) {
            if (signDate != null && key.startsWith(TIMESTAMP_PREFIX)) {
                try {
                    // Explicit locale so month/day names do not silently vary with the JVM default.
                    return new SimpleDateFormat(key.substring(TIMESTAMP_PREFIX.length()), Locale.getDefault())
                            .format(signDate);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
            return replacements.get(key);
        }
    }
}
