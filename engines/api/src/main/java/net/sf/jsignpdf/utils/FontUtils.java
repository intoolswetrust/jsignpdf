package net.sf.jsignpdf.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import net.sf.jsignpdf.Constants;

import org.apache.commons.io.IOUtils;

/**
 * Resolves the font used for the visible-signature L2 text as backend-neutral data (font name,
 * encoding and the raw font bytes). The actual font object is built by the active signing engine
 * from this data, so this shared utility carries no signing-backend dependency.
 *
 * <p>
 * The encoding defaults are the values historically exposed by iText / OpenPDF as
 * {@code BaseFont.WINANSI == "Cp1252"} and {@code BaseFont.IDENTITY_H == "Identity-H"}.
 * </p>
 *
 * @author Josef Cacek
 */
public class FontUtils {

    /** iText/OpenPDF {@code BaseFont.WINANSI} value. */
    public static final String ENCODING_WINANSI = "Cp1252";
    /** iText/OpenPDF {@code BaseFont.IDENTITY_H} value. */
    public static final String ENCODING_IDENTITY_H = "Identity-H";

    private static L2Font l2Font;

    /**
     * Immutable holder for the resolved L2 font data.
     */
    public static final class L2Font {
        private final String name;
        private final String encoding;
        private final byte[] data;

        L2Font(String name, String encoding, byte[] data) {
            this.name = name;
            this.encoding = encoding;
            this.data = data;
        }

        public String getName() {
            return name;
        }

        public String getEncoding() {
            return encoding;
        }

        public byte[] getData() {
            return data;
        }
    }

    /**
     * Resolves the L2 font (name, encoding and raw bytes) from the current {@link AppConfig#fontPath()},
     * {@link AppConfig#fontName()} and {@link AppConfig#fontEncoding()}, falling back to the bundled
     * DejaVuSans font. The result is cached until {@link #reset()} is called.
     *
     * @return the resolved font data, or {@code null} if it could not be read (the engine then uses
     *         its own built-in fallback font)
     */
    public static synchronized L2Font getL2Font() {
        if (l2Font == null) {
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
                        fontEncoding = ENCODING_WINANSI;
                    }
                    tmpIs = new FileInputStream(fontPath);
                } else {
                    fontName = Constants.L2TEXT_FONT_NAME;
                    fontEncoding = ENCODING_IDENTITY_H;
                    tmpIs = FontUtils.class.getResourceAsStream(Constants.L2TEXT_FONT_PATH);
                }
                IOUtils.copy(tmpIs, tmpBaos);
                tmpIs.close();
                tmpBaos.close();
                l2Font = new L2Font(fontName, fontEncoding, tmpBaos.toByteArray());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return l2Font;
    }

    /**
     * Drops the cached font data so the next {@link #getL2Font()} rebuilds from the current
     * {@link AppConfig#fontPath()}, {@link AppConfig#fontName()} and {@link AppConfig#fontEncoding()}.
     * Called by the Preferences dialog when a font key changes so the new font shows up on the next
     * sign / preview without restarting.
     */
    public static synchronized void reset() {
        l2Font = null;
    }

}
