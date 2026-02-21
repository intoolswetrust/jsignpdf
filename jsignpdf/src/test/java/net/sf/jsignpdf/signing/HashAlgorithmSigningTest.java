package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

/**
 * Tests that each supported {@link HashAlgorithm} produces a valid signature whose CMS
 * container uses the correct digest algorithm.
 */
public class HashAlgorithmSigningTest extends SigningTestBase {

    /** Signs with SHA-1 and verifies the CMS digest algorithm OID. */
    @Test
    public void testSha1() throws Exception {
        assertHashAlgorithm(HashAlgorithm.SHA1, "SHA-1");
    }

    /** Signs with SHA-256 and verifies the CMS digest algorithm OID. */
    @Test
    public void testSha256() throws Exception {
        assertHashAlgorithm(HashAlgorithm.SHA256, "SHA-256");
    }

    /** Signs with SHA-384 and verifies the CMS digest algorithm OID. */
    @Test
    public void testSha384() throws Exception {
        assertHashAlgorithm(HashAlgorithm.SHA384, "SHA-384");
    }

    /** Signs with SHA-512 and verifies the CMS digest algorithm OID. */
    @Test
    public void testSha512() throws Exception {
        assertHashAlgorithm(HashAlgorithm.SHA512, "SHA-512");
    }

    /** Signs with RIPEMD-160 and verifies the CMS digest algorithm OID. */
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
