package net.sf.jsignpdf.fx;

import net.sf.jsignpdf.BasicSignerOptions;

/**
 * Static launcher for the JavaFX GUI, called from {@link net.sf.jsignpdf.Signer#main}.
 */
public final class FxLauncher {

    private FxLauncher() {
    }

    /**
     * Launches the JavaFX application with the given initial options.
     *
     * @param opts initial signer options (may be null)
     */
    public static void launch(BasicSignerOptions opts) {
        JSignPdfApp.setInitialOptions(opts);
        javafx.application.Application.launch(JSignPdfApp.class);
    }
}
