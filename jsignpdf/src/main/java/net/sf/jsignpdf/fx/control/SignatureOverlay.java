package net.sf.jsignpdf.fx.control;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import net.sf.jsignpdf.fx.viewmodel.SignaturePlacementViewModel;

/**
 * Transparent overlay pane for click-drag signature rectangle placement
 * on top of the PDF page view.
 */
public class SignatureOverlay extends Pane {

    private static final double HANDLE_SIZE = 8;

    private final SignaturePlacementViewModel viewModel;
    private final Rectangle sigRect = new Rectangle();
    private final Rectangle[] handles = new Rectangle[4]; // corners: TL, TR, BL, BR

    // Drag state
    private double dragStartX, dragStartY;
    private double dragStartRelX, dragStartRelY, dragStartRelW, dragStartRelH;
    private DragMode dragMode = DragMode.NONE;

    private enum DragMode { NONE, CREATE, MOVE, RESIZE_TL, RESIZE_TR, RESIZE_BL, RESIZE_BR }

    public SignatureOverlay(SignaturePlacementViewModel viewModel) {
        this.viewModel = viewModel;
        setPickOnBounds(true);

        // Setup signature rectangle
        sigRect.getStyleClass().add("signature-rect");
        sigRect.setVisible(false);
        getChildren().add(sigRect);

        // Setup corner handles
        for (int i = 0; i < 4; i++) {
            handles[i] = new Rectangle(HANDLE_SIZE, HANDLE_SIZE);
            handles[i].getStyleClass().add("signature-handle");
            handles[i].setVisible(false);
            getChildren().add(handles[i]);
        }

        // Mouse handlers
        setOnMousePressed(this::onMousePressed);
        setOnMouseDragged(this::onMouseDragged);
        setOnMouseReleased(this::onMouseReleased);
        setOnMouseMoved(this::onMouseMoved);

        // Bind visibility to placed state
        viewModel.placedProperty().addListener((obs, o, n) -> {
            sigRect.setVisible(n);
            for (Rectangle h : handles) h.setVisible(n);
            if (!n) setCursor(Cursor.DEFAULT);
        });

        // Bind rectangle position/size to ViewModel
        viewModel.relXProperty().addListener((obs, o, n) -> updateRectPosition());
        viewModel.relYProperty().addListener((obs, o, n) -> updateRectPosition());
        viewModel.relWidthProperty().addListener((obs, o, n) -> updateRectPosition());
        viewModel.relHeightProperty().addListener((obs, o, n) -> updateRectPosition());

        // Update when overlay size changes
        widthProperty().addListener((obs, o, n) -> updateRectPosition());
        heightProperty().addListener((obs, o, n) -> updateRectPosition());
    }

    private void updateRectPosition() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        double rx = viewModel.getRelX() * w;
        double ry = viewModel.getRelY() * h;
        double rw = viewModel.getRelWidth() * w;
        double rh = viewModel.getRelHeight() * h;

        sigRect.setX(rx);
        sigRect.setY(ry);
        sigRect.setWidth(rw);
        sigRect.setHeight(rh);

        // TL
        handles[0].setX(rx - HANDLE_SIZE / 2);
        handles[0].setY(ry - HANDLE_SIZE / 2);
        // TR
        handles[1].setX(rx + rw - HANDLE_SIZE / 2);
        handles[1].setY(ry - HANDLE_SIZE / 2);
        // BL
        handles[2].setX(rx - HANDLE_SIZE / 2);
        handles[2].setY(ry + rh - HANDLE_SIZE / 2);
        // BR
        handles[3].setX(rx + rw - HANDLE_SIZE / 2);
        handles[3].setY(ry + rh - HANDLE_SIZE / 2);
    }

    private void onMousePressed(MouseEvent e) {
        double mx = e.getX();
        double my = e.getY();
        double w = getWidth();
        double h = getHeight();

        dragStartX = mx;
        dragStartY = my;
        dragStartRelX = viewModel.getRelX();
        dragStartRelY = viewModel.getRelY();
        dragStartRelW = viewModel.getRelWidth();
        dragStartRelH = viewModel.getRelHeight();

        if (viewModel.isPlaced()) {
            // Check if clicking on a handle
            DragMode handleMode = getHandleAt(mx, my);
            if (handleMode != DragMode.NONE) {
                dragMode = handleMode;
                e.consume();
                return;
            }
            // Check if clicking inside the rectangle (move)
            double rx = viewModel.getRelX() * w;
            double ry = viewModel.getRelY() * h;
            double rw = viewModel.getRelWidth() * w;
            double rh = viewModel.getRelHeight() * h;
            if (mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh) {
                dragMode = DragMode.MOVE;
                e.consume();
                return;
            }
        }

        // Require Shift to replace an existing rectangle
        if (viewModel.isPlaced() && !e.isShiftDown()) {
            dragMode = DragMode.NONE;
            e.consume();
            return;
        }

        // Start creating a new rectangle
        dragMode = DragMode.CREATE;
        viewModel.setRelX(mx / w);
        viewModel.setRelY(my / h);
        viewModel.setRelWidth(0.02);
        viewModel.setRelHeight(0.02);
        // Update drag start values to match the new rectangle
        dragStartRelW = 0.02;
        dragStartRelH = 0.02;
        viewModel.setPlaced(true);
        e.consume();
    }

    private void onMouseDragged(MouseEvent e) {
        if (dragMode == DragMode.NONE) return;

        double mx = e.getX();
        double my = e.getY();
        double w = getWidth();
        double h = getHeight();
        double dx = (mx - dragStartX) / w;
        double dy = (my - dragStartY) / h;

        switch (dragMode) {
            case CREATE:
                // Normalize origin and extent so dragging in any direction works
                double endRelX = mx / w;
                double endRelY = my / h;
                double originX = Math.min(dragStartX / w, endRelX);
                double originY = Math.min(dragStartY / h, endRelY);
                double extentW = Math.abs(endRelX - dragStartX / w);
                double extentH = Math.abs(endRelY - dragStartY / h);
                viewModel.setRelX(originX);
                viewModel.setRelY(originY);
                viewModel.setRelWidth(Math.max(0.02, extentW));
                viewModel.setRelHeight(Math.max(0.02, extentH));
                break;
            case RESIZE_BR:
                viewModel.setRelWidth(Math.max(0.02, dragStartRelW + dx));
                viewModel.setRelHeight(Math.max(0.02, dragStartRelH + dy));
                break;
            case MOVE:
                viewModel.setRelX(clamp(dragStartRelX + dx, 0, 1 - viewModel.getRelWidth()));
                viewModel.setRelY(clamp(dragStartRelY + dy, 0, 1 - viewModel.getRelHeight()));
                break;
            case RESIZE_TL:
                viewModel.setRelX(clamp(dragStartRelX + dx, 0, dragStartRelX + dragStartRelW - 0.02));
                viewModel.setRelY(clamp(dragStartRelY + dy, 0, dragStartRelY + dragStartRelH - 0.02));
                viewModel.setRelWidth(dragStartRelW - (viewModel.getRelX() - dragStartRelX));
                viewModel.setRelHeight(dragStartRelH - (viewModel.getRelY() - dragStartRelY));
                break;
            case RESIZE_TR:
                viewModel.setRelWidth(Math.max(0.02, dragStartRelW + dx));
                viewModel.setRelY(clamp(dragStartRelY + dy, 0, dragStartRelY + dragStartRelH - 0.02));
                viewModel.setRelHeight(dragStartRelH - (viewModel.getRelY() - dragStartRelY));
                break;
            case RESIZE_BL:
                viewModel.setRelX(clamp(dragStartRelX + dx, 0, dragStartRelX + dragStartRelW - 0.02));
                viewModel.setRelWidth(dragStartRelW - (viewModel.getRelX() - dragStartRelX));
                viewModel.setRelHeight(Math.max(0.02, dragStartRelH + dy));
                break;
            default:
                break;
        }
        e.consume();
    }

    private void onMouseReleased(MouseEvent e) {
        dragMode = DragMode.NONE;
        e.consume();
    }

    private void onMouseMoved(MouseEvent e) {
        if (!viewModel.isPlaced()) {
            setCursor(Cursor.CROSSHAIR);
            return;
        }

        DragMode handleMode = getHandleAt(e.getX(), e.getY());
        switch (handleMode) {
            case RESIZE_TL:
            case RESIZE_BR:
                setCursor(Cursor.NW_RESIZE);
                break;
            case RESIZE_TR:
            case RESIZE_BL:
                setCursor(Cursor.NE_RESIZE);
                break;
            default:
                double w = getWidth();
                double h = getHeight();
                double rx = viewModel.getRelX() * w;
                double ry = viewModel.getRelY() * h;
                double rw = viewModel.getRelWidth() * w;
                double rh = viewModel.getRelHeight() * h;
                if (e.getX() >= rx && e.getX() <= rx + rw && e.getY() >= ry && e.getY() <= ry + rh) {
                    setCursor(Cursor.MOVE);
                } else {
                    setCursor(Cursor.CROSSHAIR);
                }
                break;
        }
    }

    private DragMode getHandleAt(double mx, double my) {
        if (!viewModel.isPlaced()) return DragMode.NONE;
        double tolerance = HANDLE_SIZE;
        double w = getWidth();
        double h = getHeight();
        double rx = viewModel.getRelX() * w;
        double ry = viewModel.getRelY() * h;
        double rw = viewModel.getRelWidth() * w;
        double rh = viewModel.getRelHeight() * h;

        if (near(mx, my, rx, ry, tolerance)) return DragMode.RESIZE_TL;
        if (near(mx, my, rx + rw, ry, tolerance)) return DragMode.RESIZE_TR;
        if (near(mx, my, rx, ry + rh, tolerance)) return DragMode.RESIZE_BL;
        if (near(mx, my, rx + rw, ry + rh, tolerance)) return DragMode.RESIZE_BR;
        return DragMode.NONE;
    }

    private static boolean near(double x1, double y1, double x2, double y2, double tol) {
        return Math.abs(x1 - x2) < tol && Math.abs(y1 - y2) < tol;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
