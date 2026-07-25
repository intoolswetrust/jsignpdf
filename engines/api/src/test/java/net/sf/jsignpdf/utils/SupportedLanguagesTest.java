package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;

import org.junit.Test;

/**
 * Guards {@link SupportedLanguages#tags()} against the actual {@code messages*.properties} translation files: when
 * Weblate adds (or someone removes) a language, this test fails naming the diverging tag. Also verifies that the
 * renamed Norwegian bundle is really loaded for both {@code nb} and {@code nb-NO}.
 */
public class SupportedLanguagesTest {

    private static final String DEFAULT_BUNDLE = "/net/sf/jsignpdf/translations/messages.properties";

    @Test
    public void tagsMatchTranslationFiles() throws Exception {
        TreeSet<String> fromFiles = new TreeSet<>(tagsFromTranslationFiles());
        TreeSet<String> fromCode = new TreeSet<>(SupportedLanguages.tags());
        assertEquals("SupportedLanguages.tags() diverges from the messages*.properties files", fromFiles, fromCode);
    }

    @Test
    public void norwegianBundleResolvesForNbAndNbNo() {
        // A known key whose Norwegian value differs from English, proving the nb file (not the English base) is used.
        String english = ResourceBundle.getBundle(net.sf.jsignpdf.Constants.RESOURCE_BUNDLE_BASE, Locale.ENGLISH)
                .getString("console.exception");
        for (Locale locale : new Locale[] { Locale.forLanguageTag("nb"), Locale.forLanguageTag("nb-NO") }) {
            ResourceBundle bundle = ResourceBundle.getBundle(net.sf.jsignpdf.Constants.RESOURCE_BUNDLE_BASE, locale,
                    ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
            String value = bundle.getString("console.exception");
            assertTrue("Expected Norwegian text for " + locale + " but got the English base value",
                    !value.equals(english));
        }
    }

    @Test
    public void isSupportedMatchesLanguageAndRegion() {
        assertTrue(SupportedLanguages.isSupported(Locale.forLanguageTag("de")));
        assertTrue(SupportedLanguages.isSupported(Locale.forLanguageTag("zh-CN")));
        assertTrue("nb-NO resolves via language nb", SupportedLanguages.isSupported(Locale.forLanguageTag("nb-NO")));
        assertTrue("pt-BR resolves via language pt", SupportedLanguages.isSupported(Locale.forLanguageTag("pt-BR")));
    }

    /** Maps {@code messages_<code>.properties} file names to BCP-47 tags (underscore -> hyphen), plus base {@code en}. */
    private static TreeSet<String> tagsFromTranslationFiles() throws Exception {
        URL url = SupportedLanguagesTest.class.getResource(DEFAULT_BUNDLE);
        assertNotNull("Default bundle not on classpath: " + DEFAULT_BUNDLE, url);
        Path dir = Paths.get(url.toURI()).getParent();
        TreeSet<String> tags = new TreeSet<>();
        tags.add("en");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "messages_*.properties")) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                String code = name.substring("messages_".length(), name.length() - ".properties".length());
                tags.add(code.replace('_', '-'));
            }
        }
        return tags;
    }
}
