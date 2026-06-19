package net.sf.jsignpdf.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.engine.EngineMismatchValidator.Mismatch;

/**
 * Exercises {@link EngineMismatchValidator} with a deliberately reduced-capability {@link StubSigningEngine},
 * plus a no-mismatch baseline against the all-capable OpenPDF engine.
 */
public class EngineMismatchValidatorTest {

    private static Set<Capability> caps(List<Mismatch> mismatches) {
        return mismatches.stream().map(Mismatch::capability).collect(Collectors.toSet());
    }

    @Test
    public void defaultsProduceNoHashOrOverwriteMismatch() {
        // A fresh options object carries the global default hash (never explicitly chosen) and append
        // (incremental) mode on. An unchosen default is the engine's problem to honour/upgrade, and
        // incremental append is universal — so a capability-less engine flags neither.
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setAdvanced(true);
        StubSigningEngine engine = new StubSigningEngine("empty");
        Set<Capability> reported = caps(EngineMismatchValidator.findMismatches(opts, engine));
        assertFalse("default hash must not be treated as a user choice", reported.contains(Capability.HASH_SHA1));
        assertFalse("incremental append is universal", reported.contains(Capability.OVERWRITE_MODE));
    }

    @Test
    public void explicitlyChosenUnsupportedHashIsFlagged() {
        // When the user deliberately selects SHA-1 in advanced mode, an engine that lacks it must fail fast.
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setAdvanced(true);
        opts.setHashAlgorithm(net.sf.jsignpdf.types.HashAlgorithm.SHA1);
        StubSigningEngine engine = new StubSigningEngine("empty");
        Set<Capability> reported = caps(EngineMismatchValidator.findMismatches(opts, engine));
        assertTrue(reported.contains(Capability.HASH_SHA1));
        assertFalse(reported.contains(Capability.OVERWRITE_MODE));
    }

    @Test
    public void overwriteFlaggedWhenEngineLacksCapability() {
        // append=false (in advanced mode) requests a non-incremental rewrite, which needs OVERWRITE_MODE.
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setAdvanced(true);
        opts.setAppend(false);
        StubSigningEngine engine = new StubSigningEngine("noOverwrite", Capability.HASH_SHA1);
        List<Mismatch> mismatches = EngineMismatchValidator.findMismatches(opts, engine);
        Set<Capability> reported = caps(mismatches);
        assertTrue(reported.contains(Capability.OVERWRITE_MODE));
        Mismatch m = mismatches.stream().filter(x -> x.capability() == Capability.OVERWRITE_MODE)
                .findFirst().orElseThrow();
        assertEquals("--append", m.option());
    }

    @Test
    public void overwriteNotFlaggedWhenEngineSupportsIt() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setAdvanced(true);
        opts.setAppend(false);
        StubSigningEngine engine = new StubSigningEngine("overwrite",
                Capability.HASH_SHA1, Capability.OVERWRITE_MODE);
        assertFalse(caps(EngineMismatchValidator.findMismatches(opts, engine)).contains(Capability.OVERWRITE_MODE));
    }

    @Test
    public void allCapableEngineReportsNoMismatchForDefaults() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setAdvanced(true);
        SigningEngine openpdf = EngineRegistry.getInstance().findById("openpdf").orElseThrow();
        assertTrue(EngineMismatchValidator.findMismatches(opts, openpdf).isEmpty());
    }

    @Test
    public void missingVisibleUmbrellaSuppressesSubFields() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setVisible(true);
        opts.setL2Text("custom text");
        StubSigningEngine engine = new StubSigningEngine("noVisible", Capability.HASH_SHA1);
        Set<Capability> reported = caps(EngineMismatchValidator.findMismatches(opts, engine));
        assertTrue("umbrella must be reported", reported.contains(Capability.VISIBLE_SIGNATURE));
        assertFalse("sub-field must NOT be reported when umbrella is missing",
                reported.contains(Capability.VISIBLE_LAYER2_TEXT));
    }

    @Test
    public void supportedVisibleUmbrellaStillFlagsSubField() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setVisible(true);
        opts.setL2Text("custom text");
        StubSigningEngine engine = new StubSigningEngine("visibleNoL2",
                Capability.HASH_SHA1, Capability.VISIBLE_SIGNATURE);
        Set<Capability> reported = caps(EngineMismatchValidator.findMismatches(opts, engine));
        assertFalse(reported.contains(Capability.VISIBLE_SIGNATURE));
        assertTrue(reported.contains(Capability.VISIBLE_LAYER2_TEXT));
    }

    @Test
    public void tsaUmbrellaFlaggedWhenEnabledAndUnsupported() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setAdvanced(true);
        opts.setTimestamp(true);
        opts.setTsaUrl("http://tsa.example.com");
        StubSigningEngine engine = new StubSigningEngine("noTsa", Capability.HASH_SHA1);
        Set<Capability> reported = caps(EngineMismatchValidator.findMismatches(opts, engine));
        assertTrue(reported.contains(Capability.TSA));
    }

    @Test
    public void noTimestampMeansNoTsaMismatch() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setAdvanced(true);
        // timestamp disabled -> TSA not evaluated even though the engine lacks it
        StubSigningEngine engine = new StubSigningEngine("noTsa",
                Capability.HASH_SHA1);
        assertFalse(caps(EngineMismatchValidator.findMismatches(opts, engine)).contains(Capability.TSA));
    }

    @Test
    public void padesLevelFlaggedWhenEngineLacksCapability() {
        // OpenPDF declares no PADES_BASELINE_* capability, so requesting a PAdES level must be flagged.
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setAdvanced(true);
        opts.setPadesLevel(net.sf.jsignpdf.types.PadesLevel.BASELINE_LTA);
        StubSigningEngine engine = new StubSigningEngine("noPades", Capability.HASH_SHA1);
        List<Mismatch> mismatches = EngineMismatchValidator.findMismatches(opts, engine);
        Set<Capability> reported = caps(mismatches);
        assertTrue(reported.contains(Capability.PADES_BASELINE_LTA));
        Mismatch m = mismatches.stream().filter(x -> x.capability() == Capability.PADES_BASELINE_LTA)
                .findFirst().orElseThrow();
        assertEquals("--pades-level", m.option());
    }

    @Test
    public void padesLevelNotFlaggedWhenEngineSupportsIt() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setAdvanced(true);
        opts.setPadesLevel(net.sf.jsignpdf.types.PadesLevel.BASELINE_T);
        StubSigningEngine engine = new StubSigningEngine("pades",
                Capability.HASH_SHA1, Capability.PADES_BASELINE_T);
        assertFalse(caps(EngineMismatchValidator.findMismatches(opts, engine)).contains(Capability.PADES_BASELINE_T));
    }

    @Test
    public void noPadesLevelMeansNoPadesMismatch() {
        // Default (null) pades level must never produce a mismatch, even against an engine without PAdES.
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setAdvanced(true);
        StubSigningEngine engine = new StubSigningEngine("noPades", Capability.HASH_SHA1);
        Set<Capability> reported = caps(EngineMismatchValidator.findMismatches(opts, engine));
        assertFalse(reported.contains(Capability.PADES_BASELINE_B));
    }

    @Test
    public void dssEngineAcceptsPadesLevelsWithoutMismatch() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setAdvanced(true);
        opts.setHashAlgorithm(net.sf.jsignpdf.types.HashAlgorithm.SHA256);
        opts.setPadesLevel(net.sf.jsignpdf.types.PadesLevel.BASELINE_T);
        SigningEngine dss = EngineRegistry.getInstance().findById("dss").orElseThrow();
        assertTrue(EngineMismatchValidator.findMismatches(opts, dss).isEmpty());
    }

    @Test
    public void mismatchCarriesOptionLabel() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setVisible(true);
        StubSigningEngine engine = new StubSigningEngine("empty2",
                Capability.HASH_SHA1);
        List<Mismatch> mismatches = EngineMismatchValidator.findMismatches(opts, engine);
        Mismatch visible = mismatches.stream()
                .filter(m -> m.capability() == Capability.VISIBLE_SIGNATURE).findFirst().orElseThrow();
        assertEquals("--visible-signature", visible.option());
    }
}
