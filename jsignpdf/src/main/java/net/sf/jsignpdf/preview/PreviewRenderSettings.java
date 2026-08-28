package net.sf.jsignpdf.preview;

import net.sf.jsignpdf.utils.AppConfig;

/** Shared rendering constants for the PDF preview. */
public final class PreviewRenderSettings {
    /** PDF user-space unit: 72 points per inch, i.e. the 1.0 scaling baseline of the renderers. */
    public static final int PDF_POINTS_PER_INCH = 72;

    /** Lower bound on the configurable target DPI. */
    public static final int MIN_RENDER_DPI = 72;
    /** Upper bound on the configurable target DPI. */
    public static final int MAX_RENDER_DPI = 600;

    /** Hard ceiling on the preview raster size, to avoid OOM on very large (A0 / CAD) pages. */
    public static final long MAX_PREVIEW_PIXELS = 24_000_000L;

    private PreviewRenderSettings() {
    }

    /** Configured target DPI, clamped to a sane range. */
    public static int targetRenderDpi() {
        return Math.max(MIN_RENDER_DPI, Math.min(MAX_RENDER_DPI, AppConfig.previewRenderDpi()));
    }

    /**
     * Effective render DPI for a page of the given size in PDF points: the configured target,
     * lowered so the resulting raster stays within {@link #MAX_PREVIEW_PIXELS}.
     */
    public static double effectiveRenderDpi(double widthPoints, double heightPoints) {
        double target = targetRenderDpi();
        double widthInch = widthPoints / PDF_POINTS_PER_INCH;
        double heightInch = heightPoints / PDF_POINTS_PER_INCH;
        double areaInch = widthInch * heightInch;
        if (areaInch <= 0) {
            return target;
        }
        double cappedDpi = Math.sqrt(MAX_PREVIEW_PIXELS / areaInch);
        return Math.max(1.0, Math.min(target, cappedDpi));
    }
}
