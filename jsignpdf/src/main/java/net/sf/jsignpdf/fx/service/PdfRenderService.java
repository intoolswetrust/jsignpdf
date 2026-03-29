package net.sf.jsignpdf.fx.service;

import java.awt.image.BufferedImage;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.fx.util.SwingFxImageConverter;
import net.sf.jsignpdf.preview.Pdf2Image;

/**
 * Background service that renders a PDF page to a JavaFX Image using the existing Pdf2Image class.
 */
public class PdfRenderService extends Service<Image> {

    private BasicSignerOptions options;
    private int page = 1;

    public void setOptions(BasicSignerOptions options) {
        this.options = options;
    }

    public void setPage(int page) {
        this.page = page;
    }

    @Override
    protected Task<Image> createTask() {
        final BasicSignerOptions taskOptions = this.options;
        final int taskPage = this.page;

        return new Task<Image>() {
            @Override
            protected Image call() {
                // Clear any leftover interrupt flag from a previous cancel()
                Thread.interrupted();
                Pdf2Image p2i = new Pdf2Image(taskOptions);
                BufferedImage buffered = p2i.getImageForPage(taskPage);
                return SwingFxImageConverter.toFxImage(buffered);
            }
        };
    }
}
