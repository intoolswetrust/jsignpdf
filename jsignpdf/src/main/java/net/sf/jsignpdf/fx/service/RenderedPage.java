package net.sf.jsignpdf.fx.service;

import javafx.scene.image.Image;

/**
 * A rendered preview page together with the DPI it was actually rasterized at, so the view can
 * scale it to the correct on-screen size even when a large page was capped below the target DPI.
 */
public record RenderedPage(Image image, double dpi) {
}
