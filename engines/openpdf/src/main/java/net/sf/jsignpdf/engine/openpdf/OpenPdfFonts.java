package net.sf.jsignpdf.engine.openpdf;

import net.sf.jsignpdf.utils.FontUtils;
import net.sf.jsignpdf.utils.FontUtils.L2Font;

import org.openpdf.text.pdf.BaseFont;

/**
 * Builds the OpenPDF {@link BaseFont} for the visible-signature L2 text from the backend-neutral font
 * data resolved by {@link FontUtils}. Kept inside the OpenPDF engine module so the shared core stays
 * free of any OpenPDF dependency.
 *
 * @author Josef Cacek
 */
final class OpenPdfFonts {

    private OpenPdfFonts() {
    }

    /**
     * Returns the BaseFont for the visible-signature L2 text, embedding the given font and
     * falling back to a non-embedded Helvetica if it cannot be built.
     *
     * @param font the font data resolved by {@link FontUtils}, may be {@code null}
     * @return the L2 BaseFont, or {@code null} if even the fallback fails
     */
    static synchronized BaseFont getL2BaseFont(L2Font font) {
        if (font != null) {
            try {
                return BaseFont.createFont(font.getName(), font.getEncoding(), BaseFont.EMBEDDED, BaseFont.CACHED,
                        font.getData(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        } catch (Exception ex) {
            // where is the problem, dear Watson?
        }
        return null;
    }
}
