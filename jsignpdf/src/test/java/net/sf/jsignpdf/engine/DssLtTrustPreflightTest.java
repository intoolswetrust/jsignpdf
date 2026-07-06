package net.sf.jsignpdf.engine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.engine.DssLtTrustPreflight.Result;
import net.sf.jsignpdf.types.PadesLevel;

import org.junit.Test;

/**
 * Unit tests for {@link DssLtTrustPreflight}: the config preflight that catches an LT/LTA request the DSS
 * engine isn't configured to satisfy (issue #432) before any signing work happens.
 *
 * @author Josef Cacek
 */
public class DssLtTrustPreflightTest {

    private static final SigningEngine LT_ENGINE =
            new StubSigningEngine("dss", Capability.PADES_BASELINE_LT, Capability.PADES_BASELINE_LTA);

    @Test
    public void ltWithOnlineAndTrustHasNoIssues() {
        Result r = check(PadesLevel.BASELINE_LT, LT_ENGINE, Map.of(
                "online.enabled", "true", "trust.eu.enabled", "true"));
        assertFalse("a fully configured LT request must pass", r.hasIssues());
    }

    @Test
    public void ltMissingOnlineAndTrustFlagsBoth() {
        Result r = check(PadesLevel.BASELINE_LT, LT_ENGINE, Map.of());
        assertTrue(r.hasIssues());
        assertTrue("online must be flagged", r.onlineMissing());
        assertTrue("trust source must be flagged", r.trustSourceMissing());
    }

    @Test
    public void ltMissingOnlyOnline() {
        Result r = check(PadesLevel.BASELINE_LT, LT_ENGINE, Map.of("trust.eu.enabled", "true"));
        assertTrue(r.hasIssues());
        assertTrue(r.onlineMissing());
        assertFalse(r.trustSourceMissing());
    }

    @Test
    public void ltMissingOnlyTrust() {
        Result r = check(PadesLevel.BASELINE_LT, LT_ENGINE, Map.of("online.enabled", "true"));
        assertTrue(r.hasIssues());
        assertFalse(r.onlineMissing());
        assertTrue(r.trustSourceMissing());
    }

    @Test
    public void truststoreFileCountsAsTrustSource() {
        Result r = check(PadesLevel.BASELINE_LTA, LT_ENGINE, Map.of(
                "online.enabled", "true", "trust.truststoreFile", "/path/to/ts.p12"));
        assertFalse("a configured truststore satisfies the trust requirement", r.hasIssues());
    }

    @Test
    public void euLotlToggleIsNotACustomTrustSource() {
        // trust.eu.enabled is the bundled default LOTL, not user-supplied material: the GUI auto-fix may
        // (re)enable it, so it must not count as a custom source.
        Result r = check(PadesLevel.BASELINE_LT, LT_ENGINE, Map.of(
                "online.enabled", "true", "trust.eu.enabled", "true"));
        assertFalse("EU LOTL toggle is not a custom trust source", r.customTrustSourceConfigured());
    }

    @Test
    public void customTrustSourceIsFlaggedAndPreservedByAutofix() {
        // A user-supplied source (e.g. certFiles) must be reported so the GUI leaves the EU LOTL off and does
        // not clobber the user's own trust material.
        Result r = check(PadesLevel.BASELINE_LT, LT_ENGINE, Map.of(
                "online.enabled", "true", "trust.certFiles", "/path/to/ca.pem"));
        assertTrue("certFiles is a custom trust source", r.customTrustSourceConfigured());
    }

    @Test
    public void noCustomTrustSourceWhenNothingConfigured() {
        Result r = check(PadesLevel.BASELINE_LT, LT_ENGINE, Map.of());
        assertFalse("no trust material means no custom source (auto-fix will enable the EU LOTL)",
                r.customTrustSourceConfigured());
    }

    @Test
    public void lotlUrlsCountAsTrustSource() {
        Result r = check(PadesLevel.BASELINE_LT, LT_ENGINE, Map.of(
                "online.enabled", "true", "trust.lotlUrls", "https://a.test/lotl.xml"));
        assertFalse("custom lotlUrls satisfy the trust requirement", r.hasIssues());
    }

    @Test
    public void baselineBIsNotApplicable() {
        Result r = check(PadesLevel.BASELINE_B, LT_ENGINE, Map.of());
        assertFalse("B/T levels do not need the LT trust material", r.hasIssues());
    }

    @Test
    public void nullLevelIsNotApplicable() {
        Result r = check(null, LT_ENGINE, Map.of());
        assertFalse(r.hasIssues());
    }

    @Test
    public void engineWithoutLtCapabilityIsNotApplicable() {
        SigningEngine noLt = new StubSigningEngine("openpdf", Capability.PADES_BASELINE_B);
        Result r = check(PadesLevel.BASELINE_LT, noLt, Map.of());
        assertFalse("an engine that cannot produce LT is out of scope here", r.hasIssues());
    }

    private static Result check(PadesLevel level, SigningEngine engine, Map<String, String> cfg) {
        BasicSignerOptions o = new BasicSignerOptions();
        o.setPadesLevel(level);
        return DssLtTrustPreflight.check(o, engine, new MapEngineConfig(cfg));
    }

    /** Minimal in-memory {@link EngineConfig} backed by a map (keys already engine-relative). */
    private static final class MapEngineConfig implements EngineConfig {
        private final Map<String, String> map;

        MapEngineConfig(Map<String, String> map) {
            this.map = new HashMap<>(map);
        }

        @Override
        public String getString(String key) {
            return map.get(key);
        }

        @Override
        public String getString(String key, String fallback) {
            return map.getOrDefault(key, fallback);
        }

        @Override
        public boolean getBoolean(String key, boolean fallback) {
            String v = map.get(key);
            return v == null ? fallback : Boolean.parseBoolean(v);
        }

        @Override
        public int getInt(String key, int fallback) {
            String v = map.get(key);
            try {
                return v == null ? fallback : Integer.parseInt(v);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
    }
}
