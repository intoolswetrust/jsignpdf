package net.sf.jsignpdf.fx.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * ViewModel for the signature placement rectangle on the PDF preview.
 * Coordinates are relative (0.0 to 1.0) to the rendered page image.
 * Conversion to PDF coordinates (LLX, LLY, URX, URY) is done during signing.
 */
public class SignaturePlacementViewModel {

    private final BooleanProperty placementMode = new SimpleBooleanProperty(false);
    private final BooleanProperty placed = new SimpleBooleanProperty(false);

    // Relative coordinates (0.0 - 1.0) on the image
    private final DoubleProperty relX = new SimpleDoubleProperty(0);
    private final DoubleProperty relY = new SimpleDoubleProperty(0);
    private final DoubleProperty relWidth = new SimpleDoubleProperty(0.15);
    private final DoubleProperty relHeight = new SimpleDoubleProperty(0.08);

    // --- Placement mode ---
    public BooleanProperty placementModeProperty() { return placementMode; }
    public boolean isPlacementMode() { return placementMode.get(); }
    public void setPlacementMode(boolean mode) { placementMode.set(mode); }

    // --- Placed ---
    public BooleanProperty placedProperty() { return placed; }
    public boolean isPlaced() { return placed.get(); }
    public void setPlaced(boolean p) { placed.set(p); }

    // --- Relative coordinates ---
    public DoubleProperty relXProperty() { return relX; }
    public double getRelX() { return relX.get(); }
    public void setRelX(double v) { relX.set(clamp(v, 0, 1)); }

    public DoubleProperty relYProperty() { return relY; }
    public double getRelY() { return relY.get(); }
    public void setRelY(double v) { relY.set(clamp(v, 0, 1)); }

    public DoubleProperty relWidthProperty() { return relWidth; }
    public double getRelWidth() { return relWidth.get(); }
    public void setRelWidth(double v) { relWidth.set(Math.max(0.02, Math.min(v, 1))); }

    public DoubleProperty relHeightProperty() { return relHeight; }
    public double getRelHeight() { return relHeight.get(); }
    public void setRelHeight(double v) { relHeight.set(Math.max(0.02, Math.min(v, 1))); }

    /**
     * Convert relative image coordinates to PDF coordinates.
     * PDF origin is bottom-left; image origin is top-left.
     *
     * @param pageWidth  PDF page width in points
     * @param pageHeight PDF page height in points
     * @return float[4] = {LLX, LLY, URX, URY}
     */
    public float[] toPdfCoordinates(float pageWidth, float pageHeight) {
        float llx = (float) (getRelX() * pageWidth);
        float urx = (float) ((getRelX() + getRelWidth()) * pageWidth);
        // PDF Y is inverted: image top=0 -> PDF top=pageHeight
        float ury = (float) ((1.0 - getRelY()) * pageHeight);
        float lly = (float) ((1.0 - getRelY() - getRelHeight()) * pageHeight);
        return new float[]{llx, lly, urx, ury};
    }

    /**
     * Set relative image coordinates from PDF coordinates.
     * PDF origin is bottom-left; image origin is top-left.
     *
     * @param llx lower-left X in PDF points
     * @param lly lower-left Y in PDF points
     * @param urx upper-right X in PDF points
     * @param ury upper-right Y in PDF points
     * @param pageWidth  PDF page width in points
     * @param pageHeight PDF page height in points
     */
    public void fromPdfCoordinates(float llx, float lly, float urx, float ury,
                                   float pageWidth, float pageHeight) {
        double rX = llx / pageWidth;
        double rW = (urx - llx) / pageWidth;
        // PDF Y is inverted: image top=0 corresponds to PDF top=pageHeight
        double rY = 1.0 - ury / pageHeight;
        double rH = (ury - lly) / pageHeight;
        setRelX(rX);
        setRelY(rY);
        setRelWidth(rW);
        setRelHeight(rH);
        setPlaced(true);
    }

    public void reset() {
        placementMode.set(false);
        placed.set(false);
        relX.set(0);
        relY.set(0);
        relWidth.set(0.15);
        relHeight.set(0.08);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
