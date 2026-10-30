package net.sf.jsignpdf.pkcs11;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and edits single-line keys and {@code #jsignpdf:} metadata comments in a SunPKCS11 config text, keeping every
 * other line untouched.
 */
public final class Pkcs11ConfigText {

    public static final String META_PREFIX = "#jsignpdf:";
    public static final String META_LABEL = "label";
    public static final String META_PROVIDER = "provider";
    public static final String META_CATALOG = "catalog";

    public static final String KEY_NAME = "name";
    public static final String KEY_LIBRARY = "library";

    private Pkcs11ConfigText() {
    }

    /**
     * Returns the {@code #jsignpdf:key=value} metadata, in file order.
     */
    public static Map<String, String> metadata(String body) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String line : lines(body)) {
            String t = line.trim();
            if (t.startsWith(META_PREFIX)) {
                String kv = t.substring(META_PREFIX.length());
                int eq = kv.indexOf('=');
                if (eq > 0) {
                    result.putIfAbsent(kv.substring(0, eq).trim(), kv.substring(eq + 1).trim());
                }
            }
        }
        return result;
    }

    /**
     * Returns the value of the first non-comment {@code key=value} line, or {@code null}. Surrounding double quotes are
     * removed.
     */
    public static String value(String body, String key) {
        for (String line : lines(body)) {
            String v = valueOf(line, key);
            if (v != null) {
                return unquote(v);
            }
        }
        return null;
    }

    /**
     * Sets a single-line key. A {@code null} or empty value removes the line.
     */
    public static String withValue(String body, String key, String value) {
        List<String> out = new ArrayList<>();
        boolean done = false;
        boolean remove = value == null || value.isEmpty();
        for (String line : lines(body)) {
            if (valueOf(line, key) != null) {
                if (!done && !remove) {
                    out.add(key + "=" + value);
                }
                done = true;
                continue;
            }
            out.add(line);
        }
        if (!done && !remove) {
            int idx = firstNonMetadataIndex(out);
            if (KEY_NAME.equals(key)) {
                out.add(idx, key + "=" + value);
            } else {
                out.add(key + "=" + value);
            }
        }
        return join(out, body);
    }

    /**
     * Sets a {@code #jsignpdf:} metadata line. A {@code null} or empty value removes it. New lines go to the top.
     */
    public static String withMetadata(String body, String key, String value) {
        List<String> out = new ArrayList<>();
        boolean done = false;
        boolean remove = value == null || value.isEmpty();
        String prefix = META_PREFIX + key + "=";
        for (String line : lines(body)) {
            String t = line.trim();
            if (t.startsWith(META_PREFIX) && t.substring(META_PREFIX.length()).split("=", 2)[0].trim().equals(key)) {
                if (!done && !remove) {
                    out.add(prefix + value);
                }
                done = true;
                continue;
            }
            out.add(line);
        }
        if (!done && !remove) {
            out.add(firstNonMetadataIndex(out), prefix + value);
        }
        return join(out, body);
    }

    /**
     * Formats a library path as a SunPKCS11 {@code library=} value: backslashes become forward slashes (the parser
     * treats a backslash in a quoted string as an escape) and the value is quoted when it holds characters the parser
     * does not accept in a bare word.
     */
    public static String formatLibraryValue(String path) {
        return quoteIfNeeded(path.replace('\\', '/'));
    }

    /**
     * Wraps a value in double quotes unless it is a single word for the SunPKCS11 parser.
     */
    public static String quoteIfNeeded(String value) {
        if (value.matches("[A-Za-z0-9:._/$*+~-]+")) {
            return value;
        }
        return "\"" + value.replace("\"", "") + "\"";
    }

    private static int firstNonMetadataIndex(List<String> lines) {
        int i = 0;
        while (i < lines.size() && lines.get(i).trim().startsWith(META_PREFIX)) {
            i++;
        }
        return i;
    }

    private static String valueOf(String line, String key) {
        String t = line.trim();
        if (t.isEmpty() || t.startsWith("#") || !t.startsWith(key)) {
            return null;
        }
        String rest = t.substring(key.length()).trim();
        if (!rest.startsWith("=")) {
            return null;
        }
        return rest.substring(1).trim();
    }

    private static String unquote(String v) {
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    static List<String> lines(String body) {
        List<String> result = new ArrayList<>();
        if (body == null || body.isEmpty()) {
            return result;
        }
        for (String l : body.split("\\r?\\n", -1)) {
            result.add(l);
        }
        if (!result.isEmpty() && result.get(result.size() - 1).isEmpty()) {
            result.remove(result.size() - 1);
        }
        return result;
    }

    private static String join(List<String> lines, String original) {
        String nl = original != null && original.contains("\r\n") ? "\r\n" : "\n";
        return lines.isEmpty() ? "" : String.join(nl, lines) + nl;
    }
}
