package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.TestConstants.Keystore;
import net.sf.jsignpdf.TestConstants.TestPrivateKey;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

public class KeyTypeSigningTest extends SigningTestBase {

    @Test
    public void testRsa1024Jks() throws Exception {
        assertKeyType(TestPrivateKey.RSA1024, Keystore.JKS);
    }

    @Test
    public void testRsa2048Jks() throws Exception {
        assertKeyType(TestPrivateKey.RSA2048, Keystore.JKS);
    }

    @Test
    public void testRsa4096Jks() throws Exception {
        assertKeyType(TestPrivateKey.RSA4096, Keystore.JKS);
    }

    @Test
    public void testDsa1024Jks() throws Exception {
        assertKeyType(TestPrivateKey.DSA1024, Keystore.JKS);
    }

    @Test
    public void testRsa1024Pkcs12() throws Exception {
        assertKeyType(TestPrivateKey.RSA1024, Keystore.PKCS12);
    }

    @Test
    public void testRsa2048Pkcs12() throws Exception {
        assertKeyType(TestPrivateKey.RSA2048, Keystore.PKCS12);
    }

    @Test
    public void testRsa4096Pkcs12() throws Exception {
        assertKeyType(TestPrivateKey.RSA4096, Keystore.PKCS12);
    }

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
