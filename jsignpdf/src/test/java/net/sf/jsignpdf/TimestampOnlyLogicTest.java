package net.sf.jsignpdf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Covers the dispatch of {@link SignerLogic#execute()} into the document timestamp path: the operation is
 * refused - with an actionable message - when the resolved engine cannot append a document timestamp.
 */
public class TimestampOnlyLogicTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File inputFile;
    private File outputFile;

    @Before
    public void createInputPdf() throws Exception {
        inputFile = tmp.newFile("input.pdf");
        outputFile = new File(tmp.getRoot(), "timestamped.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(inputFile);
        }
    }

    @Test
    public void explicitOpenPdfEngineIsRefusedWithAHint() {
        String severe = runAndCaptureSevere(timestampOptions("openpdf"));
        assertTrue("the missing capability must be named: " + severe, severe.contains("DOC_TIMESTAMP"));
        assertTrue("the DSS engine must be pointed at: " + severe, severe.contains("dss"));
        assertFalse("nothing may be written", outputFile.exists());
    }

    @Test
    public void defaultEngineWithoutTheCapabilityIsRefusedWithTheSameHint() {
        // No --engine: the bundled default (openpdf) is resolved, and the message must be just as actionable.
        String severe = runAndCaptureSevere(timestampOptions(null));
        assertTrue("the missing capability must be named: " + severe, severe.contains("DOC_TIMESTAMP"));
        assertTrue("the DSS engine must be pointed at: " + severe, severe.contains("dss"));
    }

    private BasicSignerOptions timestampOptions(String engineId) {
        BasicSignerOptions o = new BasicSignerOptions();
        o.setAdvanced(true);
        o.setTimestampOnly(true);
        o.setTimestamp(true);
        o.setTsaUrl("http://tsa.example/tsa");
        o.setInFile(inputFile.getAbsolutePath());
        o.setOutFile(outputFile.getAbsolutePath());
        if (engineId != null) {
            o.setEngine(engineId);
        }
        return o;
    }

    private String runAndCaptureSevere(BasicSignerOptions options) {
        CapturingLogHandler handler = new CapturingLogHandler();
        Logger logger = Logger.getLogger("net.sf.jsignpdf");
        logger.addHandler(handler);
        try {
            assertFalse("an engine without DOC_TIMESTAMP must refuse", new SignerLogic(options).execute());
        } finally {
            logger.removeHandler(handler);
        }
        return handler.severeMessages();
    }

    private static final class CapturingLogHandler extends Handler {
        private final StringBuilder severe = new StringBuilder();

        @Override
        public void publish(LogRecord record) {
            if (record.getLevel().intValue() >= Level.SEVERE.intValue()) {
                severe.append(new SimpleFormatter().formatMessage(record)).append('\n');
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        String severeMessages() {
            return severe.toString();
        }
    }
}
