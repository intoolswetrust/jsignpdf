package net.sf.jsignpdf.signing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.SignerLogic;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;
import net.sf.jsignpdf.utils.AdvancedConfig;
import net.sf.jsignpdf.utils.AppConfig;
import net.sf.jsignpdf.utils.PropertyStoreFactory;

/**
 * Verifies that {@code buffering.mode=temp} changes only <em>where</em> the OpenPDF engine stages the
 * document, never what it writes, and that it leaves no temporary files behind.
 */
public class BufferingModeSigningTest extends SigningTestBase {

    private static final String TEMP_PREFIX = "jsignpdf-sig-";

    /**
     * The per-run values that OpenPDF derives from the wall clock or from randomness, all of them inside
     * the ByteRange-covered region: the signature dictionary's signing time ({@code /M(D:2026...)}), the
     * Info dictionary's modification date ({@code /ModDate(D:2026...)}) and the trailer's document ID
     * ({@code /ID [<a1..><b2..>]}). Written without a space after the key, so the patterns must not assume
     * one.
     */
    private static final Pattern[] PER_RUN_VALUES = {
            Pattern.compile("(?<key>/M\\s*\\(D:)\\d{14}"),
            Pattern.compile("(?<key>/ModDate\\s*\\(D:)\\d{14}"),
            Pattern.compile("(?<key>/ID\\s*\\[)<[0-9a-fA-F]*><[0-9a-fA-F]*>\\]"),
    };

    /** Replacements for {@link #PER_RUN_VALUES}, index-aligned. */
    private static final String[] PER_RUN_MASKS = {
            "${key}00000000000000",
            "${key}00000000000000",
            "${key}<0><0>]",
    };

    private final AdvancedConfig cfg = PropertyStoreFactory.getInstance().advancedConfig();

    @After
    public void restore() {
        cfg.removeProperty(AppConfig.KEY_BUFFERING_MODE);
        cfg.removeProperty(AppConfig.KEY_BUFFERING_TEMP_DIR);
    }

    /**
     * Signing in temp mode must still produce a valid signature. Guards the whole {@code raf} path inside
     * OpenPDF, which is where {@code preClose()} / {@code getRangeStream()} / {@code close()} diverge.
     */
    @Test
    public void tempModeProducesAValidSignature() throws Exception {
        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");

        ValidationResult result = signAndValidate(createDefaultOptions());

        assertEquals("Should have 1 signature", 1, result.signatureCount);
        assertTrue("ByteRange should start at 0", result.byteRangeStartsAtZero);
        assertTrue("ByteRange should end at EOF", result.byteRangeEndsAtEof);
        assertTrue("Signature should be cryptographically valid", result.signatureValid);
    }

    /**
     * The core correctness claim: staging changes where bytes are written, never what they are.
     *
     * <p>
     * A whole-file byte comparison is unavailable. The signing time comes from {@code Calendar.getInstance()}
     * inside OpenPDF, lands in the signature dictionary's {@code /M}, and is covered by the ByteRange &mdash;
     * so it changes on every run and drags the CMS message digest, and therefore the whole {@code /Contents}
     * blob, with it. The {@link #PER_RUN_VALUES} and the {@code /Contents} gap the ByteRange already
     * identifies are excluded for that reason; everything else must match exactly. Comparing at byte
     * granularity instead would be unsound, since bytes inside the high-entropy signature blob collide by
     * chance often enough to look stable.
     * </p>
     *
     * <p>
     * Each run is separated by a second boundary so that every masked value really does differ between
     * runs; otherwise the control below silently degrades into "the clock did not tick".
     * </p>
     */
    @Test
    public void tempModeChangesOnlyWhereStagingCannotMatter() throws Exception {
        BasicSignerOptions options = createDefaultOptions();

        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "memory");
        Signed memoryA = sign(options);
        sleepPastSecondBoundary();
        Signed memoryB = sign(options);

        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");
        sleepPastSecondBoundary();
        Signed temp = sign(options);

        // Control: two memory-mode runs must already agree under the same normalisation. If this fails the
        // masking is incomplete and the assertion below would prove nothing.
        assertCoveredBytesEqual("Two memory-mode runs must agree once per-run values are normalised",
                memoryA, memoryB);

        assertEquals("Staging must not change the document length", memoryA.bytes.length, temp.bytes.length);
        assertArrayEquals("Staging must not move the signature or resize /Contents", memoryA.range, temp.range);
        assertCoveredBytesEqual("temp mode changed a byte that staging cannot legitimately affect",
                memoryA, temp);
    }

    /** A successful sign must not leave its staging file behind. */
    @Test
    public void tempFileIsRemovedAfterSuccess() throws Exception {
        File stagingDir = tempFolder.newFolder("staging");
        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR, stagingDir.getAbsolutePath());

        assertTrue("Signing should succeed", new SignerLogic(createDefaultOptions()).signFile());

        assertEquals("No staging file may survive a successful sign", 0, stagingFiles(stagingDir).length);
    }

    /**
     * The case OpenPDF itself gets wrong: an unreachable TSA aborts between {@code preClose()} and
     * {@code close()}, and OpenPDF only deletes its temp file in {@code close()}. Owning the file is what
     * makes this pass.
     */
    @Test
    public void tempFileIsRemovedAfterFailure() throws Exception {
        File stagingDir = tempFolder.newFolder("staging");
        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR, stagingDir.getAbsolutePath());

        BasicSignerOptions options = createDefaultOptions();
        options.setTimestamp(true);
        options.setTsaUrl("http://127.0.0.1:1/tsa-does-not-exist");

        assertTrue("Signing should fail with an unreachable TSA", !new SignerLogic(options).signFile());

        assertEquals("A failed sign must not leak the staging file", 0, stagingFiles(stagingDir).length);
    }

    /**
     * Staging must happen in buffering.tempDir rather than in java.io.tmpdir. The staging file only exists
     * mid-sign, so watch the directory while the sign runs rather than inspecting it afterwards &mdash; by
     * then the engine has deleted it, and an empty directory looks the same whether staging went here or
     * somewhere else entirely.
     */
    @Test
    public void stagingHonoursTheConfiguredDirectory() throws Exception {
        File stagingDir = tempFolder.newFolder("staging");
        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR, stagingDir.getAbsolutePath());

        StagingWatcher watcher = new StagingWatcher(stagingDir);
        try {
            assertTrue("Signing should succeed", new SignerLogic(createDefaultOptions()).signFile());
        } finally {
            watcher.close();
        }

        assertTrue("The staging file must be created in buffering.tempDir, but nothing named " + TEMP_PREFIX
                + "* ever appeared there; saw " + watcher.seen(), watcher.sawPrefix(TEMP_PREFIX));
        assertEquals("No staging file may survive the sign", 0, stagingFiles(stagingDir).length);
    }

    /** An unusable buffering.tempDir must abort the sign rather than silently fall back to java.io.tmpdir. */
    @Test
    public void unusableTempDirAbortsTheSign() throws Exception {
        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR,
                new File(tempFolder.getRoot(), "does-not-exist").getAbsolutePath());

        assertTrue("Signing must fail when the configured staging directory is unusable",
                !new SignerLogic(createDefaultOptions()).signFile());
    }

    /**
     * The output file must not be created before the buffering configuration is validated, otherwise a
     * typo in buffering.tempDir truncates a file the user may still need.
     */
    @Test
    public void unusableTempDirAbortsBeforeTouchingTheOutputFile() throws Exception {
        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR,
                new File(tempFolder.getRoot(), "does-not-exist").getAbsolutePath());

        BasicSignerOptions options = createDefaultOptions();
        File out = new File(options.getOutFileX());
        Files.write(out.toPath(), "previous output".getBytes(StandardCharsets.ISO_8859_1));

        assertTrue("Signing must fail", !new SignerLogic(options).signFile());

        assertEquals("The output file must be left untouched", "previous output",
                new String(Files.readAllBytes(out.toPath()), StandardCharsets.ISO_8859_1));
    }

    /** Temp mode must also work for a visible signature, which adds an appearance stream to the staged file. */
    @Test
    public void tempModeSignsWithAVisibleSignature() throws Exception {
        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");

        BasicSignerOptions options = createDefaultOptions();
        options.setVisible(true);
        options.setPage(1);
        options.setPositionLLX(100);
        options.setPositionLLY(100);
        options.setPositionURX(300);
        options.setPositionURY(200);

        ValidationResult result = signAndValidate(options);

        assertEquals("Should have 1 signature", 1, result.signatureCount);
        assertTrue("Signature should be visible", result.hasVisibleRect);
        assertTrue("Signature should be cryptographically valid", result.signatureValid);
    }

    /**
     * Append mode is the riskiest OpenPDF path in temp mode: it writes an incremental update, so the staged
     * file starts as a copy of the input and the ByteRange offsets are relative to the whole document.
     */
    @Test
    public void tempModeSignsInAppendMode() throws Exception {
        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");

        BasicSignerOptions options = createDefaultOptions();
        options.setAppend(true);

        ValidationResult result = signAndValidate(options);

        assertEquals("Should have 1 signature", 1, result.signatureCount);
        assertTrue("ByteRange should start at 0", result.byteRangeStartsAtZero);
        assertTrue("ByteRange should end at EOF", result.byteRangeEndsAtEof);
        assertTrue("Signature should be cryptographically valid", result.signatureValid);
    }

    /** An unusable buffering.tempDir must not affect memory mode, which never stages anything. */
    @Test
    public void memoryModeIgnoresAnUnusableTempDir() throws Exception {
        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "memory");
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR,
                new File(tempFolder.getRoot(), "does-not-exist").getAbsolutePath());

        assertTrue("A stale tempDir must not break a memory-mode sign",
                new SignerLogic(createDefaultOptions()).signFile());
    }

    /** A signed document plus the ByteRange that says which of its bytes the signature covers. */
    private static final class Signed {
        final byte[] bytes;
        final int[] range;

        Signed(byte[] bytes, int[] range) {
            this.bytes = bytes;
            this.range = range;
        }
    }

    private Signed sign(BasicSignerOptions options) throws Exception {
        byte[] bytes = signToBytes(options);
        return new Signed(bytes, byteRangeOf(options));
    }

    /**
     * Compares everything the ByteRange covers, with the documented per-run values normalised away. The
     * excluded {@code /Contents} gap holds the CMS blob, which necessarily changes when {@code /M} does.
     */
    private static void assertCoveredBytesEqual(String message, Signed expected, Signed actual) {
        assertArrayEquals(message + " (before /Contents)",
                normalise(expected.bytes, expected.range[0], expected.range[1]),
                normalise(actual.bytes, actual.range[0], actual.range[1]));
        assertArrayEquals(message + " (after /Contents)",
                normalise(expected.bytes, expected.range[2], expected.range[3]),
                normalise(actual.bytes, actual.range[2], actual.range[3]));
    }

    /** Blanks every {@link #PER_RUN_VALUES} occurrence in the given slice. */
    private static byte[] normalise(byte[] pdf, int from, int length) {
        String masked = new String(pdf, from, length, StandardCharsets.ISO_8859_1);
        for (int i = 0; i < PER_RUN_VALUES.length; i++) {
            masked = PER_RUN_VALUES[i].matcher(masked).replaceAll(PER_RUN_MASKS[i]);
        }
        return masked.getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * Blocks until the wall clock crosses a whole second. The per-run values this test masks all have
     * one-second resolution, so without this the control assertion only detects an incomplete mask when the
     * two runs happen to straddle a boundary &mdash; which is how a missing {@code /ModDate} mask survived
     * review and turned the test into a coin flip.
     */
    private static void sleepPastSecondBoundary() throws InterruptedException {
        final long second = System.currentTimeMillis() / 1000L;
        do {
            Thread.sleep(50L);
        } while (System.currentTimeMillis() / 1000L == second);
    }

    private byte[] signToBytes(BasicSignerOptions options) throws Exception {
        File out = new File(tempFolder.getRoot(), "out-" + System.nanoTime() + ".pdf");
        options.setOutFile(out.getAbsolutePath());
        assertTrue("Signing should succeed", new SignerLogic(options).signFile());
        PdfSignatureValidator.validate(out);
        return Files.readAllBytes(out.toPath());
    }

    private int[] byteRangeOf(BasicSignerOptions options) throws Exception {
        return PdfSignatureValidator.validate(new File(options.getOutFileX())).byteRange;
    }

    /**
     * Every file left in the staging directory, whatever its name. Deliberately unfiltered: a prefix filter
     * would hide anything staged by a library rather than by JSignPdf itself (PDFBox names its scratch files
     * {@code PDFBox*.tmp}), which is exactly the kind of leak this assertion exists to catch.
     */
    private static File[] stagingFiles(File dir) {
        File[] left = dir.listFiles();
        return left != null ? left : new File[0];
    }

    /**
     * Records the names of files that appear in a directory while something else writes to it. The staging
     * file exists from {@code createSignature} until the engine's {@code finally}, i.e. for essentially the
     * whole sign, so a millisecond-paced sampler takes hundreds of looks inside that window.
     */
    private static final class StagingWatcher implements AutoCloseable {

        private final Set<String> seen = ConcurrentHashMap.newKeySet();
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final Thread thread;

        StagingWatcher(File dir) {
            thread = new Thread(() -> {
                while (running.get()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
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
}
