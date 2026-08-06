package net.sf.jsignpdf.utils;

import java.util.List;
import java.util.Locale;

/**
 * The bundled UI translations, as BCP-47 language tags. Kept as a static list (not a classpath scan) so it works
 * unchanged from a jar, a jlink image or a jpackage bundle. {@code SupportedLanguagesTest} guards it against the
 * actual {@code messages*.properties} files.
 */
public final class SupportedLanguages {

    /** English (the base bundle) first; the rest sorted by tag. Region kept only where it disambiguates (zh, pt). */
    private static final List<String> TAGS = List.of(
            "en", "cs", "de", "el", "es", "fr", "hr", "hu", "hy", "it",
            "ja", "nb", "pl", "pt", "pt-BR", "ru", "sk", "ta", "zh-CN", "zh-TW");

    private SupportedLanguages() {
    }

    /** BCP-47 tags of the bundled translations, English first. */
    public static List<String> tags() {
        return TAGS;
    }

    /**
     * Whether a translation exists for the given locale. Matches the exact language+region tag (so {@code zh-CN}
     * resolves) and, failing that, the language alone (so {@code nb-NO} or {@code pt-BR} resolve to {@code nb} / {@code pt}).
     */
    public static boolean isSupported(Locale locale) {
        if (locale == null) {
            return false;
        }
        return TAGS.contains(locale.toLanguageTag()) || TAGS.contains(locale.getLanguage());
    }

    /**
     * Label for the selector: native name plus English name, e.g. {@code "Deutsch (German)"}. The English part keeps
     * the list readable to someone who cannot read the current UI language — the scenario behind issue #444. Names
     * carrying a region already bracket it, so those get an en-dash separator ({@code "中文 (中国) – Chinese (China)"})
     * rather than a second, nested pair of parentheses.
     */
    public static String displayName(Locale locale) {
        String self = locale.getDisplayName(locale);
        String english = locale.getDisplayName(Locale.ENGLISH);
        if (self.isEmpty()) {
            return english;
        }
        if (self.equalsIgnoreCase(english)) {
            return self;
        }
        if (self.indexOf('(') >= 0 || english.indexOf('(') >= 0) {
            return self + " – " + english;
        }
        return self + " (" + english + ")";
    }
}
