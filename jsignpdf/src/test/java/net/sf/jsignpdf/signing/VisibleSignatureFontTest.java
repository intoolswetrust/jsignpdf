package net.sf.jsignpdf.signing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.SignerLogic;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.junit.Test;

/**
 * Verifies that both engines pick a bundled font able to render the visible-signature text, and refuse to
 * sign when no font covers it instead of dropping characters.
 */
public class VisibleSignatureFontTest extends SigningTestBase {

    private static final String[] ENGINES = { "openpdf", "dss" };

    @Test
    public void latinTextKeepsDejaVuSans() throws Exception {
        for (String engine : ENGINES) {
            assertSubsetFont(engine, signVisible(engine, "Signed by ${signer}"), "DejaVuSans");
        }
    }

    @Test
    public void cjkTextUsesNotoSansSc() throws Exception {
        for (String engine : ENGINES) {
            assertSubsetFont(engine, signVisible(engine, "署名者 ${signer} 数字签名者 數位簽章"), "NotoSansSC-Regular");
        }
    }

    @Test
    public void tamilTextUsesNotoSansTamil() throws Exception {
        for (String engine : ENGINES) {
            assertSubsetFont(engine, signVisible(engine, "தேதி ${signer}"), "NotoSansTamil-Regular");
        }
    }

    @Test
    public void uncoveredTextIsRefused() throws Exception {
        for (String engine : ENGINES) {
            BasicSignerOptions options = visibleOptions(engine, "서명자 ${signer}");
            List<String> severe = new ArrayList<>();
            Handler handler = capturing(severe);
            Constants.LOGGER.addHandler(handler);
            try {
                assertFalse(engine + " must refuse text no font covers", new SignerLogic(options).signFile());
            } finally {
                Constants.LOGGER.removeHandler(handler);
            }
            assertTrue(engine + " must name the missing characters: " + severe,
                    severe.contains(Constants.RES.get("console.font.missingGlyphs", "DejaVuSans.ttf", "서명자")));
        }
    }

    private File signVisible(String engine, String l2Text) throws Exception {
        BasicSignerOptions options = visibleOptions(engine, l2Text);
        assertTrue(engine + " signing should succeed", new SignerLogic(options).signFile());
        return new File(options.getOutFileX());
    }

    private BasicSignerOptions visibleOptions(String engine, String l2Text) throws Exception {
        tempFolder.delete();
        tempFolder.create();
        BasicSignerOptions options = createDefaultOptions();
        options.setEngine(engine);
        options.setVisible(true);
        options.setPage(1);
        options.setPositionLLX(50);
        options.setPositionLLY(50);
        options.setPositionURX(300);
        options.setPositionURY(150);
        options.setL2Text(l2Text);
        return options;
    }

    private static void assertSubsetFont(String engine, File signedPdf, String expectedFont) throws IOException {
        Set<String> fonts = appearanceFonts(signedPdf);
        assertEquals(engine + " appearance fonts: " + fonts, 1, fonts.size());
        String font = fonts.iterator().next();
        assertTrue(engine + " must embed a subset of " + expectedFont + ", got " + font,
                font.matches("[A-Z]{6}\\+" + expectedFont));
    }

    private static Set<String> appearanceFonts(File signedPdf) throws IOException {
        Set<String> fonts = new TreeSet<>();
        try (PDDocument doc = Loader.loadPDF(signedPdf)) {
            for (PDSignatureField field : doc.getSignatureFields()) {
                for (PDAnnotationWidget widget : field.getWidgets()) {
                    if (widget.getAppearance() != null && widget.getAppearance().getNormalAppearance() != null) {
                        collectFonts(widget.getAppearance().getNormalAppearance().getAppearanceStream().getResources(),
                                fonts, 0);
                    }
                }
            }
        }
        return fonts;
    }

    private static void collectFonts(PDResources resources, Set<String> fonts, int depth) throws IOException {
        if (resources == null || depth > 8) {
            return;
        }
        for (COSName name : resources.getFontNames()) {
            PDFont font = resources.getFont(name);
            if (font != null && !font.getName().startsWith("Helvetica")) {
                fonts.add(font.getName());
            }
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xobject = resources.getXObject(name);
            if (xobject instanceof PDFormXObject form) {
                collectFonts(form.getResources(), fonts, depth + 1);
            }
        }
    }

    private static Handler capturing(List<String> sink) {
        return new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.SEVERE.intValue() && record.getMessage() != null) {
                    sink.add(record.getMessage());
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
    }
}
