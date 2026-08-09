package net.sf.jsignpdf.engine.dss;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.types.HashAlgorithm;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Coverage for issue #33 on the {@code dss} engine: the RFC 3161 timestamp request must carry a nonce and
 * the echoed value must be checked. DSS omits the nonce unless a {@code NonceSource} is configured, so
 * without {@link DssSigningEngine#KEY_TSA_NONCE} handling a replayed timestamp response would be accepted.
 * The openpdf engine sends a nonce unconditionally and is not covered here.
 */
public class DssTsaNonceTest {

    private static final char[] KS_PASSWD = "keystorepass".toCharArray();
    private static final String KS_FILE = "src/test/resources/test-keystore.jks";
    private static final String KEY_ALIAS = "rsa2048";
    private static final char[] KEY_PASSWD = "RSA2048pass".toCharArray();

    private static EmbeddedTsaServer tsaServer;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File inputFile;
    private File outputFile;

    @BeforeClass
    public static void startTsa() throws Exception {
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

    @After
    public void resetTsa() {
        tsaServer.echoWrongNonce(false);
    }

    @Before
    public void createInputPdf() throws Exception {
        inputFile = tmp.newFile("input.pdf");
        outputFile = new File(tmp.getRoot(), "signed.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.setVersion(1.7f);
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("TSA nonce test");
                cs.endText();
            }
            doc.save(inputFile);
        }
    }

    @Test
    public void nonceIsSentByDefault() throws Exception {
        assertTrue("signing should succeed", sign(new HashMap<>()));
        assertNotNull("timestamp request must carry a nonce", tsaServer.getLastRequestNonce());
    }

    @Test
    public void nonceCanBeDisabled() throws Exception {
        Map<String, String> cfg = new HashMap<>();
        cfg.put(DssSigningEngine.KEY_TSA_NONCE, "false");
        assertTrue("signing should succeed", sign(cfg));
        assertNull("timestamp request must carry no nonce", tsaServer.getLastRequestNonce());
    }

    @Test
    public void mismatchedNonceEchoIsRejected() throws Exception {
        tsaServer.echoWrongNonce(true);
        assertFalse("a timestamp response echoing a different nonce must be rejected", sign(new HashMap<>()));
    }

    private boolean sign(Map<String, String> engineCfg) {
        BasicSignerOptions o = new BasicSignerOptions();
        o.setAdvanced(true);
        o.setHashAlgorithm(HashAlgorithm.SHA256);
        o.setKsType("JKS");
        o.setKsFile(KS_FILE);
        o.setKsPasswd(KS_PASSWD);
        o.setKeyAlias(KEY_ALIAS);
        o.setKeyPasswd(KEY_PASSWD);
        o.setInFile(inputFile.getAbsolutePath());
        o.setOutFile(outputFile.getAbsolutePath());
        o.setTimestamp(true);
        o.setTsaUrl(tsaServer.getUrl());
        o.setTsaHashAlg("SHA-256");
        return new DssSigningEngine().sign(o, new MapEngineConfig(engineCfg));
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
