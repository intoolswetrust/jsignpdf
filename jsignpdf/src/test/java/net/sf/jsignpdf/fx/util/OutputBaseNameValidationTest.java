package net.sf.jsignpdf.fx.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.sf.jsignpdf.fx.util.OutputBaseNameValidation.Result;

/**
 * Covers the output file-name validation, whose job is to keep the typed name a single path component so it cannot move
 * the write outside the chosen output directory — the guard that matters most under the sandbox directory grant.
 */
public class OutputBaseNameValidationTest {

    @Test
    public void blankIsValid() {
        assertTrue(OutputBaseNameValidation.isValid(null));
        assertTrue(OutputBaseNameValidation.isValid(""));
    }

    @Test
    public void plainNameIsValid() {
        assertTrue(OutputBaseNameValidation.isValid("drawing_signed.pdf"));
    }

    @Test
    public void separatorsAreRejected() {
        assertEquals(Result.ILLEGAL_CHAR, OutputBaseNameValidation.validate("../secret.pdf"));
        assertEquals(Result.ILLEGAL_CHAR, OutputBaseNameValidation.validate("/etc/passwd"));
        assertEquals(Result.ILLEGAL_CHAR, OutputBaseNameValidation.validate("sub\\dir.pdf"));
    }

    @Test
    public void traversalAndHomeExpansionAreRejected() {
        assertEquals(Result.PATH_TRAVERSAL, OutputBaseNameValidation.validate(".."));
        assertEquals(Result.PATH_TRAVERSAL, OutputBaseNameValidation.validate("."));
        assertEquals(Result.PATH_TRAVERSAL, OutputBaseNameValidation.validate("~"));
        assertEquals(Result.PATH_TRAVERSAL, OutputBaseNameValidation.validate("~root.pdf"));
    }

    @Test
    public void surroundingWhitespaceIsRejected() {
        assertEquals(Result.SURROUNDING_WHITESPACE, OutputBaseNameValidation.validate(" out.pdf"));
    }

    @Test
    public void overlongNameIsRejected() {
        assertFalse(OutputBaseNameValidation.isValid("a".repeat(OutputBaseNameValidation.MAX_LENGTH + 1)));
    }
}
