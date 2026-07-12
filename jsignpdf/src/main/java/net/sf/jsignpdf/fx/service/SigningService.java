package net.sf.jsignpdf.fx.service;

import java.util.concurrent.atomic.AtomicLong;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.SignerLogic;

/**
 * Background service that wraps SignerLogic.signFile() for JavaFX.
 *
 * <p>
 * Each run gets a brand-new daemon thread rather than a shared pooled one. Re-signing does
 * {@code cancel()} + {@code reset()} + {@code start()}, and {@code cancel()} interrupts the previous run's
 * thread; on the default shared executor that interrupt can land on the reused thread of the next run and make
 * its first interruptible NIO read (e.g. loading the PDF for a visible signature) fail with
 * {@code ClosedByInterruptException} (issue #441). A fresh thread per run keeps a cancelled run's interrupt
 * from bleeding into the next one.
 * </p>
 */
public class SigningService extends Service<Boolean> {

    private static final AtomicLong THREAD_COUNTER = new AtomicLong();

    private BasicSignerOptions options;

    public SigningService() {
        setExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jsignpdf-signing-" + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            thread.start();
        });
    }

    public void setOptions(BasicSignerOptions options) {
        this.options = options;
    }

    @Override
    protected Task<Boolean> createTask() {
        final BasicSignerOptions taskOptions = this.options;
        return new Task<Boolean>() {
            @Override
            protected Boolean call() {
                SignerLogic logic = new SignerLogic(taskOptions);
                return logic.signFile();
            }
        };
    }
}
