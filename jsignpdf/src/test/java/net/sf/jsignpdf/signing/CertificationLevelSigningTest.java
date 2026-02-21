package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

public class CertificationLevelSigningTest extends SigningTestBase {

    @Test
    public void testNotCertified() throws Exception {
        assertCertificationLevel(CertificationLevel.NOT_CERTIFIED);
    }

    @Test
    public void testCertifiedNoChanges() throws Exception {
        assertCertificationLevel(CertificationLevel.CERTIFIED_NO_CHANGES_ALLOWED);
    }

    @Test
    public void testCertifiedFormFilling() throws Exception {
        assertCertificationLevel(CertificationLevel.CERTIFIED_FORM_FILLING);
    }

    @Test
    public void testCertifiedFormFillingAndAnnotations() throws Exception {
        assertCertificationLevel(CertificationLevel.CERTIFIED_FORM_FILLING_AND_ANNOTATIONS);
    }

    private void assertCertificationLevel(CertificationLevel level) throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setCertLevel(level);
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid for " + level, result.signatureValid);
        assertEquals("Should have 1 signature", 1, result.signatureCount);
    }
}
