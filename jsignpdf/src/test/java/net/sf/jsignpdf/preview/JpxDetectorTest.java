package net.sf.jsignpdf.preview;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.OutputStream;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link JpxDetector}: structural {@code /JPXDecode} detection without decoding.
 */
public class JpxDetectorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void detects_directJpxImage() throws Exception {
        File pdf = buildPdf("simpleFilter", COSName.getPDFName("JPXDecode"));
        assertTrue(JpxDetector.containsJpx(pdf, ""));
    }

    @Test
    public void detects_jpxInFilterArray() throws Exception {
        COSArray filters = new COSArray();
        filters.add(COSName.getPDFName("JPXDecode"));
        File pdf = buildPdf("arrayFilter", filters);
        assertTrue(JpxDetector.containsJpx(pdf, ""));
    }

    @Test
    public void detects_jpxInsideNestedFormXObject() throws Exception {
        File pdf = buildNestedFormPdf();
        assertTrue(JpxDetector.containsJpx(pdf, ""));
    }

    @Test
    public void ignores_nonJpxImage() throws Exception {
        File pdf = buildPdf("flate", COSName.FLATE_DECODE);
        assertFalse(JpxDetector.containsJpx(pdf, ""));
    }

    @Test
    public void ignores_pageWithoutImages() throws Exception {
        File pdf = tmp.newFile("blank.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(pdf);
        }
        assertFalse(JpxDetector.containsJpx(pdf, ""));
    }

    @Test
    public void returnsFalse_forUnreadableFile_withoutThrowing() {
        File missing = new File(tmp.getRoot(), "does-not-exist.pdf");
        assertFalse(JpxDetector.containsJpx(missing, ""));
    }

    /** Builds a one-page PDF whose single image XObject uses the given {@code /Filter} value. */
    private File buildPdf(String name, org.apache.pdfbox.cos.COSBase filter) throws Exception {
        File pdf = tmp.newFile(name + ".pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            COSDictionary xobjects = new COSDictionary();
            xobjects.setItem(COSName.getPDFName("Im0"), newImageStream(doc, filter));
            COSDictionary resources = new COSDictionary();
            resources.setItem(COSName.XOBJECT, xobjects);
            page.getCOSObject().setItem(COSName.RESOURCES, resources);
            doc.save(pdf);
        }
        return pdf;
    }

    /** Builds a PDF where the JPX image lives inside a Form XObject's own resources. */
    private File buildNestedFormPdf() throws Exception {
        File pdf = tmp.newFile("nested.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            COSStream image = newImageStream(doc, COSName.getPDFName("JPXDecode"));
            COSDictionary formXObjects = new COSDictionary();
            formXObjects.setItem(COSName.getPDFName("Im0"), image);
            COSDictionary formResources = new COSDictionary();
            formResources.setItem(COSName.XOBJECT, formXObjects);

            COSStream form = doc.getDocument().createCOSStream();
            form.setItem(COSName.TYPE, COSName.XOBJECT);
            form.setItem(COSName.SUBTYPE, COSName.FORM);
            form.setItem(COSName.RESOURCES, formResources);
            try (OutputStream os = form.createOutputStream()) {
                os.write(new byte[] { 0 });
            }

            COSDictionary pageXObjects = new COSDictionary();
            pageXObjects.setItem(COSName.getPDFName("Fm0"), form);
            COSDictionary pageResources = new COSDictionary();
            pageResources.setItem(COSName.XOBJECT, pageXObjects);
            page.getCOSObject().setItem(COSName.RESOURCES, pageResources);
            doc.save(pdf);
        }
        return pdf;
    }

    private static COSStream newImageStream(PDDocument doc, org.apache.pdfbox.cos.COSBase filter) throws Exception {
        COSStream image = doc.getDocument().createCOSStream();
        image.setItem(COSName.TYPE, COSName.XOBJECT);
        image.setItem(COSName.SUBTYPE, COSName.IMAGE);
        image.setInt(COSName.WIDTH, 1);
        image.setInt(COSName.HEIGHT, 1);
        image.setItem(COSName.FILTER, filter);
        try (OutputStream os = image.createRawOutputStream()) {
            os.write(new byte[] { 0, 0, 0 });
        }
        return image;
    }
}
