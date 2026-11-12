package net.sf.jsignpdf.utils;

import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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

    private static final String BUNDLED_FONT_DIR = "/net/sf/jsignpdf/fonts/";
    private static final List<String> FALLBACK_FONT_NAMES = List.of("NotoSansSC-Regular.ttf",
            "NotoSansTamil-Regular.ttf");

    private static L2Font l2Font;
    private static final Map<String, L2Font> fallbackFonts = new HashMap<>();

    /**
     * Immutable holder for the resolved L2 font data.
     */
    public static final class L2Font {
        private final String name;
        private final String encoding;
        private final byte[] data;
        private Font glyphCoverage;
        private boolean glyphCoverageLoaded;

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

        /**
         * Returns the distinct characters of the given text that have no glyph in this font, in order of first
         * occurrence. Whitespace and control characters are ignored.
         *
         * @param text the text to check, may be {@code null}
         * @return the missing characters, or an empty string when the font covers the whole text
         */
        public String missingGlyphs(String text) {
            return missing(text, null);
        }

        /**
         * Like {@link #missingGlyphs(String)}, but for a non-Identity encoding a character outside that
         * encoding counts as missing too, because a simple-font encoding can not address it.
         *
         * @param text the text to check, may be {@code null}
         * @return the missing characters, or an empty string when the font covers the whole text
         */
        public String missingCharacters(String text) {
            return missing(text, encoder());
        }

        private String missing(String text, CharsetEncoder encoder) {
            if (text == null || text.isEmpty()) {
                return "";
            }
            final Font font = glyphCoverage();
            final StringBuilder missing = new StringBuilder();
            text.codePoints().filter(cp -> !Character.isWhitespace(cp) && !Character.isISOControl(cp)).distinct()
                    .filter(cp -> (font != null && !font.canDisplay(cp))
                            || (encoder != null && !encoder.canEncode(Character.toString(cp))))
                    .forEach(missing::appendCodePoint);
            return missing.toString();
        }

        private synchronized Font glyphCoverage() {
            if (!glyphCoverageLoaded) {
                glyphCoverageLoaded = true;
                try {
                    glyphCoverage = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(data));
                } catch (Exception e) {
                    Constants.LOGGER.log(Level.FINE, "Unable to read glyph coverage of font " + name, e);
                }
            }
            return glyphCoverage;
        }

        private CharsetEncoder encoder() {
            if (encoding == null || encoding.startsWith("Identity-")) {
                return null;
            }
            try {
                return Charset.forName(encoding).newEncoder();
            } catch (Exception e) {
                return null;
            }
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
     * Resolves the L2 font able to render the given text. Returns the {@link #getL2Font() configured font}
     * when it covers the text or when {@code font.path} is set explicitly; otherwise the first bundled
     * fallback font (Noto Sans SC, Noto Sans Tamil) covering the whole text. When none does, the configured
     * font is returned and {@link L2Font#missingCharacters(String)} tells what it lacks.
     *
     * @param text the final visible-signature text
     * @return the font data, or {@code null} if the configured font could not be read
     */
    public static synchronized L2Font getL2Font(String text) {
        final L2Font configured = getL2Font();
        if (configured == null || AppConfig.fontPath() != null || configured.missingCharacters(text).isEmpty()) {
            return configured;
        }
        for (String fallbackName : FALLBACK_FONT_NAMES) {
            final L2Font fallback = fallbackFonts.computeIfAbsent(fallbackName, FontUtils::loadBundledFont);
            if (fallback != null && fallback.missingCharacters(text).isEmpty()) {
                return fallback;
            }
        }
        return configured;
    }

    private static L2Font loadBundledFont(String fileName) {
        try (InputStream is = FontUtils.class.getResourceAsStream(BUNDLED_FONT_DIR + fileName)) {
            if (is != null) {
                return new L2Font(fileName, ENCODING_IDENTITY_H, is.readAllBytes());
            }
        } catch (Exception e) {
            Constants.LOGGER.log(Level.WARNING, "Unable to load bundled font " + fileName, e);
        }
        return null;
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
