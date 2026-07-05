package net.sf.jsignpdf.fx.view;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import javafx.animation.PauseTransition;
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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import net.sf.jsignpdf.fx.util.NativeFileChooser;
import net.sf.jsignpdf.fx.util.NativeFileChooser.ExtensionFilter;
import net.sf.jsignpdf.fx.util.Sandbox;

import org.openpdf.text.exceptions.BadPasswordException;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.PdfExtraInfo;
import net.sf.jsignpdf.engine.Capability;
import net.sf.jsignpdf.engine.EngineRegistry;
import net.sf.jsignpdf.engine.SigningEngine;
import net.sf.jsignpdf.fx.EngineCapabilities;
import net.sf.jsignpdf.utils.AppConfig;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import net.sf.jsignpdf.fx.control.PdfPageView;
import net.sf.jsignpdf.fx.control.SignatureOverlay;
import net.sf.jsignpdf.fx.service.PdfRenderService;
import net.sf.jsignpdf.fx.service.SigningService;
import net.sf.jsignpdf.fx.preferences.PreferencesController;
import net.sf.jsignpdf.fx.preset.ManagePresetsDialog;
import net.sf.jsignpdf.fx.preset.Preset;
import net.sf.jsignpdf.fx.preset.PresetManager;
import net.sf.jsignpdf.fx.preset.PresetValidation;
import net.sf.jsignpdf.fx.util.RecentFilesManager;
import net.sf.jsignpdf.fx.viewmodel.DocumentViewModel;
import net.sf.jsignpdf.fx.viewmodel.SignaturePlacementViewModel;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.fx.viewmodel.VisibleSignatureCoordinator;
import net.sf.jsignpdf.types.PadesLevel;
import net.sf.jsignpdf.types.PageInfo;
import net.sf.jsignpdf.utils.PropertyProvider;
import net.sf.jsignpdf.utils.PropertyStoreFactory;

import static net.sf.jsignpdf.Constants.LOGGER;
import static net.sf.jsignpdf.Constants.RES;

/**
 * Controller for the main application window.
 */
public class MainWindowController {

    private static final String KEY_LAST_OPEN_DIR = "last.open.dir";
    private static final String KEY_LAST_ZOOM = "last.zoom";

    private Stage stage;
    private File lastOpenDir;
    private double lastZoomLevel = 1.0;
    private BasicSignerOptions options;
    private final DocumentViewModel documentVM = new DocumentViewModel();
    private final SigningOptionsViewModel signingVM = new SigningOptionsViewModel();
    private final SignaturePlacementViewModel placementVM = new SignaturePlacementViewModel();
    private final PdfRenderService renderService = new PdfRenderService();
    private final SigningService signingService = new SigningService();
    private final RecentFilesManager recentFilesManager = new RecentFilesManager();
    private final PresetManager presetManager = new PresetManager();
    private final EngineCapabilities engineCapabilities = new EngineCapabilities();
    private PdfPageView pdfPageView;
    private SignatureOverlay signatureOverlay;
    /** Holds the side panel node while it's detached from the SplitPane (hidden). */
    private Node detachedSidePanel;
    private PauseTransition shiftHintTimer;

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
    @FXML private MenuItem menuPreferences;
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
    @FXML private Label lblPadesLevel;
    @FXML private ChoiceBox<PadesLevel> cmbPadesLevel;
    @FXML private ComboBox<Preset> cmbPresets;
    @FXML private Button btnSign;

    // Content area
    @FXML private SplitPane splitPane;
    @FXML private Accordion sidePanelAccordion;
    @FXML private TitledPane padesLevelAccordionPane;
    @FXML private TitledPane signatureAppearanceAccordionPane;
    @FXML private TitledPane tsaAccordionPane;
    @FXML private TitledPane encryptionAccordionPane;
    @FXML private ScrollPane scrollPane;
    @FXML private StackPane pdfArea;
    @FXML private Label lblDropHint;

    // Status bar
    @FXML private Label lblStatus;
    @FXML private Label lblSigStateBadge;
    @FXML private Label lblSigCoords;
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
        signatureOverlay.setOnReplaceBlocked(this::showShiftHint);
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
            updateSigCoordsBadge();
        });

        // Keep coords badge in sync as the rectangle is moved or resized
        Runnable syncCoordsFromPlacement = () -> {
            capturePlacementToSigningVM();
            updateSigCoordsBadge();
        };
        placementVM.relXProperty().addListener((obs, o, n) -> syncCoordsFromPlacement.run());
        placementVM.relYProperty().addListener((obs, o, n) -> syncCoordsFromPlacement.run());
        placementVM.relWidthProperty().addListener((obs, o, n) -> syncCoordsFromPlacement.run());
        placementVM.relHeightProperty().addListener((obs, o, n) -> syncCoordsFromPlacement.run());

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

        // Coords badge: managed tracks visible so it takes no space when hidden
        lblSigCoords.managedProperty().bind(lblSigCoords.visibleProperty());

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
        loadPersistedViewState();

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
            capturePlacementToSigningVM();
            updateSigCoordsBadge();
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

        // Zoom level changes update combo and remember the last zoom (persisted on document open / exit)
        documentVM.zoomLevelProperty().addListener((obs, oldVal, newVal) -> {
            lastZoomLevel = newVal.doubleValue();
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
        setupEngineCapabilityGating();
    }

    /**
     * Drives the capability-based control gating off the active signing engine. The engine itself is now
     * selected in File &gt; Preferences (persisted to the {@code engine=} key in {@code advanced.properties});
     * this method seeds {@link EngineCapabilities} from that persisted value at startup and wires the
     * capability-driven disabling of toolbar buttons and accordion sections. {@link #onPreferences()}
     * refreshes the active engine after the dialog closes so switching it takes effect without a restart.
     */
    private void setupEngineCapabilityGating() {
        final EngineRegistry registry = EngineRegistry.getInstance();
        SigningEngine current = registry.findById(AppConfig.defaultEngineId())
                .or(registry::getDefault).orElse(null);
        engineCapabilities.activeEngineProperty().set(current);

        // Capability-driven section gating at the umbrella (accordion-pane) granularity. These panes
        // are not governed by the document-loaded disable logic, so binding their disableProperty here
        // is safe. (With OpenPDF — the only engine in phase 1 — every capability is present, so nothing
        // is disabled; the wiring exists for reduced-capability engines added in phase 2.)
        engineCapabilities.gate(btnTsa, Capability.TSA);
        setupPadesLevelSelector();
        if (padesLevelAccordionPane != null) {
            engineCapabilities.gate(padesLevelAccordionPane, Capability.PADES_BASELINE_B);
        }
        if (signatureAppearanceAccordionPane != null) {
            engineCapabilities.gate(signatureAppearanceAccordionPane, Capability.VISIBLE_SIGNATURE);
        }
        if (tsaAccordionPane != null) {
            engineCapabilities.gate(tsaAccordionPane, Capability.TSA);
        }
        if (encryptionAccordionPane != null) {
            engineCapabilities.gate(encryptionAccordionPane, Capability.ENCRYPTION_PASSWORD,
                    Capability.ENCRYPTION_CERTIFICATE);
        }

        // Field-level gating for controls that live inside side-panel sub-controllers. The append toggle
        // is the first of these: an engine without OVERWRITE_MODE (DSS) always appends, so the checkbox is
        // forced on and disabled there.
        if (signaturePropertiesController != null) {
            signaturePropertiesController.gateCapabilities(engineCapabilities);
        }

        // TODO(phase-2): field-level capability gating is still missing for the remaining sub-controller
        // controls that carry their own disable logic — hash algorithm, certification level, render-mode
        // items, permission checkboxes, proxy fields, and the PKCS#11/CloudFoxy keystore-type items (see
        // the control->capability table in design-doc/3.1-signing-engines.md). The CLI path is already
        // comprehensive via EngineMismatchValidator, which is the authoritative table to mirror here.
    }

    /**
     * Populates and gates the PAdES-level dropdown. The control is bound to the signing ViewModel's
     * {@code padesLevel} property and gated as a unit on {@link Capability#PADES_BASELINE_B}: an engine
     * that cannot produce even B is not a PAdES engine, so the dropdown disables/greys for OpenPDF and
     * enables for DSS. The level defaults to {@link PadesLevel#BASELINE_B} for PAdES engines (so the
     * dropdown isn't empty) and is cleared for non-PAdES engines (a stale level would otherwise trip the
     * {@link EngineMismatchValidator}).
     */
    private void setupPadesLevelSelector() {
        if (cmbPadesLevel == null) {
            return;
        }
        cmbPadesLevel.getItems().setAll(PadesLevel.values());
        cmbPadesLevel.setConverter(new StringConverter<PadesLevel>() {
            @Override
            public String toString(PadesLevel level) {
                return level == null ? "" : level.shortName();
            }

            @Override
            public PadesLevel fromString(String s) {
                return PadesLevel.fromString(s);
            }
        });
        cmbPadesLevel.valueProperty().bindBidirectional(signingVM.padesLevelProperty());
        engineCapabilities.gate(cmbPadesLevel, Capability.PADES_BASELINE_B);
        if (lblPadesLevel != null) {
            engineCapabilities.gate(lblPadesLevel, Capability.PADES_BASELINE_B);
        }

        // Seed a sensible default for the current engine and keep it in step when the engine changes.
        applyPadesLevelDefaultForEngine(engineCapabilities.activeEngineProperty().get());
        engineCapabilities.activeEngineProperty().addListener(
                (obs, oldEngine, newEngine) -> applyPadesLevelDefaultForEngine(newEngine));
        // Re-apply the engine-appropriate value whenever the level is changed by an options/preset load or
        // a reset (those call syncFromOptions/resetToDefaults). This keeps the dropdown from going empty
        // while a PAdES engine is active, and clears a stale level loaded under a non-PAdES engine (which
        // would otherwise trip the EngineMismatchValidator at sign time).
        signingVM.padesLevelProperty().addListener((obs, oldLevel, newLevel) ->
                applyPadesLevelDefaultForEngine(engineCapabilities.activeEngineProperty().get()));
    }

    /**
     * Picks the PAdES level appropriate for the given engine: {@link PadesLevel#BASELINE_B} as a non-empty
     * default when the engine is PAdES-capable and no level is set yet, or {@code null} when the engine is
     * not PAdES-capable (OpenPDF) so the level can't reach the engine mismatch validator. An explicit
     * user choice (e.g. LT/LTA) is preserved while a PAdES engine stays active.
     */
    private void applyPadesLevelDefaultForEngine(SigningEngine engine) {
        boolean padesCapable = engine != null
                && engine.capabilities().contains(Capability.PADES_BASELINE_B);
        if (padesCapable) {
            if (signingVM.padesLevelProperty().get() == null) {
                signingVM.padesLevelProperty().set(PadesLevel.BASELINE_B);
            }
        } else if (signingVM.padesLevelProperty().get() != null) {
            signingVM.padesLevelProperty().set(null);
        }
    }

    private void setupPresetCombo() {
        cmbPresets.setItems(presetManager.getPresets());
        // JavaFX doesn't restore promptText in the button cell after clearSelection()+setValue(null);
        // a custom button cell fixes that by reading the current promptText when item is null.
        cmbPresets.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Preset item, boolean empty) {
                super.updateItem(item, empty);
                setText(item != null ? item.toString() : cmbPresets.getPromptText());
            }
        });
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

    @FXML
    private void onPreferences() {
        boolean saved = PreferencesController.show(stage);
        if (saved) {
            // The engine may have been changed in Preferences. Re-seed the capability bindings from the
            // persisted value so toolbar buttons and accordion sections re-gate immediately (no restart).
            refreshActiveEngineFromConfig();
        }
    }

    /**
     * Re-seeds {@link EngineCapabilities#activeEngineProperty()} from the engine persisted in
     * {@code advanced.properties}, so capability gating (and the PAdES-level default) re-evaluates after
     * the engine changes via Preferences or a settings reset.
     */
    private void refreshActiveEngineFromConfig() {
        EngineRegistry registry = EngineRegistry.getInstance();
        registry.findById(AppConfig.defaultEngineId()).or(registry::getDefault)
                .ifPresent(e -> engineCapabilities.activeEngineProperty().set(e));
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
        updateSigCoordsBadge();
    }

    private void updateSigCoordsBadge() {
        if (lblSigCoords == null) return;
        boolean show = signingVM.visibleProperty().get()
                    && documentVM.documentLoadedProperty().get()
                    && placementVM.isPlaced();
        lblSigCoords.setVisible(show);
        if (show) {
            lblSigCoords.setText(String.format("(%d, %d) — (%d, %d)",
                    Math.round(signingVM.positionLLXProperty().get()),
                    Math.round(signingVM.positionLLYProperty().get()),
                    Math.round(signingVM.positionURXProperty().get()),
                    Math.round(signingVM.positionURYProperty().get())));
        }
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
            showShiftHint();
        } else if (documentVM.isDocumentLoaded()) {
            cancelShiftHint();
            updateStatusForDocument();
        }
    }

    /** Briefly show the Shift-to-replace hint, then revert to filename/page. */
    private void showShiftHint() {
        if (!documentVM.isDocumentLoaded() || !placementVM.isPlaced()) return;
        final String hintText = RES.get("jfx.gui.status.shiftToReplace");
        updateStatus(hintText);
        if (shiftHintTimer == null) {
            shiftHintTimer = new PauseTransition(Duration.seconds(4));
        } else {
            shiftHintTimer.stop();
        }
        // Only revert if the hint is still the message being shown
        shiftHintTimer.setOnFinished(ev -> {
            if (hintText.equals(lblStatus.getText())) {
                updateStatusForDocument();
            }
        });
        shiftHintTimer.playFromStart();
    }

    private void cancelShiftHint() {
        if (shiftHintTimer != null) shiftHintTimer.stop();
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
        NativeFileChooser fc = new NativeFileChooser()
                .setTitle(RES.get("jfx.gui.dialog.openPdf.title"))
                .addFilter(ExtensionFilter.of("PDF Files", "*.pdf"));
        if (lastOpenDir != null) {
            fc.setInitialDirectory(lastOpenDir);
        }
        File file = fc.showOpenDialog(stage);
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
        saveViewStateToConfig();
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

            // The wiped config reverts the engine to the bundled default; re-seed capability gating and
            // the PAdES-level default to match.
            refreshActiveEngineFromConfig();

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
        promptForOutputFile();
    }

    /**
     * Opens the Save dialog and stores the chosen path in the signing VM. Returns true
     * if the user picked a file, false if they cancelled. Skips the doc-portal path as
     * an initial directory — the portal silently ignores it and writes there don't
     * persist on the host (see {@link Sandbox#isDocPortalPath}).
     */
    private boolean promptForOutputFile() {
        String current = signingVM.outFileProperty().get();
        if (current == null || current.isEmpty()) {
            current = suggestedOutFileFor(documentVM.getDocumentFile());
        }
        NativeFileChooser fc = new NativeFileChooser()
                .setTitle(RES.get("jfx.gui.dialog.selectOutputPdf"))
                .addFilter(ExtensionFilter.of("PDF Files", "*.pdf"));
        if (current != null && !current.isEmpty()) {
            File currentFile = new File(current);
            if (!Sandbox.isDocPortalPath(current)) {
                File parent = currentFile.getParentFile();
                if (parent != null && parent.isDirectory()) {
                    fc.setInitialDirectory(parent);
                }
            }
            fc.setInitialFileName(currentFile.getName());
        }
        File file = fc.showSaveDialog(stage);
        if (file == null) return false;
        signingVM.outFileProperty().set(file.getAbsolutePath());
        return true;
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

        // In a sandbox, the auto-derived "<input>_signed.pdf" lands inside the doc
        // portal FUSE mount and is not exposed back to the host. Force a Save dialog
        // (routed through the portal) so the user picks a real persistable location.
        String effectiveOut = signingVM.outFileProperty().get();
        if (effectiveOut == null || effectiveOut.isEmpty()) {
            effectiveOut = suggestedOutFileFor(documentVM.getDocumentFile());
        }
        if (Sandbox.isDocPortalPath(effectiveOut)) {
            if (!promptForOutputFile()) {
                return;
            }
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
        lastOpenDir = file.getParentFile();
        saveViewStateToConfig();
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
            documentVM.setZoomLevel(lastZoomLevel);

            lblDropHint.setVisible(false);
            setDocumentControlsDisabled(false);
            lblPageCount.setText("/ " + pages);
            txtPageNumber.setText("1");
            cmbZoom.setValue(Math.round(lastZoomLevel * 100) + "%");

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

    private void loadPersistedViewState() {
        try {
            PropertyProvider cfg = PropertyStoreFactory.getInstance().mainConfig();
            String dir = cfg.getProperty(KEY_LAST_OPEN_DIR);
            if (dir != null && !dir.isEmpty()) {
                File f = new File(dir);
                if (f.isDirectory()) {
                    lastOpenDir = f;
                }
            }
            String zoom = cfg.getProperty(KEY_LAST_ZOOM);
            if (zoom != null) {
                lastZoomLevel = Double.parseDouble(zoom);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not load persisted view state", e);
        }
    }

    private void saveViewStateToConfig() {
        try {
            PropertyProvider cfg = PropertyStoreFactory.getInstance().mainConfig();
            if (lastOpenDir != null) {
                cfg.setProperty(KEY_LAST_OPEN_DIR, lastOpenDir.getAbsolutePath());
            }
            cfg.setProperty(KEY_LAST_ZOOM, String.valueOf(lastZoomLevel));
            cfg.save();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not persist view state", e);
        }
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
