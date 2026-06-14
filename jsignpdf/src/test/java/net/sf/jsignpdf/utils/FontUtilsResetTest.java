package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import net.sf.jsignpdf.utils.FontUtils.L2Font;

/**
 * Verifies that {@link FontUtils#reset()} clears the cached font data so the next call rebuilds it.
 */
public class FontUtilsResetTest {

    @Test
    public void reset_dropsCachedFont() {
        L2Font first = FontUtils.getL2Font();
        assertNotNull(first);
        assertNotNull("bundled font bytes must be resolvable", first.getData());
        FontUtils.reset();
        L2Font second = FontUtils.getL2Font();
        assertNotNull(second);
        assertNotSame("reset() must drop the cache so a fresh instance is built", first, second);
    }
}
