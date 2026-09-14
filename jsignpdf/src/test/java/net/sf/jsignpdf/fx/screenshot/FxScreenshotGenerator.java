package net.sf.jsignpdf.fx.screenshot;

import static net.sf.jsignpdf.fx.screenshot.ScreenshotScenario.runFx;
import static net.sf.jsignpdf.fx.screenshot.ScreenshotScenario.settle;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Regenerates the JavaFX screenshots off-screen, through the headless Monocle platform the surefire
 * configuration already selects. Fast, reproducible and desktop-independent - but the images have no window
 * decorations, because off-screen rendering has no window manager to draw them. For decorated images, use
 * {@link DecoratedScreenshotRunner} through {@code website/capture-screenshots-x11.sh}.
 *
 * <p>
 * This is a manual tool, not part of the test suite: the class name deliberately falls outside surefire's
 * default include patterns, and the {@code screenshots} profile in {@code jsignpdf/pom.xml} is what selects
 * it. Refresh the images with:
 * </p>
 *
 * <pre>
 * mvn -pl jsignpdf -am -Pscreenshots test
 * </pre>
 */
public class FxScreenshotGenerator {

    private static final double SCALE = Double.parseDouble(System.getProperty("jsignpdf.screenshot.scale", "1"));

    /** Radius of the drop shadow painted under a composited dialog. */
    private static final int SHADOW_SPREAD = 8;

    /**
     * Guide images the website mirrors verbatim under its own names. Its third card,
     * {@code jsignpdf-javafx-signed.png}, is not in here: the scenario produces it directly, with the
     * confirmation dialog on top of the window.
     */
    private static final String[][] SITE_COPIES = {
            { "document-loaded.png", "jsignpdf-javafx-main.png" },
            { "visible-signature-placement.png", "jsignpdf-javafx-visible-sig.png" },
    };

    private static Path outDir;
    private static Path siteDir;

    @BeforeClass
    public static void setUpClass() throws Exception {
        requireMonocle();
        outDir = ScreenshotScenario.requiredDir("jsignpdf.screenshot.outDir");
        siteDir = ScreenshotScenario.requiredDir("jsignpdf.screenshot.siteDir");
        ScreenshotScenario.prepare();
        ScreenshotScenario.buildWindow(false);
    }

    @Test
    public void generateScreenshots() throws Exception {
        ScreenshotScenario.run(new SnapshotSink());
    }

    /** Writes each state with {@code Node.snapshot()}, compositing dialogs that live in their own stage. */
    private static final class SnapshotSink implements ScreenshotScenario.ShotSink {

        @Override
        public void shot(String fileName, Node node) throws Exception {
            write(capture(node), targetFor(fileName));
        }

        @Override
        public void dialogShot(String fileName, Node window, Alert dialog) throws Exception {
            BufferedImage background = capture(window);
            BufferedImage foreground = capture(dialog.getDialogPane());
            write(overlayCentered(background, foreground), targetFor(fileName));
        }

        @Override
        public void finished() throws Exception {
            for (String[] copy : SITE_COPIES) {
                Files.copy(outDir.resolve(copy[0]), siteDir.resolve(copy[1]), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[screenshot] " + siteDir.resolve(copy[1]));
            }
        }

        private static Path targetFor(String fileName) {
            return (fileName.startsWith("jsignpdf-") ? siteDir : outDir).resolve(fileName);
        }
    }

    private static BufferedImage capture(Node node) throws Exception {
        settle();
        AtomicReference<WritableImage> captured = new AtomicReference<>();
        runFx(() -> {
            Bounds bounds = node.getLayoutBounds();
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.WHITE);
            if (SCALE != 1.0d) {
                params.setTransform(Transform.scale(SCALE, SCALE));
            }
            WritableImage image = new WritableImage(
                    (int) Math.round(bounds.getWidth() * SCALE),
                    (int) Math.round(bounds.getHeight() * SCALE));
            captured.set(node.snapshot(params, image));
        });
        return SwingFXUtils.fromFXImage(captured.get(), null);
    }

    private static void write(BufferedImage image, Path target) throws IOException {
        ImageIO.write(image, "png", target.toFile());
        System.out.println("[screenshot] " + target + " (" + image.getWidth() + "x" + image.getHeight() + ")");
    }

    /** Draws the dialog over the middle of the window, with a soft shadow so it reads as a separate surface. */
    private static BufferedImage overlayCentered(BufferedImage window, BufferedImage dialog) {
        BufferedImage composed = new BufferedImage(window.getWidth(), window.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = composed.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(window, 0, 0, null);

        int x = (window.getWidth() - dialog.getWidth()) / 2;
        int y = (window.getHeight() - dialog.getHeight()) / 2;
        for (int spread = SHADOW_SPREAD; spread > 0; spread--) {
            g.setColor(new java.awt.Color(0, 0, 0, 10));
            g.fillRoundRect(x - spread, y - spread + 2, dialog.getWidth() + 2 * spread,
                    dialog.getHeight() + 2 * spread, 10, 10);
        }
        g.drawImage(dialog, x, y, null);
        g.setColor(new java.awt.Color(0, 0, 0, 70));
        g.drawRect(x - 1, y - 1, dialog.getWidth() + 1, dialog.getHeight() + 1);
        g.dispose();
        return composed;
    }

    private static void requireMonocle() {
        if (Platform.class.getModule().isNamed()) {
            throw new IllegalStateException("JavaFX is bundled in this JDK (javafx.graphics is a named module),"
                    + " so the headless Monocle platform cannot be applied. Build with a JDK without JavaFX"
                    + " modules (e.g. Temurin) to regenerate the screenshots.");
        }
    }
}
