package net.sf.jsignpdf;

import static net.sf.jsignpdf.Constants.NEW_LINE;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.JTextArea;

/**
 * Logging Handlerwhich appends messages to the provided {@link JTextArea} instance.
 *
 * @author Josef Cacek
 */
public class JTextAreaHandler extends Handler {

    private JTextArea jTextArea;

    /**
     * Constructor.
     *
     * @param jTextArea
     */
    public JTextAreaHandler(final JTextArea jTextArea) {
        if (jTextArea == null) {
            throw new IllegalArgumentException("JTextArea has to be not-null.");
        }
        this.jTextArea = jTextArea;
    }

    @Override
    public void close() {
        jTextArea = null;
    }

    @Override
    public void publish(LogRecord record) {
        jTextArea.append(record.getLevel() + " " + record.getMessage() + NEW_LINE);
        Throwable thrown = record.getThrown();
        if (thrown != null) {
            try (var stringWriter = new StringWriter()) {
                thrown.printStackTrace(new PrintWriter(stringWriter, true));
                jTextArea.append(stringWriter.toString());
            } catch (IOException e) {
                // should not happen :)
            }
            jTextArea.append(NEW_LINE);
        }
        // scroll TextArea
        jTextArea.setCaretPosition(jTextArea.getText().length());
    }

    @Override
    public void flush() {
    }

}
