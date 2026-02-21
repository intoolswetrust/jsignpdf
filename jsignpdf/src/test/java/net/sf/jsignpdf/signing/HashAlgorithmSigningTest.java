package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

public class HashAlgorithmSigningTest extends SigningTestBase {

    @Test
    public void testSha1() throws Exception {
        assertHashAlgorithm(HashAlgorithm.SHA1, "SHA-1");
    }

    @Test
    public void testSha256() throws Exception {
        assertHashAlgorithm(HashAlgorithm.SHA256, "SHA-256");
    }

    @Test
    public void testSha384() throws Exception {
        assertHashAlgorithm(HashAlgorithm.SHA384, "SHA-384");
    }

    @Test
    public void testSha512() throws Exception {
        assertHashAlgorithm(HashAlgorithm.SHA512, "SHA-512");
    }

    @Test
    public void testRipemd160() throws Exception {
        assertHashAlgorithm(HashAlgorithm.RIPEMD160, "RIPEMD160");
    }

    private void assertHashAlgorithm(HashAlgorithm algorithm, String expectedName) throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setHashAlgorithm(algorithm);
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        String actualName = PdfSignatureValidator.digestOidToName(result.digestAlgorithmOid);
        assertEquals("Digest algorithm should match", expectedName, actualName);
    }
}
