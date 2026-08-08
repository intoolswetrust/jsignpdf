package net.sf.jsignpdf.fx.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.sf.jsignpdf.Constants;

/**
 * Tests for {@link VisibleSignatureCoordinator} — the placement↔signing view-model sync used by the preset save/load flows
 * and the sign/close flows.
 */
public class VisibleSignatureCoordinatorTest {

    private static final float PAGE_W = 600f;
    private static final float PAGE_H = 800f;
    private static final float EPS = 0.01f;

    private static SignaturePlacementViewModel placed(double relX, double relY, double relW, double relH) {
        SignaturePlacementViewModel pvm = new SignaturePlacementViewModel();
        pvm.setRelX(relX);
        pvm.setRelY(relY);
        pvm.setRelWidth(relW);
        pvm.setRelHeight(relH);
        pvm.setPlaced(true);
        return pvm;
    }

    // ---- pushPlacementToSigning ----

    @Test
    public void pushPlacement_writesPdfCoordsAndVisibleFlag() {
        SignaturePlacementViewModel pvm = placed(0.2, 0.2, 0.4, 0.3);
        SigningOptionsViewModel svm = new SigningOptionsViewModel();

        VisibleSignatureCoordinator.pushPlacementToSigning(pvm, svm, 3, PAGE_W, PAGE_H);

        assertTrue("visible must be set true", svm.visibleProperty().get());
        assertEquals("page set", 3, svm.pageProperty().get());
        // Spot-check that the VM values came from placementVM.toPdfCoordinates (authoritative), and match it exactly.
        float[] expected = pvm.toPdfCoordinates(PAGE_W, PAGE_H);
        assertEquals(expected[0], svm.positionLLXProperty().get(), EPS);
        assertEquals(expected[1], svm.positionLLYProperty().get(), EPS);
        assertEquals(expected[2], svm.positionURXProperty().get(), EPS);
        assertEquals(expected[3], svm.positionURYProperty().get(), EPS);
    }

    @Test
    public void pushPlacement_noOp_whenNotPlaced() {
        SignaturePlacementViewModel pvm = new SignaturePlacementViewModel();
        // placed=false by default
        SigningOptionsViewModel svm = new SigningOptionsViewModel();
        svm.positionLLXProperty().set(42f);

        VisibleSignatureCoordinator.pushPlacementToSigning(pvm, svm, 5, PAGE_W, PAGE_H);

        assertFalse("visible must stay false", svm.visibleProperty().get());
        assertEquals("pre-existing LLX must not be overwritten", 42f, svm.positionLLXProperty().get(), EPS);
    }

    // ---- pushSigningToPlacement ----

    @Test
    public void pushSigning_writesRelativeRect() {
        SigningOptionsViewModel svm = new SigningOptionsViewModel();
        svm.visibleProperty().set(true);
        // A 60×80 rectangle at (120, 560). Page is 600×800 → rel (0.2, 0.25, 0.1, 0.1).
        svm.positionLLXProperty().set(120f);
        svm.positionURXProperty().set(180f);
        svm.positionLLYProperty().set(560f);
        svm.positionURYProperty().set(640f);
        SignaturePlacementViewModel pvm = new SignaturePlacementViewModel();

        VisibleSignatureCoordinator.pushSigningToPlacement(svm, pvm, PAGE_W, PAGE_H);

        assertTrue("placed must be true after sync", pvm.isPlaced());
        assertEquals(0.2, pvm.getRelX(), 0.005);
        // LLY 560 → relY = 1 - 640/800 = 0.2 (top-origin)
        assertEquals(0.2, pvm.getRelY(), 0.005);
        assertEquals(0.1, pvm.getRelWidth(), 0.005);
        assertEquals(0.1, pvm.getRelHeight(), 0.005);
    }

    @Test
    public void pushSigning_noOp_whenVisibleFalse() {
        SigningOptionsViewModel svm = new SigningOptionsViewModel();
        svm.visibleProperty().set(false);
        svm.positionLLXProperty().set(100f);
        svm.positionURXProperty().set(200f);
        svm.positionLLYProperty().set(100f);
        svm.positionURYProperty().set(200f);
        SignaturePlacementViewModel pvm = new SignaturePlacementViewModel();

        VisibleSignatureCoordinator.pushSigningToPlacement(svm, pvm, PAGE_W, PAGE_H);

        assertFalse("visible=false ⇒ placement must stay untouched", pvm.isPlaced());
    }

    @Test
    public void pushSigning_noOp_whenCoordsAreOutOfPage() {
        SigningOptionsViewModel svm = new SigningOptionsViewModel();
        svm.visibleProperty().set(true);
        // URX beyond page width: invalid, must not corrupt placement.
        svm.positionLLXProperty().set(100f);
        svm.positionURXProperty().set(PAGE_W + 50f);
        svm.positionLLYProperty().set(100f);
        svm.positionURYProperty().set(200f);
        SignaturePlacementViewModel pvm = new SignaturePlacementViewModel();

        VisibleSignatureCoordinator.pushSigningToPlacement(svm, pvm, PAGE_W, PAGE_H);

        assertFalse(pvm.isPlaced());
    }

    @Test
    public void pushSigning_noOp_whenCoordsAreDegenerate() {
        SigningOptionsViewModel svm = new SigningOptionsViewModel();
        svm.visibleProperty().set(true);
        // Zero-width rect (defaults): must not be applied.
        svm.positionLLXProperty().set(0f);
        svm.positionURXProperty().set(0f);
        svm.positionLLYProperty().set(0f);
        svm.positionURYProperty().set(0f);
        SignaturePlacementViewModel pvm = new SignaturePlacementViewModel();

        VisibleSignatureCoordinator.pushSigningToPlacement(svm, pvm, PAGE_W, PAGE_H);

        assertFalse(pvm.isPlaced());
    }

    /**
     * The default rectangle fits any page, but it is what a fresh profile carries and what selecting an existing
     * signature field writes back - restoring it would drop a 100×100 box in the lower-left corner of the page.
     */
    @Test
    public void pushSigning_noOp_whenCoordsAreTheUntouchedDefaults() {
        SigningOptionsViewModel svm = new SigningOptionsViewModel();
        svm.visibleProperty().set(true);
        svm.positionLLXProperty().set(Constants.DEFVAL_LLX);
        svm.positionLLYProperty().set(Constants.DEFVAL_LLY);
        svm.positionURXProperty().set(Constants.DEFVAL_URX);
        svm.positionURYProperty().set(Constants.DEFVAL_URY);
        SignaturePlacementViewModel pvm = new SignaturePlacementViewModel();

        VisibleSignatureCoordinator.pushSigningToPlacement(svm, pvm, PAGE_W, PAGE_H);

        assertFalse("the default rectangle means 'no position chosen yet'", pvm.isPlaced());
    }

    // ---- hasUsablePosition ----

    @Test
    public void hasUsablePosition_acceptsARectangleInsideThePage() {
        assertTrue(VisibleSignatureCoordinator.hasUsablePosition(120f, 560f, 180f, 640f, PAGE_W, PAGE_H));
    }

    @Test
    public void hasUsablePosition_rejectsTheDefaultRectangle() {
        assertFalse(VisibleSignatureCoordinator.hasUsablePosition(Constants.DEFVAL_LLX, Constants.DEFVAL_LLY,
                Constants.DEFVAL_URX, Constants.DEFVAL_URY, PAGE_W, PAGE_H));
    }

    /** Only the untouched defaults are special - the same box moved anywhere else is a real user choice. */
    @Test
    public void hasUsablePosition_acceptsTheDefaultSizedRectangleElsewhere() {
        assertTrue(VisibleSignatureCoordinator.hasUsablePosition(Constants.DEFVAL_LLX, Constants.DEFVAL_LLY + 10f,
                Constants.DEFVAL_URX, Constants.DEFVAL_URY + 10f, PAGE_W, PAGE_H));
    }

    @Test
    public void hasUsablePosition_rejectsDegenerateAndOutOfPageRectangles() {
        assertFalse("zero width", VisibleSignatureCoordinator.hasUsablePosition(10f, 10f, 10f, 80f, PAGE_W, PAGE_H));
        assertFalse("beyond the page", VisibleSignatureCoordinator.hasUsablePosition(10f, 10f, PAGE_W + 1f, 80f,
                PAGE_W, PAGE_H));
        assertFalse("negative origin", VisibleSignatureCoordinator.hasUsablePosition(-5f, 10f, 80f, 80f,
                PAGE_W, PAGE_H));
    }

    @Test
    public void pushSigning_replacesExistingPlacement() {
        // Load-preset semantics: even if a rectangle is already placed, it gets replaced with the preset's coords.
        SigningOptionsViewModel svm = new SigningOptionsViewModel();
        svm.visibleProperty().set(true);
        svm.positionLLXProperty().set(60f);
        svm.positionURXProperty().set(240f);
        svm.positionLLYProperty().set(400f);
        svm.positionURYProperty().set(560f);
        // 240-60=180 / 600 = 0.3 relW; (800-560)/800 = 0.3 relY; 60/600=0.1 relX; (560-400)/800 = 0.2 relH

        SignaturePlacementViewModel pvm = placed(0.0, 0.0, 0.05, 0.05);

        VisibleSignatureCoordinator.pushSigningToPlacement(svm, pvm, PAGE_W, PAGE_H);

        assertTrue(pvm.isPlaced());
        assertEquals(0.1, pvm.getRelX(), 0.005);
        assertEquals(0.3, pvm.getRelY(), 0.005);
        assertEquals(0.3, pvm.getRelWidth(), 0.005);
        assertEquals(0.2, pvm.getRelHeight(), 0.005);
    }

    // ---- Round trip: save → load should restore the same rectangle ----

    @Test
    public void roundTrip_placementPreservedThroughSigningVM() {
        SignaturePlacementViewModel src = placed(0.15, 0.22, 0.35, 0.28);
        SigningOptionsViewModel svm = new SigningOptionsViewModel();
        VisibleSignatureCoordinator.pushPlacementToSigning(src, svm, 1, PAGE_W, PAGE_H);

        SignaturePlacementViewModel dst = new SignaturePlacementViewModel();
        VisibleSignatureCoordinator.pushSigningToPlacement(svm, dst, PAGE_W, PAGE_H);

        assertTrue(dst.isPlaced());
        assertEquals(src.getRelX(), dst.getRelX(), 0.01);
        assertEquals(src.getRelY(), dst.getRelY(), 0.01);
        assertEquals(src.getRelWidth(), dst.getRelWidth(), 0.01);
        assertEquals(src.getRelHeight(), dst.getRelHeight(), 0.01);
    }
}
