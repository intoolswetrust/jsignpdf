package net.sf.jsignpdf.fx.control;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

/** Tests for output suffix normalization and filename generation. */
public class OutputSuffixSupportTest {

    @Test
    public void preservesUserCaseAndKeepsSignedAsEmptyDefault() {
        File input = new File("drawing.pdf");

        OutputSuffixSupport.setUserValue("eM_dl");
        assertEquals("drawing_eM_dl.pdf",
                new File(OutputSuffixSupport.suggestedFor(input)).getName());

        OutputSuffixSupport.setUserValue("Customer Review");
        assertEquals("drawing_Customer_Review.pdf",
                new File(OutputSuffixSupport.suggestedFor(input)).getName());

        OutputSuffixSupport.setUserValue("");
        assertEquals("drawing_signed.pdf",
                new File(OutputSuffixSupport.suggestedFor(input)).getName());
    }
}
