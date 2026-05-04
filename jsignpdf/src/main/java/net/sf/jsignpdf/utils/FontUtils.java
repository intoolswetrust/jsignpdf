package net.sf.jsignpdf.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import net.sf.jsignpdf.Constants;

import org.apache.commons.io.IOUtils;

import org.openpdf.text.pdf.BaseFont;

/**
 * Utilities for handling fonts in visible signature.
 * 
 * @author Josef Cacek
 */
public class FontUtils {

    public static BaseFont l2baseFont;

    /**
     * Returns BaseFont for text of visible signature;
     * 
     * @return
     */
    public static synchronized BaseFont getL2BaseFont() {
        if (l2baseFont == null) {
            try {
                final ByteArrayOutputStream tmpBaos = new ByteArrayOutputStream();
                String fontPath = AppConfig.fontPath();
                String fontName;
                String fontEncoding;
                InputStream tmpIs;
                if (fontPath != null) {
                    fontName = AppConfig.fontName();
                    if (fontName == null) {
                        fontName = new File(fontPath).getName();
                    }
                    fontEncoding = AppConfig.fontEncoding();
                    if (fontEncoding == null) {
                        fontEncoding = BaseFont.WINANSI;
                    }
                    tmpIs = new FileInputStream(fontPath);
                } else {
                    fontName = Constants.L2TEXT_FONT_NAME;
                    fontEncoding = BaseFont.IDENTITY_H;
                    tmpIs = FontUtils.class.getResourceAsStream(Constants.L2TEXT_FONT_PATH);
                }
                IOUtils.copy(tmpIs, tmpBaos);
                tmpIs.close();
                tmpBaos.close();
                l2baseFont = BaseFont.createFont(fontName, fontEncoding, BaseFont.EMBEDDED, BaseFont.CACHED,
                        tmpBaos.toByteArray(), null);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    l2baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
                } catch (Exception ex) {
                    // where is the problem, dear Watson?
                }
            }
        }
        return l2baseFont;
    }

    /**
     * Drops the cached BaseFont so the next {@link #getL2BaseFont()} rebuilds from the current {@link AppConfig#fontPath()},
     * {@link AppConfig#fontName()} and {@link AppConfig#fontEncoding()}. Called by the Preferences dialog when a font key
     * changes so the new font shows up on the next sign / preview without restarting.
     */
    public static synchronized void reset() {
        l2baseFont = null;
    }

}
