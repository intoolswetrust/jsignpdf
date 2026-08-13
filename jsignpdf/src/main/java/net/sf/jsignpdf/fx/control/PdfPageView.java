package net.sf.jsignpdf.fx.control;

import java.awt.HeadlessException;
import java.awt.Toolkit;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
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

    public PdfPageView() {
        getChildren().add(imageView);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.imageProperty().bind(pageImage);
        pageImage.addListener((obs, oldImage, newImage) -> updateSize());
        zoomLevel.addListener((obs, oldZoom, newZoom) -> updateSize());
        getStyleClass().add("pdf-page-view");
    }

    private void updateSize() {
        Image image = pageImage.get();
        if (image == null) {
            return;
        }

        double rasterToDisplayScale = getScreenDpi() / PreviewRenderSettings.RENDER_DPI;
        double displayScale = rasterToDisplayScale * zoomLevel.get();
        double width = image.getWidth() * displayScale;
        double height = image.getHeight() * displayScale;

        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
    }

    private static double getScreenDpi() {
        try {
            return Toolkit.getDefaultToolkit().getScreenResolution();
        } catch (HeadlessException e) {
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
}
