package net.sf.jsignpdf.fx.service;

import static net.sf.jsignpdf.Constants.LOGGER;
import static net.sf.jsignpdf.Constants.RES;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import net.sf.jsignpdf.preview.JpxCodecInstaller;

/**
 * JavaFX front-end for the optional JPEG 2000 codec download: the informed-consent confirmation and the modal,
 * cancellable progress dialog. All UI runs on the FX thread; the actual download runs on a background thread.
 * <p>
 * The codec is fetched from Maven Central and verified against pinned SHA-256 hashes by {@link JpxCodecInstaller}; it is
 * never bundled or redistributed by JSignPdf.
 */
public final class JpxCodecPrompt {

    private final Window owner;

    public JpxCodecPrompt(Window owner) {
        this.owner = owner;
    }

    /**
     * Asks the user whether to download the codec, showing a short note and the licence URL so consent is informed.
     *
     * @return {@code true} if the user chose to download, {@code false} on Skip or dismissal
     */
    public boolean confirmDownload() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle(RES.get("jfx.gui.dialog.jpx.title"));
        alert.setHeaderText(RES.get("jfx.gui.dialog.jpx.header"));

        ButtonType download = new ButtonType(RES.get("jfx.gui.dialog.jpx.download"), ButtonBar.ButtonData.OK_DONE);
        ButtonType skip = new ButtonType(RES.get("jfx.gui.dialog.jpx.skip"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(download, skip);

        Label message = new Label(RES.get("jfx.gui.dialog.jpx.text"));
        message.setWrapText(true);
        message.setMaxWidth(460);
        Label licenseNote = new Label(RES.get("jfx.gui.dialog.jpx.license"));
        licenseNote.setWrapText(true);
        licenseNote.setMaxWidth(460);
        TextField licenseUrl = new TextField(JpxCodecInstaller.LICENSE_URL);
        licenseUrl.setEditable(false);
        licenseUrl.getStyleClass().add("copyable-url");

        VBox content = new VBox(10, message, licenseNote, licenseUrl);
        content.setPadding(new Insets(4, 0, 0, 0));
        alert.getDialogPane().setContent(content);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == download;
    }

    /**
     * Shows a modal, determinate, cancellable progress dialog while the codec is downloaded and verified on a background
     * thread. Blocks until the download finishes, is cancelled, or fails.
     *
     * @param pluginsDir the target directory ({@code <cfg>/plugins})
     * @return {@code true} if the codec was installed successfully
     */
    public boolean runDownload(Path pluginsDir) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(RES.get("jfx.gui.dialog.jpx.progress.title"));

        Label status = new Label(RES.get("jfx.gui.dialog.jpx.progress.text"));
        ProgressBar bar = new ProgressBar(0);
        bar.setPrefWidth(360);
        VBox content = new VBox(12, status, bar);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);

        ButtonType cancelType = new ButtonType(RES.get("jfx.gui.dialog.jpx.progress.cancel"),
                ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelType);

        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicBoolean finished = new AtomicBoolean(false);
        boolean[] success = { false };
        boolean[] failed = { false };

        // Intercept the Cancel button so the dialog stays open until the worker actually stops.
        final Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelType);
        cancelButton.addEventFilter(ActionEvent.ACTION, evt -> {
            cancelled.set(true);
            cancelButton.setDisable(true);
            status.setText(RES.get("jfx.gui.dialog.jpx.progress.cancelling"));
            evt.consume();
        });
        // A user close request (window X) is treated as Cancel and blocked until the worker stops; the worker's own
        // close (finished == true) is allowed through. This guard is essential: JavaFX's Dialog.close() fires this
        // same close-request event when the result is null, so without it the programmatic close would be vetoed here.
        dialog.setOnCloseRequest(evt -> {
            if (finished.get()) {
                return;
            }
            cancelled.set(true);
            evt.consume();
        });

        Thread worker = new Thread(() -> {
            try {
                new JpxCodecInstaller(pluginsDir).install(new JpxCodecInstaller.ProgressHandler() {
                    @Override
                    public void onProgress(long done, long total) {
                        double fraction = total > 0 ? (double) done / total : ProgressBar.INDETERMINATE_PROGRESS;
                        Platform.runLater(() -> bar.setProgress(fraction));
                    }

                    @Override
                    public boolean isCancelled() {
                        return cancelled.get();
                    }
                });
                success[0] = true;
            } catch (InterruptedException e) {
                // Cancelled by the user; nothing installed.
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "JPEG 2000 codec download failed", e);
                failed[0] = true;
            } finally {
                Platform.runLater(() -> closeDialog(dialog, finished));
            }
        }, "jpx-codec-download");
        worker.setDaemon(true);
        worker.start();

        dialog.showAndWait();
        if (failed[0]) {
            showError();
        }
        return success[0];
    }

    private void showError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle(RES.get("jfx.gui.dialog.jpx.error.title"));
        alert.setHeaderText(null);
        alert.setContentText(RES.get("jfx.gui.dialog.jpx.error.text"));
        alert.showAndWait();
    }

    private static void closeDialog(Dialog<?> dialog, AtomicBoolean finished) {
        // Mark the close as worker-initiated first, so the onCloseRequest guard lets Dialog.close() through.
        finished.set(true);
        dialog.close();
    }
}
