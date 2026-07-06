package net.sf.jsignpdf.preview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link JpxPluginManager}: the guard/defensive paths, plus proof that an SPI provided by a jar in the plugins
 * directory is registered into the default {@link IIORegistry}. The positive test uses a synthetic {@link
 * DummyImageReaderSpi} so it needs no non-bundled (JJ2000) jar.
 */
public class JpxPluginManagerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void registerFromDirectory_returnsFalse_forNullDir() {
        assertFalse(JpxPluginManager.registerFromDirectory(null));
    }

    @Test
    public void registerFromDirectory_returnsFalse_forMissingDir() {
        Path missing = tmp.getRoot().toPath().resolve("nope");
        assertFalse(JpxPluginManager.registerFromDirectory(missing));
    }

    @Test
    public void registerFromDirectory_returnsFalse_forEmptyDir() throws Exception {
        Path dir = tmp.newFolder("empty").toPath();
        assertFalse(JpxPluginManager.registerFromDirectory(dir));
    }

    @Test
    public void registerFromDirectory_ignoresCorruptJar_withoutThrowing() throws Exception {
        Path dir = tmp.newFolder("corrupt").toPath();
        Files.write(dir.resolve("broken.jar"), new byte[] { 1, 2, 3, 4, 5 });
        // Must not throw and must not conjure a JPEG 2000 reader out of a garbage jar.
        JpxPluginManager.registerFromDirectory(dir);
        assertFalse(JpxPluginManager.isJpxReaderAvailable());
    }

    @Test
    public void registerFromDirectory_registersReaderSpiFromJar() throws Exception {
        Path dir = tmp.newFolder("plugins").toPath();
        writeSpiJar(dir.resolve("dummy-spi.jar"), DummyImageReaderSpi.class.getName());

        ImageReaderSpi registered = null;
        try {
            assertTrue(JpxPluginManager.registerFromDirectory(dir));
            registered = findRegistered(DummyImageReaderSpi.FORMAT);
            assertNotNull("Dummy SPI should be registered in the ImageIO registry", registered);
            assertEquals(DummyImageReaderSpi.class, registered.getClass());
        } finally {
            if (registered != null) {
                IIORegistry.getDefaultInstance().deregisterServiceProvider(registered);
            }
        }
    }

    /** Writes a jar whose only content is a services file declaring the given ImageReaderSpi implementation. */
    private static void writeSpiJar(Path jar, String spiClassName) throws Exception {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new ZipEntry("META-INF/services/javax.imageio.spi.ImageReaderSpi"));
            jos.write(spiClassName.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }

    private static ImageReaderSpi findRegistered(String format) {
        Iterator<ImageReaderSpi> it = IIORegistry.getDefaultInstance().getServiceProviders(ImageReaderSpi.class, false);
        while (it.hasNext()) {
            ImageReaderSpi spi = it.next();
            for (String name : spi.getFormatNames()) {
                if (format.equals(name)) {
                    return spi;
                }
            }
        }
        return null;
    }
}
