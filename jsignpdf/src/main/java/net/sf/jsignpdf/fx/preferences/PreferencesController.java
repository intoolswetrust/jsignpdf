package net.sf.jsignpdf.fx.preferences;

import static net.sf.jsignpdf.Constants.RES;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.engine.EngineRegistry;
import net.sf.jsignpdf.engine.SigningEngine;
import net.sf.jsignpdf.fx.util.NativeFileChooser;
import net.sf.jsignpdf.fx.util.NativeFileChooser.ExtensionFilter;
import net.sf.jsignpdf.fx.util.OutputSuffixValidation;
import net.sf.jsignpdf.fx.util.Sandbox;
import net.sf.jsignpdf.pkcs11.Pkcs11Catalog;
import net.sf.jsignpdf.pkcs11.Pkcs11ConfigText;
import net.sf.jsignpdf.pkcs11.Pkcs11Detector;
import net.sf.jsignpdf.pkcs11.Pkcs11Profiles;
import net.sf.jsignpdf.pkcs11.ProfileStatus;
import net.sf.jsignpdf.pkcs11.ProviderMode;
import net.sf.jsignpdf.ssl.SSLInitializer;
import net.sf.jsignpdf.utils.AdvancedConfig;
import net.sf.jsignpdf.utils.AppConfig;
import net.sf.jsignpdf.utils.ConfigLocationResolver;
import net.sf.jsignpdf.utils.FontUtils;
import net.sf.jsignpdf.utils.PKCS11Utils;
import net.sf.jsignpdf.utils.PropertyStoreFactory;
import net.sf.jsignpdf.utils.SupportedLanguages;
import net.sf.jsignpdf.utils.UiLocale;

/**
 * Controller for the Preferences dialog. Holds the live editing state in a {@link PreferencesViewModel}, mirrors UI changes
 * back to the VM and on OK persists the VM via {@link AdvancedConfig#save()} plus the PKCS#11 profiles via
 * {@link Pkcs11Profiles#applyEdits(List)}.
 */
public class PreferencesController {

    private static final List<String> ENCODINGS = Arrays.asList(
            "Cp1250", "Cp1252", "Cp1257", "Identity-H", "Identity-V", "MacRoman");
    private static final List<String> HASH_ALGORITHMS = Arrays.asList(
            "SHA-1", "SHA-256", "SHA-384", "SHA-512", "RIPEMD160");
    private static final String DEFAULTS_RESOURCE = "/net/sf/jsignpdf/conf/advanced.default.properties";

    @FXML private TabPane tabPane;
    @FXML private Tab tabGeneral;
    @FXML private Tab tabFont;
    @FXML private Tab tabCertificate;
    @FXML private Tab tabNetwork;
    @FXML private Tab tabPdfRender;
    @FXML private Tab tabTsa;
    @FXML private Tab tabDss;
    @FXML private Tab tabPkcs11;

    @FXML private ChoiceBox<String> cmbUiLanguage;
    @FXML private ChoiceBox<SigningEngine> cmbEngine;
    @FXML private CheckBox chkDebug;
    @FXML private TextField txtOutputSuffix;
    @FXML private TextField txtOutputSuffixTimestamp;

    @FXML private TextField txtFontPath;
    @FXML private Button btnFontPathBrowse;
    @FXML private TextField txtFontName;
    @FXML private ComboBox<String> cmbFontEncoding;

    @FXML private CheckBox chkCertCheckValidity;
    @FXML private CheckBox chkCertCheckKeyUsage;
    @FXML private CheckBox chkCertCheckCriticalExtensions;

    @FXML private CheckBox chkRelaxSslSecurity;

    @FXML private VBox vboxPdfLibs;

    @FXML private ComboBox<String> cmbTsaHashAlgorithm;

    @FXML private CheckBox chkDssOnlineEnabled;
    @FXML private CheckBox chkDssEuEnabled;
    @FXML private TextField txtDssLotlUrls;
    @FXML private CheckBox chkDssLotlMraSupport;
    @FXML private TextField txtDssCertFiles;
    @FXML private TextField txtDssCertUrls;
    @FXML private TextField txtDssTruststoreFile;
    @FXML private TextField txtDssTruststoreType;
    @FXML private TextField txtDssTruststorePassword;
    @FXML private CheckBox chkDssSystemStore;
    @FXML private CheckBox chkDssAllowUntrusted;

    @FXML private Label lblPkcs11Flatpak;
    @FXML private ListView<Pkcs11ProfileDraft> lstPkcs11Profiles;
    @FXML private Button btnPkcs11Duplicate;
    @FXML private Button btnPkcs11Rename;
    @FXML private Button btnPkcs11Remove;
    @FXML private VBox boxPkcs11Editor;
    @FXML private Label lblPkcs11NoSelection;
    @FXML private Label lblPkcs11Path;
    @FXML private Label lblPkcs11Status;
    @FXML private TextField txtPkcs11Label;
    @FXML private ChoiceBox<ProviderMode> cmbPkcs11Provider;
    @FXML private TextField txtPkcs11Library;
    @FXML private TextArea txtPkcs11Body;
    @FXML private Label lblPkcs11EmptyHint;
    @FXML private Button btnPkcs11ResetSample;

    private Pkcs11ProfileDraft selectedDraft;
    private boolean pkcs11Syncing;

    private PreferencesViewModel vm;
    private CheckBox chkLibJpedal;
    private CheckBox chkLibPdfbox;
    private CheckBox chkLibOpenpdf;

    @FXML
    private void initialize() {
        // Language selector: empty tag = "System default", then the bundled translations.
        cmbUiLanguage.getItems().add("");
        cmbUiLanguage.getItems().addAll(SupportedLanguages.tags());
        cmbUiLanguage.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String tag) {
                if (tag == null || tag.isEmpty()) {
                    return RES.get("jfx.gui.preferences.general.language.system",
                            SupportedLanguages.displayName(UiLocale.systemDefault()));
                }
                return SupportedLanguages.displayName(Locale.forLanguageTag(tag));
            }

            @Override
            public String fromString(String s) {
                return null;
            }
        });

        cmbEngine.getItems().setAll(EngineRegistry.getInstance().listAll());
        cmbEngine.setConverter(new StringConverter<SigningEngine>() {
            @Override
            public String toString(SigningEngine engine) {
                return engine == null ? "" : engine.displayName();
            }

            @Override
            public SigningEngine fromString(String s) {
                return null;
            }
        });
        cmbFontEncoding.getItems().addAll(ENCODINGS);
        cmbTsaHashAlgorithm.getItems().addAll(HASH_ALGORITHMS);
        cmbPkcs11Provider.getItems().setAll(ProviderMode.values());
        cmbPkcs11Provider.setConverter(new StringConverter<ProviderMode>() {
            @Override
            public String toString(ProviderMode mode) {
                return mode == null ? "" : mode.value();
            }

            @Override
            public ProviderMode fromString(String s) {
                return ProviderMode.parse(s);
            }
        });
        boolean flatpak = Sandbox.isLinux() && Sandbox.isSandboxed();
        lblPkcs11Flatpak.setVisible(flatpak);
        lblPkcs11Flatpak.setManaged(flatpak);
        // Empty-hint visibility is bound when bind() runs.
    }

    /**
     * Public entry point — loads FXML, builds a VM from the current {@link AdvancedConfig} plus the PKCS#11 profiles, shows the
     * dialog modally and persists changes on OK. Returns true if the user pressed OK and the save succeeded.
     */
    public static boolean show(Stage owner) {
        ResourceBundle bundle = UiLocale.bundle();
        FXMLLoader loader = new FXMLLoader(
                PreferencesController.class.getResource("/net/sf/jsignpdf/fx/view/Preferences.fxml"), bundle);
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            Constants.LOGGER.log(Level.SEVERE, "Failed to load Preferences.fxml", e);
            return false;
        }
        PreferencesController controller = loader.getController();

        AdvancedConfig cfg = PropertyStoreFactory.getInstance().advancedConfig();
        Pkcs11Profiles profiles = Pkcs11Profiles.getInstance();
        profiles.reload();

        PreferencesViewModel vm = new PreferencesViewModel();
        vm.loadFrom(cfg);
        vm.loadPkcs11Profiles(profiles.list(), profiles::status);
        controller.bind(vm);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(RES.get("jfx.gui.preferences.title"));
        dialog.setHeaderText(null);
        dialog.setResizable(true);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(PreferencesController.class
                .getResource("/net/sf/jsignpdf/fx/styles/jsignpdf.css").toExternalForm());

        ButtonType resetSection = new ButtonType(RES.get("jfx.gui.preferences.button.resetSection"), ButtonData.OTHER);
        ButtonType cancel = new ButtonType(RES.get("jfx.gui.preferences.button.cancel"), ButtonData.CANCEL_CLOSE);
        ButtonType ok = new ButtonType(RES.get("jfx.gui.preferences.button.ok"), ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(resetSection, cancel, ok);
        pane.setContent(root);
        pane.setPrefSize(720, 560);
        pane.setMinSize(640, 480);

        // Hijack the "Reset section" button to restore current-tab defaults without closing the dialog.
        Button resetButton = (Button) pane.lookupButton(resetSection);
        resetButton.addEventFilter(ActionEvent.ACTION, e -> {
            controller.resetActiveTabToDefaults();
            e.consume();
        });

        // OK handler runs validation and saves. On validation failure, consume the event so the dialog stays open.
        Button okButton = (Button) pane.lookupButton(ok);
        okButton.addEventFilter(ActionEvent.ACTION, e -> {
            if (!controller.validate()) {
                e.consume();
            }
        });

        java.util.Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ok) {
            return controller.persist(cfg);
        }
        return false;
    }

    void bind(PreferencesViewModel viewModel) {
        this.vm = viewModel;

        cmbUiLanguage.valueProperty().bindBidirectional(vm.uiLanguageProperty());

        // The engine ChoiceBox holds SigningEngine objects; the VM stores the engine id string. Keep the two
        // in sync manually (both directions, so "Reset section" reselecting the default reflects in the combo).
        selectEngineById(vm.engineIdProperty().get());
        cmbEngine.getSelectionModel().selectedItemProperty().addListener((obs, oldEngine, sel) -> {
            if (sel != null) {
                vm.engineIdProperty().set(sel.id());
            }
        });
        vm.engineIdProperty().addListener((obs, oldId, newId) -> {
            SigningEngine sel = cmbEngine.getSelectionModel().getSelectedItem();
            if (sel == null || !sel.id().equals(newId)) {
                selectEngineById(newId);
            }
        });

        chkDebug.selectedProperty().bindBidirectional(vm.debugProperty());
        txtOutputSuffix.textProperty().bindBidirectional(vm.outputSuffixProperty());
        txtOutputSuffixTimestamp.textProperty().bindBidirectional(vm.outputSuffixTimestampProperty());

        txtFontPath.textProperty().bindBidirectional(vm.fontPathProperty());
        txtFontName.textProperty().bindBidirectional(vm.fontNameProperty());
        cmbFontEncoding.valueProperty().bindBidirectional(vm.fontEncodingProperty());

        chkCertCheckValidity.selectedProperty().bindBidirectional(vm.checkValidityProperty());
        chkCertCheckKeyUsage.selectedProperty().bindBidirectional(vm.checkKeyUsageProperty());
        chkCertCheckCriticalExtensions.selectedProperty().bindBidirectional(vm.checkCriticalExtensionsProperty());

        chkRelaxSslSecurity.selectedProperty().bindBidirectional(vm.relaxSslSecurityProperty());

        cmbTsaHashAlgorithm.valueProperty().bindBidirectional(vm.tsaHashAlgorithmProperty());

        chkDssOnlineEnabled.selectedProperty().bindBidirectional(vm.dssOnlineEnabledProperty());
        chkDssEuEnabled.selectedProperty().bindBidirectional(vm.dssEuEnabledProperty());
        txtDssLotlUrls.textProperty().bindBidirectional(vm.dssLotlUrlsProperty());
        chkDssLotlMraSupport.selectedProperty().bindBidirectional(vm.dssLotlMraSupportProperty());
        txtDssCertFiles.textProperty().bindBidirectional(vm.dssCertFilesProperty());
        txtDssCertUrls.textProperty().bindBidirectional(vm.dssCertUrlsProperty());
        txtDssTruststoreFile.textProperty().bindBidirectional(vm.dssTruststoreFileProperty());
        txtDssTruststoreType.textProperty().bindBidirectional(vm.dssTruststoreTypeProperty());
        txtDssTruststorePassword.textProperty().bindBidirectional(vm.dssTruststorePasswordProperty());
        chkDssSystemStore.selectedProperty().bindBidirectional(vm.dssSystemStoreProperty());
        chkDssAllowUntrusted.selectedProperty().bindBidirectional(vm.dssAllowUntrustedProperty());

        bindPkcs11();

        rebuildPdfLibsPanel();
        // Re-render the PDF-libs panel whenever the order changes, so rows visually re-sort.
        Runnable rerender = this::rebuildPdfLibsPanel;
        vm.pdfLibJpedalOrderProperty().addListener((o, a, b) -> rerender.run());
        vm.pdfLibPdfboxOrderProperty().addListener((o, a, b) -> rerender.run());
        vm.pdfLibOpenpdfOrderProperty().addListener((o, a, b) -> rerender.run());
    }

    private void selectEngineById(String id) {
        EngineRegistry registry = EngineRegistry.getInstance();
        SigningEngine target = registry.findById(id).or(registry::getDefault).orElse(null);
        if (target != null) {
            cmbEngine.getSelectionModel().select(target);
        }
    }

    private void rebuildPdfLibsPanel() {
        vboxPdfLibs.getChildren().clear();
        // Iterate in current order (1, 2, 3).
        for (int i = 1; i <= 3; i++) {
            String lib = libAt(i);
            if (lib == null) continue;
            vboxPdfLibs.getChildren().add(buildPdfLibRow(lib));
        }
    }

    private String libAt(int order) {
        if (vm.pdfLibJpedalOrderProperty().get() == order) return PreferencesViewModel.LIB_JPEDAL;
        if (vm.pdfLibPdfboxOrderProperty().get() == order) return PreferencesViewModel.LIB_PDFBOX;
        if (vm.pdfLibOpenpdfOrderProperty().get() == order) return PreferencesViewModel.LIB_OPENPDF;
        return null;
    }

    private HBox buildPdfLibRow(String lib) {
        CheckBox cb = new CheckBox(RES.get("jfx.gui.preferences.pdfRender.lib." + lib));
        switch (lib) {
            case PreferencesViewModel.LIB_JPEDAL -> {
                cb.selectedProperty().bindBidirectional(vm.pdfLibJpedalProperty());
                chkLibJpedal = cb;
            }
            case PreferencesViewModel.LIB_PDFBOX -> {
                cb.selectedProperty().bindBidirectional(vm.pdfLibPdfboxProperty());
                chkLibPdfbox = cb;
            }
            case PreferencesViewModel.LIB_OPENPDF -> {
                cb.selectedProperty().bindBidirectional(vm.pdfLibOpenpdfProperty());
                chkLibOpenpdf = cb;
            }
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button up = new Button("▲");
        up.setTooltip(new javafx.scene.control.Tooltip(RES.get("jfx.gui.preferences.pdfRender.moveUp")));
        up.setOnAction(e -> vm.moveUp(lib));
        up.setDisable(vm.orderOf(lib) == 1);
        Button down = new Button("▼");
        down.setTooltip(new javafx.scene.control.Tooltip(RES.get("jfx.gui.preferences.pdfRender.moveDown")));
        down.setOnAction(e -> vm.moveDown(lib));
        down.setDisable(vm.orderOf(lib) == 3);
        HBox row = new HBox(8, cb, spacer, up, down);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void bindPkcs11() {
        lstPkcs11Profiles.setItems(vm.pkcs11Profiles());
        lstPkcs11Profiles.setCellFactory(lv -> new ListCell<Pkcs11ProfileDraft>() {
            @Override
            protected void updateItem(Pkcs11ProfileDraft item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    String text = item.displayLabel();
                    ProfileStatus st = item.status();
                    if (st != null && st.state() != ProfileStatus.State.NOT_LOADED) {
                        text = text + " [" + RES.get("jfx.gui.preferences.pkcs11.status." + st.state().name()) + "]";
                    }
                    setText(text);
                    setTooltip(st != null && st.message() != null ? new Tooltip(st.message()) : null);
                }
            }
        });
        lstPkcs11Profiles.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldD, newD) -> selectPkcs11Draft(newD));
        txtPkcs11Label.textProperty().addListener((obs, o, n) -> {
            if (!pkcs11Syncing && selectedDraft != null) {
                editPkcs11(() -> selectedDraft.setLabel(n));
                lstPkcs11Profiles.refresh();
            }
        });
        cmbPkcs11Provider.valueProperty().addListener((obs, o, n) -> {
            if (!pkcs11Syncing && selectedDraft != null && n != null) {
                editPkcs11(() -> selectedDraft.setProvider(n));
            }
        });
        txtPkcs11Library.textProperty().addListener((obs, o, n) -> {
            if (!pkcs11Syncing && selectedDraft != null) {
                editPkcs11(() -> selectedDraft.setLibrary(n));
            }
        });
        txtPkcs11Body.textProperty().addListener((obs, o, n) -> {
            if (!pkcs11Syncing && selectedDraft != null) {
                selectedDraft.bodyProperty().set(n == null ? "" : n);
                refreshPkcs11Fields(false);
                lstPkcs11Profiles.refresh();
            }
        });
        lblPkcs11EmptyHint.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> selectedDraft != null && selectedDraft.isDefault()
                        && (txtPkcs11Body.getText() == null || txtPkcs11Body.getText().isBlank()),
                txtPkcs11Body.textProperty()));
        lblPkcs11EmptyHint.managedProperty().bind(lblPkcs11EmptyHint.visibleProperty());
        if (!vm.pkcs11Profiles().isEmpty()) {
            lstPkcs11Profiles.getSelectionModel().selectFirst();
        } else {
            selectPkcs11Draft(null);
        }
    }

    private void editPkcs11(Runnable edit) {
        pkcs11Syncing = true;
        try {
            edit.run();
            txtPkcs11Body.setText(selectedDraft.body());
        } finally {
            pkcs11Syncing = false;
        }
    }

    private void selectPkcs11Draft(Pkcs11ProfileDraft draft) {
        selectedDraft = draft;
        boolean has = draft != null;
        boxPkcs11Editor.setVisible(has);
        boxPkcs11Editor.setManaged(has);
        lblPkcs11NoSelection.setVisible(!has);
        lblPkcs11NoSelection.setManaged(!has);
        btnPkcs11Duplicate.setDisable(!has);
        btnPkcs11Remove.setDisable(!has);
        btnPkcs11Rename.setDisable(!has || draft.isDefault());
        refreshPkcs11Fields(true);
    }

    private void refreshPkcs11Fields(boolean includeBody) {
        pkcs11Syncing = true;
        try {
            Pkcs11ProfileDraft d = selectedDraft;
            if (d == null) {
                txtPkcs11Label.setText("");
                txtPkcs11Library.setText("");
                cmbPkcs11Provider.setValue(null);
                txtPkcs11Body.setText("");
                lblPkcs11Path.setText("");
                lblPkcs11Status.setText("");
                return;
            }
            txtPkcs11Label.setText(StringUtils.defaultString(d.label()));
            txtPkcs11Library.setText(StringUtils.defaultString(d.library()));
            cmbPkcs11Provider.setValue(d.provider());
            if (includeBody) {
                txtPkcs11Body.setText(d.body());
            }
            Path file = Pkcs11Profiles.getInstance().fileFor(d.id());
            String displayed = file == null ? "—" : file.toAbsolutePath().toString();
            lblPkcs11Path.setText(MessageFormat.format(RES.get("jfx.gui.preferences.pkcs11.pathLabel"), displayed));
            ProfileStatus st = d.status();
            String status = st == null ? "" : RES.get("jfx.gui.preferences.pkcs11.status." + st.state().name());
            if (st != null && st.message() != null) {
                status = status + ": " + st.message();
            }
            lblPkcs11Status.setText(status);
            lblPkcs11Status.setVisible(!status.isEmpty());
            lblPkcs11Status.setManaged(!status.isEmpty());
        } finally {
            pkcs11Syncing = false;
        }
    }

    private String askPkcs11Id(String initial, Pkcs11ProfileDraft self) {
        TextInputDialog dialog = new TextInputDialog(initial);
        dialog.setTitle(RES.get("jfx.gui.preferences.pkcs11.newId.title"));
        dialog.setHeaderText(null);
        dialog.setContentText(RES.get("jfx.gui.preferences.pkcs11.newId.prompt"));
        initOwner(dialog);
        while (true) {
            java.util.Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return null;
            }
            String id = result.get().trim();
            String problem = pkcs11IdProblem(id, self);
            if (problem == null) {
                return id;
            }
            showError(problem);
            dialog.getEditor().setText(id);
        }
    }

    private String pkcs11IdProblem(String id, Pkcs11ProfileDraft self) {
        if (!Pkcs11Profiles.isValidId(id)) {
            return RES.get("console.pkcs11.invalidId", id);
        }
        for (Pkcs11ProfileDraft d : vm.pkcs11Profiles()) {
            if (d != self && d.id().equalsIgnoreCase(id)) {
                return RES.get("console.pkcs11.caseCollision", id, d.id());
            }
        }
        if (Pkcs11Profiles.DEFAULT_ID.equalsIgnoreCase(id) && (self == null || !self.isDefault())
                && vm.pkcs11Profiles().stream().anyMatch(Pkcs11ProfileDraft::isDefault)) {
            return RES.get("console.pkcs11.reservedId", id);
        }
        return null;
    }

    private String uniquePkcs11Id(String base) {
        String id = base;
        int n = 2;
        while (pkcs11IdProblem(id, null) != null) {
            id = base + "-" + n++;
        }
        return id;
    }

    private void addPkcs11Draft(String id, String body) {
        String b = Pkcs11Profiles.DEFAULT_ID.equalsIgnoreCase(id) ? body
                : Pkcs11ConfigText.withValue(body, Pkcs11ConfigText.KEY_NAME, id);
        Pkcs11ProfileDraft d = new Pkcs11ProfileDraft(null, id, b, null);
        vm.pkcs11Profiles().add(d);
        lstPkcs11Profiles.getSelectionModel().select(d);
    }

    @FXML
    private void onPkcs11Add() {
        String id = askPkcs11Id("", null);
        if (id != null) {
            addPkcs11Draft(id, PKCS11Utils.getSampleConfig());
        }
    }

    @FXML
    private void onPkcs11Duplicate() {
        if (selectedDraft == null) {
            return;
        }
        String id = askPkcs11Id(uniquePkcs11Id(selectedDraft.id() + "-copy"), null);
        if (id != null) {
            addPkcs11Draft(id, selectedDraft.body());
        }
    }

    @FXML
    private void onPkcs11Rename() {
        if (selectedDraft == null) {
            return;
        }
        if (selectedDraft.isDefault()) {
            showError(RES.get("jfx.gui.preferences.pkcs11.defaultNoRename"));
            return;
        }
        String id = askPkcs11Id(selectedDraft.id(), selectedDraft);
        if (id != null) {
            selectedDraft.idProperty().set(id);
            lstPkcs11Profiles.refresh();
            refreshPkcs11Fields(false);
        }
    }

    @FXML
    private void onPkcs11Remove() {
        if (selectedDraft != null) {
            vm.pkcs11Profiles().remove(selectedDraft);
        }
    }

    @FXML
    private void onPkcs11BrowseLibrary() {
        NativeFileChooser fc = new NativeFileChooser().setTitle(RES.get("jfx.gui.preferences.pkcs11.library.browse"));
        String os = Pkcs11Detector.normalizeOs(System.getProperty("os.name"));
        String filter = RES.get("jfx.gui.preferences.pkcs11.library.filter");
        switch (os) {
            case "windows" -> fc.addFilter(ExtensionFilter.of(filter, "*.dll"));
            case "macos" -> fc.addFilter(ExtensionFilter.of(filter, "*.dylib", "*.so"));
            default -> fc.addFilter(ExtensionFilter.of(filter, "*.so", "*.so.*"));
        }
        fc.addFilter(ExtensionFilter.of("All Files", "*.*"));
        String current = txtPkcs11Library.getText();
        if (StringUtils.isNotBlank(current)) {
            File parent = new File(current).getParentFile();
            if (parent != null && parent.isDirectory()) {
                fc.setInitialDirectory(parent);
            }
        }
        File picked = fc.showOpenDialog(txtPkcs11Library.getScene().getWindow());
        if (picked != null) {
            txtPkcs11Library.setText(picked.getAbsolutePath());
        }
    }

    @FXML
    private void onPkcs11Detect() {
        Pkcs11Detector detector = Pkcs11Detector.forCurrentPlatform(Sandbox.isLinux() && Sandbox.isSandboxed());
        List<Pkcs11Detector.Candidate> found = detector.detect(Pkcs11Catalog.loadBundled());
        if (found.isEmpty()) {
            showInfo(RES.get("jfx.gui.preferences.pkcs11.detect.none"));
            return;
        }
        Pkcs11Detector.Candidate picked = choose(RES.get("jfx.gui.preferences.pkcs11.detect.title"),
                RES.get("jfx.gui.preferences.pkcs11.detect.header"), found,
                c -> c.entry().label() + " — " + c.library());
        if (picked != null) {
            createFromCatalog(picked.entry(), picked.library().toString());
        }
    }

    @FXML
    private void onPkcs11Catalog() {
        Pkcs11Detector detector = Pkcs11Detector.forCurrentPlatform(Sandbox.isLinux() && Sandbox.isSandboxed());
        List<Pkcs11Catalog.Entry> entries = detector.applicableEntries(Pkcs11Catalog.loadBundled());
        if (entries.isEmpty()) {
            showInfo(RES.get("jfx.gui.preferences.pkcs11.catalog.none"));
            return;
        }
        Pkcs11Catalog.Entry entry = choose(RES.get("jfx.gui.preferences.pkcs11.catalog.title"),
                RES.get("jfx.gui.preferences.pkcs11.catalog.header"), entries, Pkcs11Catalog.Entry::label);
        if (entry == null) {
            return;
        }
        List<Path> found = detector.find(entry);
        String library;
        if (found.size() == 1) {
            library = found.get(0).toString();
        } else if (found.size() > 1) {
            Path p = choose(RES.get("jfx.gui.preferences.pkcs11.detect.title"),
                    RES.get("jfx.gui.preferences.pkcs11.detect.header"), found, Path::toString);
            if (p == null) {
                return;
            }
            library = p.toString();
        } else {
            List<String> patterns = detector.expandedPatterns(entry);
            library = patterns.stream().filter(x -> !x.contains("*") && !x.contains("?")).findFirst()
                    .orElse(patterns.isEmpty() ? "" : patterns.get(0));
        }
        createFromCatalog(entry, library);
    }

    private void createFromCatalog(Pkcs11Catalog.Entry entry, String library) {
        String id = uniquePkcs11Id(entry.id());
        StringBuilder text = new StringBuilder();
        text.append(RES.get("jfx.gui.preferences.pkcs11.confirm.library", library)).append('\n');
        boolean exists = !library.isEmpty() && Files.isRegularFile(Path.of(library));
        text.append(RES.get(exists ? "jfx.gui.preferences.pkcs11.confirm.exists"
                : "jfx.gui.preferences.pkcs11.confirm.missing")).append("\n\n");
        if (StringUtils.isNotBlank(entry.notes())) {
            text.append(RES.get("jfx.gui.preferences.pkcs11.confirm.notes", entry.notes())).append('\n');
        }
        Pkcs11Catalog.Attestation latest = Pkcs11Catalog.latestAttestation(entry);
        if (latest != null) {
            text.append(RES.get("jfx.gui.preferences.pkcs11.confirm.tested", String.valueOf(entry.tested().size()),
                    StringUtils.defaultString(latest.date(), "?"),
                    StringUtils.defaultIfBlank(latest.osVersion(), latest.os())));
        } else {
            text.append(RES.get("jfx.gui.preferences.pkcs11.confirm.untested"));
        }
        text.append('\n').append(RES.get("jfx.gui.preferences.pkcs11.confirm.reviewed"));
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(RES.get("jfx.gui.preferences.pkcs11.confirm.title"));
        confirm.setHeaderText(RES.get("jfx.gui.preferences.pkcs11.confirm.header", id));
        confirm.setContentText(text.toString());
        confirm.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        initOwner(confirm);
        java.util.Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK) {
            addPkcs11Draft(id, Pkcs11Catalog.toProfileBody(entry, id, library));
        }
    }

    private <T> T choose(String title, String header, List<T> items, Function<T, String> label) {
        List<String> labels = new ArrayList<>();
        for (T item : items) {
            labels.add(label.apply(item));
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(labels.get(0), labels);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        initOwner(dialog);
        java.util.Optional<String> result = dialog.showAndWait();
        return result.map(labels::indexOf).filter(i -> i >= 0).map(items::get).orElse(null);
    }

    private void initOwner(Dialog<?> dialog) {
        if (tabPane.getScene() != null && tabPane.getScene().getWindow() != null) {
            dialog.initOwner(tabPane.getScene().getWindow());
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(RES.get("jfx.gui.preferences.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        initOwner(alert);
        alert.showAndWait();
    }

    @FXML
    private void onBrowseFontPath() {
        File initial = null;
        String current = txtFontPath.getText();
        if (current != null && !current.isEmpty()) {
            File c = new File(current);
            if (c.getParentFile() != null && c.getParentFile().isDirectory()) {
                initial = c.getParentFile();
            }
        }
        NativeFileChooser fc = new NativeFileChooser()
                .setTitle(RES.get("jfx.gui.preferences.font.path.browse"))
                .addFilter(ExtensionFilter.of("TTF/OTF", "*.ttf", "*.otf"));
        if (initial != null) fc.setInitialDirectory(initial);
        Stage owner = (Stage) txtFontPath.getScene().getWindow();
        File picked = fc.showOpenDialog(owner);
        if (picked != null) {
            txtFontPath.setText(picked.getAbsolutePath());
        }
    }

    @FXML
    private void onPkcs11ResetSample() {
        resetSelectedPkcs11ToSample();
    }

    private void resetSelectedPkcs11ToSample() {
        if (selectedDraft == null) {
            return;
        }
        String sample = PKCS11Utils.getSampleConfig();
        txtPkcs11Body.setText(selectedDraft.isDefault() ? sample
                : Pkcs11ConfigText.withValue(sample, Pkcs11ConfigText.KEY_NAME, selectedDraft.id()));
    }

    private void resetActiveTabToDefaults() {
        Tab active = tabPane.getSelectionModel().getSelectedItem();
        AdvancedConfig defaults = bundledDefaultsHolder();
        if (active == tabGeneral) {
            vm.applyGeneralDefaults(defaults);
        } else if (active == tabFont) {
            vm.applyFontDefaults(defaults);
        } else if (active == tabCertificate) {
            vm.applyCertificateDefaults(defaults);
        } else if (active == tabNetwork) {
            vm.applyNetworkDefaults(defaults);
        } else if (active == tabPdfRender) {
            vm.applyPdfRenderDefaults(defaults);
        } else if (active == tabTsa) {
            vm.applyTsaDefaults(defaults);
        } else if (active == tabDss) {
            vm.applyDssDefaults(defaults);
        } else if (active == tabPkcs11) {
            resetSelectedPkcs11ToSample();
        }
    }

    /** Builds an in-memory AdvancedConfig from the bundled defaults so we can read defaults without disk access. */
    private static AdvancedConfig bundledDefaultsHolder() {
        Properties bundled = new Properties();
        try (InputStream is = PreferencesController.class.getResourceAsStream(DEFAULTS_RESOURCE)) {
            if (is != null) {
                bundled.load(is);
            }
        } catch (IOException e) {
            Constants.LOGGER.log(Level.WARNING, "Failed to read bundled advanced.default.properties", e);
        }
        return new AdvancedConfig(null, bundled);
    }

    private boolean validate() {
        if (!OutputSuffixValidation.isValid(vm.outputSuffixProperty().get())) {
            tabPane.getSelectionModel().select(tabGeneral);
            showError(RES.get("jfx.gui.preferences.validation.outputSuffix"));
            return false;
        }
        if (!OutputSuffixValidation.isValid(vm.outputSuffixTimestampProperty().get())) {
            tabPane.getSelectionModel().select(tabGeneral);
            showError(RES.get("jfx.gui.preferences.validation.outputSuffixTimestamp"));
            return false;
        }
        if (!PreferencesValidation.validateFontPath(vm.fontPathProperty().get())) {
            tabPane.getSelectionModel().select(tabFont);
            showError(RES.get("jfx.gui.preferences.validation.fontFileUnreadable"));
            return false;
        }
        if (!PreferencesValidation.validatePdfLibSelection(
                vm.pdfLibJpedalProperty().get(),
                vm.pdfLibPdfboxProperty().get(),
                vm.pdfLibOpenpdfProperty().get())) {
            tabPane.getSelectionModel().select(tabPdfRender);
            showError(RES.get("jfx.gui.preferences.validation.pdfNoLibrary"));
            return false;
        }
        String pkcs11Problem = Pkcs11Profiles.getInstance().validateEdits(vm.pkcs11Edits());
        if (pkcs11Problem != null) {
            tabPane.getSelectionModel().select(tabPkcs11);
            showError(RES.get("jfx.gui.preferences.validation.pkcs11", pkcs11Problem));
            return false;
        }
        return true;
    }

    private boolean persist(AdvancedConfig cfg) {
        try {
            vm.writeTo(cfg);
            Set<String> changed = cfg.save();
            if (changed.stream().anyMatch(k -> k.startsWith("font."))) {
                FontUtils.reset();
            }
            if (changed.contains("debug")) {
                // The logger is a process-global singleton, so a debug toggle takes effect for the next
                // signing run without a restart.
                AppConfig.applyDebugLogLevel();
            }
            if (changed.contains("relax.ssl.security")) {
                // Re-apply the trust-manager / hostname-verifier so a false→true toggle takes effect for the next request.
                // The JVM-wide system properties (jsse.enableSNIExtension, etc.) still need a restart — covered by the hint label.
                try {
                    SSLInitializer.init();
                } catch (Exception sslEx) {
                    Constants.LOGGER.log(Level.WARNING, "Failed to re-init SSL after relax.ssl.security change", sslEx);
                }
            }
            Pkcs11Profiles profiles = Pkcs11Profiles.getInstance();
            profiles.applyEdits(vm.pkcs11Edits());
            profiles.clearFailures();
            return true;
        } catch (Exception e) {
            Constants.LOGGER.log(Level.SEVERE, "Failed to save preferences", e);
            showError(e.getMessage());
            return false;
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(RES.get("jfx.gui.preferences.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
