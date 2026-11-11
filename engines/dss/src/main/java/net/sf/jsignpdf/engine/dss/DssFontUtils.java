package net.sf.jsignpdf.engine.dss;

import java.util.logging.Level;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.FontUtils.L2Font;

import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.pades.DSSFileFont;
import eu.europa.esig.dss.pades.DSSFont;

/**
 * Builds the {@link DSSFont} used for the visible-signature text from the font data resolved by
 * {@link net.sf.jsignpdf.utils.FontUtils}.
 *
 * @author Josef Cacek
 */
final class DssFontUtils {

    private DssFontUtils() {
    }

    /**
     * Returns a subset-embedding {@link DSSFont} for the given font data.
     *
     * @param font the resolved font data
     * @return the DSS font, or {@code null} if it could not be built
     */
    static DSSFont toDssFont(L2Font font) {
        try {
            final DSSFileFont dssFont = new DSSFileFont(new InMemoryDocument(font.getData()));
            dssFont.setEmbedFontSubset(true);
            return dssFont;
        } catch (Exception e) {
            Constants.LOGGER.log(Level.SEVERE, Constants.RES.get("console.font.loadFailed", font.getName()), e);
        }
        return null;
    }
}
