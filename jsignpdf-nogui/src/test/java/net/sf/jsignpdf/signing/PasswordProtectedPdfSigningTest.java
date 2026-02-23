package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.SignerLogic;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

/**
 * Tests signing of password-protected PDF documents. Verifies that the DSS PAdES
 * signing flow correctly handles PDFs encrypted with owner/user passwords via
 * {@code PAdESSignatureParameters.setPasswordProtection(char[])}.
 *
 * <p><b>Note on features not implemented in DSS-based {@link SignerLogic}:</b></p>
 * <ul>
 *   <li>PDF output encryption ({@code PDFEncryption.PASSWORD} / {@code PDFEncryption.CERTIFICATE})
 *       — the option fields exist in {@code BasicSignerOptions} but are not used during signing.</li>
 *   <li>Encryption certificate ({@code pdfEncryptionCertFile}) — similarly not implemented.</li>
 * </ul>
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
}
