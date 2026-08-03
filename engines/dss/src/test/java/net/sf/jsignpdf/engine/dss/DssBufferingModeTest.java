package net.sf.jsignpdf.engine.dss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.utils.AdvancedConfig;
import net.sf.jsignpdf.utils.AppConfig;
import net.sf.jsignpdf.utils.PropertyStoreFactory;

/**
 * Covers {@code buffering.mode=temp} on the DSS engine: signing still succeeds, and the staged temp files
 * are released &mdash; both at the end of a sign and, crucially, between attempts of the undersize-retry
 * loop, which stages a full copy of the document per {@code getDataToSign} / {@code signDocument} call.
 */
public class DssBufferingModeTest {

    private static final String KS_FILE = "src/test/resources/test-keystore.jks";
    private static final char[] KS_PASSWD = "keystorepass".toCharArray();
    private static final String KEY_ALIAS = "rsa2048";
    private static final char[] KEY_PASSWD = "RSA2048pass".toCharArray();

    private static final EngineConfig EMPTY_CONFIG = new MapEngineConfig(new HashMap<>());

    /**
     * Most staged files a single sign may hold at once. One attempt's worth is 2; the loop reaches 3 when a
     * failed attempt's files are not released before the retry.
     */
    private static final int MAX_CONCURRENT_STAGED = 2;

    /** Prefix of the temp the encrypt-before-sign path writes; see {@code DssSigningEngine.encryptPdf}. */
    private static final String ENC_PREFIX = "jsignpdf-dss-enc-";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final AdvancedConfig cfg = PropertyStoreFactory.getInstance().advancedConfig();

    private File inputFile;
    private File outputFile;
    private File stagingDir;

    @BeforeClass
    public static void addProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void setUp() throws Exception {
        inputFile = tmp.newFile("input.pdf");
        outputFile = tmp.newFile("output.pdf");
        outputFile.delete();
        stagingDir = tmp.newFolder("staging");

        try (PDDocument doc = new PDDocument()) {
            doc.setVersion(1.7f);
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("Test PDF for DSS buffering");
                cs.endText();
            }
            doc.save(inputFile);
        }

        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR, stagingDir.getAbsolutePath());
    }

    @After
    public void restore() {
        cfg.removeProperty(AppConfig.KEY_BUFFERING_MODE);
        cfg.removeProperty(AppConfig.KEY_BUFFERING_TEMP_DIR);
    }

    @Test
    public void tempModeSignsAndCleansUp() throws Exception {
        assertTrue("Signing in temp mode must succeed",
                new DssSigningEngine().sign(baseOptions(), EMPTY_CONFIG));
        assertTrue("Output must exist", outputFile.length() > 0);
        assertNoStagedFiles();
    }

    /** The visible-signature path loads the input through PDFBox directly, outside DSS's resource handler. */
    @Test
    public void tempModeSignsWithVisibleSignature() throws Exception {
        BasicSignerOptions o = baseOptions();
        o.setVisible(true);
        o.setPage(1);
        o.setPositionLLX(100);
        o.setPositionLLY(100);
        o.setPositionURX(300);
        o.setPositionURY(200);

        assertTrue("Visible signing in temp mode must succeed", new DssSigningEngine().sign(o, EMPTY_CONFIG));
        assertNoStagedFiles();
    }

    /**
     * Each attempt of the undersize-retry loop stages a full copy of the document, so without a
     * {@code clear()} between attempts they pile up: measured peak is 3 concurrent staged files without the
     * per-attempt release and 2 with it.
     *
     * <p>
     * The end-of-sign assertion alone cannot catch this &mdash; the final {@code clear()} in the engine's
     * {@code finally} removes accumulated files too &mdash; hence the sampling below. It is deliberately
     * one-sided: a sampler that misses the peak under-reports and the test passes, so it can miss a
     * regression but can never fail spuriously.
     * </p>
     */
    @Test
    public void retryLoopReleasesStagedFilesBetweenAttempts() throws Exception {
        Map<String, String> engineCfg = new HashMap<>();
        // Forces DSS's "signature size too small" error, so the loop runs more than one attempt.
        engineCfg.put(DssSigningEngine.KEY_CONTENT_SIZE, "100");

        StagingWatcher watcher = new StagingWatcher(stagingDir);
        try {
            assertTrue("The undersize retry must still recover in temp mode",
                    new DssSigningEngine().sign(baseOptions(), new MapEngineConfig(engineCfg)));
        } finally {
            watcher.close();
        }

        // Doubles as the proof that DSS stages in buffering.tempDir at all: with the directory ignored the
        // peak would be 0 and the upper bound below would hold vacuously.
        assertTrue("DSS must stage in the configured directory, but nothing ever appeared there",
                watcher.peak() > 0);
        assertTrue("A failed attempt's staged files must be released before retrying, but " + watcher.peak()
                + " were alive at once", watcher.peak() <= MAX_CONCURRENT_STAGED);
        assertNoStagedFiles();
    }

    /** A failed sign must not leave staged files behind either. */
    @Test
    public void failedSignLeavesNoStagedFiles() throws Exception {
        Map<String, String> engineCfg = new HashMap<>();
        engineCfg.put(DssSigningEngine.KEY_CONTENT_SIZE, "100");
        engineCfg.put(DssSigningEngine.KEY_RETRY_ON_UNDERSIZE, "false");

        assertTrue("Signing must fail with the retry disabled",
                !new DssSigningEngine().sign(baseOptions(), new MapEngineConfig(engineCfg)));
        assertNoStagedFiles();
    }

    /**
     * The encrypt-before-sign temp must land in buffering.tempDir, not java.io.tmpdir. It exists from
     * {@code encryptPdf} until the engine's {@code finally}, so it is only observable while the sign runs.
     */
    @Test
    public void encryptBeforeSignStagesInTheConfiguredDirectory() throws Exception {
        BasicSignerOptions o = baseOptions();
        o.setPdfEncryption(net.sf.jsignpdf.types.PDFEncryption.PASSWORD);
        o.setPdfOwnerPwd("owner".toCharArray());
        o.setPdfUserPwd("user".toCharArray());

        StagingWatcher watcher = new StagingWatcher(stagingDir);
        try {
            assertTrue("Encrypt-then-sign must succeed in temp mode", new DssSigningEngine().sign(o, EMPTY_CONFIG));
        } finally {
            watcher.close();
        }

        assertTrue("The encrypt-before-sign temp must be created in buffering.tempDir, but nothing named "
                + ENC_PREFIX + "* ever appeared there; saw " + watcher.seen(), watcher.sawPrefix(ENC_PREFIX));
        assertNoStagedFiles();
    }

    /** An unusable buffering.tempDir must abort the sign rather than silently fall back to java.io.tmpdir. */
    @Test
    public void unusableTempDirAbortsTheSign() throws Exception {
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR,
                new File(tmp.getRoot(), "does-not-exist").getAbsolutePath());

        assertTrue("Signing must fail when the configured staging directory is unusable",
                !new DssSigningEngine().sign(baseOptions(), EMPTY_CONFIG));
    }

    /**
     * Everything left in the staging directory, whatever its name. Deliberately unfiltered: a
     * {@code jsignpdf-} prefix filter would hide PDFBox's own scratch files ({@code PDFBox*.tmp}), which the
     * engine's stream-cache wiring also directs here.
     */
    private void assertNoStagedFiles() {
        File[] left = stagingDir.listFiles();
        assertEquals("Staged temp files must not survive the sign: " + java.util.Arrays.toString(left),
                0, left.length);
    }

    /**
     * Records what appears in the staging directory while a sign runs, and the highest number of files
     * alive at once. Millisecond-paced: the staged files live for a large fraction of the sign, so this
     * takes hundreds of looks inside the window without spinning a core flat out.
     */
    private static final class StagingWatcher implements AutoCloseable {

        private final Set<String> seen = ConcurrentHashMap.newKeySet();
        private final AtomicInteger peak = new AtomicInteger();
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final Thread thread;

        StagingWatcher(File dir) {
            thread = new Thread(() -> {
                while (running.get()) {
                    File[] staged = dir.listFiles();
                    if (staged != null) {
                        peak.accumulateAndGet(staged.length, Math::max);
                        for (File f : staged) {
                            seen.add(f.getName());
                        }
                    }
                    try {
                        Thread.sleep(1L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, "staging-watcher");
            thread.setDaemon(true);
            thread.start();
        }

        int peak() {
            return peak.get();
        }

        boolean sawPrefix(String prefix) {
            return seen.stream().anyMatch(name -> name.startsWith(prefix));
        }

        Set<String> seen() {
            return seen;
        }

        @Override
        public void close() throws InterruptedException {
            running.set(false);
            thread.join();
        }
    }

    /** Minimal {@link EngineConfig} over a map; each DSS test class carries its own copy. */
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
            return v == null ? fallback : Integer.parseInt(v);
        }
    }

    private BasicSignerOptions baseOptions() {
        BasicSignerOptions o = new BasicSignerOptions();
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
}
