package net.sf.jsignpdf.preview;

import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * Minimal {@link ImageReaderSpi} used by {@link JpxPluginManagerTest} to prove that SPIs found in a plugin jar are
 * registered into the ImageIO registry. It never actually decodes anything; it exists only to be discovered under its
 * unique {@link #FORMAT} name.
 */
public class DummyImageReaderSpi extends ImageReaderSpi {

    /** Format name unique to the test, so it can't clash with any real reader. */
    public static final String FORMAT = "JSIGNPDF_TEST_JPX";

    public DummyImageReaderSpi() {
        super("jsignpdf-test", "1.0", new String[] { FORMAT }, new String[] { "jsx" },
                new String[] { "image/x-jsignpdf-test" }, DummyImageReaderSpi.class.getName(),
                new Class<?>[] { ImageInputStream.class }, null, false, null, null, null, null,
                false, null, null, null, null);
    }

    @Override
    public boolean canDecodeInput(Object source) {
        return false;
    }

    @Override
    public ImageReader createReaderInstance(Object extension) {
        throw new UnsupportedOperationException("test stub");
    }

    @Override
    public String getDescription(Locale locale) {
        return "JSignPdf test image reader";
    }
}
