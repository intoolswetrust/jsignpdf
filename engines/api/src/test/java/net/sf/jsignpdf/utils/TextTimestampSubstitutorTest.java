package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

public class TextTimestampSubstitutorTest {

    private static Map<String, String> map(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }

    private static Date date(String yyyyMmDd) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd").parse(yyyyMmDd);
    }

    @Test
    public void nullTemplateReturnsNull() {
        assertNull(TextTimestampSubstitutor.replace(null, map("signer", "Alice")));
    }

    @Test
    public void plainPlaceholdersAreReplaced() {
        assertEquals("Signed by Alice",
                TextTimestampSubstitutor.replace("Signed by ${signer}", map("signer", "Alice")));
    }

    @Test
    public void formattedTimestampUsesSignDate() throws Exception {
        Date signDate = date("2026-08-26");
        assertEquals("Date: 2026.08.26",
                TextTimestampSubstitutor.replace("Date: ${timestamp:yyyy.MM.dd}", map(), signDate));
    }

    @Test
    public void formattedTimestampKeepsInternalColons() throws Exception {
        Date signDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2026-08-26 14:05");
        assertEquals("14:05",
                TextTimestampSubstitutor.replace("${timestamp:HH:mm}", map(), signDate));
    }

    @Test
    public void invalidPatternIsLeftVisible() throws Exception {
        assertEquals("${timestamp:qqqq}",
                TextTimestampSubstitutor.replace("${timestamp:qqqq}", map(), date("2026-08-26")));
    }

    @Test
    public void formattedTimestampLeftLiteralWithoutSignDate() {
        assertEquals("${timestamp:yyyy}",
                TextTimestampSubstitutor.replace("${timestamp:yyyy}", map()));
    }

    @Test
    public void substitutionIsIndependentOfMapOrder() {
        // A value that itself contains a placeholder must resolve the same way
        // regardless of iteration order (the old sequential replace did not).
        String template = "${certificate}";
        String forward = TextTimestampSubstitutor.replace(template, map("certificate", "CN=${reason}", "reason", "R"));
        String reverse = TextTimestampSubstitutor.replace(template, map("reason", "R", "certificate", "CN=${reason}"));
        assertEquals("CN=R", forward);
        assertEquals(forward, reverse);
    }

    @Test
    public void dollarEscapeIsHonoured() {
        assertEquals("${signer}",
                TextTimestampSubstitutor.replace("$${signer}", map("signer", "Alice")));
    }

    @Test
    public void defaultValueSyntaxIsHonoured() {
        assertEquals("fallback",
                TextTimestampSubstitutor.replace("${missing:-fallback}", map("signer", "Alice")));
    }

    @Test
    public void formattedAndPlainCombine() throws Exception {
        Date signDate = date("2026-08-26");
        String out = TextTimestampSubstitutor.replace(
                "${signer} on ${timestamp:yyyy.MM.dd}", map("signer", "Alice"), signDate);
        assertTrue(out, out.startsWith("Alice on 2026.08.26"));
    }

    @Test
    public void monthNameUsesExplicitLocale() throws Exception {
        Date signDate = date("2026-08-26");
        String expected = new SimpleDateFormat("MMMM", Locale.getDefault()).format(signDate);
        assertEquals(expected,
                TextTimestampSubstitutor.replace("${timestamp:MMMM}", map(), signDate));
    }
}
