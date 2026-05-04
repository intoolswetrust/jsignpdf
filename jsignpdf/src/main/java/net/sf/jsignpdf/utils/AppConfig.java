package net.sf.jsignpdf.utils;

import net.sf.jsignpdf.Constants;

/**
 * Static facade over {@link AdvancedConfig}. Call sites read app-global toggles through these typed accessors so they stay
 * compact and don't depend on the singleton-resolution path.
 */
public final class AppConfig {

    private AppConfig() {
    }

    public static boolean relaxSslSecurity() {
        return cfg().getAsBool("relax.ssl.security", false);
    }

    public static String pdf2imageLibraries() {
        return cfg().getNotEmptyProperty("pdf2image.libraries", Constants.PDF2IMAGE_LIBRARIES_DEFAULT);
    }

    public static String defaultTsaHashAlg() {
        return cfg().getNotEmptyProperty("tsa.hashAlgorithm", "SHA-256");
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

    private static AdvancedConfig cfg() {
        return PropertyStoreFactory.getInstance().advancedConfig();
    }
}
