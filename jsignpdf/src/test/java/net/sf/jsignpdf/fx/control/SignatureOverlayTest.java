package net.sf.jsignpdf.fx.control;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jsignpdf.fx.MonocleAssumption;
import net.sf.jsignpdf.fx.viewmodel.SignaturePlacementViewModel;

/**
 * The unsigned signature fields of a page are marked by JSignPdf itself, because only one of the preview
 * backends shades form fields on its own - with the others the fields would be invisible on the page.
 */
public class SignatureOverlayTest {

    private static final double OVERLAY_WIDTH = 200;
    private static final double OVERLAY_HEIGHT = 400;

    @BeforeClass
    public static void initFx() throws Exception {
        MonocleAssumption.assumeUsable();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void markersAreScaledToTheOverlaySize() throws Exception {
        List<Rectangle> markers = onFxThread(overlay -> {
            overlay.setBlankFieldMarkers(List.of(new double[] { 0.1, 0.2, 0.3, 0.4 }));
            return blankFieldRects(overlay);
        });

        assertEquals(1, markers.size());
        Rectangle marker = markers.get(0);
        assertEquals(20, marker.getX(), 0.001);
        assertEquals(80, marker.getY(), 0.001);
        assertEquals(60, marker.getWidth(), 0.001);
        assertEquals(160, marker.getHeight(), 0.001);
    }

    /**
     * A zero-size field is an invisible signature - there is nothing to mark.
     */
    @Test
    public void degenerateMarkersAreDropped() throws Exception {
        List<Rectangle> markers = onFxThread(overlay -> {
            overlay.setBlankFieldMarkers(
                    List.of(new double[] { 0.1, 0.2, 0, 0.4 }, new double[] { 0.1, 0.2, 0.3, 0.4 }));
            return blankFieldRects(overlay);
        });

        assertEquals(1, markers.size());
    }

    @Test
    public void markersOfThePreviousPageAreReplaced() throws Exception {
        List<Rectangle> markers = onFxThread(overlay -> {
            overlay.setBlankFieldMarkers(List.of(new double[] { 0.1, 0.2, 0.3, 0.4 }));
            overlay.setBlankFieldMarkers(List.of(new double[] { 0.5, 0.5, 0.1, 0.1 }));
            return blankFieldRects(overlay);
        });

        assertEquals(1, markers.size());
        assertEquals(100, markers.get(0).getX(), 0.001);
    }

    @Test
    public void clearingRemovesTheMarkers() throws Exception {
        List<Rectangle> markers = onFxThread(overlay -> {
            overlay.setBlankFieldMarkers(List.of(new double[] { 0.1, 0.2, 0.3, 0.4 }));
            overlay.clearBlankFieldMarkers();
            return blankFieldRects(overlay);
        });

        assertTrue(markers.isEmpty());
    }

    /**
     * The markers must not cover the draggable placement rectangle.
     */
    @Test
    public void markersAreDrawnUnderneathThePlacementRectangle() throws Exception {
        Integer[] indexes = onFxThread(overlay -> {
            overlay.setBlankFieldMarkers(List.of(new double[] { 0.1, 0.2, 0.3, 0.4 }));
            List<Node> children = new ArrayList<>(overlay.getChildrenUnmodifiable());
            return new Integer[] { indexOfStyleClass(children, "signature-field-blank-rect"),
                    indexOfStyleClass(children, "signature-rect") };
        });

        assertTrue(indexes[0] < indexes[1]);
    }

    private static int indexOfStyleClass(List<Node> nodes, String styleClass) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getStyleClass().contains(styleClass)) {
                return i;
            }
        }
        return -1;
    }

    private static List<Rectangle> blankFieldRects(SignatureOverlay overlay) {
        List<Rectangle> result = new ArrayList<>();
        for (Node node : overlay.getChildrenUnmodifiable()) {
            if (node.getStyleClass().contains("signature-field-blank-rect")) {
                result.add((Rectangle) node);
            }
        }
        return result;
    }

    private static <T> T onFxThread(Function<SignatureOverlay, T> action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                SignatureOverlay overlay = new SignatureOverlay(new SignaturePlacementViewModel());
                overlay.resize(OVERLAY_WIDTH, OVERLAY_HEIGHT);
                result.set(action.apply(overlay));
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
        return result.get();
    }
}
