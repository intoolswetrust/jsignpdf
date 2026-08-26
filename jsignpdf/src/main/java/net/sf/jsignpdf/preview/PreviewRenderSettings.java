package net.sf.jsignpdf.preview;

/** Shared rendering constants for the PDF preview. */
public final class PreviewRenderSettings {
    /** Raster resolution used to create the high-detail PDF preview. */
    public static final int RENDER_DPI = 300;

    /** PDF user-space unit: 72 points per inch, i.e. the 1.0 scaling baseline of the renderers. */
    public static final int PDF_POINTS_PER_INCH = 72;

    /** Scaling factor that lifts a renderer's 72-DPI baseline up to {@link #RENDER_DPI}. */
    public static final float RENDER_SCALE = (float) RENDER_DPI / PDF_POINTS_PER_INCH;

    private PreviewRenderSettings() {
    }
}
