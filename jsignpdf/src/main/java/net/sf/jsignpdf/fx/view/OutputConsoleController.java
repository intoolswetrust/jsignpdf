package net.sf.jsignpdf.fx.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static net.sf.jsignpdf.Constants.LOGGER;

/**
 * Controller for the output console panel that captures log messages.
 */
public class OutputConsoleController {

    @FXML private TextArea txtOutput;

    private Handler logHandler;

    @FXML
    private void initialize() {
        // Attach a log handler to capture signing output
        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record == null) return;
                StringBuilder sb = new StringBuilder();
                if (record.getLevel() != null
                        && record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    sb.append(record.getLevel().getName()).append(" ");
                }
                if (record.getMessage() != null) {
                    sb.append(record.getMessage());
                }
                if (record.getThrown() != null) {
                    sb.append("\n");
                    StringWriter sw = new StringWriter();
                    record.getThrown().printStackTrace(new PrintWriter(sw));
                    sb.append(sw);
                }
                if (sb.length() > 0) {
                    Platform.runLater(() -> txtOutput.appendText(sb.toString() + "\n"));
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        LOGGER.addHandler(logHandler);
    }

    @FXML
    private void onClear() {
        txtOutput.clear();
    }

    public void appendMessage(String message) {
        Platform.runLater(() -> txtOutput.appendText(message + "\n"));
    }
}
