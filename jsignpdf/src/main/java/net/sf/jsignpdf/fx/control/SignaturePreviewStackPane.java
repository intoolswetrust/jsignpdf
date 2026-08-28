package net.sf.jsignpdf.fx.control;

import javafx.beans.InvalidationListener;
import javafx.scene.layout.StackPane;
import net.sf.jsignpdf.fx.viewmodel.SignaturePlacementViewModel;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;

/**
 * PDF area that adds the live signature preview as a sibling underneath the
 * original {@link SignatureOverlay}. The overlay itself remains completely
 * untouched, so all interactive mouse/cursor/move/resize behaviour stays owned
 * by JSignPdf.
 *
 * <p>The preview follows the interactive rectangle purely through observables -
 * the placement view model plus the overlay's own layout properties - so there
 * is no polling {@code AnimationTimer}: the preview repaints only when the
 * rectangle, the page layout, or an appearance value actually changes.</p>
 */
public final class SignaturePreviewStackPane extends StackPane {

    private SignaturePreviewPane preview;
    private SignatureOverlay overlay;
    private SignaturePlacementViewModel placementVM;

    public SignaturePreviewStackPane() {
        super();
    }

    /**
     * Wires the preview to the given view models and interactive overlay. Must be
     * called once, on the FX thread, after {@code overlay} has been added to this
     * pane. The preview is inserted directly underneath the overlay and tracks it
     * via observables from then on.
     */
    public void installPreview(SigningOptionsViewModel signingVM,
                               SignaturePlacementViewModel placementVM,
                               SignatureOverlay overlay) {
        this.placementVM = placementVM;
        this.overlay = overlay;

        preview = new SignaturePreviewPane(signingVM);
        preview.setManaged(false);
        preview.setMouseTransparent(true);
        int overlayIndex = Math.max(0, getChildren().indexOf(overlay));
        getChildren().add(overlayIndex, preview);

        InvalidationListener geometry = o -> updateGeometry();
        placementVM.placedProperty().addListener(geometry);
        placementVM.relXProperty().addListener(geometry);
        placementVM.relYProperty().addListener(geometry);
        placementVM.relWidthProperty().addListener(geometry);
        placementVM.relHeightProperty().addListener(geometry);
        // Centering inside the StackPane and page resizes move the overlay without
        // changing the relative rectangle, so track the overlay's own layout too.
        overlay.layoutXProperty().addListener(geometry);
        overlay.layoutYProperty().addListener(geometry);
        overlay.widthProperty().addListener(geometry);
        overlay.heightProperty().addListener(geometry);
        overlay.visibleProperty().addListener(geometry);

        updateGeometry();
    }

    private void updateGeometry() {
        if (preview == null) return;
        if (!overlay.isVisible() || !placementVM.isPlaced()) {
            preview.setVisible(false);
            return;
        }
        double ow = overlay.getWidth();
        double oh = overlay.getHeight();
        if (ow <= 0.0 || oh <= 0.0) {
            preview.setVisible(false);
            return;
        }
        double w = placementVM.getRelWidth() * ow;
        double h = placementVM.getRelHeight() * oh;
        if (w <= 0.0 || h <= 0.0) {
            preview.setVisible(false);
            return;
        }
        // The overlay draws its rectangle at (relX*width, relY*height) in its own
        // coordinate space; the overlay's layoutX/Y place that space inside this
        // StackPane, so the same offsets position the preview on top of it.
        double x = overlay.getLayoutX() + placementVM.getRelX() * ow;
        double y = overlay.getLayoutY() + placementVM.getRelY() * oh;
        preview.setVisible(true);
        preview.updateBounds(x, y, w, h);
    }
}
