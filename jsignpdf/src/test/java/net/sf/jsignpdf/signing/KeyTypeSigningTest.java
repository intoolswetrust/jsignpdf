package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.TestConstants.Keystore;
import net.sf.jsignpdf.TestConstants.TestPrivateKey;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

/**
 * Tests signing with all supported key types (RSA-1024/2048/4096, DSA-1024) across both
 * JKS and PKCS#12 keystore formats. Verifies that each combination produces a valid
 * signature with an embedded certificate.
 */
public class KeyTypeSigningTest extends SigningTestBase {

    /** Signs with RSA-1024 from a JKS keystore. */
    @Test
    public void testRsa1024Jks() throws Exception {
        assertKeyType(TestPrivateKey.RSA1024, Keystore.JKS);
    }

    /** Signs with RSA-2048 from a JKS keystore. */
    @Test
    public void testRsa2048Jks() throws Exception {
        assertKeyType(TestPrivateKey.RSA2048, Keystore.JKS);
    }

    /** Signs with RSA-4096 from a JKS keystore. */
    @Test
    public void testRsa4096Jks() throws Exception {
        assertKeyType(TestPrivateKey.RSA4096, Keystore.JKS);
    }

    /** Signs with DSA-1024 from a JKS keystore. */
    @Test
    public void testDsa1024Jks() throws Exception {
        assertKeyType(TestPrivateKey.DSA1024, Keystore.JKS);
    }

    /** Signs with RSA-1024 from a PKCS#12 keystore. */
    @Test
    public void testRsa1024Pkcs12() throws Exception {
        assertKeyType(TestPrivateKey.RSA1024, Keystore.PKCS12);
    }

    /** Signs with RSA-2048 from a PKCS#12 keystore. */
    @Test
    public void testRsa2048Pkcs12() throws Exception {
        assertKeyType(TestPrivateKey.RSA2048, Keystore.PKCS12);
    }

    /** Signs with RSA-4096 from a PKCS#12 keystore. */
    @Test
    public void testRsa4096Pkcs12() throws Exception {
        assertKeyType(TestPrivateKey.RSA4096, Keystore.PKCS12);
    }

    /** Signs with DSA-1024 from a PKCS#12 keystore. */
    @Test
    public void testDsa1024Pkcs12() throws Exception {
        assertKeyType(TestPrivateKey.DSA1024, Keystore.PKCS12);
    }

    private void assertKeyType(TestPrivateKey key, Keystore keystore) throws Exception {
        BasicSignerOptions options = createOptions(key, keystore);
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid for " + key + " with " + keystore, result.signatureValid);
        assertNotNull("Certificate subject should be present", result.signerCertificateSubject);
        assertTrue("Certificate subject should not be empty", result.signerCertificateSubject.length() > 0);
    }
}
