package net.sf.jsignpdf.engine.dss.pdfbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.europa.esig.dss.enumerations.TextWrapping;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.pades.DSSJavaFont;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pdf.pdfbox.visible.AbstractPdfBoxSignatureDrawer;
import eu.europa.esig.dss.pdf.visible.SignatureFieldDimensionAndPosition;

/**
 * Unit tests for the drawers behind {@link JSignPdfSignatureDrawerFactory}. Lives in the drawer's own
 * package so the protected DSS extension points can be exercised directly.
 */
public class JSignPdfSignatureDrawerTest {

    private static final int PREFERRED_FONT_SIZE = 8;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /**
     * A background-only signature must derive the colour space from the background image. Without it the
     * fallback lands on DEVICEGRAY and DSS registers a grayscale output intent for a document that
     * actually carries an RGB image.
     */
    @Test
    public void backgroundImageDrivesColorSpaceWhenThereIsNoForegroundImage() throws Exception {
        JSignPdfSignatureImageParameters params = baseParameters();
        params.setBackgroundImage(new FileDocument(rgbImage()));
        params.getTextParameters().setText("");

        JSignPdfOverlaySignatureDrawer drawer = new JSignPdfOverlaySignatureDrawer();
        try (PDDocument doc = singlePageDocument(); SignatureOptions options = new SignatureOptions()) {
            drawer.init(params, doc, options);
            assertEquals(COSName.DEVICERGB.getName(), drawer.getExpectedColorSpaceName());
        }
    }

    /**
     * {@code init()} must keep DSS's {@code checkColorSpace()} step; dropping it silently disables the
     * output intent registration for every signature drawn with a background image.
     */
    @Test
    public void initRegistersOutputIntentForBackgroundOnlySignature() throws Exception {
        JSignPdfSignatureImageParameters params = baseParameters();
        params.setBackgroundImage(new FileDocument(rgbImage()));
        params.getTextParameters().setText("");

        JSignPdfOverlaySignatureDrawer drawer = new JSignPdfOverlaySignatureDrawer();
        try (PDDocument doc = singlePageDocument(); SignatureOptions options = new SignatureOptions()) {
            drawer.init(params, doc, options);
            List<PDOutputIntent> intents = doc.getDocumentCatalog().getOutputIntents();
            assertEquals("an output intent must be registered for the background image", 1, intents.size());
            assertEquals("sRGB", intents.get(0).getOutputConditionIdentifier());
        }
    }

    /**
     * The font size is an upper bound on both drawer paths, not only when a background image happens to
     * be configured. FILL_BOX_AND_LINEBREAK on a 400x300 box would otherwise scale the text far above it.
     */
    @Test
    public void textSizeIsCappedOnBothDrawerPaths() throws Exception {
        assertTextSizeCapped(baseParameters());

        JSignPdfSignatureImageParameters withBackground = baseParameters();
        withBackground.setBackgroundImage(new FileDocument(rgbImage()));
        assertTextSizeCapped(withBackground);
    }

    private void assertTextSizeCapped(JSignPdfSignatureImageParameters params) throws Exception {
        AbstractPdfBoxSignatureDrawer drawer =
                (AbstractPdfBoxSignatureDrawer) new JSignPdfSignatureDrawerFactory().getSignatureDrawer(params);
        try (PDDocument doc = singlePageDocument(); SignatureOptions options = new SignatureOptions()) {
            drawer.init(params, doc, options);
            SignatureFieldDimensionAndPosition dim = drawer.buildSignatureFieldBox();
            assertNotNull(dim);
            assertTrue("text size " + dim.getTextSize() + " must not exceed the configured " + PREFERRED_FONT_SIZE,
                    dim.getTextSize() <= PREFERRED_FONT_SIZE);
        }
    }

    @Test
    public void factoryUsesTheStockDrawerWhenNoBackgroundImageIsSet() {
        JSignPdfSignatureDrawerFactory factory = new JSignPdfSignatureDrawerFactory();
        assertTrue(factory.getSignatureDrawer(baseParameters()) instanceof JSignPdfNativeSignatureDrawer);

        JSignPdfSignatureImageParameters withBackground = baseParameters();
        withBackground.setBackgroundImage(new InMemoryDocument(new byte[0], "empty.png"));
        assertTrue(factory.getSignatureDrawer(withBackground) instanceof JSignPdfOverlaySignatureDrawer);
    }

    private JSignPdfSignatureImageParameters baseParameters() {
        JSignPdfSignatureImageParameters params = new JSignPdfSignatureImageParameters();
        SignatureFieldParameters field = new SignatureFieldParameters();
        field.setPage(1);
        field.setOriginX(50f);
        field.setOriginY(50f);
        field.setWidth(400f);
        field.setHeight(300f);
        params.setFieldParameters(field);

        SignatureImageTextParameters text = new SignatureImageTextParameters();
        text.setText("Signed by Test\n2026.08.02 12:00:00 CEST");
        text.setTextWrapping(TextWrapping.FILL_BOX_AND_LINEBREAK);
        text.setBackgroundColor(null);
        text.setFont(new DSSJavaFont("Helvetica", java.awt.Font.PLAIN, PREFERRED_FONT_SIZE));
        params.setTextParameters(text);
        return params;
    }

    private PDDocument singlePageDocument() {
        PDDocument doc = new PDDocument();
        doc.addPage(new PDPage());
        return doc;
    }

    /** Creates a minimal RGB PNG; PNG truecolor keeps PDFBox reporting DeviceRGB. */
    private File rgbImage() throws Exception {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, 10, 10);
        g2d.dispose();
        File file = tmp.newFile("rgb-" + System.nanoTime() + ".png");
        ImageIO.write(img, "png", file);
        return file;
    }
}
