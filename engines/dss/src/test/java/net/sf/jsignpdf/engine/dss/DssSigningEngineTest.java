package net.sf.jsignpdf.engine.dss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.engine.Capability;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.types.PadesLevel;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;

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

    /** In-JVM RFC 3161 TSA on a loopback port, so level-T signatures can be produced offline. */
    private static EmbeddedTsaServer tsaServer;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File inputFile;
    private File outputFile;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // BC must be registered before the embedded TSA generates its signing material.
        Security.addProvider(new BouncyCastleProvider());
        tsaServer = new EmbeddedTsaServer();
        tsaServer.start();
    }

    @AfterClass
    public static void stopTsa() {
        if (tsaServer != null) {
            tsaServer.stop();
        }
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

    /** Points the options at the embedded loopback TSA (no auth), so signing reaches baseline level T. */
    private void useEmbeddedTsa(BasicSignerOptions o) {
        o.setTimestamp(true);
        o.setTsaUrl(tsaServer.getUrl());
        o.setTsaHashAlg("SHA-256");
    }

    @Test
    public void defaultLevelProducesBaselineB() throws Exception {
        BasicSignerOptions o = baseOptions(); // padesLevel == null -> BASELINE_B
        boolean ok = new DssSigningEngine().sign(o, EMPTY_CONFIG);
        assertTrue("signing should succeed", ok);
        assertTrue("output should exist", outputFile.exists());
        assertSignatureLevel(outputFile, SignatureLevel.PAdES_BASELINE_B);
    }

    @Test
    public void explicitBaselineBProducesBaselineB() throws Exception {
        BasicSignerOptions o = baseOptions();
        o.setPadesLevel(PadesLevel.BASELINE_B);
        assertTrue(new DssSigningEngine().sign(o, EMPTY_CONFIG));
        assertSignatureLevel(outputFile, SignatureLevel.PAdES_BASELINE_B);
    }

    @Test
    public void baselineBWithTsaUpgradesToT() throws Exception {
        BasicSignerOptions o = baseOptions(); // padesLevel == null -> BASELINE_B, auto-upgraded to T by the TSA
        useEmbeddedTsa(o);
        assertTrue(new DssSigningEngine().sign(o, EMPTY_CONFIG));
        assertSignatureLevel(outputFile, SignatureLevel.PAdES_BASELINE_T);
    }

    @Test
    public void explicitBaselineTWithTsaProducesT() throws Exception {
        BasicSignerOptions o = baseOptions();
        o.setPadesLevel(PadesLevel.BASELINE_T);
        useEmbeddedTsa(o);
        assertTrue(new DssSigningEngine().sign(o, EMPTY_CONFIG));
        assertSignatureLevel(outputFile, SignatureLevel.PAdES_BASELINE_T);
    }

    @Test
    public void visibleSignatureIsPlacedOnRequestedPage() throws Exception {
        BasicSignerOptions o = baseOptions();
        o.setVisible(true);
        o.setPage(1);
        o.setPositionLLX(100f);
        o.setPositionLLY(100f);
        o.setPositionURX(300f);
        o.setPositionURY(250f);
        o.setReason("testing");
        o.setLocation("here");
        assertTrue(new DssSigningEngine().sign(o, EMPTY_CONFIG));
        assertSignatureLevel(outputFile, SignatureLevel.PAdES_BASELINE_B);

        try (PDDocument doc = Loader.loadPDF(outputFile)) {
            List<PDSignatureField> fields = doc.getSignatureFields();
            assertEquals("exactly one signature field expected", 1, fields.size());
            PDAnnotationWidget widget = fields.get(0).getWidgets().get(0);
            assertTrue("signature widget must sit on the requested (first) page",
                    doc.getPage(0).getAnnotations().contains(widget));
            PDRectangle rect = widget.getRectangle();
            // The visible signature spans the requested box (URX-LLX x URY-LLY = 200 x 150).
            assertEquals("visible signature width", 200f, rect.getWidth(), 1f);
            assertEquals("visible signature height", 150f, rect.getHeight(), 1f);
        }
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

    /**
     * Asserts both the PAdES subfilter (structural) and the <em>achieved</em> baseline level. The level is
     * read back through DSS's own validator, since the subfilter alone ({@code ETSI.CAdES.detached}) is
     * identical for B / T / LT / LTA and so cannot distinguish them.
     */
    private static void assertSignatureLevel(File pdf, SignatureLevel expected) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertFalse("a signature must be present", doc.getSignatureDictionaries().isEmpty());
            PDSignature sig = doc.getSignatureDictionaries().get(0);
            assertEquals("ETSI.CAdES.detached", sig.getSubFilter());
        }
        SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(new FileDocument(pdf));
        validator.setCertificateVerifier(new CommonCertificateVerifier());
        Reports reports = validator.validateDocument();
        SimpleReport simpleReport = reports.getSimpleReport();
        assertEquals("exactly one signature expected", 1, simpleReport.getSignaturesCount());
        assertEquals("achieved PAdES baseline level", expected,
                simpleReport.getSignatureFormat(simpleReport.getFirstSignatureId()));
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
