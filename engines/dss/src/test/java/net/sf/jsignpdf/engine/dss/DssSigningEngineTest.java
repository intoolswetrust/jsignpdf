package net.sf.jsignpdf.engine.dss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.engine.Capability;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.types.PadesLevel;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Signing tests for {@link DssSigningEngine}, adapted from the {@code jsignpdf-pades} signing suite to
 * drive the engine through the JSignPdf {@link BasicSignerOptions} model. Output is validated
 * structurally (PAdES subfilter, signature presence) rather than by byte equality.
 */
public class DssSigningEngineTest {

    private static final char[] KS_PASSWD = "keystorepass".toCharArray();
    private static final String KS_FILE = "src/test/resources/test-keystore.jks";
    private static final String KEY_ALIAS = "rsa2048";
    private static final char[] KEY_PASSWD = "RSA2048pass".toCharArray();

    private static final EngineConfig EMPTY_CONFIG = new MapEngineConfig(new HashMap<>());

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File inputFile;
    private File outputFile;

    @BeforeClass
    public static void registerBc() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void createInputPdf() throws Exception {
        inputFile = tmp.newFile("input.pdf");
        outputFile = tmp.newFile("output.pdf");
        outputFile.delete(); // engine writes it
        try (PDDocument doc = new PDDocument()) {
            doc.setVersion(1.7f);
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("Test PDF for DSS signing");
                cs.endText();
            }
            doc.save(inputFile);
        }
    }

    private BasicSignerOptions baseOptions() {
        BasicSignerOptions o = new BasicSignerOptions();
        // advanced mode so the dedicated key password (distinct from the keystore password) is used
        o.setAdvanced(true);
        o.setHashAlgorithm(net.sf.jsignpdf.types.HashAlgorithm.SHA256);
        o.setKsType("JKS");
        o.setKsFile(KS_FILE);
        o.setKsPasswd(KS_PASSWD);
        o.setKeyAlias(KEY_ALIAS);
        o.setKeyPasswd(KEY_PASSWD);
        o.setInFile(inputFile.getAbsolutePath());
        o.setOutFile(outputFile.getAbsolutePath());
        return o;
    }

    @Test
    public void defaultLevelProducesPadesSignature() throws Exception {
        BasicSignerOptions o = baseOptions(); // padesLevel == null -> BASELINE_B
        boolean ok = new DssSigningEngine().sign(o, EMPTY_CONFIG);
        assertTrue("signing should succeed", ok);
        assertTrue("output should exist", outputFile.exists());
        assertPadesSignature(outputFile);
    }

    @Test
    public void explicitBaselineBProducesPadesSignature() throws Exception {
        BasicSignerOptions o = baseOptions();
        o.setPadesLevel(PadesLevel.BASELINE_B);
        assertTrue(new DssSigningEngine().sign(o, EMPTY_CONFIG));
        assertPadesSignature(outputFile);
    }

    @Test
    public void visibleSignatureIsPlacedAndSigned() throws Exception {
        BasicSignerOptions o = baseOptions();
        o.setVisible(true);
        o.setPage(1);
        o.setReason("testing");
        o.setLocation("here");
        assertTrue(new DssSigningEngine().sign(o, EMPTY_CONFIG));
        assertPadesSignature(outputFile);
    }

    @Test
    public void ltWithoutTsaFails() throws Exception {
        BasicSignerOptions o = baseOptions();
        o.setPadesLevel(PadesLevel.BASELINE_LT);
        // LT/LTA build on a signature timestamp; with no TSA the engine must fail fast rather than
        // dropping to a weaker level or blowing up deep inside DSS.
        boolean ok = new DssSigningEngine().sign(o, EMPTY_CONFIG);
        assertFalse("LT without a TSA must fail", ok);
    }

    @Test
    public void ltWithTsaButOfflineFails() throws Exception {
        BasicSignerOptions o = baseOptions();
        o.setPadesLevel(PadesLevel.BASELINE_LT);
        o.setTimestamp(true);
        o.setTsaUrl("http://tsa.example.com/tsr");
        // A TSA is configured but online revocation fetching is disabled (empty config) -> the engine
        // must refuse rather than emit a weaker level. The guard fires before any network access.
        boolean ok = new DssSigningEngine().sign(o, EMPTY_CONFIG);
        assertFalse("LT without revocation data must fail", ok);
    }

    @Test
    public void capabilitiesAreStaticAndDeclarePades() {
        DssSigningEngine engine = new DssSigningEngine();
        assertEquals("dss", engine.id());
        assertTrue(engine.capabilities().contains(Capability.PADES_BASELINE_B));
        assertTrue(engine.capabilities().contains(Capability.PADES_BASELINE_LTA));
        assertFalse("DSS must not declare the legacy Adobe subfilter",
                engine.capabilities().contains(Capability.SUBFILTER_ADBE_PKCS7_DETACHED));
        assertFalse("PAdES disallows SHA-1", engine.capabilities().contains(Capability.HASH_SHA1));
    }

    private static void assertPadesSignature(File pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertFalse("a signature must be present", doc.getSignatureDictionaries().isEmpty());
            PDSignature sig = doc.getSignatureDictionaries().get(0);
            assertEquals("ETSI.CAdES.detached", sig.getSubFilter());
        }
    }

    /** Minimal in-memory {@link EngineConfig} backed by a map (keys already engine-relative). */
    private static final class MapEngineConfig implements EngineConfig {
        private final Map<String, String> map;

        MapEngineConfig(Map<String, String> map) {
            this.map = map;
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
