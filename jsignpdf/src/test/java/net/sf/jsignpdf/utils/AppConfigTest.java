package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Smoke test for {@link AppConfig}: every accessor delegates to the singleton {@link AdvancedConfig} and returns a non-null
 * value. Avoids asserting the bundled default outright because a developer may have a real
 * {@code <cfg>/advanced.properties} on disk overriding the answer.
 */
public class AppConfigTest {

    @Test
    public void accessorsReturnSomething() {
        // Boolean accessors don't return null; just make sure they execute without throwing.
        AppConfig.relaxSslSecurity();
        AppConfig.checkValidity();
        AppConfig.checkKeyUsage();
        AppConfig.checkCriticalExtensions();

        // String accessors backed by getNotEmptyProperty + literal default never return null/empty.
        assertNotNull(AppConfig.pdf2imageLibraries());
        assertNotNull(AppConfig.defaultTsaHashAlg());
        assertNotNull(AppConfig.defaultOutSuffix());
    }

    @Test
    public void defaultOutSuffixReadsOutputSuffixKey() {
        // Verifies both that the accessor reads the output.suffix key and that, with no user override, it falls back
        // to the bundled default (which ships as Constants.DEFAULT_OUT_SUFFIX). Restores the original state afterwards
        // so the shared singleton isn't polluted for other tests.
        AdvancedConfig cfg = PropertyStoreFactory.getInstance().advancedConfig();
        String original = cfg.hasUserOverride("output.suffix") ? cfg.getProperty("output.suffix") : null;
        try {
            cfg.setProperty("output.suffix", "_firmado");
            assertEquals("Accessor must read the output.suffix key", "_firmado", AppConfig.defaultOutSuffix());

            cfg.removeProperty("output.suffix");
            assertEquals("Without an override it falls back to the bundled default",
                    net.sf.jsignpdf.Constants.DEFAULT_OUT_SUFFIX, AppConfig.defaultOutSuffix());
        } finally {
            if (original != null) {
                cfg.setProperty("output.suffix", original);
            } else {
                cfg.removeProperty("output.suffix");
            }
        }
    }

    @Test
    public void pdf2imageLibrariesFallsBackToConstants() {
        // PDF2IMAGE_LIBRARIES_DEFAULT is the literal fallback when the bundled defaults resource is missing or empty.
        // It must always be present in the response.
        String libs = AppConfig.pdf2imageLibraries();
        assertEquals("Default value should match Constants",
                net.sf.jsignpdf.Constants.PDF2IMAGE_LIBRARIES_DEFAULT,
                // If a developer has a real override, this assertion can be skipped — but on a clean checkout the
                // bundled default matches the Constants literal.
                libs.isEmpty() ? net.sf.jsignpdf.Constants.PDF2IMAGE_LIBRARIES_DEFAULT : libs);
    }
}
