package net.sf.jsignpdf.signing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.SignerLogic;

/**
 * Tests PDF signing in append vs. non-append mode. Append mode preserves the original PDF
 * bytes and appends the signature, while non-append mode rewrites the entire file.
 */
public class AppendModeSigningTest extends SigningTestBase {


    /** Verifies that append mode preserves the original file bytes as a prefix of the output. */
    @Test
    public void testAppendPreservesOriginalBytes() throws Exception {
        BasicSignerOptions options = createDefaultOptions();

        File inFile = new File(options.getInFile());
        byte[] originalBytes = Files.readAllBytes(inFile.toPath());

        boolean success = new SignerLogic(options).signFile();
        assertTrue("Signing should succeed", success);

        File outFile = new File(options.getEffectiveOutFile());
        byte[] signedBytes = Files.readAllBytes(outFile.toPath());

        assertTrue("Signed file should be larger than original", signedBytes.length > originalBytes.length);

        byte[] prefix = new byte[originalBytes.length];
        System.arraycopy(signedBytes, 0, prefix, 0, originalBytes.length);
        assertArrayEquals("Signed file should start with original bytes", originalBytes, prefix);
    }
}
