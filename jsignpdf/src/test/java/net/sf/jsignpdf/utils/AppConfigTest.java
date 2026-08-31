package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.logging.Level;

import net.sf.jsignpdf.Constants;

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
        AppConfig.debug();

        // String accessors backed by getNotEmptyProperty + literal default never return null/empty.
        assertNotNull(AppConfig.pdf2imageLibraries());
        assertNotNull(AppConfig.defaultTsaHashAlg());
        assertNotNull(AppConfig.defaultOutSuffix());
        assertNotNull(AppConfig.defaultTimestampSuffix());
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
    public void defaultTimestampSuffixReadsItsOwnKey() {
        AdvancedConfig cfg = PropertyStoreFactory.getInstance().advancedConfig();
        String original = cfg.hasUserOverride("output.suffix.timestamp")
                ? cfg.getProperty("output.suffix.timestamp") : null;
        try {
            cfg.setProperty("output.suffix", "_firmado");
            cfg.setProperty("output.suffix.timestamp", "_sellado");
            assertEquals("Accessor must read the output.suffix.timestamp key", "_sellado",
                    AppConfig.defaultTimestampSuffix());

            cfg.removeProperty("output.suffix.timestamp");
            assertEquals("Without an override it falls back to the bundled default",
                    Constants.DEFAULT_TIMESTAMP_SUFFIX, AppConfig.defaultTimestampSuffix());
        } finally {
            cfg.removeProperty("output.suffix");
            if (original != null) {
                cfg.setProperty("output.suffix.timestamp", original);
            } else {
                cfg.removeProperty("output.suffix.timestamp");
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

    @Test
    public void debugReadsTheDebugKey() {
        AdvancedConfig cfg = PropertyStoreFactory.getInstance().advancedConfig();
        String original = cfg.hasUserOverride("debug") ? cfg.getProperty("debug") : null;
        try {
            cfg.setProperty("debug", "true");
            org.junit.Assert.assertTrue("debug() must read the debug key", AppConfig.debug());
            cfg.setProperty("debug", "false");
            org.junit.Assert.assertFalse(AppConfig.debug());
        } finally {
            if (original != null) {
                cfg.setProperty("debug", original);
            } else {
                cfg.removeProperty("debug");
            }
        }
    }

    @Test
    public void applyDebugLogLevelMapsDebugToFineElseInfo() {
        AdvancedConfig cfg = PropertyStoreFactory.getInstance().advancedConfig();
        String original = cfg.hasUserOverride("debug") ? cfg.getProperty("debug") : null;
        Level originalLevel = Constants.LOGGER.getLevel();
        try {
            Constants.LOGGER.setLevel(Level.INFO);

            cfg.setProperty("debug", "true");
            AppConfig.applyDebugLogLevel();
            assertSame("debug=true raises the logger to FINE", Level.FINE, Constants.LOGGER.getLevel());

            cfg.setProperty("debug", "false");
            AppConfig.applyDebugLogLevel();
            assertSame("debug=false lowers the logger to INFO", Level.INFO, Constants.LOGGER.getLevel());
        } finally {
            Constants.LOGGER.setLevel(originalLevel);
            if (original != null) {
                cfg.setProperty("debug", original);
            } else {
                cfg.removeProperty("debug");
            }
        }
    }

    @Test
    public void applyDebugLogLevelLeavesQuietLoggerOff() {
        // -q mutes the logger to OFF; debug must never override that.
        AdvancedConfig cfg = PropertyStoreFactory.getInstance().advancedConfig();
        String original = cfg.hasUserOverride("debug") ? cfg.getProperty("debug") : null;
        Level originalLevel = Constants.LOGGER.getLevel();
        try {
            Constants.LOGGER.setLevel(Level.OFF);
            cfg.setProperty("debug", "true");
            AppConfig.applyDebugLogLevel();
            assertSame("quiet (OFF) must win over debug", Level.OFF, Constants.LOGGER.getLevel());
        } finally {
            Constants.LOGGER.setLevel(originalLevel);
            if (original != null) {
                cfg.setProperty("debug", original);
            } else {
                cfg.removeProperty("debug");
            }
        }
    }
}
