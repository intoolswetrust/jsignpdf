package net.sf.jsignpdf.fx.service;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.utils.KeyStoreUtils;

/**
 * Background service that loads key aliases from a keystore.
 */
public class KeyStoreService extends Service<String[]> {

    private BasicSignerOptions options;

    public void setOptions(BasicSignerOptions options) {
        this.options = options;
    }

    @Override
    protected Task<String[]> createTask() {
        final BasicSignerOptions taskOptions = this.options;
        return new Task<String[]>() {
            @Override
            protected String[] call() {
                return KeyStoreUtils.getKeyAliases(taskOptions);
            }
        };
    }
}
