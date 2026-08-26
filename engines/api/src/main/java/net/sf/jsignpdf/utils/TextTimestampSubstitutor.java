package net.sf.jsignpdf.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.text.StrSubstitutor;

/**
 * Expands normal JSignPdf placeholders and optional formatted timestamp
 * placeholders such as ${timestamp:yyyy.MM.dd}.
 */
public final class TextTimestampSubstitutor {
    private static final Pattern FORMATTED_TIMESTAMP = Pattern.compile("\\$\\{timestamp:([^}]+)}");

    private TextTimestampSubstitutor() {
    }

    public static String replace(Object template, Map<String, String> replacements) {
        return replace(template, replacements, null);
    }

    /**
     * Expands the placeholders in {@code template}. Formatted timestamp
     * placeholders (${timestamp:pattern}) are rendered from {@code signDate}, or
     * left untouched when it is null. The remaining ${...} placeholders are
     * resolved by {@link StrSubstitutor}, so $$ escaping, ${var:-default} and
     * cycle detection all apply and the result never depends on map iteration
     * order.
     */
    public static String replace(Object template, Map<String, String> replacements, Date signDate) {
        if (template == null) {
            return null;
        }

        String result = String.valueOf(template);
        if (signDate != null && result.contains("${timestamp:")) {
            result = expandFormattedTimestamps(result, signDate);
        }

        if (replacements == null || replacements.isEmpty()) {
            return result;
        }
        return new StrSubstitutor(replacements).replace(result);
    }

    private static String expandFormattedTimestamps(String template, Date signDate) {
        Matcher matcher = FORMATTED_TIMESTAMP.matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(0);
            try {
                // Explicit locale so month/day names do not silently vary with the JVM default.
                replacement = new SimpleDateFormat(matcher.group(1), Locale.getDefault()).format(signDate);
            } catch (IllegalArgumentException ignored) {
                // Invalid format: leave the original placeholder visible.
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
