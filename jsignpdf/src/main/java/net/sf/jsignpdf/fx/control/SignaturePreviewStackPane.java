package net.sf.jsignpdf.fx.control;

import javafx.animation.AnimationTimer;
import javafx.scene.Node;
import javafx.geometry.Point2D;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * PDF area that adds the live signature preview as a sibling underneath the
 * original SignatureOverlay. The overlay itself remains completely untouched,
 * so all original mouse/cursor/move/resize behavior stays owned by JSignPdf.
 */
public final class SignaturePreviewStackPane extends StackPane {
    private SignatureOverlay overlay;
    private Rectangle signatureRect;
    private SignaturePreviewPane preview;

    private double lastX = Double.NaN;
    private double lastY = Double.NaN;
    private double lastW = Double.NaN;
    private double lastH = Double.NaN;
    private boolean lastVisible;
    private long lastRefreshNanos;

    public SignaturePreviewStackPane() {
        super();
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                syncPreview(now);
            }
        }.start();
    }

    private void syncPreview(long now) {
        if (!resolveOverlayAndRect()) {
            if (preview != null) preview.setVisible(false);
            return;
        }

        ensurePreviewBelowOverlay();

        boolean visible = signatureRect.isVisible();
        double rectX = signatureRect.getX();
        double rectY = signatureRect.getY();
        double rectW = signatureRect.getWidth();
        double rectH = signatureRect.getHeight();

        if (!visible || rectW <= 0.0 || rectH <= 0.0) {
            if (lastVisible) preview.setVisible(false);
            lastVisible = false;
            return;
        }

        // The rectangle coordinates are local to SignatureOverlay. After the
        // window/ScrollPane is resized, the overlay can be re-laid out even
        // though the rectangle's own x/y/width/height do not change. Convert
        // both rectangle corners through scene coordinates into this sibling
        // StackPane so the visual preview follows the interactive rectangle.
        Point2D topLeftScene = signatureRect.localToScene(rectX, rectY);
        Point2D bottomRightScene = signatureRect.localToScene(rectX + rectW, rectY + rectH);
        if (topLeftScene == null || bottomRightScene == null) {
            if (lastVisible) preview.setVisible(false);
            lastVisible = false;
            return;
        }
        Point2D topLeft = sceneToLocal(topLeftScene);
        Point2D bottomRight = sceneToLocal(bottomRightScene);
        if (topLeft == null || bottomRight == null) {
            if (lastVisible) preview.setVisible(false);
            lastVisible = false;
            return;
        }

        double x = Math.min(topLeft.getX(), bottomRight.getX());
        double y = Math.min(topLeft.getY(), bottomRight.getY());
        double w = Math.abs(bottomRight.getX() - topLeft.getX());
        double h = Math.abs(bottomRight.getY() - topLeft.getY());

        if (w <= 0.0 || h <= 0.0) {
            if (lastVisible) preview.setVisible(false);
            lastVisible = false;
            return;
        }

        boolean geometryChanged = x != lastX || y != lastY || w != lastW || h != lastH;
        if (!lastVisible) {
            preview.setVisible(true);
            geometryChanged = true;
        }
        if (geometryChanged) {
            preview.updateBounds(x, y, w, h);
            lastX = x;
            lastY = y;
            lastW = w;
            lastH = h;
        }

        // Appearance controls can change without geometry changing. Refresh at a
        // modest rate; this is visual-only and never participates in mouse picking.
        if (geometryChanged || now - lastRefreshNanos >= 150_000_000L) {
            preview.refresh();
            lastRefreshNanos = now;
        }
        lastVisible = true;
    }

    private boolean resolveOverlayAndRect() {
        if (overlay == null || !getChildren().contains(overlay)) {
            overlay = null;
            signatureRect = null;
            for (Node node : getChildren()) {
                if (node instanceof SignatureOverlay) {
                    overlay = (SignatureOverlay) node;
                    break;
                }
            }
        }
        if (overlay == null) return false;

        if (signatureRect == null) {
            for (Node node : overlay.getChildren()) {
                if (node instanceof Rectangle
                        && node.getStyleClass().contains("signature-rect")) {
                    signatureRect = (Rectangle) node;
                    break;
                }
            }
        }
        return signatureRect != null;
    }

    private void ensurePreviewBelowOverlay() {
        if (preview == null) {
            preview = new SignaturePreviewPane();
            preview.setManaged(false);
            preview.setMouseTransparent(true);
        }

        int overlayIndex = getChildren().indexOf(overlay);
        int previewIndex = getChildren().indexOf(preview);
        int desiredIndex = Math.max(0, overlayIndex);

        if (previewIndex < 0) {
            getChildren().add(desiredIndex, preview);
        } else if (previewIndex != desiredIndex - 1 && previewIndex != desiredIndex) {
            // Normally never needed; keeps the preview directly underneath overlay
            // if other children are added dynamically.
            getChildren().remove(preview);
            overlayIndex = getChildren().indexOf(overlay);
            getChildren().add(Math.max(0, overlayIndex), preview);
        }
    }
}
