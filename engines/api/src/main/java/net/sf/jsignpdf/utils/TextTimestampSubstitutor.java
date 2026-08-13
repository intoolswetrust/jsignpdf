package net.sf.jsignpdf.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands normal JSignPdf placeholders and optional formatted timestamp
 * placeholders such as ${timestamp:yyyy.MM.dd}.
 */
public final class TextTimestampSubstitutor {
    private static final String BASE_TIMESTAMP_PATTERN = "yyyy.MM.dd HH:mm:ss z";
    private static final Pattern FORMATTED_TIMESTAMP = Pattern.compile("\\$\\{timestamp:([^}]+)}");
    private static final DateTimeFormatter WALL_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    private TextTimestampSubstitutor() {
    }

    public static String replace(Object template, Map<?, ?> replacements) {
        if (template == null) {
            return null;
        }

        String result = String.valueOf(template);
        Object timestamp = replacements == null ? null : replacements.get("timestamp");
        if (timestamp != null && result.contains("${timestamp:")) {
            result = expandFormattedTimestamps(result, String.valueOf(timestamp));
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

    private static String expandFormattedTimestamps(String template, String baseTimestamp) {
        Matcher matcher = FORMATTED_TIMESTAMP.matcher(template);
        if (!matcher.find()) {
            return template;
        }

        Date date = null;
        TimeZone timeZone = null;
        try {
            SimpleDateFormat baseFormat = new SimpleDateFormat(BASE_TIMESTAMP_PATTERN);
            baseFormat.setLenient(false);
            date = baseFormat.parse(baseTimestamp);
            timeZone = resolveTimestampOffset(baseTimestamp, date);
        } catch (ParseException ignored) {
            // Keep formatted placeholders unchanged if the base timestamp cannot be parsed.
        }

        matcher.reset();
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(0);
            if (date != null) {
                try {
                    SimpleDateFormat requestedFormat = new SimpleDateFormat(matcher.group(1));
                    if (timeZone != null) {
                        requestedFormat.setTimeZone(timeZone);
                    }
                    replacement = requestedFormat.format(date);
                } catch (IllegalArgumentException ignored) {
                    // Invalid format: leave the original placeholder visible.
                }
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    /**
     * SimpleDateFormat parses a zone token into the resulting instant but does
     * not reliably retain that parsed zone on its Calendar. Reconstruct the
     * effective offset from the wall-clock part and the parsed instant so a
     * formatted placeholder keeps the same local date/time as ${timestamp}.
     */
    private static TimeZone resolveTimestampOffset(String baseTimestamp, Date instant) {
        try {
            if (baseTimestamp.length() < 19) {
                return TimeZone.getDefault();
            }
            LocalDateTime wallTime = LocalDateTime.parse(
                    baseTimestamp.substring(0, 19), WALL_TIME_FORMAT);
            long wallAsUtcMillis = wallTime.toInstant(ZoneOffset.UTC).toEpochMilli();
            long offsetMillis = wallAsUtcMillis - instant.getTime();
            long maxReasonableOffset = 18L * 60L * 60L * 1000L;
            if (Math.abs(offsetMillis) <= maxReasonableOffset) {
                return new SimpleTimeZone((int) offsetMillis, "timestamp-offset");
            }
        } catch (RuntimeException ignored) {
            // Fall back to the JVM zone below.
        }
        return TimeZone.getDefault();
    }
}
