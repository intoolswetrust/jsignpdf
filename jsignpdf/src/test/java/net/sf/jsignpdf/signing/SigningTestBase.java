package net.sf.jsignpdf.signing;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.Security;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.SignerLogic;
import net.sf.jsignpdf.TestConstants.Keystore;
import net.sf.jsignpdf.TestConstants.TestPrivateKey;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

public abstract class SigningTestBase {

    private static File unsignedPdf;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpClass() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        unsignedPdf = File.createTempFile("unsigned-", ".pdf");
        unsignedPdf.deleteOnExit();
        PDDocument doc = new PDDocument();
        doc.getDocument().setVersion(1.7f);
        PDPage page = new PDPage();
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 12);
        cs.newLineAtOffset(100, 700);
        cs.showText("Test PDF for signing");
        cs.endText();
        cs.close();
        doc.save(unsignedPdf);
        doc.close();
    }

    protected BasicSignerOptions createOptions(TestPrivateKey key, Keystore keystore) throws Exception {
        File inFile = new File(tempFolder.getRoot(), "input.pdf");
        Files.copy(unsignedPdf.toPath(), inFile.toPath());
        File outFile = new File(tempFolder.getRoot(), "output.pdf");

        BasicSignerOptions options = key.toSignerOptions(keystore);
        options.setInFile(inFile.getAbsolutePath());
        options.setOutFile(outFile.getAbsolutePath());
        return options;
    }

    protected BasicSignerOptions createDefaultOptions() throws Exception {
        return createOptions(TestPrivateKey.RSA2048, Keystore.JKS);
    }

    protected ValidationResult signAndValidate(BasicSignerOptions options) throws Exception {
        boolean result = new SignerLogic(options).signFile();
        assertTrue("Signing should succeed", result);
        File outFile = new File(options.getOutFileX());
        assertTrue("Output file should exist", outFile.exists());
        return PdfSignatureValidator.validate(outFile);
    }

    protected File getUnsignedPdf() {
        return unsignedPdf;
    }
}
