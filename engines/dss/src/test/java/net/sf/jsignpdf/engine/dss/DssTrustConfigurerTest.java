package net.sf.jsignpdf.engine.dss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import net.sf.jsignpdf.engine.EngineConfig;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.source.LOTLSource;

/**
 * Offline wiring tests for {@link DssTrustConfigurer#getLotlSources()}. They assert the LOTL sources are
 * built with a non-null URL, a certificate source, the OJ announcement predicate and pivot support &mdash;
 * the regression guard for issue #432 (the bare {@code new LOTLSource()} with a null URL that NPE'd) and the
 * RC3 fix (the custom {@code lotlUrls} branch left with an empty certificate source). No network is used.
 *
 * @author Josef Cacek
 */
public class DssTrustConfigurerTest {

    @Test
    public void euEnabledBuildsEuropeanLotlSource() throws Exception {
        LOTLSource[] sources = lotlSources(Map.of(DssTrustConfigurer.KEY_EU_ENABLED, "true"));

        assertEquals("trust.eu.enabled must yield exactly one LOTL source", 1, sources.length);
        LOTLSource lotl = sources[0];
        assertEquals("default EU LOTL URL", DssTrustConfigurer.DEFAULT_EU_LOTL_URL, lotl.getUrl());
        assertNotNull("OJ keystore certificate source must be wired", lotl.getCertificateSource());
        assertTrue("OJ announcement predicate must be the Official Journal one",
                lotl.getSigningCertificatesAnnouncementPredicate() instanceof OfficialJournalSchemeInformationURI);
        assertTrue("pivot support must be enabled", lotl.isPivotSupport());
    }

    @Test
    public void euLotlUrlOverrideIsApplied() throws Exception {
        String override = "https://example.test/eu-lotl.xml";
        LOTLSource[] sources = lotlSources(Map.of(
                DssTrustConfigurer.KEY_EU_ENABLED, "true",
                DssTrustConfigurer.KEY_EU_LOTL_URL, override));

        assertEquals(1, sources.length);
        assertEquals("euLotlUrl must relocate the EU LOTL", override, sources[0].getUrl());
    }

    @Test
    public void customLotlUrlsWiredWithOjCertSourceAndPivotNoPredicate() throws Exception {
        LOTLSource[] sources = lotlSources(Map.of(
                DssTrustConfigurer.KEY_LOTL_URLS, "https://a.test/lotl.xml, https://b.test/lotl.xml"));

        assertEquals("two custom LOTL URLs -> two sources", 2, sources.length);
        for (LOTLSource lotl : sources) {
            assertTrue("custom LOTL must have a non-blank URL", StringUtils.isNotBlank(lotl.getUrl()));
            assertNotNull("custom LOTL must reuse the OJ certificate source", lotl.getCertificateSource());
            assertTrue("custom LOTL must keep pivot support", lotl.isPivotSupport());
            assertNull("custom LOTL must not carry the EU OJ predicate",
                    lotl.getSigningCertificatesAnnouncementPredicate());
        }
    }

    @Test
    public void mraSupportIsOffByDefaultAndOptInForCustomLotlUrls() throws Exception {
        LOTLSource[] defaultSources = lotlSources(Map.of(
                DssTrustConfigurer.KEY_LOTL_URLS, "https://mra.test/mra_lotl.xml"));
        assertEquals(1, defaultSources.length);
        assertFalse("MRA support must be off by default", defaultSources[0].isMraSupport());

        LOTLSource[] mraSources = lotlSources(Map.of(
                DssTrustConfigurer.KEY_LOTL_URLS, "https://mra.test/mra_lotl.xml",
                DssTrustConfigurer.KEY_LOTL_MRA_SUPPORT, "true"));
        assertEquals(1, mraSources.length);
        assertTrue("MRA support must be enabled when opted in", mraSources[0].isMraSupport());
    }

    @Test
    public void blankAndGarbageLotlUrlEntriesAreSkipped() throws Exception {
        // Empty / whitespace-only entries between separators must never become a LOTLSource with a null URL.
        LOTLSource[] sources = lotlSources(Map.of(
                DssTrustConfigurer.KEY_LOTL_URLS, "https://a.test/lotl.xml,, ; https://b.test/lotl.xml ,"));

        assertEquals("only the two real URLs survive", 2, sources.length);
        for (LOTLSource lotl : sources) {
            assertTrue("no LOTL source may have a blank URL", StringUtils.isNotBlank(lotl.getUrl()));
        }
    }

    @Test
    public void noLotlConfigYieldsNoSources() throws Exception {
        assertEquals("no LOTL config -> no sources", 0, lotlSources(Map.of()).length);
    }

    @Test
    public void systemStoreEnabledLoadsTrustAnchors() throws Exception {
        // cacerts ships with every JDK, so the enabled flag always yields at least the portable store offline
        // (issue #447). Windows/macOS dev machines add their OS stores on top, hence the >= assertions.
        CertificateSource[] sources = new DssTrustConfigurer(new MapEngineConfig(
                Map.of(DssTrustConfigurer.KEY_SYSTEM_STORE, "true"))).createTrustedCertSources();

        assertTrue("enabling systemStore must add at least the cacerts source", sources.length >= 1);
        int anchors = 0;
        for (CertificateSource source : sources) {
            anchors += source.getCertificates().size();
        }
        assertTrue("the system stores must contribute trust anchors", anchors > 0);
    }

    @Test
    public void systemStoreDisabledYieldsNoSources() throws Exception {
        CertificateSource[] sources = new DssTrustConfigurer(new MapEngineConfig(
                Map.of(DssTrustConfigurer.KEY_SYSTEM_STORE, "false"))).createTrustedCertSources();
        assertEquals("systemStore off must contribute no trusted source", 0, sources.length);
    }

    private static LOTLSource[] lotlSources(Map<String, String> cfg) throws Exception {
        return new DssTrustConfigurer(new MapEngineConfig(cfg)).getLotlSources();
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
