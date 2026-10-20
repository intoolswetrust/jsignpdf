package net.sf.jsignpdf.pkcs11;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader for the bundled PKCS#11 catalog. Produces {@link Map}, {@link List}, {@link String},
 * {@link Long}, {@link Double}, {@link Boolean} and {@code null}.
 */
final class MiniJson {

    private final String s;
    private int pos;

    private MiniJson(String s) {
        this.s = s;
    }

    static Object parse(String text) {
        MiniJson p = new MiniJson(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.pos != p.s.length()) {
            throw p.error("Trailing content");
        }
        return v;
    }

    private Object value() {
        if (pos >= s.length()) {
            throw error("Unexpected end");
        }
        char c = s.charAt(pos);
        switch (c) {
            case '{':
                return object();
            case '[':
                return array();
            case '"':
                return string();
            case 't':
                return literal("true", Boolean.TRUE);
            case 'f':
                return literal("false", Boolean.FALSE);
            case 'n':
                return literal("null", null);
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return number();
                }
                throw error("Unexpected character '" + c + "'");
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        pos++;
        ws();
        if (peek('}')) {
            pos++;
            return m;
        }
        while (true) {
            ws();
            if (!peek('"')) {
                throw error("Expected a key");
            }
            String k = string();
            ws();
            expect(':');
            ws();
            if (m.containsKey(k)) {
                throw error("Duplicate key " + k);
            }
            m.put(k, value());
            ws();
            if (peek(',')) {
                pos++;
                continue;
            }
            expect('}');
            return m;
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        pos++;
        ws();
        if (peek(']')) {
            pos++;
            return l;
        }
        while (true) {
            ws();
            l.add(value());
            ws();
            if (peek(',')) {
                pos++;
                continue;
            }
            expect(']');
            return l;
        }
    }

    private String string() {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < s.length()) {
            char c = s.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                if (pos >= s.length()) {
                    break;
                }
                char e = s.charAt(pos++);
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > s.length()) {
                            throw error("Bad unicode escape");
                        }
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw error("Bad escape");
                }
            } else if (c < 0x20) {
                throw error("Control character in string");
            } else {
                sb.append(c);
            }
        }
        throw error("Unterminated string");
    }

    private Object number() {
        int start = pos;
        if (peek('-')) {
            pos++;
        }
        boolean fraction = false;
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c >= '0' && c <= '9') {
                pos++;
            } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                fraction = true;
                pos++;
            } else {
                break;
            }
        }
        String n = s.substring(start, pos);
        try {
            return fraction ? (Object) Double.valueOf(n) : (Object) Long.valueOf(n);
        } catch (NumberFormatException e) {
            throw error("Bad number " + n);
        }
    }

    private Object literal(String word, Object v) {
        if (!s.startsWith(word, pos)) {
            throw error("Unexpected token");
        }
        pos += word.length();
        return v;
    }

    private boolean peek(char c) {
        return pos < s.length() && s.charAt(pos) == c;
    }

    private void expect(char c) {
        if (!peek(c)) {
            throw error("Expected '" + c + "'");
        }
        pos++;
    }

    private void ws() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
            pos++;
        }
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException(msg + " at offset " + pos);
    }
}
