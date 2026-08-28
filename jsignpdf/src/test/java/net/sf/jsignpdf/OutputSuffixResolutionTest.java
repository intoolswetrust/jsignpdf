package net.sf.jsignpdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.file.Path;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.sf.jsignpdf.utils.AdvancedConfig;
import net.sf.jsignpdf.utils.PropertyProvider;
import net.sf.jsignpdf.utils.PropertyStoreFactory;

/**
 * Covers the suffix precedence chain: the value set on the options, then the {@code output.suffix} advanced-config key,
 * then {@link Constants#DEFAULT_OUT_SUFFIX}.
 */
public class OutputSuffixResolutionTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final AdvancedConfig cfg = PropertyStoreFactory.getInstance().advancedConfig();

    @After
    public void tearDown() {
        cfg.removeProperty("output.suffix");
    }

    @Test
    public void unsetSuffixFallsBackToConfiguredValue() {
        cfg.setProperty("output.suffix", "_firmado");
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile("/docs/drawing.pdf");
        assertNull(options.getOutSuffix());
        assertEquals("_firmado", options.getOutSuffixX());
        assertEquals("/docs/drawing_firmado.pdf", options.getOutFileX());
    }

    @Test
    public void unsetSuffixFallsBackToBundledDefaultWithoutConfiguredValue() {
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile("/docs/drawing.pdf");
        assertEquals("/docs/drawing" + Constants.DEFAULT_OUT_SUFFIX + ".pdf", options.getOutFileX());
    }

    @Test
    public void explicitSuffixWinsOverConfiguredValue() {
        cfg.setProperty("output.suffix", "_firmado");
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile("/docs/drawing.pdf");
        options.setOutSuffix("-DL");
        assertEquals("/docs/drawing-DL.pdf", options.getOutFileX());
    }

    @Test
    public void cliEmptySuffixKeepsTheInputName() {
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile("/docs/drawing.pdf");
        options.setOutSuffix("");
        assertEquals("/docs/drawing.pdf", options.getOutFileX());
    }

    @Test
    public void explicitOutputFileWinsOverAnySuffix() {
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile("/docs/drawing.pdf");
        options.setOutSuffix("-DL");
        options.setOutFile("/elsewhere/final.pdf");
        assertEquals("/elsewhere/final.pdf", options.getOutFileX());
    }

    @Test
    public void suffixIsUsedVerbatim() {
        assertEquals("/docs/drawingDL.pdf", BasicSignerOptions.deriveOutFileName("/docs/drawing.pdf", "DL"));
        assertEquals("/docs/drawing-DL.pdf", BasicSignerOptions.deriveOutFileName("/docs/drawing.pdf", "-DL"));
    }

    @Test
    public void extensionlessInputKeepsTheSuffixAtTheEnd() {
        assertEquals("/docs/drawing_signed", BasicSignerOptions.deriveOutFileName("/docs/drawing", "_signed"));
    }

    @Test
    public void outputDirectoryReducesInputToBaseNameUnderThatDirectory() {
        assertEquals("/out/drawing-DL.pdf",
                BasicSignerOptions.composeOutFileName("/out", null, "/docs/drawing.pdf", "-DL"));
    }

    @Test
    public void outputDirectoryGetsATrailingSlashAndPrefixGoesBeforeTheBaseName() {
        assertEquals("/out/signed-drawing-DL.pdf",
                BasicSignerOptions.composeOutFileName("/out/", "signed-", "/docs/drawing.pdf", "-DL"));
    }

    @Test
    public void outputDirectoryBackslashesAreNormalized() {
        assertEquals("C:/out/drawing-DL.pdf",
                BasicSignerOptions.composeOutFileName("C:\\out", null, "C:\\docs\\drawing.pdf", "-DL"));
    }

    @Test
    public void blankOutputDirectoryDerivesNextToInputAndIgnoresPrefix() {
        assertEquals("/docs/drawing-DL.pdf",
                BasicSignerOptions.composeOutFileName("", "ignored-", "/docs/drawing.pdf", "-DL"));
    }

    @Test
    public void getOutFileXHonorsConfiguredOutputDirectoryAndPrefix() {
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile("/docs/drawing.pdf");
        options.setOutSuffix("-DL");
        options.setOutPath("/out");
        options.setOutPrefix("signed-");
        assertEquals("/out/signed-drawing-DL.pdf", options.getOutFileX());
    }

    @Test
    public void presetRoundTripPreservesUnsetAndEmpty() throws Exception {
        Path file = tmp.newFile("preset.properties").toPath();
        PropertyProvider store = new PropertyProvider(file);

        BasicSignerOptions source = new BasicSignerOptions();
        source.storeToPreset(store);
        BasicSignerOptions target = new BasicSignerOptions();
        target.setOutSuffix("-stale");
        target.loadFromPreset(store);
        assertNull("A preset without a suffix must clear a previously loaded one", target.getOutSuffix());

        source.setOutSuffix("");
        source.storeToPreset(store);
        target.loadFromPreset(store);
        assertEquals("An empty suffix must survive the round trip as empty, not unset", "", target.getOutSuffix());

        source.setOutSuffix("-DL");
        source.storeToPreset(store);
        target.loadFromPreset(store);
        assertEquals("-DL", target.getOutSuffix());
    }
}
