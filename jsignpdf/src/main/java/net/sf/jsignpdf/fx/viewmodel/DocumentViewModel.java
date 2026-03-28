package net.sf.jsignpdf.fx.viewmodel;

import java.io.File;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

/**
 * ViewModel holding the state of the currently loaded PDF document.
 */
public class DocumentViewModel {

    private final ObjectProperty<File> documentFile = new SimpleObjectProperty<>();
    private final IntegerProperty pageCount = new SimpleIntegerProperty(0);
    private final IntegerProperty currentPage = new SimpleIntegerProperty(1);
    private final DoubleProperty zoomLevel = new SimpleDoubleProperty(1.0);
    private final ObjectProperty<Image> currentPageImage = new SimpleObjectProperty<>();
    private final ReadOnlyBooleanWrapper documentLoaded = new ReadOnlyBooleanWrapper(false);
    private final StringProperty statusText = new SimpleStringProperty("");

    public DocumentViewModel() {
        documentFile.addListener((obs, oldVal, newVal) ->
                documentLoaded.set(newVal != null));
    }

    // --- Document file ---
    public ObjectProperty<File> documentFileProperty() { return documentFile; }
    public File getDocumentFile() { return documentFile.get(); }
    public void setDocumentFile(File file) { documentFile.set(file); }

    // --- Page count ---
    public IntegerProperty pageCountProperty() { return pageCount; }
    public int getPageCount() { return pageCount.get(); }
    public void setPageCount(int count) { pageCount.set(count); }

    // --- Current page (1-based) ---
    public IntegerProperty currentPageProperty() { return currentPage; }
    public int getCurrentPage() { return currentPage.get(); }
    public void setCurrentPage(int page) {
        if (page >= 1 && page <= getPageCount()) {
            currentPage.set(page);
        }
    }

    // --- Zoom level (1.0 = 100%) ---
    public DoubleProperty zoomLevelProperty() { return zoomLevel; }
    public double getZoomLevel() { return zoomLevel.get(); }
    public void setZoomLevel(double zoom) {
        zoomLevel.set(Math.max(0.25, Math.min(4.0, zoom)));
    }

    // --- Current page image ---
    public ObjectProperty<Image> currentPageImageProperty() { return currentPageImage; }
    public Image getCurrentPageImage() { return currentPageImage.get(); }
    public void setCurrentPageImage(Image image) { currentPageImage.set(image); }

    // --- Document loaded (read-only) ---
    public ReadOnlyBooleanProperty documentLoadedProperty() { return documentLoaded.getReadOnlyProperty(); }
    public boolean isDocumentLoaded() { return documentLoaded.get(); }

    // --- Status text ---
    public StringProperty statusTextProperty() { return statusText; }
    public String getStatusText() { return statusText.get(); }
    public void setStatusText(String text) { statusText.set(text); }

    // --- Navigation helpers ---
    public boolean canGoNext() { return getCurrentPage() < getPageCount(); }
    public boolean canGoPrev() { return getCurrentPage() > 1; }

    public void nextPage() {
        if (canGoNext()) setCurrentPage(getCurrentPage() + 1);
    }

    public void prevPage() {
        if (canGoPrev()) setCurrentPage(getCurrentPage() - 1);
    }

    public void zoomIn() { setZoomLevel(getZoomLevel() + 0.25); }
    public void zoomOut() { setZoomLevel(getZoomLevel() - 0.25); }

    public void reset() {
        documentFile.set(null);
        pageCount.set(0);
        currentPage.set(1);
        zoomLevel.set(1.0);
        currentPageImage.set(null);
        statusText.set("");
    }
}
