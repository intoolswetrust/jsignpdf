package net.sf.jsignpdf.engine.dss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.engine.Capability;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PadesLevel;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Coverage for the append-only document timestamp (issue #141): {@link DssSigningEngine#timestamp} appends an
 * ETSI.RFC3161 revision to any input - unsigned, signed, already timestamped - without a private key, and
 * without judging the validity of the signatures already in the document.
 */
public class DssDocTimestampTest {

    private static final String SUBFILTER_DOC_TIMESTAMP = "ETSI.RFC3161";
    private static final String SUBFILTER_SIGNATURE = "ETSI.CAdES.detached";

    private static final char[] KS_PASSWD = "keystorepass".toCharArray();
    private static final String KS_FILE = "src/test/resources/test-keystore.jks";
    private static final String KEY_ALIAS = "rsa2048";
    private static final char[] KEY_PASSWD = "RSA2048pass".toCharArray();

    private static final char[] CA_KS_PASSWD = "castorepass".toCharArray();
    private static final String CA_KEY_ALIAS = "ca-signer";
    private static final char[] CA_KEY_PASSWD = "caKeyPass".toCharArray();

    private static final EngineConfig EMPTY_CONFIG = new MapEngineConfig(new HashMap<>());

    /** A {@code /Contents} reservation no timestamp token can fit into, used by the undersize tests. */
    private static final int TOO_SMALL_CONTENT_SIZE = 200;

    /** Validity window of the signing certificate in {@code expiredSignerCertificateStillGetsATimestamp}. */
    private static final long SHORT_VALIDITY_MS = 5000L;

    private static EmbeddedTsaServer tsaServer;
    private static EmbeddedCa embeddedCa;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File inputFile;
    private File outputFile;

    @BeforeClass
    public static void startServers() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        tsaServer = new EmbeddedTsaServer();
        tsaServer.start();
        embeddedCa = new EmbeddedCa();
        embeddedCa.start();
    }

    @AfterClass
    public static void stopServers() {
        if (tsaServer != null) {
            tsaServer.stop();
        }
        if (embeddedCa != null) {
            embeddedCa.stop();
        }
    }

    @Before
    public void createInputPdf() throws Exception {
        inputFile = tmp.newFile("input.pdf");
        outputFile = new File(tmp.getRoot(), "timestamped.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.setVersion(1.7f);
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("Document timestamp test");
                cs.endText();
            }
            doc.save(inputFile);
        }
    }

    @Test
    public void engineDeclaresTheCapability() {
        assertTrue(new DssSigningEngine().capabilities().contains(Capability.DOC_TIMESTAMP));
    }

    @Test
    public void unsignedPdfGetsOneAppendedDocTimestamp() throws Exception {
        byte[] original = Files.readAllBytes(inputFile.toPath());

        assertTrue("timestamping an unsigned PDF must succeed",
                new DssSigningEngine().timestamp(timestampOptions(), EMPTY_CONFIG));

        assertEquals(List.of(SUBFILTER_DOC_TIMESTAMP), subFilters(outputFile));
        byte[] result = Files.readAllBytes(outputFile.toPath());
        assertTrue("the timestamp must be an incremental append, keeping the original bytes untouched",
                result.length > original.length
                        && java.util.Arrays.equals(original, java.util.Arrays.copyOf(result, original.length)));
        assertFalse("no validation data is collectable for an unsigned document", hasDssDictionary(outputFile));
    }

    @Test
    public void signedInputKeepsItsSignatureAndGetsATimestamp() throws Exception {
        signInput(baseSignerOptions(), EMPTY_CONFIG);

        assertTrue(new DssSigningEngine().timestamp(timestampOptions(), EMPTY_CONFIG));

        assertEquals(List.of(SUBFILTER_SIGNATURE, SUBFILTER_DOC_TIMESTAMP), subFilters(outputFile));
        assertFalse("a B-level signature has no timestamp to anchor validation data to",
                hasDssDictionary(outputFile));
    }

    @Test
    public void timestampingATLevelSignatureEmbedsValidationData() throws Exception {
        BasicSignerOptions signOptions = caSignerOptions();
        useEmbeddedTsa(signOptions);
        signOptions.setPadesLevel(PadesLevel.BASELINE_T);
        signInput(signOptions, EMPTY_CONFIG);

        assertTrue(new DssSigningEngine().timestamp(timestampOptions(), caTrustConfig()));

        assertEquals(List.of(SUBFILTER_SIGNATURE, SUBFILTER_DOC_TIMESTAMP), subFilters(outputFile));
        assertTrue("validation data for the signature timestamp must be embedded", hasDssDictionary(outputFile));
    }

    @Test
    public void untrustedSignerChainIsAWarningNotAFailure() throws Exception {
        BasicSignerOptions signOptions = caSignerOptions();
        useEmbeddedTsa(signOptions);
        signOptions.setPadesLevel(PadesLevel.BASELINE_T);
        signInput(signOptions, EMPTY_CONFIG);

        // Online fetching is on, but the signer's issuing CA is not among the trust anchors: DSS cannot
        // collect revocation data, which for a signature would abort with an AlertException.
        Map<String, String> cfg = new HashMap<>();
        cfg.put(DssTrustConfigurer.KEY_ONLINE_ENABLED, "true");

        CapturingLogHandler handler = new CapturingLogHandler();
        Logger logger = Logger.getLogger("net.sf.jsignpdf");
        logger.addHandler(handler);
        try {
            assertTrue("an untrusted chain must not stop the document timestamp",
                    new DssSigningEngine().timestamp(timestampOptions(), new MapEngineConfig(cfg)));
        } finally {
            logger.removeHandler(handler);
        }

        assertTrue("nothing may be logged as an error", handler.severeMessages().isEmpty());
        assertEquals(List.of(SUBFILTER_SIGNATURE, SUBFILTER_DOC_TIMESTAMP), subFilters(outputFile));
    }

    @Test
    public void expiredSignerCertificateStillGetsATimestamp() throws Exception {
        // The normal state of an archived document: the certificate that signed it has expired, which is why
        // its owner is refreshing the timestamp chain in the first place. DSS refuses to *create* a signature
        // with an expired certificate, so the fixture is signed inside a short validity window and the test
        // waits for it to lapse.
        long day = 24 * 60 * 60 * 1000L;
        Date notAfter = new Date(System.currentTimeMillis() + SHORT_VALIDITY_MS);
        KeyStore ks = embeddedCa.issueSigningKeyStore(CA_KEY_ALIAS, CA_KEY_PASSWD,
                new Date(System.currentTimeMillis() - 400 * day), notAfter);
        signInput(signerOptionsFor(ks, "expired-signer.jks"), EMPTY_CONFIG);
        long waitMs = notAfter.getTime() + 500 - System.currentTimeMillis();
        if (waitMs > 0) {
            Thread.sleep(waitMs);
        }

        Map<String, String> cfg = new HashMap<>();
        cfg.put(DssTrustConfigurer.KEY_ONLINE_ENABLED, "true");
        cfg.put(DssTrustConfigurer.KEY_CERT_FILES, caCertFile().getAbsolutePath());

        assertTrue("an expired signer certificate must not stop the document timestamp",
                new DssSigningEngine().timestamp(timestampOptions(), new MapEngineConfig(cfg)));
        assertEquals(List.of(SUBFILTER_SIGNATURE, SUBFILTER_DOC_TIMESTAMP), subFilters(outputFile));
    }

    @Test
    public void timestampingTwiceAppendsTwoTokens() throws Exception {
        assertTrue(new DssSigningEngine().timestamp(timestampOptions(), EMPTY_CONFIG));

        File secondOutput = new File(tmp.getRoot(), "timestamped-twice.pdf");
        BasicSignerOptions second = timestampOptions();
        second.setInFile(outputFile.getAbsolutePath());
        second.setOutFile(secondOutput.getAbsolutePath());
        assertTrue(new DssSigningEngine().timestamp(second, EMPTY_CONFIG));

        assertEquals(List.of(SUBFILTER_DOC_TIMESTAMP, SUBFILTER_DOC_TIMESTAMP), subFilters(secondOutput));
        try (PDDocument doc = Loader.loadPDF(secondOutput)) {
            assertEquals("the page content must be untouched", 1, doc.getNumberOfPages());
        }
    }

    @Test
    public void tsaHashAlgorithmIsHonoured() throws Exception {
        BasicSignerOptions o = timestampOptions();
        o.setTsaHashAlg("SHA-512");
        assertTrue(new DssSigningEngine().timestamp(o, EMPTY_CONFIG));
        assertEquals("SHA-512 message imprint", "2.16.840.1.101.3.4.2.3", tsaServer.getLastRequestImprintAlgOid());

        o = timestampOptions();
        o.setTsaHashAlg("SHA-256");
        assertTrue(new DssSigningEngine().timestamp(o, EMPTY_CONFIG));
        assertEquals("SHA-256 message imprint", "2.16.840.1.101.3.4.2.1", tsaServer.getLastRequestImprintAlgOid());
    }

    @Test
    public void undersizeContentSizeIsRetried() throws Exception {
        Map<String, String> cfg = new HashMap<>();
        cfg.put(DssSigningEngine.KEY_CONTENT_SIZE, String.valueOf(TOO_SMALL_CONTENT_SIZE));
        cfg.put(DssSigningEngine.KEY_RETRY_ON_UNDERSIZE, "true");

        CapturingLogHandler handler = new CapturingLogHandler();
        Logger logger = Logger.getLogger("net.sf.jsignpdf");
        logger.addHandler(handler);
        try {
            assertTrue("an undersized reservation must be grown and retried",
                    new DssSigningEngine().timestamp(timestampOptions(), new MapEngineConfig(cfg)));
        } finally {
            logger.removeHandler(handler);
        }
        assertTrue("the retry must be reported", handler.allMessages().contains("was too small"));
        assertEquals(List.of(SUBFILTER_DOC_TIMESTAMP), subFilters(outputFile));
    }

    @Test
    public void undersizeContentSizeFailsWithTheRetryDisabled() throws Exception {
        Map<String, String> cfg = new HashMap<>();
        cfg.put(DssSigningEngine.KEY_CONTENT_SIZE, String.valueOf(TOO_SMALL_CONTENT_SIZE));
        cfg.put(DssSigningEngine.KEY_RETRY_ON_UNDERSIZE, "false");
        assertFalse("without the retry an undersized reservation must fail",
                new DssSigningEngine().timestamp(timestampOptions(), new MapEngineConfig(cfg)));
    }

    @Test
    public void encryptedInputIsOpenedAndStaysEncrypted() throws Exception {
        BasicSignerOptions signOptions = baseSignerOptions();
        signOptions.setPdfEncryption(net.sf.jsignpdf.types.PDFEncryption.PASSWORD);
        signOptions.setPdfOwnerPwd("ownerpwd");
        signOptions.setPdfUserPwd("userpwd");
        signInput(signOptions, EMPTY_CONFIG);

        BasicSignerOptions o = timestampOptions();
        o.setPdfOwnerPwd("ownerpwd");
        assertTrue("an encrypted input must be opened with the owner password",
                new DssSigningEngine().timestamp(o, EMPTY_CONFIG));

        try (PDDocument doc = Loader.loadPDF(outputFile, "ownerpwd")) {
            assertTrue("the incremental append must preserve the input's encryption", doc.isEncrypted());
            assertEquals(SUBFILTER_DOC_TIMESTAMP,
                    doc.getSignatureDictionaries().get(doc.getSignatureDictionaries().size() - 1).getSubFilter());
        }
    }

    @Test
    public void certifiedInputIsStillTimestamped() throws Exception {
        // DocMDP level 1 forbids any change to a signed document, but a document-timestamp layer is exempt.
        BasicSignerOptions signOptions = baseSignerOptions();
        signOptions.setCertLevel(net.sf.jsignpdf.types.CertificationLevel.CERTIFIED_NO_CHANGES_ALLOWED);
        signInput(signOptions, EMPTY_CONFIG);

        assertTrue("a certified document must still accept a document timestamp",
                new DssSigningEngine().timestamp(timestampOptions(), EMPTY_CONFIG));
        assertEquals(List.of(SUBFILTER_SIGNATURE, SUBFILTER_DOC_TIMESTAMP), subFilters(outputFile));
    }

    @Test
    public void offlineRunWarnsAndStillTimestamps() throws Exception {
        BasicSignerOptions signOptions = caSignerOptions();
        useEmbeddedTsa(signOptions);
        signOptions.setPadesLevel(PadesLevel.BASELINE_T);
        signInput(signOptions, EMPTY_CONFIG);

        CapturingLogHandler handler = new CapturingLogHandler();
        Logger logger = Logger.getLogger("net.sf.jsignpdf");
        logger.addHandler(handler);
        try {
            assertTrue("a missing trust source only costs validation data",
                    new DssSigningEngine().timestamp(timestampOptions(), EMPTY_CONFIG));
        } finally {
            logger.removeHandler(handler);
        }

        assertTrue("the missing trust source must be reported",
                handler.allMessages().contains("engine.dss.online.enabled=false"));
        assertEquals(List.of(SUBFILTER_SIGNATURE, SUBFILTER_DOC_TIMESTAMP), subFilters(outputFile));
    }

    /** Options for the timestamp operation: no key material, only the input, the output and the TSA. */
    private BasicSignerOptions timestampOptions() {
        BasicSignerOptions o = new BasicSignerOptions();
        o.setAdvanced(true);
        o.setTimestampOnly(true);
        o.setInFile(inputFile.getAbsolutePath());
        o.setOutFile(outputFile.getAbsolutePath());
        useEmbeddedTsa(o);
        return o;
    }

    private BasicSignerOptions baseSignerOptions() {
        BasicSignerOptions o = new BasicSignerOptions();
        o.setAdvanced(true);
        o.setHashAlgorithm(HashAlgorithm.SHA256);
        o.setKsType("JKS");
        o.setKsFile(KS_FILE);
        o.setKsPasswd(KS_PASSWD);
        o.setKeyAlias(KEY_ALIAS);
        o.setKeyPasswd(KEY_PASSWD);
        return o;
    }

    private BasicSignerOptions caSignerOptions() throws Exception {
        return signerOptionsFor(embeddedCa.issueSigningKeyStore(CA_KEY_ALIAS, CA_KEY_PASSWD), "ca-signer.jks");
    }

    private BasicSignerOptions signerOptionsFor(KeyStore ks, String ksFileName) throws Exception {
        File ksFile = tmp.newFile(ksFileName);
        try (FileOutputStream fos = new FileOutputStream(ksFile)) {
            ks.store(fos, CA_KS_PASSWD);
        }
        BasicSignerOptions o = new BasicSignerOptions();
        o.setAdvanced(true);
        o.setHashAlgorithm(HashAlgorithm.SHA256);
        o.setKsType("JKS");
        o.setKsFile(ksFile.getAbsolutePath());
        o.setKsPasswd(CA_KS_PASSWD);
        o.setKeyAlias(CA_KEY_ALIAS);
        o.setKeyPasswd(CA_KEY_PASSWD);
        return o;
    }

    /**
     * Signs {@link #inputFile} in place, so the timestamp tests all run against a single input path whatever
     * the document already carries.
     */
    private void signInput(BasicSignerOptions signOptions, EngineConfig config) throws Exception {
        File signed = tmp.newFile("signed-" + System.nanoTime() + ".pdf");
        signOptions.setInFile(inputFile.getAbsolutePath());
        signOptions.setOutFile(signed.getAbsolutePath());
        assertTrue("the test fixture must sign", new DssSigningEngine().sign(signOptions, config));
        Files.copy(signed.toPath(), inputFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private void useEmbeddedTsa(BasicSignerOptions o) {
        o.setTimestamp(true);
        o.setTsaUrl(tsaServer.getUrl());
        o.setTsaHashAlg("SHA-256");
    }

    private File caCertFile() throws Exception {
        File caFile = tmp.newFile("ca-" + System.nanoTime() + ".crt");
        Files.write(caFile.toPath(), embeddedCa.getCaCertificate().getEncoded());
        return caFile;
    }

    private EngineConfig caTrustConfig() throws Exception {
        File tsaFile = tmp.newFile("tsa-" + System.nanoTime() + ".crt");
        Files.write(tsaFile.toPath(), tsaServer.getCertificate().getEncoded());
        Map<String, String> cfg = new HashMap<>();
        cfg.put(DssTrustConfigurer.KEY_ONLINE_ENABLED, "true");
        cfg.put(DssTrustConfigurer.KEY_CERT_FILES,
                caCertFile().getAbsolutePath() + "," + tsaFile.getAbsolutePath());
        return new MapEngineConfig(cfg);
    }

    /** The {@code /SubFilter} of every signature dictionary, in document order. */
    private static List<String> subFilters(File pdf) throws Exception {
        List<String> out = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            for (PDSignature sig : doc.getSignatureDictionaries()) {
                out.add(sig.getSubFilter());
            }
        }
        return out;
    }

    /** Whether the document carries a DSS dictionary, i.e. embedded validation data. */
    private static boolean hasDssDictionary(File pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return doc.getDocumentCatalog().getCOSObject().containsKey(COSName.getPDFName("DSS"));
        }
    }

    private static final class CapturingLogHandler extends Handler {
        private final StringBuilder severe = new StringBuilder();
        private final StringBuilder all = new StringBuilder();

        @Override
        public void publish(LogRecord record) {
            final String message = new SimpleFormatter().formatMessage(record);
            all.append(message).append('\n');
            if (record.getLevel().intValue() >= java.util.logging.Level.SEVERE.intValue()) {
                severe.append(message).append('\n');
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        String severeMessages() {
            return severe.toString();
        }

        String allMessages() {
            return all.toString();
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
