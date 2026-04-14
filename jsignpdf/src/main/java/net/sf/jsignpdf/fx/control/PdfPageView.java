package net.sf.jsignpdf.fx.control;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

/**
 * Custom Region that displays a rendered PDF page with zoom support.
 * The image is scaled by the zoomLevel property.
 */
public class PdfPageView extends Region {

    private final ImageView imageView = new ImageView();
    private final ObjectProperty<Image> pageImage = new SimpleObjectProperty<>();
    private final DoubleProperty zoomLevel = new SimpleDoubleProperty(1.0);

    public PdfPageView() {
        getChildren().add(imageView);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // Bind image
        imageView.imageProperty().bind(pageImage);

        // Update size when image or zoom changes
        pageImage.addListener((obs, o, n) -> updateSize());
        zoomLevel.addListener((obs, o, n) -> updateSize());

        getStyleClass().add("pdf-page-view");
    }

    private void updateSize() {
        Image img = pageImage.get();
        if (img != null) {
            double zoom = zoomLevel.get();
            double w = img.getWidth() * zoom;
            double h = img.getHeight() * zoom;
            imageView.setFitWidth(w);
            imageView.setFitHeight(h);
            setPrefSize(w, h);
            setMinSize(w, h);
            setMaxSize(w, h);
        }
    }

    @Override
    protected void layoutChildren() {
        imageView.relocate(0, 0);
    }

    // --- Properties ---
    public ObjectProperty<Image> pageImageProperty() { return pageImage; }
    public Image getPageImage() { return pageImage.get(); }
    public void setPageImage(Image image) { pageImage.set(image); }

    public DoubleProperty zoomLevelProperty() { return zoomLevel; }
    public double getZoomLevel() { return zoomLevel.get(); }
    public void setZoomLevel(double zoom) { zoomLevel.set(zoom); }
}
