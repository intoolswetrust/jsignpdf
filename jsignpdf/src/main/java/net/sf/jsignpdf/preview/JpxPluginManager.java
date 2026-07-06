package net.sf.jsignpdf.preview;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

import net.sf.jsignpdf.utils.ConfigLocationResolver;

/**
 * Loads user-provided ImageIO codec jars from {@code <cfg>/plugins} and registers their {@link ImageReaderSpi} providers
 * into the default {@link IIORegistry}, so that PDFBox can render otherwise-unsupported images (namely JPEG 2000 /
 * {@code /JPXDecode}) in the preview.
 * <p>
 * Nothing is bundled: the jars are fetched on demand by {@link JpxCodecInstaller} after an explicit user confirmation.
 * Once a reader is in the registry it serves both the JavaFX and the legacy Swing preview paths.
 */
public final class JpxPluginManager {

    /** Format name PDFBox looks up via {@code ImageIO.getImageReadersByFormatName} for {@code /JPXDecode} streams. */
    static final String JPEG2000_FORMAT = "JPEG2000";

    /** Keeps the plugin class loader reachable for the lifetime of the JVM so its SPIs stay usable. */
    private static volatile URLClassLoader pluginClassLoader;

    private JpxPluginManager() {
    }

    /** Whether a JPEG 2000 image reader is currently available in the ImageIO registry. */
    public static boolean isJpxReaderAvailable() {
        return ImageIO.getImageReadersByFormatName(JPEG2000_FORMAT).hasNext();
    }

    /**
     * Scans {@code <cfg>/plugins} for jars and registers any image-reader SPIs they contain. Idempotent and safe to call
     * again after a fresh download: if a JPEG 2000 reader is already registered, it does nothing. Never throws — any
     * failure is logged and the preview simply keeps working without the codec.
     *
     * @return {@code true} if a JPEG 2000 reader is available after this call
     */
    public static synchronized boolean registerInstalledPlugins() {
        if (isJpxReaderAvailable()) {
            return true;
        }
        registerFromDirectory(ConfigLocationResolver.getInstance().getPluginsDir());
        return isJpxReaderAvailable();
    }

    /**
     * Registers the image-reader SPIs found in the given directory's jars. Visible for tests; the public entry point
     * resolves the directory from {@link ConfigLocationResolver}. Never throws.
     *
     * @return {@code true} if at least one jar was found and loaded without error
     */
    static synchronized boolean registerFromDirectory(Path pluginsDir) {
        if (pluginsDir == null || !Files.isDirectory(pluginsDir)) {
            return false;
        }
        List<URL> jars = listJars(pluginsDir);
        if (jars.isEmpty()) {
            return false;
        }
        try {
            URLClassLoader cl = new URLClassLoader(jars.toArray(new URL[0]), JpxPluginManager.class.getClassLoader());
            registerReaderSpis(cl);
            pluginClassLoader = cl;
            return true;
        } catch (Exception | LinkageError e) {
            LOGGER.log(Level.WARNING, "Failed to load image codecs from " + pluginsDir, e);
            return false;
        }
    }

    private static List<URL> listJars(Path pluginsDir) {
        List<URL> jars = new ArrayList<>();
        try (Stream<Path> files = Files.list(pluginsDir)) {
            files.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .forEach(p -> {
                        try {
                            jars.add(p.toUri().toURL());
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Skipping unusable plugin jar " + p, e);
                        }
                    });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to list plugins directory " + pluginsDir, e);
        }
        return jars;
    }

    private static void registerReaderSpis(URLClassLoader cl) {
        IIORegistry registry = IIORegistry.getDefaultInstance();
        // Instantiate each provider defensively: the jai-imageio jar declares a native-backed CodecLib SPI whose
        // construction fails when the optional native library is absent. That must not prevent the pure-Java reader
        // (which we do want) from being registered, so we advance the iterator provider-by-provider.
        Iterator<ImageReaderSpi> it = ServiceLoader.load(ImageReaderSpi.class, cl).iterator();
        while (true) {
            try {
                if (!it.hasNext()) {
                    break;
                }
            } catch (Throwable t) {
                // hasNext() cannot reliably be advanced past a bad entry, so stop scanning rather than spin.
                LOGGER.log(Level.FINE, "Aborting image reader provider scan", t);
                break;
            }
            ImageReaderSpi spi;
            try {
                // next() consumes the current entry even when instantiation fails, so continuing is safe here.
                spi = it.next();
            } catch (Throwable t) {
                LOGGER.log(Level.FINE, "Skipping unavailable image reader provider", t);
                continue;
            }
            try {
                registry.registerServiceProvider(spi, ImageReaderSpi.class);
                LOGGER.info("Registered image reader plugin: " + spi.getClass().getName());
            } catch (Throwable t) {
                LOGGER.log(Level.FINE, "Failed to register image reader provider " + spi.getClass().getName(), t);
            }
        }
    }
}
