package net.sf.jsignpdf.fx.viewmodel;

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
     * No-op if the signing VM has {@code visible=false} or the coordinates do not describe a meaningful rectangle
     * (non-positive dimensions or out-of-page bounds). The latter guard means callers can pass stale default coordinates
     * without corrupting the placement — the user just has to place the rectangle themselves.
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
        boolean fits = urx - llx > 1f && ury - lly > 1f
                && llx >= 0f && lly >= 0f
                && urx <= pageWidth && ury <= pageHeight;
        if (!fits) {
            return;
        }
        placementVM.fromPdfCoordinates(llx, lly, urx, ury, pageWidth, pageHeight);
    }
}
