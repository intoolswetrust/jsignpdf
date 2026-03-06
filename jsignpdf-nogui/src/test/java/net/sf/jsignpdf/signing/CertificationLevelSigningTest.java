package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.jsignpdf.SignerConfig;
import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

/**
 * Tests that all {@link CertificationLevel} values produce valid signatures.
 */
public class CertificationLevelSigningTest extends SigningTestBase {

    /** Signs with {@link CertificationLevel#NOT_CERTIFIED} (default, approval signature). */
    @Test
    public void testNotCertified() throws Exception {
        assertCertificationLevel(CertificationLevel.NOT_CERTIFIED);
    }

    /** Signs with certification that disallows any subsequent changes. */
    @Test
    public void testCertifiedNoChanges() throws Exception {
        assertCertificationLevel(CertificationLevel.CERTIFIED_NO_CHANGES_ALLOWED);
    }

    /** Signs with certification that allows form filling only. */
    @Test
    public void testCertifiedFormFilling() throws Exception {
        assertCertificationLevel(CertificationLevel.CERTIFIED_FORM_FILLING);
    }

    /** Signs with certification that allows form filling and annotations. */
    @Test
    public void testCertifiedFormFillingAndAnnotations() throws Exception {
        assertCertificationLevel(CertificationLevel.CERTIFIED_FORM_FILLING_AND_ANNOTATIONS);
    }

    private void assertCertificationLevel(CertificationLevel level) throws Exception {
        SignerConfig options = createDefaultOptions();
        options.setCertLevel(level);
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid for " + level, result.signatureValid);
        assertEquals("Should have 1 signature", 1, result.signatureCount);

        if (level == CertificationLevel.NOT_CERTIFIED) {
            assertFalse("NOT_CERTIFIED should not have DocMDP", result.isCertified);
        } else {
            assertTrue(level + " should have DocMDP", result.isCertified);
            assertEquals("DocMDP permission should match for " + level,
                    level.getLevel(), result.docMdpPermission);
        }
    }
}
