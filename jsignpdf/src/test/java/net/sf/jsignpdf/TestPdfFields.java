package net.sf.jsignpdf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

/**
 * Builds PDFs with pre-placed signature form fields - the documents a form author hands to the signers. The
 * fixtures are generated rather than committed so the field names, pages and rectangles stay readable next to
 * the assertions, and so the AcroForm {@code /Fields} order can deliberately be made to differ from the page
 * order (which is what document-order numbering has to survive).
 */
public final class TestPdfFields {

    private TestPdfFields() {
    }

    /**
     * A signature field to place. {@code page} is 1-based; a zero-size rectangle is what a form author uses for
     * an invisible signature.
     */
    public record FieldSpec(String name, int page, PDRectangle rect, boolean signed, boolean hidden) {

        public static FieldSpec blank(String name, int page, float llx, float lly, float urx, float ury) {
            return new FieldSpec(name, page, new PDRectangle(llx, lly, urx - llx, ury - lly), false, false);
        }

        public static FieldSpec signed(String name, int page, float llx, float lly, float urx, float ury) {
            return new FieldSpec(name, page, new PDRectangle(llx, lly, urx - llx, ury - lly), true, false);
        }

        public static FieldSpec zeroSize(String name, int page) {
            return new FieldSpec(name, page, new PDRectangle(0, 0, 0, 0), false, false);
        }

        public FieldSpec asHidden() {
            return new FieldSpec(name, page, rect, signed, true);
        }
    }

    /**
     * Writes a PDF with the given number of pages and signature fields. The fields are added to
     * {@code /Fields} in the order given, independently of the pages their widgets sit on.
     *
     * @param target file to write
     * @param pageCount number of pages
     * @param fields the fields to place
     * @return the written file
     */
    public static File create(File target, int pageCount, List<FieldSpec> fields) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            doc.getDocument().setVersion(1.7f);
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.newLineAtOffset(100, 780);
                    cs.showText("Page " + (i + 1));
                    cs.endText();
                }
            }

            PDAcroForm acroForm = new PDAcroForm(doc);
            doc.getDocumentCatalog().setAcroForm(acroForm);
            List<PDField> acroFields = new ArrayList<>();
            for (FieldSpec spec : fields) {
                PDSignatureField field = new PDSignatureField(acroForm);
                field.setPartialName(spec.name());

                PDAnnotationWidget widget = field.getWidgets().get(0);
                widget.setRectangle(spec.rect());
                widget.setHidden(spec.hidden());
                PDPage page = doc.getPage(spec.page() - 1);
                widget.setPage(page);
                page.getAnnotations().add(widget);

                if (spec.signed()) {
                    // A /V entry is what makes a field "already signed" for both engines and for the listing.
                    // The fixture only needs the entry, not a real CMS blob.
                    PDSignature signature = new PDSignature();
                    signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
                    signature.setSubFilter(COSName.getPDFName("adbe.pkcs7.detached"));
                    field.getCOSObject().setItem(COSName.V, signature);
                }
                acroFields.add(field);
            }
            acroForm.setFields(acroFields);
            doc.save(target);
        }
        return target;
    }
}
