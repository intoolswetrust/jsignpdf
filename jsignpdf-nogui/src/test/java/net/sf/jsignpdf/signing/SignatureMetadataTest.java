package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.jsignpdf.SignerConfig;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

/**
 * Tests that signature metadata (reason, location, contact, sign date) is correctly
 * embedded in the PDF signature dictionary and can be read back from the signed PDF.
 */
public class SignatureMetadataTest extends SigningTestBase {

    /** Verifies that the reason field is stored in the signature dictionary. */
    @Test
    public void testReason() throws Exception {
        SignerConfig options = createDefaultOptions();
        options.setReason("Test signing reason");
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Reason should match", "Test signing reason", result.reason);
    }

    /** Verifies that the location field is stored in the signature dictionary. */
    @Test
    public void testLocation() throws Exception {
        SignerConfig options = createDefaultOptions();
        options.setLocation("Test Location City");
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Location should match", "Test Location City", result.location);
    }

    /** Verifies that the contact field is stored in the signature dictionary. */
    @Test
    public void testContact() throws Exception {
        SignerConfig options = createDefaultOptions();
        options.setContact("test@example.com");
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Contact should match", "test@example.com", result.contactInfo);
    }

    /** Verifies that reason, location, and contact can all be set together. */
    @Test
    public void testAllMetadata() throws Exception {
        SignerConfig options = createDefaultOptions();
        options.setReason("Combined reason");
        options.setLocation("Combined location");
        options.setContact("combined@example.com");
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Reason should match", "Combined reason", result.reason);
        assertEquals("Location should match", "Combined location", result.location);
        assertEquals("Contact should match", "combined@example.com", result.contactInfo);
    }

    /** Verifies that metadata fields are absent when not configured. */
    @Test
    public void testEmptyMetadata() throws Exception {
        SignerConfig options = createDefaultOptions();
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertNull("Reason should be null", result.reason);
        assertNull("Location should be null", result.location);
        assertNull("Contact should be null", result.contactInfo);
    }

    /** Verifies that the signing date is present in the signature dictionary. */
    @Test
    public void testSignDate() throws Exception {
        SignerConfig options = createDefaultOptions();
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertNotNull("Sign date should be present", result.signDate);
    }
}
