package net.sf.jsignpdf.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;

/**
 * Verifies that {@link EngineRegistry} discovers the bundled OpenPDF engine via {@link java.util.ServiceLoader}
 * and resolves the right engine for a given invocation. The OpenPDF engine is on the test classpath
 * (test-scope dependency on jsignpdf-engine-openpdf).
 */
public class EngineRegistryTest {

    @Test
    public void serviceLoaderDiscoversOpenPdfEngine() {
        SigningEngine openpdf = EngineRegistry.getInstance().findById("openpdf").orElseThrow();
        assertEquals("openpdf", openpdf.id());
        assertEquals("OpenPDF", openpdf.displayName());
    }

    @Test
    public void serviceLoaderDiscoversDssEngine() {
        SigningEngine dss = EngineRegistry.getInstance().findById("dss").orElseThrow();
        assertEquals("dss", dss.id());
        assertEquals("EU DSS (PAdES)", dss.displayName());
        assertTrue(dss.capabilities().contains(Capability.PADES_BASELINE_LTA));
        assertTrue(dss.capabilities().contains(Capability.SUBFILTER_ETSI_CADES_DETACHED));
    }

    @Test
    public void dssIsListedButNotDefault() {
        List<SigningEngine> all = EngineRegistry.getInstance().listAll();
        assertTrue(all.stream().anyMatch(e -> e.id().equals("dss")));
        // default stays openpdf
        assertEquals("openpdf", EngineRegistry.getInstance().getDefault().orElseThrow().id());
    }

    @Test
    public void findByIdIsCaseInsensitive() {
        assertTrue(EngineRegistry.getInstance().findById("OpenPDF").isPresent());
        assertTrue(EngineRegistry.getInstance().findById("OPENPDF").isPresent());
        assertFalse(EngineRegistry.getInstance().findById("nope").isPresent());
    }

    @Test
    public void listAllHasDefaultFirst() {
        List<SigningEngine> all = EngineRegistry.getInstance().listAll();
        assertFalse(all.isEmpty());
        assertEquals("openpdf", all.get(0).id());
    }

    @Test
    public void defaultEngineIsOpenPdf() {
        assertEquals("openpdf", EngineRegistry.getInstance().getDefault().orElseThrow().id());
    }

    @Test
    public void resolveUsesCliOverride() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setEngine("openpdf");
        assertEquals("openpdf", EngineRegistry.getInstance().resolve(opts).id());
    }

    @Test
    public void resolveFallsBackToDefaultWhenNoOverride() {
        BasicSignerOptions opts = new BasicSignerOptions();
        SigningEngine resolved = EngineRegistry.getInstance().resolve(opts);
        assertSame(EngineRegistry.getInstance().getDefault().orElseThrow(), resolved);
    }

    @Test
    public void resolveThrowsOnUnknownExplicitEngine() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setEngine("does-not-exist");
        assertThrows(IllegalArgumentException.class, () -> EngineRegistry.getInstance().resolve(opts));
    }
}
