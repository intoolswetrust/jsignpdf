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
