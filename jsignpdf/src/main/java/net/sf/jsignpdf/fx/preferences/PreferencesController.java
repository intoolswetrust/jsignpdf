package net.sf.jsignpdf.fx.preferences;

import static net.sf.jsignpdf.Constants.RES;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
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
import javafx.scene.control.ComboBox;
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
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.fx.util.NativeFileChooser;
import net.sf.jsignpdf.fx.util.NativeFileChooser.ExtensionFilter;
import net.sf.jsignpdf.utils.AdvancedConfig;
import net.sf.jsignpdf.utils.ConfigLocationResolver;
import net.sf.jsignpdf.utils.FontUtils;
import net.sf.jsignpdf.utils.PKCS11Utils;
import net.sf.jsignpdf.utils.PropertyStoreFactory;

/**
 * Controller for the Preferences dialog. Holds the live editing state in a {@link PreferencesViewModel}, mirrors UI changes
 * back to the VM and on OK persists the VM via {@link AdvancedConfig#save()} plus a separate write of the PKCS#11 textarea
 * body to {@code <cfg>/pkcs11.cfg}.
 */
public class PreferencesController {

    private static final List<String> ENCODINGS = Arrays.asList(
            "Cp1250", "Cp1252", "Cp1257", "Identity-H", "Identity-V", "MacRoman");
    private static final List<String> HASH_ALGORITHMS = Arrays.asList(
            "SHA-1", "SHA-256", "SHA-384", "SHA-512", "RIPEMD160");
    private static final String DEFAULTS_RESOURCE = "/net/sf/jsignpdf/conf/advanced.default.properties";

    @FXML private TabPane tabPane;
    @FXML private Tab tabFont;
    @FXML private Tab tabCertificate;
    @FXML private Tab tabNetwork;
    @FXML private Tab tabPdfRender;
    @FXML private Tab tabTsa;
    @FXML private Tab tabPkcs11;

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

    @FXML private Label lblPkcs11Path;
    @FXML private TextArea txtPkcs11Body;
    @FXML private Label lblPkcs11EmptyHint;
    @FXML private Button btnPkcs11ResetSample;

    private PreferencesViewModel vm;
    private CheckBox chkLibJpedal;
    private CheckBox chkLibPdfbox;
    private CheckBox chkLibOpenpdf;

    @FXML
    private void initialize() {
        cmbFontEncoding.getItems().addAll(ENCODINGS);
        cmbTsaHashAlgorithm.getItems().addAll(HASH_ALGORITHMS);
        // Empty-hint visibility is bound when bind() runs.
    }

    /**
     * Public entry point — loads FXML, builds a VM from the current {@link AdvancedConfig} plus pkcs11 file body, shows the
     * dialog modally and persists changes on OK. Returns true if the user pressed OK and the save succeeded.
     */
    public static boolean show(Stage owner) {
        ResourceBundle bundle = ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE_BASE);
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
        Path pkcs11Path = ConfigLocationResolver.getInstance().getPkcs11ConfigFile();
        String pkcs11Body = readPkcs11Body(pkcs11Path);

        PreferencesViewModel vm = new PreferencesViewModel();
        vm.loadFrom(cfg, pkcs11Body);
        controller.bind(vm);
        controller.showPkcs11Path(pkcs11Path);

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
            return controller.persist(cfg, pkcs11Path);
        }
        return false;
    }

    void bind(PreferencesViewModel viewModel) {
        this.vm = viewModel;
        txtFontPath.textProperty().bindBidirectional(vm.fontPathProperty());
        txtFontName.textProperty().bindBidirectional(vm.fontNameProperty());
        cmbFontEncoding.valueProperty().bindBidirectional(vm.fontEncodingProperty());

        chkCertCheckValidity.selectedProperty().bindBidirectional(vm.checkValidityProperty());
        chkCertCheckKeyUsage.selectedProperty().bindBidirectional(vm.checkKeyUsageProperty());
        chkCertCheckCriticalExtensions.selectedProperty().bindBidirectional(vm.checkCriticalExtensionsProperty());

        chkRelaxSslSecurity.selectedProperty().bindBidirectional(vm.relaxSslSecurityProperty());

        cmbTsaHashAlgorithm.valueProperty().bindBidirectional(vm.tsaHashAlgorithmProperty());

        txtPkcs11Body.textProperty().bindBidirectional(vm.pkcs11BodyProperty());
        lblPkcs11EmptyHint.visibleProperty().bind(
                Bindings.createBooleanBinding(() -> txtPkcs11Body.getText() == null || txtPkcs11Body.getText().isEmpty(),
                        txtPkcs11Body.textProperty()));
        lblPkcs11EmptyHint.managedProperty().bind(lblPkcs11EmptyHint.visibleProperty());

        rebuildPdfLibsPanel();
        // Re-render the PDF-libs panel whenever the order changes, so rows visually re-sort.
        Runnable rerender = this::rebuildPdfLibsPanel;
        vm.pdfLibJpedalOrderProperty().addListener((o, a, b) -> rerender.run());
        vm.pdfLibPdfboxOrderProperty().addListener((o, a, b) -> rerender.run());
        vm.pdfLibOpenpdfOrderProperty().addListener((o, a, b) -> rerender.run());
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

    private void showPkcs11Path(Path path) {
        String displayed = path == null ? "—" : path.toAbsolutePath().toString();
        lblPkcs11Path.setText(MessageFormat.format(RES.get("jfx.gui.preferences.pkcs11.pathLabel"), displayed));
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
        txtPkcs11Body.setText(PKCS11Utils.getSampleConfig());
    }

    private void resetActiveTabToDefaults() {
        Tab active = tabPane.getSelectionModel().getSelectedItem();
        AdvancedConfig defaults = bundledDefaultsHolder();
        if (active == tabFont) {
            vm.fontPathProperty().set("");
            vm.fontNameProperty().set("");
            vm.fontEncodingProperty().set("");
        } else if (active == tabCertificate) {
            vm.checkValidityProperty().set(defaults.getAsBool("certificate.checkValidity", true));
            vm.checkKeyUsageProperty().set(defaults.getAsBool("certificate.checkKeyUsage", true));
            vm.checkCriticalExtensionsProperty().set(defaults.getAsBool("certificate.checkCriticalExtensions", false));
        } else if (active == tabNetwork) {
            vm.relaxSslSecurityProperty().set(defaults.getAsBool("relax.ssl.security", false));
        } else if (active == tabPdfRender) {
            vm.decodePdfLibraries(defaults.getNotEmptyProperty("pdf2image.libraries", "jpedal,pdfbox,openpdf"));
        } else if (active == tabTsa) {
            vm.tsaHashAlgorithmProperty().set(defaults.getNotEmptyProperty("tsa.hashAlgorithm", "SHA-256"));
        } else if (active == tabPkcs11) {
            vm.pkcs11BodyProperty().set(PKCS11Utils.getSampleConfig());
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
        return true;
    }

    private boolean persist(AdvancedConfig cfg, Path pkcs11Path) {
        try {
            vm.writeTo(cfg);
            Set<String> changed = cfg.save();
            if (changed.stream().anyMatch(k -> k.startsWith("font."))) {
                FontUtils.reset();
            }
            if (pkcs11Path != null) {
                String body = vm.pkcs11BodyProperty().get();
                if (body == null || body.isBlank()) {
                    Files.deleteIfExists(pkcs11Path);
                } else {
                    Path parent = pkcs11Path.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.writeString(pkcs11Path, body, StandardCharsets.UTF_8);
                }
            }
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

    private static String readPkcs11Body(Path pkcs11Path) {
        if (pkcs11Path == null || !Files.isRegularFile(pkcs11Path)) {
            return "";
        }
        try {
            return Files.readString(pkcs11Path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Constants.LOGGER.log(Level.WARNING, "Failed to read PKCS#11 file", e);
            return "";
        }
    }
}
