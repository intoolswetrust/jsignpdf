package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.SignerLogic;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

/**
 * Basic smoke tests for {@link SignerLogic#signFile()} verifying that the default signing
 * flow produces a valid PDF with a correct PKCS#7 signature structure.
 */
public class BasicSigningTest extends SigningTestBase {

    /** Verifies signature presence, SubFilter, ByteRange, CMS structure, and cryptographic validity. */
    @Test
    public void testDefaultSigningWorks() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        ValidationResult result = signAndValidate(options);

        assertEquals("Should have 1 signature", 1, result.signatureCount);
        assertEquals("SubFilter should be ETSI.CAdES.detached", "ETSI.CAdES.detached", result.subFilter);
        assertTrue("ByteRange should start at 0", result.byteRangeStartsAtZero);
        assertTrue("ByteRange should end at EOF", result.byteRangeEndsAtEof);
        assertTrue("ByteRange should have gap for Contents", result.byteRangeHasGap);
        assertEquals("CMS should have 1 signer", 1, result.cmsSignerCount);
        assertTrue("Certificate should be present", result.certificateCount > 0);
        assertTrue("Signature should be cryptographically valid", result.signatureValid);
    }

    /** Verifies that {@link SignerLogic#signFile()} returns {@code true} on success. */
    @Test
    public void testSignFileReturnsTrue() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        boolean success = new SignerLogic(options).signFile();
        assertTrue("signFile() should return true", success);
    }

    /** Verifies that the signed output file is larger than the unsigned input (signature adds bytes). */
    @Test
    public void testOutputFileIsLargerThanInput() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        File inFile = new File(options.getInFile());
        long inputSize = inFile.length();

        new SignerLogic(options).signFile();

        File outFile = new File(options.getOutFileX());
        assertTrue("Output file should exist", outFile.exists());
        assertTrue("Output should be larger than input", outFile.length() > inputSize);
    }
}
