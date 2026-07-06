package net.sf.jsignpdf.fx.service;

import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Headless (Monocle) test for {@link JpxCodecPrompt#runDownload}, focused on the progress dialog's lifecycle: it must
 * actually close once the background download finishes. Regression guard for the bug where a completed download left the
 * dialog open, because JavaFX's {@code Dialog.close()} fires a close-request event (when the result is null) that the
 * window-close guard was vetoing.
 */
public class JpxCodecPromptTest {

    @BeforeClass
    public static void initFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test(timeout = 15000)
    public void runDownload_closesDialogAndReturnsTrue_whenDownloadSucceeds() throws Exception {
        // Fake download: report determinate progress to completion, then return normally.
        JpxCodecPrompt.Downloader downloader = (dir, handler) -> {
            for (long done = 0; done <= 1000; done += 250) {
                handler.onProgress(done, 1000);
            }
        };

        Path pluginsDir = Paths.get("unused");
        CountDownLatch done = new CountDownLatch(1);
        boolean[] result = { false };
        // runDownload blocks on the FX thread (showAndWait), so invoke it there; the worker thread it spawns must close
        // the dialog for this to return at all.
        Platform.runLater(() -> {
            try {
                result[0] = new JpxCodecPrompt(null).runDownload(pluginsDir, downloader);
            } finally {
                done.countDown();
            }
        });

        assertTrue("runDownload did not return - the dialog likely never closed", done.await(12, TimeUnit.SECONDS));
        assertTrue("runDownload should report success", result[0]);
    }
}
