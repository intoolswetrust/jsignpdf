package net.sf.jsignpdf.fx.screenshot;

import static net.sf.jsignpdf.fx.screenshot.ScreenshotScenario.runFx;
import static net.sf.jsignpdf.fx.screenshot.ScreenshotScenario.settle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Shows the JavaFX main window on a real X display and walks it through the documented states, letting an
 * external screen grabber take the pictures. Where {@link FxScreenshotGenerator} renders off-screen and gets
 * no window decorations, this one produces images with the title bar and borders the window manager draws -
 * at the cost of needing a display, a window manager, and the tools in
 * {@code website/capture-screenshots-x11.sh}, which is what normally launches it.
 *
 * <h2>Handshake</h2>
 *
 * <p>
 * The Java side owns the application state, the shell side owns the pixels, and they meet over marker files in
 * the directory named by {@code -Djsignpdf.screenshot.handshakeDir}:
 * </p>
 *
 * <ul>
 * <li>{@code NNN.ready} - written once a state is on screen, numbered in capture order. Three keys:
 * <ul>
 * <li>{@code path} - where the image goes, as {@code guide/...} or {@code site/...};</li>
 * <li>{@code region} - {@code full} for the whole decorated window, or {@code crop <x> <y> <w> <h>}, a
 * rectangle in client-area coordinates that the grabber offsets by the frame it measured;</li>
 * <li>{@code window} - the exact title of the window to grab, which is not always the main one (the
 * Preferences dialog is its own window, and the main title changes when a document is open).</li>
 * </ul>
 * </li>
 * <li>{@code NNN.done} - written by the grabber once the file exists. The runner blocks on it, so the UI never
 * moves on mid-capture.</li>
 * <li>{@code finished} - written after the last shot. {@code error} - written instead, with the message, if
 * the run dies.</li>
 * </ul>
 */
public final class DecoratedScreenshotRunner {

    private static final long CAPTURE_TIMEOUT_SECONDS = 120;

    private DecoratedScreenshotRunner() {
    }

    public static void main(String[] args) {
        int status = 0;
        Path handshakeDir = null;
        try {
            handshakeDir = ScreenshotScenario.requiredDir("jsignpdf.screenshot.handshakeDir");
            String galleryLocale = System.getProperty("jsignpdf.screenshot.galleryLocale");
            if (galleryLocale == null || galleryLocale.isBlank()) {
                ScreenshotScenario.prepare();
                ScreenshotScenario.buildWindow(true);
                ScreenshotScenario.run(new HandshakeSink(handshakeDir));
            } else {
                // One translation per JVM; the script starts it in that language. See prepareGallery.
                ScreenshotScenario.prepareGallery(galleryLocale);
                ScreenshotScenario.buildWindow(true);
                ScreenshotScenario.galleryShot(new HandshakeSink(handshakeDir), galleryLocale);
            }
            Files.writeString(handshakeDir.resolve("finished"), "ok\n");
        } catch (Throwable t) {
            status = 1;
            t.printStackTrace();
            writeError(handshakeDir, t);
        } finally {
            Platform.exit();
        }
        // The FX toolkit keeps non-daemon threads around; nothing is left to do once the walk is over.
        System.exit(status);
    }

    /**
     * Announces each finished state to the grabber and waits for it to confirm the capture.
     */
    private static final class HandshakeSink implements ScreenshotScenario.ShotSink {

        private final Path handshakeDir;
        private int sequence;

        HandshakeSink(Path handshakeDir) {
            this.handshakeDir = handshakeDir;
        }

        @Override
        public void shot(String path, Node node) throws Exception {
            settle();
            request(path, regionOf(node), titleOf(node));
        }

        @Override
        public void dialogShot(String path, Node window, Alert dialog) throws Exception {
            // The dialog is a separate stage physically on top of the window, so a screen grab of the window
            // rectangle already contains it - no compositing needed on this path.
            settle();
            request(path, "full", titleOf(window));
        }

        @Override
        public void finished() {
            // Nothing to mirror: the grabber writes every image straight to its final location.
        }

        /**
         * {@code full} for a whole window, otherwise the node's rectangle in client-area coordinates.
         */
        private static String regionOf(Node node) throws Exception {
            AtomicReference<String> region = new AtomicReference<>("full");
            runFx(() -> {
                if (node == node.getScene().getRoot()) {
                    return;
                }
                Bounds bounds = node.localToScene(node.getLayoutBounds());
                region.set(String.format("crop %d %d %d %d",
                        Math.round(bounds.getMinX()), Math.round(bounds.getMinY()),
                        Math.round(bounds.getWidth()), Math.round(bounds.getHeight())));
            });
            return region.get();
        }

        private static String titleOf(Node node) throws Exception {
            AtomicReference<String> title = new AtomicReference<>();
            runFx(() -> {
                Window window = node.getScene().getWindow();
                title.set(window instanceof Stage stage ? stage.getTitle() : null);
            });
            if (title.get() == null || title.get().isBlank()) {
                throw new IllegalStateException("The window holding " + node + " has no title to search for");
            }
            return title.get();
        }

        private void request(String path, String region, String window) throws Exception {
            String id = String.format("%03d", ++sequence);
            Path done = handshakeDir.resolve(id + ".done");
            Files.deleteIfExists(done);
            Files.writeString(handshakeDir.resolve(id + ".ready"),
                    "path=" + path + "\nregion=" + region + "\nwindow=" + window + "\n");
            System.out.println("[runner] waiting for " + path + " (" + region + " of \"" + window + "\")");

            long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(CAPTURE_TIMEOUT_SECONDS);
            while (!Files.exists(done)) {
                if (System.currentTimeMillis() > deadline) {
                    throw new IllegalStateException("The screen grabber did not capture " + path
                            + " within " + CAPTURE_TIMEOUT_SECONDS + "s");
                }
                Thread.sleep(100);
            }
        }
    }

    private static void writeError(Path handshakeDir, Throwable t) {
        if (handshakeDir == null || !Files.isDirectory(handshakeDir)) {
            return;
        }
        try {
            Files.writeString(handshakeDir.resolve("error"), String.valueOf(t) + "\n");
        } catch (IOException ignored) {
            // The stack trace already went to stderr; the grabber will time out and report that.
        }
    }
}
