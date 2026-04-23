package net.sf.jsignpdf.fx.preset;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.Test;

public class PresetValidationTest {

    private static final Predicate<String> NOT_DUPLICATE = n -> false;

    @Test
    public void accepts_plainName() {
        assertEquals(PresetValidation.Result.OK, PresetValidation.validate("My preset", NOT_DUPLICATE));
    }

    @Test
    public void accepts_unicodeAndDiacritics() {
        assertEquals(PresetValidation.Result.OK, PresetValidation.validate("TSA — přítomný", NOT_DUPLICATE));
    }

    @Test
    public void trims_whitespace() {
        assertEquals(PresetValidation.Result.OK, PresetValidation.validate("   hello   ", NOT_DUPLICATE));
    }

    @Test
    public void rejects_empty() {
        assertEquals(PresetValidation.Result.EMPTY, PresetValidation.validate("", NOT_DUPLICATE));
    }

    @Test
    public void rejects_whitespaceOnly() {
        assertEquals(PresetValidation.Result.EMPTY, PresetValidation.validate("   \t  ", NOT_DUPLICATE));
    }

    @Test
    public void rejects_null() {
        assertEquals(PresetValidation.Result.EMPTY, PresetValidation.validate(null, NOT_DUPLICATE));
    }

    @Test
    public void rejects_controlChar_tab() {
        assertEquals(PresetValidation.Result.ILLEGAL_CHAR, PresetValidation.validate("a\tb", NOT_DUPLICATE));
    }

    @Test
    public void rejects_controlChar_del() {
        assertEquals(PresetValidation.Result.ILLEGAL_CHAR, PresetValidation.validate("ab", NOT_DUPLICATE));
    }

    @Test
    public void rejects_tooLong() {
        String name = "a".repeat(PresetValidation.MAX_DISPLAY_NAME_LENGTH + 1);
        assertEquals(PresetValidation.Result.TOO_LONG, PresetValidation.validate(name, NOT_DUPLICATE));
    }

    @Test
    public void accepts_atExactMaxLength() {
        String name = "a".repeat(PresetValidation.MAX_DISPLAY_NAME_LENGTH);
        assertEquals(PresetValidation.Result.OK, PresetValidation.validate(name, NOT_DUPLICATE));
    }

    @Test
    public void rejects_duplicate_caseInsensitive() {
        Set<String> existing = new HashSet<>();
        existing.add("Work preset");
        assertEquals(PresetValidation.Result.DUPLICATE,
                PresetValidation.validate("WORK PRESET", n -> existing.stream().anyMatch(e -> e.equalsIgnoreCase(n))));
    }

    @Test
    public void trim_returnsTrimmed() {
        assertEquals("hi", PresetValidation.trim("  hi  "));
    }

    @Test
    public void trim_nullBecomesEmpty() {
        assertEquals("", PresetValidation.trim(null));
    }
}
