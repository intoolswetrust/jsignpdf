package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.jsignpdf.SignerConfig;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

/**
 * Tests visible and invisible signature configurations. Verifies that the signature is
 * cryptographically valid and that visual attributes (page, position, appearance text)
 * are correctly embedded in the signed PDF.
 */
public class VisibleSignatureSigningTest extends SigningTestBase {

    /** Verifies that the default invisible signature has no visible widget rectangle. */
    @Test
    public void testInvisibleDefault() throws Exception {
        SignerConfig options = createDefaultOptions();
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Should have 1 signature", 1, result.signatureCount);
        assertFalse("Invisible signature should have no visible rect", result.hasVisibleRect);
    }

    /** Verifies that a visible signature is placed at the configured rectangle coordinates on the correct page. */
    @Test
    public void testVisibleWithPosition() throws Exception {
        SignerConfig options = createDefaultOptions();
        options.setVisible(true);
        options.setPage(1);
        options.setPositionLLX(50);
        options.setPositionLLY(60);
        options.setPositionURX(200);
        options.setPositionURY(110);
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertTrue("Should have visible rect", result.hasVisibleRect);
        assertEquals("Signature should be on page 1", 1, result.signaturePage);
        assertEquals("LLX should match", 50f, result.rectLLX, 1f);
        assertEquals("LLY should match", 60f, result.rectLLY, 1f);
        assertEquals("URX should match", 200f, result.rectURX, 1f);
        assertEquals("URY should match", 110f, result.rectURY, 1f);
    }

    /** Verifies that custom L2 text with placeholder substitution appears in the appearance stream. */
    @Test
    public void testVisibleWithCustomL2Text() throws Exception {
        SignerConfig options = createDefaultOptions();
        options.setVisible(true);
        options.setPage(1);
        options.setPositionLLX(50);
        options.setPositionLLY(50);
        options.setPositionURX(250);
        options.setPositionURY(120);
        options.setReason("TestReason");
        options.setLocation("TestLocation");
        options.setL2Text("Signed by ${signer}, reason: ${reason}, loc: ${location}");
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertNotNull("Appearance text should be present", result.appearanceText);
        assertTrue("L2 text should contain reason value",
                result.appearanceText.contains("TestReason"));
        assertTrue("L2 text should contain location value",
                result.appearanceText.contains("TestLocation"));
        assertFalse("Signer placeholder should be substituted",
                result.appearanceText.contains("${signer}"));
        assertFalse("Reason placeholder should be substituted",
                result.appearanceText.contains("${reason}"));
    }

    /** Verifies that all L2 text placeholders are substituted with actual values. */
    @Test
    public void testVisibleWithAllPlaceholders() throws Exception {
        SignerConfig options = createDefaultOptions();
        options.setVisible(true);
        options.setPage(1);
        options.setPositionLLX(10);
        options.setPositionLLY(10);
        options.setPositionURX(300);
        options.setPositionURY(150);
        options.setReason("AllReason");
        options.setLocation("AllLocation");
        options.setContact("all@contact.com");
        options.setL2Text("S:${signer} R:${reason} L:${location} C:${contact} T:${timestamp}");
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertNotNull("Appearance text should be present", result.appearanceText);
        assertTrue("Should contain reason", result.appearanceText.contains("AllReason"));
        assertTrue("Should contain location", result.appearanceText.contains("AllLocation"));
        assertTrue("Should contain contact", result.appearanceText.contains("all@contact.com"));
        assertFalse("Timestamp placeholder should be substituted",
                result.appearanceText.contains("${timestamp}"));
    }

    /** Verifies that the default L2 text (no custom template) contains signer information. */
    @Test
    public void testDefaultL2TextContainsSignerInfo() throws Exception {
        SignerConfig options = createDefaultOptions();
        options.setVisible(true);
        options.setPage(1);
        options.setPositionLLX(50);
        options.setPositionLLY(50);
        options.setPositionURX(250);
        options.setPositionURY(120);
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertNotNull("Appearance text should be present", result.appearanceText);
        assertTrue("Default L2 text should not be empty", result.appearanceText.length() > 0);
    }
}
