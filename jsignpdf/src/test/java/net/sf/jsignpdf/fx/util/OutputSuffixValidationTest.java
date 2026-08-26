package net.sf.jsignpdf.fx.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.sf.jsignpdf.fx.util.OutputSuffixValidation.Result;

public class OutputSuffixValidationTest {

    @Test
    public void acceptsArbitraryText() {
        assertTrue(OutputSuffixValidation.isValid("_signed"));
        assertTrue(OutputSuffixValidation.isValid("-DL"));
        assertTrue(OutputSuffixValidation.isValid("EM"));
        assertTrue(OutputSuffixValidation.isValid("_podepsáno"));
    }

    @Test
    public void unsetAndEmptyAreValid() {
        assertTrue(OutputSuffixValidation.isValid(null));
        assertTrue(OutputSuffixValidation.isValid(""));
    }

    @Test
    public void rejectsPathSeparators() {
        assertEquals(Result.ILLEGAL_CHAR, OutputSuffixValidation.validate("a/b"));
        assertEquals(Result.ILLEGAL_CHAR, OutputSuffixValidation.validate("a\\b"));
    }

    @Test
    public void rejectsWindowsIllegalCharacters() {
        for (String s : new String[] { "a:b", "a*b", "a?b", "a\"b", "a<b", "a>b", "a|b" }) {
            assertEquals(s, Result.ILLEGAL_CHAR, OutputSuffixValidation.validate(s));
        }
    }

    @Test
    public void rejectsControlCharacters() {
        assertEquals(Result.ILLEGAL_CHAR, OutputSuffixValidation.validate("a\nb"));
    }

    @Test
    public void rejectsSurroundingWhitespace() {
        assertEquals(Result.SURROUNDING_WHITESPACE, OutputSuffixValidation.validate(" _signed"));
        assertEquals(Result.SURROUNDING_WHITESPACE, OutputSuffixValidation.validate("_signed "));
    }

    @Test
    public void rejectsOverlyLongValues() {
        assertEquals(Result.TOO_LONG,
                OutputSuffixValidation.validate("x".repeat(OutputSuffixValidation.MAX_LENGTH + 1)));
    }
}
