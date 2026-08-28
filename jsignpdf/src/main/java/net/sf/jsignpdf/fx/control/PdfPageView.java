package net.sf.jsignpdf.fx.control;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import net.sf.jsignpdf.preview.PreviewRenderSettings;

/**
 * Displays a high-resolution raster preview at a logical UI zoom.
 * Raster resolution affects image detail only; it does not change the
 * apparent size of the page on screen.
 */
public class PdfPageView extends Region {
    private static final double FALLBACK_SCREEN_DPI = 96.0;

    private final ImageView imageView = new ImageView();
    private final ObjectProperty<Image> pageImage = new SimpleObjectProperty<>();
    private final DoubleProperty zoomLevel = new SimpleDoubleProperty(1.0);
    private final DoubleProperty renderDpi = new SimpleDoubleProperty(PreviewRenderSettings.targetRenderDpi());

    public PdfPageView() {
        getChildren().add(imageView);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.imageProperty().bind(pageImage);
        pageImage.addListener((obs, oldImage, newImage) -> updateSize());
        zoomLevel.addListener((obs, oldZoom, newZoom) -> updateSize());
        renderDpi.addListener((obs, oldDpi, newDpi) -> updateSize());
        getStyleClass().add("pdf-page-view");
    }

    private void updateSize() {
        Image image = pageImage.get();
        if (image == null) {
            return;
        }

        double displayScale = rasterToDisplayScale() * zoomLevel.get();
        double width = image.getWidth() * displayScale;
        double height = image.getHeight() * displayScale;

        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
    }

    private double rasterToDisplayScale() {
        double dpi = renderDpi.get() > 0 ? renderDpi.get() : PreviewRenderSettings.targetRenderDpi();
        return getScreenDpi() / dpi;
    }

    /**
     * Zoom level at which the current page image exactly fits the given viewport, or {@code 0}
     * when there is nothing to fit. The raster is displayed at {@code screenDpi / renderDpi}, so
     * the viewport must be compared against that display size rather than the raster pixel size.
     */
    public double zoomToFit(double viewportWidth, double viewportHeight) {
        Image image = pageImage.get();
        if (image == null || viewportWidth <= 0 || viewportHeight <= 0) {
            return 0;
        }
        double scale = rasterToDisplayScale();
        double displayWidth = image.getWidth() * scale;
        double displayHeight = image.getHeight() * scale;
        if (displayWidth <= 0 || displayHeight <= 0) {
            return 0;
        }
        return Math.min(viewportWidth / displayWidth, viewportHeight / displayHeight);
    }

    /**
     * Screen DPI from JavaFX rather than AWT's {@code Toolkit}, which would initialize AWT on the FX thread
     * and can fail with an {@code AWTError} in a headless run.
     */
    private static double getScreenDpi() {
        try {
            double dpi = Screen.getPrimary().getDpi();
            return dpi > 0 ? dpi : FALLBACK_SCREEN_DPI;
        } catch (RuntimeException e) {
            return FALLBACK_SCREEN_DPI;
        }
    }

    @Override
    protected void layoutChildren() {
        imageView.relocate(0, 0);
    }

    public ObjectProperty<Image> pageImageProperty() { return pageImage; }
    public Image getPageImage() { return pageImage.get(); }
    public void setPageImage(Image image) { pageImage.set(image); }

    public DoubleProperty zoomLevelProperty() { return zoomLevel; }
    public double getZoomLevel() { return zoomLevel.get(); }
    public void setZoomLevel(double zoom) { zoomLevel.set(zoom); }

    public DoubleProperty renderDpiProperty() { return renderDpi; }
    public double getRenderDpi() { return renderDpi.get(); }
    public void setRenderDpi(double dpi) { renderDpi.set(dpi); }
}
