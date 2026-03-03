package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.SignerLogic;
import net.sf.jsignpdf.TestConstants.Keystore;
import net.sf.jsignpdf.TestConstants.TestPrivateKey;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

/**
 * Tests signing a PDF that already contains a signature. Verifies that append-mode signing
 * preserves the first signature and adds a second one, both remaining cryptographically valid.
 */
public class MultipleSignaturesTest extends SigningTestBase {

    /** Signs a PDF twice (with different keys) in append mode and validates both signatures. */
    @Test
    public void testDoubleSign() throws Exception {
        // First signing
        BasicSignerOptions options1 = createDefaultOptions();
        boolean success1 = new SignerLogic(options1).signFile();
        assertTrue("First signing should succeed", success1);

        File firstSigned = new File(options1.getEffectiveOutFile());

        // Second signing: use the first output as input
        File secondInput = new File(tempFolder.getRoot(), "second_input.pdf");
        Files.copy(firstSigned.toPath(), secondInput.toPath());
        File secondOutput = new File(tempFolder.getRoot(), "second_output.pdf");

        BasicSignerOptions options2 = TestPrivateKey.RSA4096.toSignerOptions(Keystore.JKS);
        options2.setInFile(secondInput.getAbsolutePath());
        options2.setOutFile(secondOutput.getAbsolutePath());

        boolean success2 = new SignerLogic(options2).signFile();
        assertTrue("Second signing should succeed", success2);

        // Validate both signatures
        int sigCount = PdfSignatureValidator.getSignatureCount(secondOutput);
        assertEquals("Should have 2 signatures", 2, sigCount);

        ValidationResult result1 = PdfSignatureValidator.validate(secondOutput, 0);
        assertTrue("First signature should be valid", result1.signatureValid);

        ValidationResult result2 = PdfSignatureValidator.validate(secondOutput, 1);
        assertTrue("Second signature should be valid", result2.signatureValid);
    }
}
