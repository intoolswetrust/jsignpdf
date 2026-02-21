package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.SignerLogic;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

public class AppendModeSigningTest extends SigningTestBase {

    @Test
    public void testAppendMode() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setAppend(true);
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid in append mode", result.signatureValid);
        assertEquals("Should have 1 signature", 1, result.signatureCount);
    }

    @Test
    public void testNonAppendMode() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setAppend(false);
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid in non-append mode", result.signatureValid);
        assertEquals("Should have 1 signature", 1, result.signatureCount);
    }

    @Test
    public void testAppendPreservesOriginalBytes() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setAppend(true);

        File inFile = new File(options.getInFile());
        byte[] originalBytes = Files.readAllBytes(inFile.toPath());

        boolean success = new SignerLogic(options).signFile();
        assertTrue("Signing should succeed", success);

        File outFile = new File(options.getOutFileX());
        byte[] signedBytes = Files.readAllBytes(outFile.toPath());

        assertTrue("Signed file should be larger than original", signedBytes.length > originalBytes.length);

        byte[] prefix = new byte[originalBytes.length];
        System.arraycopy(signedBytes, 0, prefix, 0, originalBytes.length);
        assertArrayEquals("Signed file should start with original bytes", originalBytes, prefix);
    }
}
