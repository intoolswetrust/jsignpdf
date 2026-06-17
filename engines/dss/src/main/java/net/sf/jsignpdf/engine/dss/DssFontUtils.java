package net.sf.jsignpdf.engine.dss;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Level;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.AppConfig;

import org.apache.commons.lang3.StringUtils;

import eu.europa.esig.dss.pades.DSSFileFont;
import eu.europa.esig.dss.pades.DSSFont;

/**
 * Resolves the {@link DSSFont} used for the visible-signature text. It honours the same
 * {@code font.path} advanced-config knob the OpenPDF engine uses (capability
 * {@code VISIBLE_CUSTOM_FONT}) and otherwise falls back to the DejaVuSans font bundled in
 * {@code jsignpdf-engine-api} (so non-Latin text renders correctly).
 *
 * @author Josef Cacek
 */
final class DssFontUtils {

    /** DejaVuSans bundled as a resource in jsignpdf-engine-api. */
    private static final String DEFAULT_EMBEDDED_FONT_PATH = "/net/sf/jsignpdf/fonts/DejaVuSans.ttf";

    private DssFontUtils() {
    }

    /**
     * @return a {@link DSSFont} for the visible-signature text, or {@code null} if no font could be
     *         loaded
     */
    static DSSFont getVisibleSignatureFont() {
        final String fontPath = AppConfig.fontPath();
        try (InputStream is = fontPath != null ? new FileInputStream(fontPath)
                : DssFontUtils.class.getResourceAsStream(DEFAULT_EMBEDDED_FONT_PATH)) {
            if (is != null) {
                return new DSSFileFont(is);
            }
        } catch (Exception e) {
            Constants.LOGGER.log(Level.SEVERE, "Font loading failed" + (StringUtils.isNotEmpty(fontPath) ? ": " + fontPath : ""),
                    e);
        }
        return null;
    }
}
