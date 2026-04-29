package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openpdf.text.pdf.BaseFont;

/**
 * Verifies that {@link FontUtils#reset()} clears the cached BaseFont so the next call rebuilds it.
 */
public class FontUtilsResetTest {

    @Test
    public void reset_dropsCachedFont() {
        BaseFont first = FontUtils.getL2BaseFont();
        assertNotNull(first);
        FontUtils.reset();
        assertNull("reset() must drop the cache", FontUtils.l2baseFont);
        BaseFont second = FontUtils.getL2BaseFont();
        assertNotNull(second);
    }
}
