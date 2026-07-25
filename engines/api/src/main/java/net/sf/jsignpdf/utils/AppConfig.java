package net.sf.jsignpdf.utils;

import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.engine.AdvancedEngineConfig;
import net.sf.jsignpdf.engine.EngineConfig;

/**
 * Static facade over {@link AdvancedConfig}. Call sites read app-global toggles through these typed accessors so they stay
 * compact and don't depend on the singleton-resolution path.
 */
public final class AppConfig {

    /** Bundled-default signing engine id (used when {@code advanced.properties} has no {@code engine} key). */
    public static final String DEFAULT_ENGINE_ID = "openpdf";

    private AppConfig() {
    }

    /**
     * Identifier of the signing engine selected in {@code advanced.properties} (the GUI default and the
     * fallback for the CLI), or {@value #DEFAULT_ENGINE_ID} when unset.
     */
    public static String defaultEngineId() {
        return cfg().getNotEmptyProperty("engine", DEFAULT_ENGINE_ID);
    }

    /**
     * Returns a read-only view of the advanced configuration scoped to the {@code engine.<id>.*} namespace
     * of the given engine.
     *
     * @param engineId the engine identifier
     * @return the engine-scoped configuration view
     */
    public static EngineConfig engineConfigFor(String engineId) {
        return new AdvancedEngineConfig(cfg(), "engine." + engineId + ".");
    }

    public static boolean relaxSslSecurity() {
        return cfg().getAsBool("relax.ssl.security", false);
    }

    /**
     * BCP-47 language tag of the user interface ({@code ui.language} in {@code advanced.properties}), or {@code null}
     * when unset. Empty / unset means "follow the OS locale". Read once at startup by {@link UiLocale}.
     */
    public static String uiLanguage() {
        return cfg().getProperty(UiLocale.KEY);
    }

    /**
     * Whether verbose signing diagnostics are enabled ({@code debug} in {@code advanced.properties}). When on,
     * the signing pipeline logs the certificate chain, the loaded trust anchors, and every TSA / AIA / CRL /
     * OCSP call at {@link Level#FINE}.
     */
    public static boolean debug() {
        return cfg().getAsBool("debug", false);
    }

    /**
     * Aligns the {@code net.sf.jsignpdf} logger level with the {@link #debug()} setting: {@link Level#FINE}
     * when debug is on, {@link Level#INFO} otherwise. A logger already muted to {@link Level#OFF} (the CLI
     * {@code -q} / {@code --quiet} flag) is left untouched, so quiet always wins over debug. Safe to call at
     * startup and again whenever the setting changes (e.g. the Preferences dialog toggles it live).
     */
    public static void applyDebugLogLevel() {
        if (Constants.LOGGER.getLevel() == Level.OFF) {
            return;
        }
        Constants.LOGGER.setLevel(debug() ? Level.FINE : Level.INFO);
    }

    public static String pdf2imageLibraries() {
        return cfg().getNotEmptyProperty("pdf2image.libraries", Constants.PDF2IMAGE_LIBRARIES_DEFAULT);
    }

    public static String defaultTsaHashAlg() {
        return cfg().getNotEmptyProperty("tsa.hashAlgorithm", "SHA-256");
    }

    /**
     * Suffix appended to the input file name to build the default output file name (the GUI suggestion and the
     * fallback for the CLI {@code -osuffix} option), or {@value Constants#DEFAULT_OUT_SUFFIX} when unset. Lets users
     * localize the {@code _signed} marker (e.g. {@code _signe}, {@code _firmado}, {@code _unterschrieben}).
     */
    public static String defaultOutSuffix() {
        return cfg().getNotEmptyProperty("output.suffix", Constants.DEFAULT_OUT_SUFFIX);
    }

    public static boolean checkValidity() {
        return cfg().getAsBool("certificate.checkValidity", true);
    }

    public static boolean checkKeyUsage() {
        return cfg().getAsBool("certificate.checkKeyUsage", true);
    }

    public static boolean checkCriticalExtensions() {
        return cfg().getAsBool("certificate.checkCriticalExtensions", false);
    }

    public static String fontPath() {
        return cfg().getNotEmptyProperty("font.path", null);
    }

    public static String fontName() {
        return cfg().getNotEmptyProperty("font.name", null);
    }

    public static String fontEncoding() {
        return cfg().getNotEmptyProperty("font.encoding", null);
    }

    /**
     * Installs CLI-supplied {@code advanced.properties} overrides into the shared advanced configuration as a transient,
     * highest-priority layer. Applied entries are logged at INFO with secret values masked. No-op for a {@code null} or
     * empty map.
     */
    public static void applyAdvancedOverrides(Map<String, String> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return;
        }
        overrides.forEach((k, v) -> Constants.LOGGER.info("Applied advanced override: " + k + "=" + mask(k, v)));
        cfg().applyOverrides(overrides);
    }

    private static String mask(String key, String value) {
        String lower = key == null ? "" : key.toLowerCase(Locale.ENGLISH);
        return lower.contains("password") || lower.contains("pwd") ? "***" : value;
    }

    private static AdvancedConfig cfg() {
        return PropertyStoreFactory.getInstance().advancedConfig();
    }
}
