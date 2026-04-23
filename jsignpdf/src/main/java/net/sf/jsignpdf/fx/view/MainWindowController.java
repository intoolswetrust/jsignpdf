package net.sf.jsignpdf.fx.view;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tooltip;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.openpdf.text.exceptions.BadPasswordException;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.PdfExtraInfo;
import javafx.scene.layout.VBox;
import net.sf.jsignpdf.fx.control.PdfPageView;
import net.sf.jsignpdf.fx.control.SignatureOverlay;
import net.sf.jsignpdf.fx.service.PdfRenderService;
import net.sf.jsignpdf.fx.service.SigningService;
import net.sf.jsignpdf.fx.preset.ManagePresetsDialog;
import net.sf.jsignpdf.fx.preset.Preset;
import net.sf.jsignpdf.fx.preset.PresetManager;
import net.sf.jsignpdf.fx.preset.PresetValidation;
import net.sf.jsignpdf.fx.util.RecentFilesManager;
import net.sf.jsignpdf.fx.viewmodel.DocumentViewModel;
import net.sf.jsignpdf.utils.PropertyStoreFactory;
import net.sf.jsignpdf.fx.viewmodel.SignaturePlacementViewModel;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.fx.viewmodel.VisibleSignatureCoordinator;
import net.sf.jsignpdf.types.PageInfo;

import static net.sf.jsignpdf.Constants.LOGGER;
import static net.sf.jsignpdf.Constants.RES;

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
    private final RecentFilesManager recentFilesManager = new RecentFilesManager();
    private final PresetManager presetManager = new PresetManager();
    private PdfPageView pdfPageView;
    private SignatureOverlay signatureOverlay;
    /** Holds the side panel node while it's detached from the SplitPane (hidden). */
    private Node detachedSidePanel;

    // Included sub-controllers (fx:id + "Controller" naming convention)
    @FXML private VBox certificateSettings;
    @FXML private CertificateSettingsController certificateSettingsController;
    @FXML private VBox signatureProperties;
    @FXML private SignaturePropertiesController signaturePropertiesController;
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
    @FXML private MenuItem menuSaveAs;
    @FXML private Menu menuRecentFiles;
    @FXML private CheckMenuItem menuVisibleSig;
    @FXML private MenuItem menuSign;
    @FXML private MenuItem menuExit;
    @FXML private MenuItem menuZoomIn;
    @FXML private MenuItem menuZoomOut;
    @FXML private MenuItem menuZoomFit;
    @FXML private MenuItem menuToggleSidePanel;
    @FXML private MenuItem menuResetSettings;
    @FXML private MenuItem menuSavePresetAsNew;
    @FXML private MenuItem menuManagePresets;
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
    @FXML private ToggleButton btnVisibleSig;
    @FXML private ToggleButton btnTsa;
    @FXML private ComboBox<Preset> cmbPresets;
    @FXML private Button btnSign;

    // Content area
    @FXML private SplitPane splitPane;
    @FXML private Accordion sidePanelAccordion;
    @FXML private TitledPane tsaAccordionPane;
    @FXML private TitledPane encryptionAccordionPane;
    @FXML private ScrollPane scrollPane;
    @FXML private StackPane pdfArea;
    @FXML private Label lblDropHint;

    // Status bar
    @FXML private Label lblStatus;
    @FXML private Label lblSigStateBadge;
    @FXML private Label lblOutputPath;
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
        // No document is loaded at startup, so the visible-signature toggle must
        // start disabled. The persisted position coordinates on signingVM are
        // preserved so we can auto-place at the last-known location once a
        // document is opened and the user re-enables it.
        signingVM.visibleProperty().set(false);
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

            // Sync placement rectangle coordinates to the signing ViewModel before persisting
            capturePlacementToSigningVM();

            signingVM.syncToOptions(options);
            options.storeOptions();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to store options", e);
        }
        if (outputConsoleController != null) {
            outputConsoleController.dispose();
        }
    }

    @FXML
    private void initialize() {
        // Wire sub-controllers
        if (certificateSettingsController != null) {
            certificateSettingsController.setViewModel(signingVM);
        }
        if (signaturePropertiesController != null) {
            signaturePropertiesController.setViewModel(signingVM);
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

        // Bind pdfArea min size to pdfPageView so scrollbars appear when the page
        // exceeds the viewport. With fitToWidth/fitToHeight on the ScrollPane, the
        // StackPane expands to fill the viewport (centering children) but minSize
        // prevents it from shrinking below the rendered page, triggering scrollbars.
        pdfArea.minWidthProperty().bind(pdfPageView.prefWidthProperty());
        pdfArea.minHeightProperty().bind(pdfPageView.prefHeightProperty());

        // Auto-enable visible signature when a rectangle is placed
        placementVM.placedProperty().addListener((obs, wasPlaced, isPlaced) -> {
            if (isPlaced) {
                signingVM.visibleProperty().set(true);
            }
            updateStatusWithHint();
        });

        // React to visible-signature state changes:
        //  - disabled: clear the placement rectangle
        //  - enabled:  auto-place at last-known PDF coordinates (or safe default)
        //              if no rectangle is already placed (e.g. via drag)
        signingVM.visibleProperty().addListener((obs, wasVisible, isVisible) -> {
            if (!isVisible) {
                placementVM.reset();
            } else {
                autoPlaceVisibleSignature();
            }
            updateSigStateBadge();
        });

        // Bind the visible-signature controls (CheckMenuItem + toolbar toggle)
        // bidirectionally to the ViewModel so they mirror the side-panel checkbox.
        menuVisibleSig.selectedProperty().bindBidirectional(signingVM.visibleProperty());
        btnVisibleSig.selectedProperty().bindBidirectional(signingVM.visibleProperty());
        btnTsa.selectedProperty().bindBidirectional(signingVM.tsaEnabledProperty());

        // When TSA is turned on but no URL is configured yet, jump the side-panel
        // accordion to the TSA section so the user can fill the required field.
        signingVM.tsaEnabledProperty().addListener((obs, was, on) -> {
            if (on && isBlank(signingVM.tsaUrlProperty().get())) {
                expandTsaPane();
            }
        });

        // Status-bar badge: visible whenever a document is loaded. Its text and
        // colour swap based on whether visible signature is on or off.
        lblSigStateBadge.managedProperty().bind(lblSigStateBadge.visibleProperty());
        lblSigStateBadge.visibleProperty().bind(documentVM.documentLoadedProperty());

        // Status-bar output path label: visible when a document is loaded
        lblOutputPath.managedProperty().bind(lblOutputPath.visibleProperty());
        signingVM.outFileProperty().addListener((obs, oldVal, newVal) -> updateOutputPathLabel());
        documentVM.documentFileProperty().addListener((obs, oldVal, newVal) -> updateOutputPathLabel());

        // Initial state for the visible-signature controls.
        // The badge's initial text and style come from FXML (correct for
        // visibleProperty=false on startup); listeners take over on state changes.
        updateOutputPathLabel();

        // Keep overlay sized to match the pdf page view
        signatureOverlay.prefWidthProperty().bind(pdfPageView.prefWidthProperty());
        signatureOverlay.prefHeightProperty().bind(pdfPageView.prefHeightProperty());
        signatureOverlay.minWidthProperty().bind(pdfPageView.minWidthProperty());
        signatureOverlay.minHeightProperty().bind(pdfPageView.minHeightProperty());
        signatureOverlay.maxWidthProperty().bind(pdfPageView.maxWidthProperty());
        signatureOverlay.maxHeightProperty().bind(pdfPageView.maxHeightProperty());

        progressBar.setVisible(false);
        updateStatus(RES.get("jfx.gui.status.ready"));

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
            updateStatus(RES.get("jfx.gui.status.renderError"));
        });

        // Signing service callbacks
        signingService.setOnSucceeded(e -> {
            progressBar.setVisible(false);
            boolean success = signingService.getValue();
            if (success) {
                updateStatus(RES.get("jfx.gui.status.signingOk"));
                showAlert(Alert.AlertType.INFORMATION,
                        RES.get("jfx.gui.dialog.signingComplete.title"),
                        RES.get("jfx.gui.dialog.signingComplete.text"));
            } else {
                updateStatus(RES.get("jfx.gui.status.signingFailed"));
                showAlert(Alert.AlertType.ERROR,
                        RES.get("jfx.gui.dialog.signingFailed.title"),
                        RES.get("jfx.gui.dialog.signingFailed.text"));
            }
        });
        signingService.setOnFailed(e -> {
            progressBar.setVisible(false);
            LOGGER.log(Level.SEVERE, "Signing service error", signingService.getException());
            updateStatus(RES.get("jfx.gui.status.signingFailed") + ": "
                    + signingService.getException().getMessage());
            showAlert(Alert.AlertType.ERROR,
                    RES.get("jfx.gui.dialog.signingError.title"),
                    signingService.getException().getMessage());
        });

        refreshRecentFilesMenu();
        setupPresetCombo();
    }

    private void setupPresetCombo() {
        cmbPresets.setItems(presetManager.getPresets());
        updatePresetComboState();
        presetManager.getPresets().addListener((javafx.collections.ListChangeListener<Preset>) c -> updatePresetComboState());
        cmbPresets.getSelectionModel().selectedItemProperty().addListener((obs, oldP, newP) -> {
            if (newP == null) {
                return;
            }
            Preset toLoad = newP;
            // Run the actual load after the selection event finishes — JavaFX doesn't like mutating
            // the combo's selection from inside its own change listener.
            Platform.runLater(() -> {
                loadPreset(toLoad);
                cmbPresets.getSelectionModel().clearSelection();
                cmbPresets.setValue(null);
            });
        });
    }

    private void updatePresetComboState() {
        boolean empty = presetManager.getPresets().isEmpty();
        cmbPresets.setDisable(empty);
        cmbPresets.setPromptText(RES.get(empty
                ? "jfx.gui.preset.placeholder.empty"
                : "jfx.gui.preset.placeholder"));
    }

    private void loadPreset(Preset preset) {
        if (options == null) {
            options = new BasicSignerOptions();
        }
        // Flush any pending edits from the VM so that "load" is clearly "replace current".
        signingVM.syncToOptions(options);
        presetManager.load(preset, options);
        signingVM.syncFromOptions(options);
        // Move the on-screen rectangle to match the preset's position.
        applySigningVMPositionToPlacement();
        updateStatus(java.text.MessageFormat.format(
                RES.get("jfx.gui.status.presetLoaded"), preset.getDisplayName()));
    }

    /**
     * Writes the current placement rectangle to the signing VM (as PDF coords) if one is placed and a document is loaded.
     * Called before saving to preset, storing the main config, and signing.
     */
    private void capturePlacementToSigningVM() {
        if (!placementVM.isPlaced() || !documentVM.isDocumentLoaded() || options == null) {
            return;
        }
        PageInfo pageInfo = new PdfExtraInfo(options).getPageInfo(documentVM.getCurrentPage());
        if (pageInfo == null) {
            return;
        }
        VisibleSignatureCoordinator.pushPlacementToSigning(placementVM, signingVM,
                documentVM.getCurrentPage(), pageInfo.getWidth(), pageInfo.getHeight());
    }

    /**
     * Moves the on-screen placement rectangle to match the coordinates currently held by the signing VM. Used right after
     * loading a preset, so the canvas reflects the new position even if {@code visible} hasn't toggled.
     */
    private void applySigningVMPositionToPlacement() {
        if (!documentVM.isDocumentLoaded() || options == null) {
            return;
        }
        PageInfo pageInfo = new PdfExtraInfo(options).getPageInfo(documentVM.getCurrentPage());
        if (pageInfo == null) {
            return;
        }
        VisibleSignatureCoordinator.pushSigningToPlacement(signingVM, placementVM,
                pageInfo.getWidth(), pageInfo.getHeight());
    }

    @FXML
    private void onSavePresetAsNew() {
        if (options == null) {
            options = new BasicSignerOptions();
        }
        capturePlacementToSigningVM();
        signingVM.syncToOptions(options);

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(RES.get("jfx.gui.preset.dialog.saveAsNew.title"));
        dialog.setHeaderText(RES.get("jfx.gui.preset.dialog.saveAsNew.header"));
        dialog.setContentText(RES.get("jfx.gui.preset.dialog.saveAsNew.prompt"));
        dialog.initOwner(stage);

        while (true) {
            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return;
            }
            String name = result.get();
            PresetValidation.Result validation = PresetValidation.validate(name, presetManager::hasDisplayName);
            if (validation != PresetValidation.Result.OK) {
                showAlert(Alert.AlertType.ERROR,
                        RES.get("jfx.gui.preset.dialog.saveAsNew.title"),
                        validationMessage(validation));
                dialog.getEditor().setText(PresetValidation.trim(name));
                continue;
            }
            Preset saved = presetManager.saveAsNew(options, PresetValidation.trim(name));
            updateStatus(java.text.MessageFormat.format(
                    RES.get("jfx.gui.status.presetSaved"), saved.getDisplayName()));
            return;
        }
    }

    @FXML
    private void onManagePresets() {
        if (options == null) {
            options = new BasicSignerOptions();
        }
        capturePlacementToSigningVM();
        signingVM.syncToOptions(options);
        ManagePresetsDialog dialog = new ManagePresetsDialog(presetManager, options, stage);
        dialog.showAndWait();
    }

    private String validationMessage(PresetValidation.Result result) {
        switch (result) {
            case EMPTY: return RES.get("jfx.gui.preset.validation.empty");
            case ILLEGAL_CHAR: return RES.get("jfx.gui.preset.validation.illegalChar");
            case TOO_LONG: return RES.get("jfx.gui.preset.validation.tooLong");
            case DUPLICATE: return RES.get("jfx.gui.preset.validation.duplicate");
            default: return "";
        }
    }

    private void refreshRecentFilesMenu() {
        menuRecentFiles.getItems().clear();
        List<String> recentFiles = recentFilesManager.getRecentFiles();
        if (recentFiles.isEmpty()) {
            MenuItem empty = new MenuItem(RES.get("jfx.gui.menu.file.recentFiles.empty"));
            empty.setDisable(true);
            menuRecentFiles.getItems().add(empty);
        } else {
            for (String path : recentFiles) {
                MenuItem item = new MenuItem(path);
                item.setMnemonicParsing(false);
                item.setOnAction(e -> openDocument(new File(path)));
                menuRecentFiles.getItems().add(item);
            }
        }
    }

    private void setDocumentControlsDisabled(boolean disabled) {
        btnZoomIn.setDisable(disabled);
        btnZoomOut.setDisable(disabled);
        btnZoomFit.setDisable(disabled);
        cmbZoom.setDisable(disabled);
        btnPrevPage.setDisable(disabled);
        txtPageNumber.setDisable(disabled);
        btnNextPage.setDisable(disabled);
        btnSign.setDisable(disabled);
        btnVisibleSig.setDisable(disabled);
        menuSign.setDisable(disabled);
        menuClose.setDisable(disabled);
        menuSaveAs.setDisable(disabled);
        menuVisibleSig.setDisable(disabled);
        menuZoomIn.setDisable(disabled);
        menuZoomOut.setDisable(disabled);
        menuZoomFit.setDisable(disabled);
        if (signatureSettingsController != null) {
            signatureSettingsController.setVisibleSigCheckBoxDisabled(disabled);
        }
    }

    /**
     * Updates the status-bar badge that reflects whether the visible signature
     * is currently enabled. The badge swaps between two distinct styles so the
     * state is always obvious at a glance when a document is loaded.
     */
    private void updateSigStateBadge() {
        if (lblSigStateBadge == null) return;
        boolean on = signingVM.visibleProperty().get();
        lblSigStateBadge.setText(on
                ? RES.get("jfx.gui.status.visibleSigEnabled")
                : RES.get("jfx.gui.status.invisibleSig"));
        lblSigStateBadge.getStyleClass().removeAll("visible-sig-badge", "invisible-sig-badge");
        lblSigStateBadge.getStyleClass().add(on ? "visible-sig-badge" : "invisible-sig-badge");
    }

    /**
     * Auto-places the signature rectangle when the user enables visible
     * signature without dragging a rectangle first. Prefers the last-known PDF
     * coordinates persisted in the ViewModel — if they form a valid rectangle
     * that fits the current page — and falls back to a safe bottom-right
     * default otherwise. Always re-targets the current page.
     */
    private void autoPlaceVisibleSignature() {
        if (!documentVM.isDocumentLoaded() || placementVM.isPlaced() || options == null) {
            return;
        }
        PageInfo pageInfo = new PdfExtraInfo(options).getPageInfo(documentVM.getCurrentPage());
        if (pageInfo == null) {
            return;
        }
        float pw = pageInfo.getWidth();
        float ph = pageInfo.getHeight();

        float llx = signingVM.positionLLXProperty().get();
        float lly = signingVM.positionLLYProperty().get();
        float urx = signingVM.positionURXProperty().get();
        float ury = signingVM.positionURYProperty().get();

        boolean fits = urx - llx > 1f && ury - lly > 1f
                && llx >= 0f && lly >= 0f
                && urx <= pw && ury <= ph;

        if (fits) {
            placementVM.fromPdfCoordinates(llx, lly, urx, ury, pw, ph);
        } else {
            // Safe default: bottom-right, 15% × 8% of the page with ~5% margins.
            placementVM.setRelWidth(0.15);
            placementVM.setRelHeight(0.08);
            placementVM.setRelX(0.80);
            placementVM.setRelY(0.87);
            placementVM.setPlaced(true);
        }
        signingVM.pageProperty().set(documentVM.getCurrentPage());
    }

    /**
     * Updates the status-bar output path label to show the destination filename
     * (or the default "{input}_signed.pdf" suggestion if no explicit path is set).
     */
    private void updateOutputPathLabel() {
        if (lblOutputPath == null) {
            return;
        }
        if (!documentVM.isDocumentLoaded()) {
            lblOutputPath.setVisible(false);
            lblOutputPath.setText("");
            lblOutputPath.setTooltip(null);
            return;
        }
        String outPath = signingVM.outFileProperty().get();
        String displayPath = (outPath != null && !outPath.isEmpty())
                ? outPath
                : suggestedOutFileFor(documentVM.getDocumentFile());
        if (displayPath == null || displayPath.isEmpty()) {
            lblOutputPath.setVisible(false);
            lblOutputPath.setTooltip(null);
            return;
        }
        File f = new File(displayPath);
        lblOutputPath.setText("→ " + f.getName());
        lblOutputPath.setTooltip(new Tooltip(displayPath));
        lblOutputPath.setVisible(true);
    }

    /**
     * Computes the default "{input}_signed.pdf" output path for a given input file.
     * Returns null if the input is null.
     */
    private static String suggestedOutFileFor(File inputFile) {
        if (inputFile == null) return null;
        String inFile = inputFile.getAbsolutePath();
        String suffix = ".pdf";
        String nameBase = inFile;
        if (inFile.toLowerCase().endsWith(suffix)) {
            nameBase = inFile.substring(0, inFile.length() - 4);
            suffix = inFile.substring(inFile.length() - 4);
        }
        return nameBase + Constants.DEFAULT_OUT_SUFFIX + suffix;
    }


    private void updateNavButtonState() {
        btnPrevPage.setDisable(!documentVM.canGoPrev());
        btnNextPage.setDisable(!documentVM.canGoNext());
    }

    private void expandTsaPane() {
        if (sidePanelAccordion != null && tsaAccordionPane != null) {
            sidePanelAccordion.setExpandedPane(tsaAccordionPane);
        }
    }

    private void expandEncryptionPane() {
        if (sidePanelAccordion != null && encryptionAccordionPane != null) {
            sidePanelAccordion.setExpandedPane(encryptionAccordionPane);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
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

    private void updateStatusWithHint() {
        if (documentVM.isDocumentLoaded() && placementVM.isPlaced()) {
            updateStatus(RES.get("jfx.gui.status.shiftToReplace"));
        } else if (documentVM.isDocumentLoaded()) {
            updateStatusForDocument();
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
            event.consume();
        }
    }

    // --- Menu handlers ---

    @FXML
    private void onFileOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(RES.get("jfx.gui.dialog.openPdf.title"));
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
    private void onResetSettings() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(RES.get("jfx.gui.dialog.resetSettings.title"));
        confirm.setHeaderText(null);
        confirm.setContentText(RES.get("jfx.gui.dialog.resetSettings.text"));
        confirm.initOwner(stage);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Wipe the config directory (main config + presets) and clear in-memory state.
            PropertyStoreFactory.getInstance().resetAll();
            options = new BasicSignerOptions();

            // Refresh preset list from disk (now empty) — this updates the toolbar combo.
            presetManager.scan();

            // Reset the ViewModel (which updates all bound UI controls)
            signingVM.resetToDefaults();

            // Clear placement and close any open document
            if (documentVM.isDocumentLoaded()) {
                closeDocument();
            }
            placementVM.reset();

            // Refresh the recent files menu (now empty)
            refreshRecentFilesMenu();

            updateStatus(RES.get("jfx.gui.status.settingsReset"));
        }
    }

    @FXML
    private void onSaveAs() {
        if (!documentVM.isDocumentLoaded()) {
            showAlert(Alert.AlertType.WARNING,
                    RES.get("jfx.gui.dialog.noDocument.title"),
                    RES.get("jfx.gui.dialog.noDocument.text"));
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle(RES.get("jfx.gui.dialog.selectOutputPdf"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        String current = signingVM.outFileProperty().get();
        if (current == null || current.isEmpty()) {
            current = suggestedOutFileFor(documentVM.getDocumentFile());
        }
        if (current != null && !current.isEmpty()) {
            File currentFile = new File(current);
            File parent = currentFile.getParentFile();
            if (parent != null && parent.isDirectory()) {
                fc.setInitialDirectory(parent);
            }
            fc.setInitialFileName(currentFile.getName());
        }
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            signingVM.outFileProperty().set(file.getAbsolutePath());
        }
    }

    @FXML
    private void onSign() {
        if (options == null || !documentVM.isDocumentLoaded()) {
            showAlert(Alert.AlertType.WARNING,
                    RES.get("jfx.gui.dialog.noDocument.title"),
                    RES.get("jfx.gui.dialog.noDocument.text"));
            return;
        }

        // Validate encryption-dependent required fields before signing
        if (encryptionSettingsController != null && !encryptionSettingsController.isEncryptionConfigValid()) {
            expandEncryptionPane();
            String prefix = encryptionSettingsController.getValidationErrorKeyPrefix();
            showAlert(Alert.AlertType.WARNING,
                    RES.get(prefix + ".title"),
                    RES.get(prefix + ".text"));
            return;
        }

        // Validate TSA: when enabled, the TSA server URL is mandatory.
        if (tsaSettingsController != null && !tsaSettingsController.isTsaConfigValid()) {
            expandTsaPane();
            showAlert(Alert.AlertType.WARNING,
                    RES.get("jfx.gui.dialog.missingTsaUrl.title"),
                    RES.get("jfx.gui.dialog.missingTsaUrl.text"));
            return;
        }

        // Capture live placement into the signing VM, then sync VM → options.
        capturePlacementToSigningVM();
        signingVM.syncToOptions(options);

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
        signingService.setOptions(options.createCopy());
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // indeterminate
        updateStatus(RES.get("jfx.gui.status.signingInProgress"));
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
        double imgHeight = documentVM.getCurrentPageImage().getHeight();
        double viewWidth = scrollPane.getViewportBounds().getWidth();
        double viewHeight = scrollPane.getViewportBounds().getHeight();
        if (imgWidth > 0 && imgHeight > 0 && viewWidth > 0 && viewHeight > 0) {
            documentVM.setZoomLevel(Math.min(viewWidth / imgWidth, viewHeight / imgHeight));
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
    private void onToggleSidePanel() {
        // Simply moving the SplitPane divider doesn't hide the accordion:
        // the side-panel container has a minWidth, so the SplitPane refuses
        // to shrink it below that. Instead, detach the node entirely when
        // hiding and re-insert it when showing.
        if (detachedSidePanel == null) {
            if (splitPane.getItems().size() < 2) return;
            detachedSidePanel = splitPane.getItems().remove(0);
        } else {
            splitPane.getItems().add(0, detachedSidePanel);
            detachedSidePanel = null;
            splitPane.setDividerPositions(0.28);
        }
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(RES.get("jfx.gui.menu.help.about"));
        alert.setHeaderText("JSignPdf " + Constants.VERSION);
        alert.setContentText(RES.get("jfx.gui.dialog.about.description"));
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

            // Reset visible signature and placement from previous document
            signingVM.visibleProperty().set(false);
            placementVM.reset();

            options.setInFile(file.getAbsolutePath());

            // Always reset the output path to the default for the new input file.
            // Any custom path from a previous session is intentionally discarded —
            // the user can still override via the field or File > Save Output As.
            signingVM.outFileProperty().set(suggestedOutFileFor(file));

            // Try to open PDF, prompting for owner password if needed
            int pages;
            try {
                PdfExtraInfo extraInfo = new PdfExtraInfo(options);
                pages = extraInfo.getNumberOfPages();
            } catch (BadPasswordException e) {
                pages = promptPasswordAndRetry(file);
            }

            if (pages < 1) {
                updateStatus(RES.get("jfx.gui.status.readError"));
                return;
            }

            documentVM.setDocumentFile(file);
            documentVM.setPageCount(pages);
            documentVM.setZoomLevel(1.0);

            lblDropHint.setVisible(false);
            setDocumentControlsDisabled(false);
            lblPageCount.setText("/ " + pages);
            txtPageNumber.setText("1");
            cmbZoom.setValue("100%");

            stage.setTitle("JSignPdf " + Constants.VERSION + " - " + file.getName());
            LOGGER.info("Opened document: " + file.getAbsolutePath());

            recentFilesManager.addFile(file);
            refreshRecentFilesMenu();

            // Show signature overlay and render first page
            signatureOverlay.setVisible(true);
            documentVM.setCurrentPage(1);
            renderCurrentPage();
            updateNavButtonState();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to open document", e);
            updateStatus("Error: " + e.getMessage());
        }
    }

    /**
     * Prompts the user for the owner password and retries opening the PDF.
     * Loops until the password works, or the user cancels.
     *
     * @return number of pages, or -1 if cancelled/failed
     */
    private int promptPasswordAndRetry(File file) {
        boolean firstAttempt = true;
        while (true) {
            Optional<String> password = showPasswordDialog(firstAttempt);
            if (password.isEmpty()) {
                // User cancelled
                return -1;
            }
            firstAttempt = false;

            options.setPdfOwnerPwd(password.get().toCharArray());
            options.setAdvanced(true);
            signingVM.syncFromOptions(options);

            try {
                PdfExtraInfo extraInfo = new PdfExtraInfo(options);
                return extraInfo.getNumberOfPages();
            } catch (BadPasswordException e) {
                // Wrong password - loop to ask again
            }
        }
    }

    /**
     * Shows a dialog asking the user for the PDF owner password.
     *
     * @param firstAttempt true for the initial prompt, false after a wrong password
     * @return the entered password, or empty if cancelled
     */
    private Optional<String> showPasswordDialog(boolean firstAttempt) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(RES.get("jfx.gui.dialog.ownerPassword.title"));
        dialog.setHeaderText(firstAttempt
                ? RES.get("jfx.gui.dialog.ownerPassword.header")
                : RES.get("jfx.gui.dialog.ownerPassword.headerRetry"));
        dialog.initOwner(stage);

        ButtonType okButtonType = new ButtonType(RES.get("jfx.gui.dialog.ownerPassword.ok"),
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(RES.get("jfx.gui.dialog.ownerPassword.prompt"));
        passwordField.setPrefColumnCount(20);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.add(new Label(RES.get("jfx.gui.dialog.ownerPassword.label")), 0, 0);
        grid.add(passwordField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        // Disable OK button when password field is empty
        Button okButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setDisable(true);
        passwordField.textProperty().addListener((obs, oldVal, newVal) ->
                okButton.setDisable(newVal.trim().isEmpty()));

        // Focus the password field when dialog opens
        Platform.runLater(passwordField::requestFocus);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == okButtonType) {
                return passwordField.getText();
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void closeDocument() {
        renderService.cancel();
        signingVM.visibleProperty().set(false);
        documentVM.reset();
        placementVM.reset();
        signatureOverlay.setVisible(false);
        if (options != null) {
            options.setInFile(null);
        }
        pdfPageView.setVisible(false);
        lblDropHint.setVisible(true);
        setDocumentControlsDisabled(true);
        lblPageCount.setText("/ 0");
        txtPageNumber.setText("");
        updateStatus(RES.get("jfx.gui.status.ready"));
        updateOutputPathLabel();
        updateSigStateBadge();
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
