package net.sf.jsignpdf.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands normal JSignPdf placeholders and optional formatted timestamp
 * placeholders such as ${timestamp:yyyy.MM.dd}.
 */
public final class TextTimestampSubstitutor {
    private static final Pattern FORMATTED_TIMESTAMP = Pattern.compile("\\$\\{timestamp:([^}]+)}");

    private TextTimestampSubstitutor() {
    }

    public static String replace(Object template, Map<?, ?> replacements) {
        return replace(template, replacements, null);
    }

    /**
     * Expands the placeholders in {@code template}. Formatted timestamp
     * placeholders (${timestamp:pattern}) are rendered from {@code signDate};
     * they are left untouched when {@code signDate} is null.
     */
    public static String replace(Object template, Map<?, ?> replacements, Date signDate) {
        if (template == null) {
            return null;
        }

        String result = String.valueOf(template);
        if (signDate != null && result.contains("${timestamp:")) {
            result = expandFormattedTimestamps(result, signDate);
        }

        if (replacements == null) {
            return result;
        }
        for (Map.Entry<?, ?> entry : replacements.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, String.valueOf(entry.getValue()));
        }
        return result;
    }

    private static String expandFormattedTimestamps(String template, Date signDate) {
        Matcher matcher = FORMATTED_TIMESTAMP.matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(0);
            try {
                replacement = new SimpleDateFormat(matcher.group(1)).format(signDate);
            } catch (IllegalArgumentException ignored) {
                // Invalid format: leave the original placeholder visible.
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
