package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

/**
 * Tests visible and invisible signature configurations. Verifies that the signature is
 * cryptographically valid regardless of its visual appearance settings.
 */
public class VisibleSignatureSigningTest extends SigningTestBase {

    /** Verifies that the default invisible signature is valid. */
    @Test
    public void testInvisibleDefault() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Should have 1 signature", 1, result.signatureCount);
    }

    /** Verifies that a visible signature with explicit rectangle coordinates is valid. */
    @Test
    public void testVisibleWithPosition() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setVisible(true);
        options.setPage(1);
        options.setPositionLLX(50);
        options.setPositionLLY(50);
        options.setPositionURX(200);
        options.setPositionURY(100);
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Should have 1 signature", 1, result.signatureCount);
    }

    /** Verifies that a visible signature with custom Layer 2 text (using placeholder substitution) is valid. */
    @Test
    public void testVisibleWithCustomL2Text() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setVisible(true);
        options.setPage(1);
        options.setPositionLLX(50);
        options.setPositionLLY(50);
        options.setPositionURX(250);
        options.setPositionURY(120);
        options.setL2Text("Custom Layer 2 Text: Signed by ${signer}");
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Should have 1 signature", 1, result.signatureCount);
    }
}
