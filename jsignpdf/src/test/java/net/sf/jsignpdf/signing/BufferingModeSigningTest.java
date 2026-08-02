package net.sf.jsignpdf.signing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    /** The signature dictionary's signing time, e.g. {@code /M (D:20260802120000+02'00')}. */
    private static final Pattern SIGNING_TIME = Pattern.compile("/M \\(D:\\d{14}");

    /** The trailer's document ID, regenerated on every save, e.g. {@code /ID [<a1..><b2..>]}. */
    private static final Pattern DOCUMENT_ID = Pattern.compile("/ID \\[<[0-9a-fA-F]*><[0-9a-fA-F]*>\\]");

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
     * blob, with it. Two regions are excluded for that reason and everything else must match exactly:
     * {@code /M} and the {@code /Contents} gap the ByteRange already identifies. Comparing at byte
     * granularity instead would be unsound, since bytes inside the high-entropy signature blob collide by
     * chance often enough to look stable.
     * </p>
     */
    @Test
    public void tempModeChangesOnlyWhereStagingCannotMatter() throws Exception {
        BasicSignerOptions options = createDefaultOptions();

        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "memory");
        Signed memoryA = sign(options);
        Signed memoryB = sign(options);

        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");
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

    /** Staging must honour buffering.tempDir rather than always using java.io.tmpdir. */
    @Test
    public void stagingHonoursTheConfiguredDirectory() throws Exception {
        File stagingDir = tempFolder.newFolder("staging");
        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR, stagingDir.getAbsolutePath());

        // The staging file only exists mid-sign, so observe it indirectly: a read-only directory makes
        // File.createTempFile fail there, which proves that is where the engine tried to write.
        assertTrue(stagingDir.setWritable(false));
        try {
            org.junit.Assume.assumeFalse("running as root, permissions are not enforced", stagingDir.canWrite());
            assertTrue("Signing must fail when the configured staging directory is unusable",
                    !new SignerLogic(createDefaultOptions()).signFile());
        } finally {
            stagingDir.setWritable(true);
        }
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
     * Compares everything the ByteRange covers, with the two documented per-run values normalised away:
     * the signature dictionary's {@code /M} signing time and the trailer's regenerated {@code /ID}. The
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

    /** Blanks the per-run signing time and document ID in the given slice. */
    private static byte[] normalise(byte[] pdf, int from, int length) {
        String slice = new String(pdf, from, length, StandardCharsets.ISO_8859_1);
        String masked = SIGNING_TIME.matcher(slice).replaceAll("/M (D:00000000000000");
        masked = DOCUMENT_ID.matcher(masked).replaceAll("/ID [<0><0>]");
        return masked.getBytes(StandardCharsets.ISO_8859_1);
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

    private static File[] stagingFiles(File dir) {
        return dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File d, String name) {
                return name.startsWith(TEMP_PREFIX);
            }
        });
    }
}
