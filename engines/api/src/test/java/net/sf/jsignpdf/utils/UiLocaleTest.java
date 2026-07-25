package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link UiLocale}: tag resolution (including underscore normalisation and the legacy {@code no -> nb}
 * mapping) and the DISPLAY-only install (FORMAT category left untouched, so signed output cannot change).
 */
public class UiLocaleTest {

    private Locale savedDisplay;
    private Locale savedFormat;

    @Before
    public void saveDefaults() {
        savedDisplay = Locale.getDefault(Locale.Category.DISPLAY);
        savedFormat = Locale.getDefault(Locale.Category.FORMAT);
    }

    @After
    public void restoreDefaults() {
        Locale.setDefault(Locale.Category.DISPLAY, savedDisplay);
        Locale.setDefault(Locale.Category.FORMAT, savedFormat);
    }

    @Test
    public void resolveKnownTags() {
        assertEquals("de", UiLocale.resolve("de").toLanguageTag());
        assertEquals("zh-CN", UiLocale.resolve("zh-CN").toLanguageTag());
        assertEquals("nb", UiLocale.resolve("nb").toLanguageTag());
        assertEquals("nb-NO", UiLocale.resolve("nb-NO").toLanguageTag());
    }

    @Test
    public void resolveNormalisesUnderscoreAndTrims() {
        assertEquals("zh-CN", UiLocale.resolve("zh_CN").toLanguageTag());
        assertEquals("de", UiLocale.resolve("  de  ").toLanguageTag());
    }

    @Test
    public void resolveMapsLegacyNorwegianCode() {
        assertEquals("nb", UiLocale.resolve("no").toLanguageTag());
        assertEquals("nb", UiLocale.resolve("NO").toLanguageTag());
        assertEquals("nb-NO", UiLocale.resolve("no-NO").toLanguageTag());
        assertEquals("nb-NO", UiLocale.resolve("no_NO").toLanguageTag());
    }

    @Test
    public void resolveRejectsEmptyUnknownAndGarbage() {
        assertNull(UiLocale.resolve(null));
        assertNull(UiLocale.resolve(""));
        assertNull(UiLocale.resolve("   "));
        assertNull("unsupported language", UiLocale.resolve("xx"));
        assertNull("plain zh has no bundle (only zh-CN/zh-TW)", UiLocale.resolve("zh"));
        assertNull("garbage", UiLocale.resolve("@@@"));
    }

    @Test
    public void initInstallsDisplayOnlyFromCliOverride() {
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
        UiLocale.init(new String[] { "-o", "ui.language=de" });
        assertEquals("de", UiLocale.current().getLanguage());
        assertEquals("DISPLAY category follows the choice", "de",
                Locale.getDefault(Locale.Category.DISPLAY).getLanguage());
        assertEquals("FORMAT category is left untouched", Locale.US, Locale.getDefault(Locale.Category.FORMAT));
    }

    @Test
    public void initKeepsSystemDefaultForUnknownOverride() {
        Locale.setDefault(Locale.Category.DISPLAY, Locale.ITALIAN);
        UiLocale.init(new String[] { "-o", "ui.language=xx" });
        assertEquals(Locale.ITALIAN, UiLocale.current());
    }
}
