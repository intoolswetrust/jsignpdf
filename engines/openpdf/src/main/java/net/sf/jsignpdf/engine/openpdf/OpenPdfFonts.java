package net.sf.jsignpdf.engine.openpdf;

import java.util.logging.Level;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.FontUtils.L2Font;

import org.openpdf.text.pdf.BaseFont;

/**
 * Builds the OpenPDF {@link BaseFont} for the visible-signature L2 text from the backend-neutral font
 * data resolved by {@link net.sf.jsignpdf.utils.FontUtils}. Kept inside the OpenPDF engine module so the
 * shared core stays free of any OpenPDF dependency.
 *
 * @author Josef Cacek
 */
final class OpenPdfFonts {

    private OpenPdfFonts() {
    }

    /**
     * Returns the embedded BaseFont for the visible-signature L2 text.
     *
     * @param font the font data resolved by {@link net.sf.jsignpdf.utils.FontUtils}, may be {@code null}
     * @return the L2 BaseFont, or {@code null} if the font is missing or can not be built
     */
    static synchronized BaseFont getL2BaseFont(L2Font font) {
        if (font == null) {
            return null;
        }
        try {
            return BaseFont.createFont(font.getName(), font.getEncoding(), BaseFont.EMBEDDED, BaseFont.CACHED,
                    font.getData(), null);
        } catch (Exception e) {
            Constants.LOGGER.log(Level.FINE, "Unable to build font " + font.getName(), e);
            return null;
        }
    }
}
