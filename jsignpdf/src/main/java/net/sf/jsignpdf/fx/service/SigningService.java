package net.sf.jsignpdf.fx.service;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.SignerLogic;

/**
 * Background service that wraps SignerLogic.signFile() for JavaFX.
 */
public class SigningService extends Service<Boolean> {

    private BasicSignerOptions options;

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
