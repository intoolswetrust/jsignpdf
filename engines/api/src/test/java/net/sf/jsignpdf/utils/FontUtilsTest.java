package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.FontUtils.L2Font;

import org.junit.Test;

public class FontUtilsTest {

    @Test
    public void latinTextKeepsConfiguredFont() {
        assertResolved("Digitally signed by John Smith\nDate: 2026.11.10", "DejaVuSans.ttf");
    }

    @Test
    public void cyrillicAndGreekStayInDejaVuSans() {
        assertResolved("Подписано: Ωμέγα", "DejaVuSans.ttf");
    }

    @Test
    public void cjkTextFallsBackToNotoSansSc() {
        assertResolved("署名者: John Smith", "NotoSansSC-Regular.ttf");
        assertResolved("数字签名者： 數位簽章由 カタカナ", "NotoSansSC-Regular.ttf");
    }

    @Test
    public void tamilTextFallsBackToNotoSansTamil() {
        assertResolved("தேதி: 2026.11.10", "NotoSansTamil-Regular.ttf");
    }

    @Test
    public void uncoveredTextReportsDistinctMissingCharacters() {
        L2Font font = FontUtils.getL2Font("서명 서명 John");
        assertEquals("DejaVuSans.ttf", font.getName());
        assertEquals("서명", font.missingGlyphs("서명 서명 John"));
    }

    @Test
    public void whitespaceAndControlCharactersAreIgnored() {
        assertEquals("", FontUtils.getL2Font().missingGlyphs("a\tb\nc　d"));
    }

    @Test
    public void simpleEncodingLimitsCoverage() throws Exception {
        L2Font winAnsi = new L2Font("DejaVuSans.ttf", FontUtils.ENCODING_WINANSI, bundledDejaVuSans());
        assertEquals("", winAnsi.missingGlyphs("Ж€"));
        assertEquals("Ж", winAnsi.missingCharacters("Ж€"));
    }

    private static void assertResolved(String text, String expectedFont) {
        L2Font font = FontUtils.getL2Font(text);
        assertEquals(expectedFont, font.getName());
        assertEquals("", font.missingCharacters(text));
    }

    private static byte[] bundledDejaVuSans() throws Exception {
        try (InputStream is = FontUtils.class.getResourceAsStream(Constants.L2TEXT_FONT_PATH)) {
            return is.readAllBytes();
        }
    }
}
