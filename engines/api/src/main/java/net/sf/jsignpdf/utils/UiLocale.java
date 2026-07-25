package net.sf.jsignpdf.utils;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;

import net.sf.jsignpdf.Constants;

/**
 * Resolves the configured UI locale ({@code ui.language} in the advanced configuration) and installs it before any
 * translated string is read. {@link #init(String[])} must run first thing in {@code main()}, because
 * {@link Constants#RES} and the commons-cli option descriptions capture strings during class initialization.
 *
 * <p>The scope is deliberately {@link Locale.Category#DISPLAY} only: UI text follows the choice while number/date
 * formatting keeps following the OS locale, so a UI-language preference can never change signed output.
 */
public final class UiLocale {

    /** Advanced-config key holding the BCP-47 UI language tag. */
    public static final String KEY = "ui.language";

    // getBundle(base, locale) falls back to the default-locale bundle before the base bundle. A no-fallback control
    // keeps the chain messages_<chosen> -> messages (English) only, so a missing key never leaks the OS-locale text.
    // The Control overloads are unusable from a *named* module; JSignPdf ships on the classpath (jpackage images
    // included), so this is fine.
    private static final ResourceBundle.Control NO_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    private static final Locale SYSTEM_DEFAULT = Locale.getDefault(Locale.Category.DISPLAY);

    private static volatile Locale current = SYSTEM_DEFAULT;

    private UiLocale() {
    }

    /**
     * Resolves and installs the UI locale. Reads {@code -o ui.language=<tag>} from the raw argv first (so
     * {@code --help} is consistent), then the advanced configuration. An empty, missing, unparseable or unsupported
     * value leaves the system default in place.
     */
    public static void init(String[] args) {
        String tag = scanArgs(args);
        if (tag == null) {
            tag = AppConfig.uiLanguage();
        }
        Locale locale = resolve(tag);
        if (locale == null) {
            current = SYSTEM_DEFAULT;
            return;
        }
        current = locale;
        Locale.setDefault(Locale.Category.DISPLAY, locale);
        ResourceBundle.clearCache();
        Constants.RES.reload(locale);
    }

    /** The resolved UI locale (the system default when unset). */
    public static Locale current() {
        return current;
    }

    /** The OS display locale, unaffected by an installed {@code ui.language} choice. */
    public static Locale systemDefault() {
        return SYSTEM_DEFAULT;
    }

    /** A {@link ResourceBundle} for {@link #current()} with English-only fallback (no OS-locale fallback). */
    public static ResourceBundle bundle() {
        return ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE_BASE, current, NO_FALLBACK);
    }

    /**
     * Resolves a configured tag to a supported {@link Locale}, or {@code null} to keep the system default. Underscores
     * are normalised to hyphens (so a hand-edited {@code zh_CN} works) and the legacy Norwegian macrolanguage code
     * {@code no} is mapped to {@code nb} (Java does not do this itself).
     */
    static Locale resolve(String tag) {
        if (tag == null) {
            return null;
        }
        String normalized = tag.trim().replace('_', '-');
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.equalsIgnoreCase("no")) {
            normalized = "nb";
        } else if (normalized.regionMatches(true, 0, "no-", 0, 3)) {
            normalized = "nb" + normalized.substring(2);
        }
        Locale locale = Locale.forLanguageTag(normalized);
        if (locale.getLanguage().isEmpty()) {
            Constants.LOGGER.log(Level.INFO, "Ignoring unparseable ui.language=" + tag);
            return null;
        }
        if (!SupportedLanguages.isSupported(locale)) {
            Constants.LOGGER.log(Level.INFO, "Ignoring unsupported ui.language=" + tag);
            return null;
        }
        return locale;
    }

    /**
     * Pre-scans raw argv for {@code -o ui.language=<tag>} / {@code --option ui.language=<tag>} (also the attached
     * {@code --option=...} form). A deliberate, tiny duplicate of the commons-cli parse in {@code SignerOptionsFromCmdLine},
     * whose class init already needs the final locale. Returns the last matching tag, or {@code null}.
     */
    private static String scanArgs(String[] args) {
        if (args == null) {
            return null;
        }
        String result = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null) {
                continue;
            }
            String value = null;
            if (("-" + Constants.ARG_OPTION).equals(arg) || ("--" + Constants.ARG_OPTION_LONG).equals(arg)) {
                if (i + 1 < args.length) {
                    value = args[i + 1];
                }
            } else if (arg.startsWith("--" + Constants.ARG_OPTION_LONG + "=")) {
                value = arg.substring(("--" + Constants.ARG_OPTION_LONG + "=").length());
            } else if (arg.startsWith("-" + Constants.ARG_OPTION)) {
                value = arg.substring(("-" + Constants.ARG_OPTION).length());
            }
            String tag = tagOf(value);
            if (tag != null) {
                result = tag;
            }
        }
        return result;
    }

    /** Extracts the tag from a {@code ui.language=<tag>} token, or {@code null} if it is not that key. */
    private static String tagOf(String value) {
        if (value == null) {
            return null;
        }
        String prefix = KEY + "=";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : null;
    }
}
