package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.jsignpdf.Constants;

/**
 * Unit tests for {@link UiLocale}: tag resolution (including underscore normalisation and the legacy {@code no -> nb}
 * mapping) and the install, which moves the base default and the DISPLAY category while pinning FORMAT to the OS
 * locale, so signed output cannot change.
 */
public class UiLocaleTest {

    private Locale savedDefault;
    private Locale savedDisplay;
    private Locale savedFormat;

    @Before
    public void saveDefaults() {
        savedDefault = Locale.getDefault();
        savedDisplay = Locale.getDefault(Locale.Category.DISPLAY);
        savedFormat = Locale.getDefault(Locale.Category.FORMAT);
    }

    @After
    public void restoreDefaults() {
        Locale.setDefault(savedDefault);
        Locale.setDefault(Locale.Category.DISPLAY, savedDisplay);
        Locale.setDefault(Locale.Category.FORMAT, savedFormat);
        // init() swaps the process-wide Constants.RES; put it back so test execution order cannot leak a translated
        // bundle into a later test in the same surefire fork.
        Constants.RES.reload(UiLocale.bundle(savedDisplay));
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
        assertNull("garbage", UiLocale.resolve("@@@"));
    }

    @Test
    public void resolveExpandsRegionlessChinese() {
        assertEquals("zh-CN", UiLocale.resolve("zh").toLanguageTag());
        assertEquals("zh-TW", UiLocale.resolve("zh-TW").toLanguageTag());
    }

    @Test
    public void canonicalTagMapsToASelectorItem() {
        assertEquals("de", UiLocale.canonicalTag("de"));
        assertEquals("zh-CN", UiLocale.canonicalTag("zh_CN"));
        assertEquals("regional variant collapses to its bundle", "de", UiLocale.canonicalTag("de-AT"));
        assertEquals("nb", UiLocale.canonicalTag("nb-NO"));
        assertEquals("nb", UiLocale.canonicalTag("no"));
        assertEquals("unsupported means System default", "", UiLocale.canonicalTag("xx"));
        assertEquals("", UiLocale.canonicalTag(""));
        assertEquals("", UiLocale.canonicalTag(null));
    }

    @Test
    public void initInstallsBaseAndDisplayButPinsFormatToTheOsLocale() {
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
        UiLocale.init(new String[] { "-o", "ui.language=de" });
        assertEquals("de", UiLocale.current().getLanguage());
        assertEquals("DISPLAY category follows the choice", "de",
                Locale.getDefault(Locale.Category.DISPLAY).getLanguage());
        // getBundle(baseName) - as JavaFX uses for its own control strings - resolves against the base default, not
        // the DISPLAY category, so the base default has to move too or the UI ends up half-translated.
        assertEquals("base default follows the choice", "de", Locale.getDefault().getLanguage());
        assertEquals("FORMAT category is left untouched", Locale.US, Locale.getDefault(Locale.Category.FORMAT));
    }

    @Test
    public void initKeepsSystemDefaultForUnknownOverride() {
        Locale.setDefault(Locale.ITALIAN);
        UiLocale.init(new String[] { "-o", "ui.language=xx" });
        assertEquals(UiLocale.systemDefault(), UiLocale.current());
        assertEquals("DISPLAY category left untouched", Locale.ITALIAN,
                Locale.getDefault(Locale.Category.DISPLAY));
        assertEquals("base default left untouched", Locale.ITALIAN, Locale.getDefault());
    }

    @Test
    public void systemDefaultIgnoresInstalledChoice() {
        Locale osLocale = UiLocale.systemDefault();
        UiLocale.init(new String[] { "-o", "ui.language=cs" });
        assertEquals("cs", Locale.getDefault(Locale.Category.DISPLAY).getLanguage());
        assertEquals("systemDefault() reports the OS locale, not the chosen one", osLocale, UiLocale.systemDefault());
    }
}
