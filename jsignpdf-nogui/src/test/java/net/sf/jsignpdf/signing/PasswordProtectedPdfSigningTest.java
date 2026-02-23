package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.SignerLogic;
import net.sf.jsignpdf.TestConstants;
import net.sf.jsignpdf.types.PDFEncryption;
import net.sf.jsignpdf.types.PrintRight;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

/**
 * Tests signing of password-protected PDF documents and PDF output encryption
 * (encrypt-before-sign). Verifies that the DSS PAdES signing flow correctly
 * handles PDFs encrypted with owner/user passwords via
 * {@code PAdESSignatureParameters.setPasswordProtection(char[])} and that
 * {@link SignerLogic} can encrypt a plain PDF before signing it.
 */
public class PasswordProtectedPdfSigningTest extends SigningTestBase {

    private static final String OWNER_PASSWORD = "ownerTestPassword";
    private static final String USER_PASSWORD = "userTestPassword";

    /** Signs a PDF protected with only an owner password. */
    @Test
    public void testSignWithOwnerPassword() throws Exception {
        File protectedPdf = createPasswordProtectedPdf(OWNER_PASSWORD, "");
        BasicSignerOptions options = createDefaultOptions();
        options.setInFile(protectedPdf.getAbsolutePath());
        options.setPdfOwnerPwd(OWNER_PASSWORD.toCharArray());

        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Should have 1 signature", 1, result.signatureCount);
    }

    /** Signs a PDF protected with both owner and user passwords. */
    @Test
    public void testSignWithOwnerAndUserPassword() throws Exception {
        File protectedPdf = createPasswordProtectedPdf(OWNER_PASSWORD, USER_PASSWORD);
        BasicSignerOptions options = createDefaultOptions();
        options.setInFile(protectedPdf.getAbsolutePath());
        options.setPdfOwnerPwd(OWNER_PASSWORD.toCharArray());

        boolean success = new SignerLogic(options).signFile();
        assertTrue("Signing should succeed", success);

        File outFile = new File(options.getOutFileX());
        assertTrue("Output file should exist", outFile.exists());

        // The signed output retains PDF encryption, so the validator needs a password
        ValidationResult result = PdfSignatureValidator.validate(outFile, OWNER_PASSWORD);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Should have 1 signature", 1, result.signatureCount);
    }

    /** Signing a password-protected PDF without providing the password should fail. */
    @Test
    public void testSignWithoutPasswordFails() throws Exception {
        File protectedPdf = createPasswordProtectedPdf(OWNER_PASSWORD, USER_PASSWORD);
        BasicSignerOptions options = createDefaultOptions();
        options.setInFile(protectedPdf.getAbsolutePath());
        // Do not set pdfOwnerPwd

        boolean success = new SignerLogic(options).signFile();
        assertFalse("Signing should fail without password", success);
    }

    /** Full structural validation of a signed password-protected PDF. */
    @Test
    public void testSignedOutputHasValidStructure() throws Exception {
        File protectedPdf = createPasswordProtectedPdf(OWNER_PASSWORD, "");
        BasicSignerOptions options = createDefaultOptions();
        options.setInFile(protectedPdf.getAbsolutePath());
        options.setPdfOwnerPwd(OWNER_PASSWORD.toCharArray());

        ValidationResult result = signAndValidate(options);

        assertEquals("SubFilter should be ETSI.CAdES.detached", "ETSI.CAdES.detached", result.subFilter);
        assertTrue("ByteRange should start at 0", result.byteRangeStartsAtZero);
        assertTrue("ByteRange should end at EOF", result.byteRangeEndsAtEof);
        assertTrue("ByteRange should have gap for Contents", result.byteRangeHasGap);
        assertEquals("CMS should have 1 signer", 1, result.cmsSignerCount);
        assertTrue("Certificate should be present", result.certificateCount > 0);
        assertTrue("Signature should be cryptographically valid", result.signatureValid);
    }

    // --- Encrypt-before-sign tests ---

    /** Encrypts an unprotected PDF with PASSWORD encryption then signs it. */
    @Test
    public void testPasswordEncryptionBeforeSigning() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setPdfEncryption(PDFEncryption.PASSWORD);
        options.setPdfOwnerPwd(OWNER_PASSWORD.toCharArray());
        options.setPdfUserPwd(USER_PASSWORD.toCharArray());

        boolean success = new SignerLogic(options).signFile();
        assertTrue("Signing with password encryption should succeed", success);

        File outFile = new File(options.getOutFileX());
        assertTrue("Output file should exist", outFile.exists());

        // Output should be encrypted — loading without password should fail or report encrypted
        try (PDDocument doc = PDDocument.load(outFile, OWNER_PASSWORD)) {
            assertTrue("Output PDF should be encrypted", doc.isEncrypted());
        }

        // Validate signature with the owner password
        ValidationResult result = PdfSignatureValidator.validate(outFile, OWNER_PASSWORD);
        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Should have 1 signature", 1, result.signatureCount);
    }

    /**
     * Certificate-based encryption is not supported because DSS PAdES cannot decrypt
     * certificate-encrypted PDFs, and post-signing encryption would invalidate the signature.
     */
    @Test
    public void testCertificateEncryptionIsNotSupported() throws Exception {
        File certFile = exportCertificateFromKeystore();

        BasicSignerOptions options = createDefaultOptions();
        options.setPdfEncryption(PDFEncryption.CERTIFICATE);
        options.setPdfEncryptionCertFile(certFile.getAbsolutePath());

        boolean success = new SignerLogic(options).signFile();
        assertFalse("Certificate encryption should be rejected", success);
    }

    /** Attempting to encrypt a PDF that already has signatures should fail. */
    @Test
    public void testEncryptionBlockedWhenExistingSignatures() throws Exception {
        // First, sign the PDF normally
        BasicSignerOptions options1 = createDefaultOptions();
        boolean success1 = new SignerLogic(options1).signFile();
        assertTrue("First signing should succeed", success1);

        File signedPdf = new File(options1.getOutFileX());
        assertTrue("Signed PDF should exist", signedPdf.exists());

        // Now try to encrypt+sign the already-signed PDF
        BasicSignerOptions options2 = TestConstants.TestPrivateKey.RSA2048.toSignerOptions(TestConstants.Keystore.JKS);
        options2.setInFile(signedPdf.getAbsolutePath());
        File outFile2 = new File(tempFolder.getRoot(), "output2.pdf");
        options2.setOutFile(outFile2.getAbsolutePath());
        options2.setPdfEncryption(PDFEncryption.PASSWORD);
        options2.setPdfOwnerPwd("owner".toCharArray());
        options2.setPdfUserPwd("user".toCharArray());

        boolean success2 = new SignerLogic(options2).signFile();
        assertFalse("Encrypting an already-signed PDF should fail", success2);
    }

    /** Encrypts a PDF with restricted permissions and verifies them in the output. */
    @Test
    public void testEncryptionWithPermissions() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setPdfEncryption(PDFEncryption.PASSWORD);
        options.setPdfOwnerPwd(OWNER_PASSWORD.toCharArray());
        options.setPdfUserPwd(USER_PASSWORD.toCharArray());
        options.setRightPrinting(PrintRight.DISALLOW_PRINTING);
        options.setRightCopy(false);
        options.setRightModifyContents(false);

        boolean success = new SignerLogic(options).signFile();
        assertTrue("Signing with permissions should succeed", success);

        File outFile = new File(options.getOutFileX());

        // Validate signature is cryptographically valid
        ValidationResult result = PdfSignatureValidator.validate(outFile, OWNER_PASSWORD);
        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Should have 1 signature", 1, result.signatureCount);

        // Load with the user password to see restricted permissions
        // (owner password grants full access per PDF spec)
        try (PDDocument doc = PDDocument.load(outFile, USER_PASSWORD)) {
            assertTrue("Output PDF should be encrypted", doc.isEncrypted());
            AccessPermission ap = doc.getCurrentAccessPermission();
            assertFalse("Printing should be disallowed", ap.canPrint());
            assertFalse("Content extraction should be disallowed", ap.canExtractContent());
            assertFalse("Modification should be disallowed", ap.canModify());
        }
    }

    // --- Helper methods ---

    /**
     * Creates a minimal password-protected PDF using PDFBox {@link StandardProtectionPolicy}.
     */
    private File createPasswordProtectedPdf(String ownerPassword, String userPassword) throws Exception {
        File protectedPdf = new File(tempFolder.getRoot(), "protected.pdf");
        PDDocument doc = new PDDocument();
        doc.getDocument().setVersion(1.7f);
        PDPage page = new PDPage();
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 12);
        cs.newLineAtOffset(100, 700);
        cs.showText("Password-protected test PDF");
        cs.endText();
        cs.close();

        AccessPermission accessPermission = new AccessPermission();
        StandardProtectionPolicy policy = new StandardProtectionPolicy(
                ownerPassword, userPassword, accessPermission);
        policy.setEncryptionKeyLength(128);
        doc.protect(policy);
        doc.save(protectedPdf);
        doc.close();
        return protectedPdf;
    }

    /**
     * Exports the RSA2048 certificate from the test JKS keystore to a DER-encoded temp file.
     */
    private File exportCertificateFromKeystore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (java.io.FileInputStream fis = new java.io.FileInputStream(TestConstants.KEYSTORE_FILE_JKS)) {
            ks.load(fis, TestConstants.KEYSTORE_TEST_PASSWD);
        }
        Certificate cert = ks.getCertificate(TestConstants.TestPrivateKey.RSA2048.getAlias());
        File certFile = new File(tempFolder.getRoot(), "encrypt-cert.cer");
        try (FileOutputStream fos = new FileOutputStream(certFile)) {
            fos.write(cert.getEncoded());
        }
        return certFile;
    }
}
