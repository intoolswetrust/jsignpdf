package net.sf.jsignpdf.fx.viewmodel;

import net.sf.jsignpdf.Constants;

/**
 * Static helpers for moving the visible-signature rectangle between the placement overlay (relative page coords in
 * {@link SignaturePlacementViewModel}) and the signing configuration (PDF coords in {@link SigningOptionsViewModel}).
 * <p>
 * The callers in {@code MainWindowController} need to do this sync at several moments (window close, signing, save
 * preset, load preset). Centralising it here avoids three-way drift and makes the coordinate conversion easy to test
 * without a real PDF document.
 */
public final class VisibleSignatureCoordinator {

    private VisibleSignatureCoordinator() {
    }

    /**
     * Captures the placement rectangle as PDF coordinates and stores them on the signing view model, along with
     * {@code visible=true} and the active page number. No-op if no rectangle is placed.
     *
     * @param placementVM source of the rectangle (in relative 0..1 page coords)
     * @param signingVM target view model receiving PDF coords + visibility flag
     * @param page the current (1-based) page number the signature is placed on
     * @param pageWidth width of that page in PDF units
     * @param pageHeight height of that page in PDF units
     */
    public static void pushPlacementToSigning(SignaturePlacementViewModel placementVM,
                                              SigningOptionsViewModel signingVM,
                                              int page, float pageWidth, float pageHeight) {
        if (!placementVM.isPlaced()) {
            return;
        }
        signingVM.visibleProperty().set(true);
        signingVM.pageProperty().set(page);
        float[] coords = placementVM.toPdfCoordinates(pageWidth, pageHeight);
        signingVM.positionLLXProperty().set(coords[0]);
        signingVM.positionLLYProperty().set(coords[1]);
        signingVM.positionURXProperty().set(coords[2]);
        signingVM.positionURYProperty().set(coords[3]);
    }

    /**
     * Applies the signing view model's PDF coordinates to the placement overlay (converting to relative page coords).
     * Intended for "preset just loaded — move the rectangle to match" flows; unlike
     * {@code MainWindowController#autoPlaceVisibleSignature()}, this always replaces the existing placement rather than
     * bailing out when one is already present.
     * <p>
     * No-op if the signing VM has {@code visible=false} or {@link #hasUsablePosition} rejects the coordinates. The
     * latter guard means callers can pass stale default coordinates without corrupting the placement — the user just
     * has to place the rectangle themselves.
     */
    public static void pushSigningToPlacement(SigningOptionsViewModel signingVM,
                                              SignaturePlacementViewModel placementVM,
                                              float pageWidth, float pageHeight) {
        if (!signingVM.visibleProperty().get()) {
            return;
        }
        float llx = signingVM.positionLLXProperty().get();
        float lly = signingVM.positionLLYProperty().get();
        float urx = signingVM.positionURXProperty().get();
        float ury = signingVM.positionURYProperty().get();
        if (!hasUsablePosition(llx, lly, urx, ury, pageWidth, pageHeight)) {
            return;
        }
        placementVM.fromPdfCoordinates(llx, lly, urx, ury, pageWidth, pageHeight);
    }

    /**
     * Decides whether the given PDF coordinates are a position worth restoring on screen, i.e. a rectangle with
     * positive dimensions that sits inside the page.
     * <p>
     * The untouched default rectangle ({@code DEFVAL_LLX..DEFVAL_URY}) is rejected even though it technically fits:
     * it is what a fresh profile starts with, and what selecting an existing signature field writes back, so it means
     * "no position chosen yet" rather than "put a 100×100 box in the lower-left corner". Callers fall back to their
     * own default placement instead.
     *
     * @param llx lower-left X in PDF points
     * @param lly lower-left Y in PDF points
     * @param urx upper-right X in PDF points
     * @param ury upper-right Y in PDF points
     * @param pageWidth width of the target page in PDF units
     * @param pageHeight height of the target page in PDF units
     * @return true when the rectangle can be shown as-is
     */
    public static boolean hasUsablePosition(float llx, float lly, float urx, float ury,
                                            float pageWidth, float pageHeight) {
        if (llx == Constants.DEFVAL_LLX && lly == Constants.DEFVAL_LLY
                && urx == Constants.DEFVAL_URX && ury == Constants.DEFVAL_URY) {
            return false;
        }
        return urx - llx > 1f && ury - lly > 1f
                && llx >= 0f && lly >= 0f
                && urx <= pageWidth && ury <= pageHeight;
    }
}
