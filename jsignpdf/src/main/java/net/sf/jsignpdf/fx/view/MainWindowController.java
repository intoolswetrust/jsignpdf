package net.sf.jsignpdf.fx.view;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.PdfExtraInfo;
import javafx.scene.layout.VBox;
import net.sf.jsignpdf.fx.control.PdfPageView;
import net.sf.jsignpdf.fx.control.SignatureOverlay;
import net.sf.jsignpdf.fx.service.PdfRenderService;
import net.sf.jsignpdf.fx.service.SigningService;
import net.sf.jsignpdf.fx.viewmodel.DocumentViewModel;
import net.sf.jsignpdf.fx.viewmodel.SignaturePlacementViewModel;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.types.PageInfo;

import static net.sf.jsignpdf.Constants.LOGGER;

/**
 * Controller for the main application window.
 */
public class MainWindowController {

    private Stage stage;
    private BasicSignerOptions options;
    private final DocumentViewModel documentVM = new DocumentViewModel();
    private final SigningOptionsViewModel signingVM = new SigningOptionsViewModel();
    private final SignaturePlacementViewModel placementVM = new SignaturePlacementViewModel();
    private final PdfRenderService renderService = new PdfRenderService();
    private final SigningService signingService = new SigningService();
    private PdfPageView pdfPageView;
    private SignatureOverlay signatureOverlay;

    // Included sub-controllers (fx:id + "Controller" naming convention)
    @FXML private VBox certificateSettings;
    @FXML private CertificateSettingsController certificateSettingsController;
    @FXML private VBox signatureSettings;
    @FXML private SignatureSettingsController signatureSettingsController;
    @FXML private VBox tsaSettings;
    @FXML private TsaSettingsController tsaSettingsController;
    @FXML private VBox encryptionSettings;
    @FXML private EncryptionSettingsController encryptionSettingsController;
    @FXML private VBox outputConsole;
    @FXML private OutputConsoleController outputConsoleController;

    // Menu items
    @FXML private MenuItem menuOpen;
    @FXML private MenuItem menuClose;
    @FXML private MenuItem menuSign;
    @FXML private MenuItem menuExit;
    @FXML private MenuItem menuZoomIn;
    @FXML private MenuItem menuZoomOut;
    @FXML private MenuItem menuZoomFit;
    @FXML private MenuItem menuToggleSidePanel;
    @FXML private MenuItem menuAbout;

    // Toolbar
    @FXML private Button btnOpen;
    @FXML private Button btnZoomIn;
    @FXML private Button btnZoomOut;
    @FXML private Button btnZoomFit;
    @FXML private ComboBox<String> cmbZoom;
    @FXML private Button btnPrevPage;
    @FXML private TextField txtPageNumber;
    @FXML private Label lblPageCount;
    @FXML private Button btnNextPage;
    @FXML private ToggleButton btnPlaceSig;
    @FXML private Button btnSign;

    // Content area
    @FXML private SplitPane splitPane;
    @FXML private ScrollPane scrollPane;
    @FXML private StackPane pdfArea;
    @FXML private Label lblDropHint;

    // Status bar
    @FXML private Label lblStatus;
    @FXML private ProgressBar progressBar;

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
    }

    /**
     * Initializes the UI from persisted BasicSignerOptions (called after loadOptions()).
     * Populates all ViewModel properties from the options so the UI is prefilled.
     */
    public void initFromOptions(BasicSignerOptions opts) {
        this.options = opts;
        signingVM.syncFromOptions(opts);
    }

    /**
     * Stores current UI state to BasicSignerOptions and persists to disk.
     * Called on window close.
     */
    public void storeAndCleanup() {
        try {
            if (options == null) {
                options = new BasicSignerOptions();
            }
            signingVM.syncToOptions(options);
            options.storeOptions();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to store options", e);
        }
    }

    @FXML
    private void initialize() {
        // Wire sub-controllers
        if (certificateSettingsController != null) {
            certificateSettingsController.setViewModel(signingVM);
        }
        if (signatureSettingsController != null) {
            signatureSettingsController.setViewModel(signingVM);
        }
        if (tsaSettingsController != null) {
            tsaSettingsController.setViewModel(signingVM);
        }
        if (encryptionSettingsController != null) {
            encryptionSettingsController.setViewModel(signingVM);
        }

        // Setup PDF page view and signature overlay
        pdfPageView = new PdfPageView();
        pdfPageView.setVisible(false);
        signatureOverlay = new SignatureOverlay(placementVM);
        signatureOverlay.setVisible(false);
        signatureOverlay.setMouseTransparent(false);
        pdfArea.getChildren().add(0, pdfPageView);
        pdfArea.getChildren().add(1, signatureOverlay);

        // Keep overlay sized to match the pdf page view
        signatureOverlay.prefWidthProperty().bind(pdfPageView.prefWidthProperty());
        signatureOverlay.prefHeightProperty().bind(pdfPageView.prefHeightProperty());
        signatureOverlay.minWidthProperty().bind(pdfPageView.minWidthProperty());
        signatureOverlay.minHeightProperty().bind(pdfPageView.minHeightProperty());
        signatureOverlay.maxWidthProperty().bind(pdfPageView.maxWidthProperty());
        signatureOverlay.maxHeightProperty().bind(pdfPageView.maxHeightProperty());

        progressBar.setVisible(false);
        updateStatus("Ready");

        cmbZoom.getItems().addAll("50%", "75%", "100%", "125%", "150%", "200%");
        cmbZoom.setValue("100%");

        // Disable controls that require a loaded document
        setDocumentControlsDisabled(true);

        // Bind PDF page view to ViewModel
        pdfPageView.pageImageProperty().bind(documentVM.currentPageImageProperty());
        pdfPageView.zoomLevelProperty().bind(documentVM.zoomLevelProperty());

        // Listen for page changes to trigger re-render
        documentVM.currentPageProperty().addListener((obs, oldVal, newVal) -> {
            txtPageNumber.setText(String.valueOf(newVal.intValue()));
            renderCurrentPage();
            updateNavButtonState();
        });

        // Zoom combo box changes
        cmbZoom.setOnAction(e -> {
            String val = cmbZoom.getValue();
            if (val != null) {
                try {
                    double zoom = Double.parseDouble(val.replace("%", "").trim()) / 100.0;
                    documentVM.setZoomLevel(zoom);
                } catch (NumberFormatException ignored) {
                }
            }
        });

        // Zoom level changes update combo
        documentVM.zoomLevelProperty().addListener((obs, oldVal, newVal) -> {
            String formatted = Math.round(newVal.doubleValue() * 100) + "%";
            if (!formatted.equals(cmbZoom.getValue())) {
                cmbZoom.setValue(formatted);
            }
        });

        // Page number text field commit
        txtPageNumber.setOnAction(e -> {
            try {
                int page = Integer.parseInt(txtPageNumber.getText().trim());
                documentVM.setCurrentPage(page);
            } catch (NumberFormatException ignored) {
                txtPageNumber.setText(String.valueOf(documentVM.getCurrentPage()));
            }
        });

        // Setup render service callbacks
        renderService.setOnSucceeded(e -> {
            documentVM.setCurrentPageImage(renderService.getValue());
            pdfPageView.setVisible(true);
            progressBar.setVisible(false);
            updateStatusForDocument();
        });
        renderService.setOnFailed(e -> {
            LOGGER.log(Level.WARNING, "Failed to render page", renderService.getException());
            progressBar.setVisible(false);
            updateStatus("Error rendering page");
        });

        // Signing service callbacks
        signingService.setOnSucceeded(e -> {
            progressBar.setVisible(false);
            boolean success = signingService.getValue();
            if (success) {
                updateStatus("Signing completed successfully");
                showAlert(Alert.AlertType.INFORMATION, "Signing Complete",
                        "The PDF has been signed successfully.");
            } else {
                updateStatus("Signing failed");
                showAlert(Alert.AlertType.ERROR, "Signing Failed",
                        "The signing process failed. Check the output console for details.");
            }
        });
        signingService.setOnFailed(e -> {
            progressBar.setVisible(false);
            LOGGER.log(Level.SEVERE, "Signing service error", signingService.getException());
            updateStatus("Signing error: " + signingService.getException().getMessage());
            showAlert(Alert.AlertType.ERROR, "Signing Error",
                    signingService.getException().getMessage());
        });
    }

    private void setDocumentControlsDisabled(boolean disabled) {
        btnZoomIn.setDisable(disabled);
        btnZoomOut.setDisable(disabled);
        btnZoomFit.setDisable(disabled);
        cmbZoom.setDisable(disabled);
        btnPrevPage.setDisable(disabled);
        txtPageNumber.setDisable(disabled);
        btnNextPage.setDisable(disabled);
        btnPlaceSig.setDisable(disabled);
        btnSign.setDisable(disabled);
        menuSign.setDisable(disabled);
        menuClose.setDisable(disabled);
        menuZoomIn.setDisable(disabled);
        menuZoomOut.setDisable(disabled);
        menuZoomFit.setDisable(disabled);
    }

    private void updateNavButtonState() {
        btnPrevPage.setDisable(!documentVM.canGoPrev());
        btnNextPage.setDisable(!documentVM.canGoNext());
    }

    private void updateStatus(String message) {
        lblStatus.setText(message);
    }

    private void updateStatusForDocument() {
        File file = documentVM.getDocumentFile();
        if (file != null) {
            updateStatus(file.getName() + " - Page " + documentVM.getCurrentPage()
                    + "/" + documentVM.getPageCount());
        }
    }

    private void renderCurrentPage() {
        if (options == null || !documentVM.isDocumentLoaded()) {
            return;
        }
        renderService.cancel();
        renderService.reset();
        renderService.setOptions(options);
        renderService.setPage(documentVM.getCurrentPage());
        progressBar.setVisible(true);
        renderService.start();
    }

    // --- Keyboard handling ---
    private void handleKeyPress(KeyEvent event) {
        if (!documentVM.isDocumentLoaded()) return;
        if (event.getCode() == KeyCode.PAGE_DOWN) {
            documentVM.nextPage();
            event.consume();
        } else if (event.getCode() == KeyCode.PAGE_UP) {
            documentVM.prevPage();
            event.consume();
        } else if (event.getCode() == KeyCode.Z && event.isShortcutDown() && placementVM.isPlaced()) {
            placementVM.reset();
            btnPlaceSig.setSelected(false);
            event.consume();
        }
    }

    // --- Menu handlers ---

    @FXML
    private void onFileOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            openDocument(file);
        }
    }

    @FXML
    private void onFileClose() {
        closeDocument();
    }

    @FXML
    private void onFileExit() {
        stage.close();
    }

    @FXML
    private void onSign() {
        if (options == null || !documentVM.isDocumentLoaded()) {
            showAlert(Alert.AlertType.WARNING, "No Document",
                    "Please open a PDF document first.");
            return;
        }

        // Sync ViewModel to options
        signingVM.syncToOptions(options);

        // Apply signature placement coordinates if placed
        if (placementVM.isPlaced()) {
            options.setVisible(true);
            options.setPage(documentVM.getCurrentPage());

            PdfExtraInfo extraInfo = new PdfExtraInfo(options);
            PageInfo pageInfo = extraInfo.getPageInfo(documentVM.getCurrentPage());
            if (pageInfo != null) {
                float[] coords = placementVM.toPdfCoordinates(
                        pageInfo.getWidth(), pageInfo.getHeight());
                options.setPositionLLX(coords[0]);
                options.setPositionLLY(coords[1]);
                options.setPositionURX(coords[2]);
                options.setPositionURY(coords[3]);
            }
        }

        // Generate output file name if not set
        if (options.getOutFile() == null || options.getOutFile().isEmpty()) {
            String inFile = options.getInFile();
            String suffix = ".pdf";
            String nameBase = inFile;
            if (inFile.toLowerCase().endsWith(suffix)) {
                nameBase = inFile.substring(0, inFile.length() - 4);
                suffix = inFile.substring(inFile.length() - 4);
            }
            options.setOutFile(nameBase + Constants.DEFAULT_OUT_SUFFIX + suffix);
        }

        // Start signing
        signingService.cancel();
        signingService.reset();
        signingService.setOptions(options);
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // indeterminate
        updateStatus("Signing...");
        signingService.start();
    }

    @FXML
    private void onZoomIn() {
        documentVM.zoomIn();
    }

    @FXML
    private void onZoomOut() {
        documentVM.zoomOut();
    }

    @FXML
    private void onZoomFit() {
        if (!documentVM.isDocumentLoaded() || documentVM.getCurrentPageImage() == null) return;
        double imgWidth = documentVM.getCurrentPageImage().getWidth();
        double viewWidth = scrollPane.getViewportBounds().getWidth();
        if (imgWidth > 0 && viewWidth > 0) {
            documentVM.setZoomLevel(viewWidth / imgWidth);
        }
    }

    @FXML
    private void onPrevPage() {
        documentVM.prevPage();
    }

    @FXML
    private void onNextPage() {
        documentVM.nextPage();
    }

    @FXML
    private void onPlaceSignature() {
        if (btnPlaceSig.isSelected()) {
            placementVM.setPlacementMode(true);
        } else {
            placementVM.setPlacementMode(false);
        }
    }

    @FXML
    private void onToggleSidePanel() {
        if (splitPane.getItems().size() > 1) {
            // Toggle visibility of side panel by manipulating divider position
            double pos = splitPane.getDividerPositions()[0];
            if (pos < 0.05) {
                splitPane.setDividerPositions(0.28);
            } else {
                splitPane.setDividerPositions(0.0);
            }
        }
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About JSignPdf");
        alert.setHeaderText("JSignPdf " + Constants.VERSION);
        alert.setContentText("A free application for PDF signing.");
        alert.showAndWait();
    }

    // --- Drag and drop ---

    @FXML
    private void onDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            List<File> files = db.getFiles();
            if (files.size() == 1 && files.get(0).getName().toLowerCase().endsWith(".pdf")) {
                event.acceptTransferModes(TransferMode.COPY);
            }
        }
        event.consume();
    }

    @FXML
    private void onDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            List<File> files = db.getFiles();
            if (files.size() == 1 && files.get(0).getName().toLowerCase().endsWith(".pdf")) {
                openDocument(files.get(0));
                success = true;
            }
        }
        event.setDropCompleted(success);
        event.consume();
    }

    // --- Document handling ---

    private void openDocument(File file) {
        try {
            if (options == null) {
                options = new BasicSignerOptions();
                options.loadOptions();
                signingVM.syncFromOptions(options);
            }
            options.setInFile(file.getAbsolutePath());

            // Get page count
            PdfExtraInfo extraInfo = new PdfExtraInfo(options);
            int pages = extraInfo.getNumberOfPages();
            if (pages < 1) {
                updateStatus("Error: Cannot read PDF file");
                return;
            }

            documentVM.setDocumentFile(file);
            documentVM.setPageCount(pages);
            documentVM.setCurrentPage(1);
            documentVM.setZoomLevel(1.0);

            lblDropHint.setVisible(false);
            setDocumentControlsDisabled(false);
            lblPageCount.setText("/ " + pages);
            txtPageNumber.setText("1");
            cmbZoom.setValue("100%");

            stage.setTitle("JSignPdf " + Constants.VERSION + " - " + file.getName());
            LOGGER.info("Opened document: " + file.getAbsolutePath());

            // Show signature overlay and render first page
            signatureOverlay.setVisible(true);
            renderCurrentPage();
            updateNavButtonState();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to open document", e);
            updateStatus("Error: " + e.getMessage());
        }
    }

    private void closeDocument() {
        renderService.cancel();
        documentVM.reset();
        placementVM.reset();
        signatureOverlay.setVisible(false);
        btnPlaceSig.setSelected(false);
        if (options != null) {
            options.setInFile(null);
        }
        pdfPageView.setVisible(false);
        lblDropHint.setVisible(true);
        setDocumentControlsDisabled(true);
        lblPageCount.setText("/ 0");
        txtPageNumber.setText("");
        updateStatus("Ready");
        stage.setTitle("JSignPdf " + Constants.VERSION);
    }

    /**
     * Returns the document view model (used by other controllers).
     */
    public DocumentViewModel getDocumentViewModel() {
        return documentVM;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Returns the signature placement view model.
     */
    public SignaturePlacementViewModel getPlacementViewModel() {
        return placementVM;
    }

    /**
     * Returns the signing options view model.
     */
    public SigningOptionsViewModel getSigningOptionsViewModel() {
        return signingVM;
    }

    /**
     * Returns the current signer options (used by other controllers).
     */
    public BasicSignerOptions getOptions() {
        return options;
    }
}
