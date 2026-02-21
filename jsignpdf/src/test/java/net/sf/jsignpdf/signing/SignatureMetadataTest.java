package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

public class SignatureMetadataTest extends SigningTestBase {

    @Test
    public void testReason() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setReason("Test signing reason");
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Reason should match", "Test signing reason", result.reason);
    }

    @Test
    public void testLocation() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setLocation("Test Location City");
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Location should match", "Test Location City", result.location);
    }

    @Test
    public void testContact() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setContact("test@example.com");
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Contact should match", "test@example.com", result.contactInfo);
    }

    @Test
    public void testAllMetadata() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setReason("Combined reason");
        options.setLocation("Combined location");
        options.setContact("combined@example.com");
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Reason should match", "Combined reason", result.reason);
        assertEquals("Location should match", "Combined location", result.location);
        assertEquals("Contact should match", "combined@example.com", result.contactInfo);
    }

    @Test
    public void testEmptyMetadata() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertNull("Reason should be null", result.reason);
        assertNull("Location should be null", result.location);
        assertNull("Contact should be null", result.contactInfo);
    }

    @Test
    public void testSignDate() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertNotNull("Sign date should be present", result.signDate);
    }
}
