package net.sf.jsignpdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.security.Security;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.openpdf.text.exceptions.BadPasswordException;

/**
 * Tests for {@link PdfExtraInfo}, focusing on password-protected PDF handling.
 */
public class PdfExtraInfoTest {

    private static final String OWNER_PASSWORD = "owner-secret";
    private static final String USER_PASSWORD = "user-secret";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpClass() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testUnprotectedPdf() throws Exception {
        File pdf = createUnprotectedPdf();
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile(pdf.getAbsolutePath());

        PdfExtraInfo extraInfo = new PdfExtraInfo(options);
        int pages = extraInfo.getNumberOfPages();
        assertEquals("Unprotected PDF should have 1 page", 1, pages);
    }

    @Test
    public void testOwnerPasswordOnlyPdf_withoutPassword() throws Exception {
        File pdf = createOwnerPasswordOnlyPdf();
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile(pdf.getAbsolutePath());

        PdfExtraInfo extraInfo = new PdfExtraInfo(options);
        // Owner-password-only PDFs can typically be opened without password for reading
        // (user password is empty), so this should succeed
        int pages = extraInfo.getNumberOfPages();
        assertEquals("Owner-password-only PDF should be readable without password", 1, pages);
    }

    @Test
    public void testOwnerPasswordOnlyPdf_withCorrectOwnerPassword() throws Exception {
        File pdf = createOwnerPasswordOnlyPdf();
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile(pdf.getAbsolutePath());
        options.setPdfOwnerPwd(OWNER_PASSWORD.toCharArray());
        options.setAdvanced(true);

        PdfExtraInfo extraInfo = new PdfExtraInfo(options);
        int pages = extraInfo.getNumberOfPages();
        assertEquals(1, pages);
    }

    @Test(expected = BadPasswordException.class)
    public void testBothPasswordsPdf_withoutPassword_throwsBadPasswordException() throws Exception {
        File pdf = createBothPasswordsPdf();
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile(pdf.getAbsolutePath());

        PdfExtraInfo extraInfo = new PdfExtraInfo(options);
        extraInfo.getNumberOfPages();
    }

    @Test
    public void testBothPasswordsPdf_withCorrectOwnerPassword() throws Exception {
        File pdf = createBothPasswordsPdf();
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile(pdf.getAbsolutePath());
        options.setPdfOwnerPwd(OWNER_PASSWORD.toCharArray());
        options.setAdvanced(true);

        PdfExtraInfo extraInfo = new PdfExtraInfo(options);
        int pages = extraInfo.getNumberOfPages();
        assertEquals(1, pages);
    }

    @Test
    public void testBothPasswordsPdf_withUserPasswordAsOwnerPassword() throws Exception {
        // Verify that providing the user password in the owner password field also works,
        // since PdfReader accepts either password for opening
        File pdf = createBothPasswordsPdf();
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile(pdf.getAbsolutePath());
        options.setPdfOwnerPwd(USER_PASSWORD.toCharArray());
        options.setAdvanced(true);

        PdfExtraInfo extraInfo = new PdfExtraInfo(options);
        int pages = extraInfo.getNumberOfPages();
        assertEquals("User password should also be accepted for opening", 1, pages);
    }

    @Test(expected = BadPasswordException.class)
    public void testBothPasswordsPdf_withWrongPassword_throwsBadPasswordException() throws Exception {
        File pdf = createBothPasswordsPdf();
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile(pdf.getAbsolutePath());
        options.setPdfOwnerPwd("wrong-password".toCharArray());
        options.setAdvanced(true);

        PdfExtraInfo extraInfo = new PdfExtraInfo(options);
        extraInfo.getNumberOfPages();
    }

    @Test
    public void testNonExistentFile_returnsMinusOne() throws Exception {
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile("/non/existent/file.pdf");

        PdfExtraInfo extraInfo = new PdfExtraInfo(options);
        int pages = extraInfo.getNumberOfPages();
        assertEquals("Non-existent file should return -1", -1, pages);
    }

    @Test
    public void testCorruptFile_returnsMinusOne() throws Exception {
        File corrupt = new File(tempFolder.getRoot(), "corrupt.pdf");
        java.nio.file.Files.write(corrupt.toPath(), "not a real pdf".getBytes());

        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile(corrupt.getAbsolutePath());

        PdfExtraInfo extraInfo = new PdfExtraInfo(options);
        int pages = extraInfo.getNumberOfPages();
        assertEquals("Corrupt file should return -1", -1, pages);
    }

    @Test
    public void testGetPageInfo_withOwnerPassword() throws Exception {
        File pdf = createBothPasswordsPdf();
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile(pdf.getAbsolutePath());
        options.setPdfOwnerPwd(OWNER_PASSWORD.toCharArray());
        options.setAdvanced(true);

        PdfExtraInfo extraInfo = new PdfExtraInfo(options);
        net.sf.jsignpdf.types.PageInfo pageInfo = extraInfo.getPageInfo(1);
        assertTrue("Page width should be positive", pageInfo.getWidth() > 0);
        assertTrue("Page height should be positive", pageInfo.getHeight() > 0);
    }

    @Test
    public void testAdvancedFlagDisabled_passwordNotUsed() throws Exception {
        // When advanced=false, getPdfOwnerPwdStrX() returns empty string,
        // so the password is effectively ignored
        File pdf = createBothPasswordsPdf();
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile(pdf.getAbsolutePath());
        options.setPdfOwnerPwd(OWNER_PASSWORD.toCharArray());
        options.setAdvanced(false);

        PdfExtraInfo extraInfo = new PdfExtraInfo(options);
        try {
            extraInfo.getNumberOfPages();
            fail("Should throw BadPasswordException when advanced=false hides the password");
        } catch (BadPasswordException e) {
            // expected - password not used because advanced is false
        }
    }

    // --- Helper methods to create test PDFs ---

    private File createUnprotectedPdf() throws Exception {
        File file = new File(tempFolder.getRoot(), "unprotected.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.getDocument().setVersion(1.7f);
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("Unprotected test PDF");
                cs.endText();
            }
            doc.save(file);
        }
        return file;
    }

    private File createOwnerPasswordOnlyPdf() throws Exception {
        File file = new File(tempFolder.getRoot(), "owner-only.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.getDocument().setVersion(1.7f);
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("Owner password only test PDF");
                cs.endText();
            }
            AccessPermission ap = new AccessPermission();
            ap.setCanModify(false);
            StandardProtectionPolicy policy = new StandardProtectionPolicy(OWNER_PASSWORD, "", ap);
            policy.setEncryptionKeyLength(128);
            doc.protect(policy);
            doc.save(file);
        }
        return file;
    }

    private File createBothPasswordsPdf() throws Exception {
        File file = new File(tempFolder.getRoot(), "both-passwords.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.getDocument().setVersion(1.7f);
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("Both passwords test PDF");
                cs.endText();
            }
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    OWNER_PASSWORD, USER_PASSWORD, ap);
            policy.setEncryptionKeyLength(128);
            doc.protect(policy);
            doc.save(file);
        }
        return file;
    }
}
