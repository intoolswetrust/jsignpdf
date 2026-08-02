package net.sf.jsignpdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import net.sf.jsignpdf.TestPdfFields.FieldSpec;
import net.sf.jsignpdf.types.SignatureFieldInfo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests the signature field inventory of {@link PdfExtraInfo} and the resolution of the {@code --sig-field}
 * selectors: document-order numbering, {@code #N} / {@code auto}, and the precedence of an exact field name.
 */
public class SignatureFieldResolutionTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * The fields must come out in document order (page, then position in the page's /Annots) even though the
     * AcroForm /Fields array lists them the other way round - neither OpenPDF's blank-signature listing (a
     * HashMap iteration) nor the /Fields order gives that for free.
     */
    @Test
    public void fieldsAreNumberedInDocumentOrder() throws Exception {
        File pdf = createPdf(3,
                FieldSpec.blank("OnPageThree", 3, 70, 500, 300, 560),
                FieldSpec.blank("OnPageOne", 1, 70, 700, 300, 760),
                FieldSpec.blank("AlsoOnPageOne", 1, 70, 600, 300, 660));

        List<SignatureFieldInfo> fields = extraInfo(pdf).getSignatureFields();

        assertEquals(3, fields.size());
        assertEquals("OnPageOne", fields.get(0).name());
        assertEquals(1, fields.get(0).number());
        assertEquals(1, fields.get(0).page());
        assertEquals("AlsoOnPageOne", fields.get(1).name());
        assertEquals(2, fields.get(1).number());
        assertEquals("OnPageThree", fields.get(2).name());
        assertEquals(3, fields.get(2).number());
        assertEquals(3, fields.get(2).page());
    }

    @Test
    public void rectangleAndFlagsAreReported() throws Exception {
        File pdf = createPdf(1, FieldSpec.blank("Visible", 1, 70, 700, 300, 760),
                FieldSpec.zeroSize("Invisible", 1), FieldSpec.blank("Hidden", 1, 70, 100, 300, 160).asHidden());

        List<SignatureFieldInfo> fields = extraInfo(pdf).getSignatureFields();

        SignatureFieldInfo visible = byName(fields, "Visible");
        assertEquals(70f, visible.llx(), 0.01f);
        assertEquals(700f, visible.lly(), 0.01f);
        assertEquals(300f, visible.urx(), 0.01f);
        assertEquals(760f, visible.ury(), 0.01f);
        assertTrue(visible.hasVisibleRect());
        assertFalse(visible.hidden());

        assertFalse("a zero-size rect means an invisible signature", byName(fields, "Invisible").hasVisibleRect());
        assertTrue(byName(fields, "Hidden").hidden());
    }

    @Test
    public void signedFieldsAreListedButNotSignable() throws Exception {
        File pdf = createPdf(1, FieldSpec.signed("First", 1, 70, 700, 300, 760),
                FieldSpec.blank("Second", 1, 70, 600, 300, 660));

        List<SignatureFieldInfo> fields = extraInfo(pdf).getSignatureFields();
        assertEquals("signed fields are numbered too, so numbers don't shift", 2, fields.size());
        assertTrue(fields.get(0).signed());
        assertFalse(fields.get(0).blank());

        assertEquals("Second", resolve(pdf, "auto").name());
        assertResolutionFails(pdf, "First");
        assertResolutionFails(pdf, "#1");
    }

    @Test
    public void resolvesByNameNumberAndAuto() throws Exception {
        File pdf = createPdf(2, FieldSpec.blank("Signature1", 1, 70, 700, 300, 760),
                FieldSpec.blank("Signature2", 2, 70, 700, 300, 760));

        assertEquals("Signature2", resolve(pdf, "Signature2").name());
        assertEquals("Signature2", resolve(pdf, "#2").name());
        assertEquals("Signature1", resolve(pdf, "auto").name());
        assertEquals(2, resolve(pdf, "#2").page());
    }

    /** An exact field name always wins, so a field really named "auto" or "#1" stays reachable. */
    @Test
    public void exactNameWinsOverSelector() throws Exception {
        File pdf = createPdf(1, FieldSpec.blank("Signature1", 1, 70, 700, 300, 760),
                FieldSpec.blank("auto", 1, 70, 600, 300, 660), FieldSpec.blank("#1", 1, 70, 500, 300, 560));

        assertEquals("auto", resolve(pdf, "auto").name());
        assertEquals("#1", resolve(pdf, "#1").name());

        // Without such a field, the same spellings are selectors again: #1 is the first field in document order.
        File plain = createPdf(1, FieldSpec.blank("Signature1", 1, 70, 700, 300, 760),
                FieldSpec.blank("Signature2", 1, 70, 600, 300, 660));
        assertEquals("Signature1", resolve(plain, "#1").name());
        assertEquals("Signature1", resolve(plain, "auto").name());
    }

    @Test
    public void unknownNameAndOutOfRangeNumberAreRejected() throws Exception {
        File pdf = createPdf(1, FieldSpec.blank("Signature1", 1, 70, 700, 300, 760));

        assertResolutionFails(pdf, "Nope");
        assertResolutionFails(pdf, "#2");
        assertResolutionFails(pdf, "#0");
        assertResolutionFails(pdf, "#x");
    }

    @Test
    public void documentWithoutSignatureFields() throws Exception {
        File pdf = createPdf(1);

        assertTrue(extraInfo(pdf).getSignatureFields().isEmpty());
        assertResolutionFails(pdf, "auto");
        assertResolutionFails(pdf, "Signature1");
    }

    @Test
    public void allFieldsSignedLeavesAutoNothingToFill() throws Exception {
        File pdf = createPdf(1, FieldSpec.signed("Signature1", 1, 70, 700, 300, 760));

        assertResolutionFails(pdf, "auto");
    }

    private File createPdf(int pageCount, FieldSpec... fields) throws Exception {
        File pdf = new File(tempFolder.getRoot(), "fields-" + System.nanoTime() + ".pdf");
        return TestPdfFields.create(pdf, pageCount, List.of(fields));
    }

    private PdfExtraInfo extraInfo(File pdf) {
        BasicSignerOptions options = new BasicSignerOptions();
        options.setInFile(pdf.getAbsolutePath());
        return new PdfExtraInfo(options);
    }

    private SignatureFieldInfo resolve(File pdf, String selector) throws Exception {
        SignatureFieldInfo field = extraInfo(pdf).resolveSignatureField(selector);
        assertNotNull(field);
        return field;
    }

    private void assertResolutionFails(File pdf, String selector) throws Exception {
        try {
            SignatureFieldInfo resolved = extraInfo(pdf).resolveSignatureField(selector);
            fail("Selector '" + selector + "' should not resolve, but gave " + resolved.name());
        } catch (SignatureFieldException expected) {
            assertNotNull("the message is shown to the user as-is", expected.getMessage());
        }
    }

    private static SignatureFieldInfo byName(List<SignatureFieldInfo> fields, String name) {
        return fields.stream().filter(f -> f.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("No field named " + name));
    }
}
