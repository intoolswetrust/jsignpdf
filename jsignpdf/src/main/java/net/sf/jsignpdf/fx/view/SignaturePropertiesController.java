package net.sf.jsignpdf.fx.view;

import java.io.File;

import static net.sf.jsignpdf.Constants.RES;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import net.sf.jsignpdf.engine.Capability;
import net.sf.jsignpdf.engine.SigningEngine;
import net.sf.jsignpdf.fx.EngineCapabilities;
import net.sf.jsignpdf.fx.util.NativeFileChooser;
import net.sf.jsignpdf.fx.util.NativeFileChooser.ExtensionFilter;
import net.sf.jsignpdf.fx.util.OutputBaseNameValidation;
import net.sf.jsignpdf.fx.util.OutputSuffixValidation;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.utils.AppConfig;

/**
 * Controller for signature-properties settings (hash algorithm, certification
 * level, signer metadata, append toggle, output file). These fields are
 * intentionally separate from the visible-signature appearance settings in
 * {@link SignatureSettingsController}.
 */
public class SignaturePropertiesController {

    @FXML private ComboBox<HashAlgorithm> cmbHashAlgorithm;
    @FXML private ComboBox<CertificationLevel> cmbCertLevel;
    @FXML private TextField txtSignerName;
    @FXML private TextField txtReason;
    @FXML private TextField txtLocation;
    @FXML private TextField txtContact;
    @FXML private CheckBox chkAppend;
    @FXML private TextField txtOutDir;
    @FXML private TextField txtOutFile;
    @FXML private CheckBox chkUseSuffix;
    @FXML private TextField txtOutSuffix;

    private SigningOptionsViewModel viewModel;
    private boolean syncingSuffix;
    private boolean syncingBaseName;
    private boolean syncingDir;

    @FXML
    private void initialize() {
        cmbHashAlgorithm.setItems(FXCollections.observableArrayList(HashAlgorithm.values()));
        cmbCertLevel.setItems(FXCollections.observableArrayList(CertificationLevel.values()));
    }

    public void setViewModel(SigningOptionsViewModel vm) {
        this.viewModel = vm;
        bindToViewModel();
    }

    private void bindToViewModel() {
        cmbHashAlgorithm.valueProperty().bindBidirectional(viewModel.hashAlgorithmProperty());
        cmbCertLevel.valueProperty().bindBidirectional(viewModel.certLevelProperty());
        txtSignerName.textProperty().bindBidirectional(viewModel.signerNameProperty());
        txtReason.textProperty().bindBidirectional(viewModel.reasonProperty());
        txtLocation.textProperty().bindBidirectional(viewModel.locationProperty());
        txtContact.textProperty().bindBidirectional(viewModel.contactProperty());
        chkAppend.selectedProperty().bindBidirectional(viewModel.appendProperty());
        bindOutDir();
        bindOutBaseName();
        bindOutSuffix();
    }

    /**
     * Binds the output directory field to the view model. A blank field means "next to the input file"; anything typed is
     * a fixed output directory that survives opening a different document and is stored with presets.
     */
    private void bindOutDir() {
        showDir(viewModel.outPathProperty().get());
        viewModel.outPathProperty().addListener((obs, o, n) -> {
            if (!syncingDir) {
                showDir(n);
            }
        });
        txtOutDir.textProperty().addListener((obs, o, n) -> {
            syncingDir = true;
            try {
                viewModel.outPathProperty().set(n == null || n.isEmpty() ? null : n);
            } finally {
                syncingDir = false;
            }
        });
    }

    private void showDir(String dir) {
        syncingDir = true;
        try {
            txtOutDir.setText(dir != null ? dir : "");
        } finally {
            syncingDir = false;
        }
    }

    /**
     * Binds the output file name field to the view model. A blank field means "use the name derived from the input file
     * and the suffix", shown as the prompt text; anything typed is an explicit base name. Path separators and traversal
     * are rejected so the name cannot move the write out of the chosen directory.
     */
    private void bindOutBaseName() {
        showBaseName(viewModel.outBaseNameProperty().get());
        viewModel.outBaseNameProperty().addListener((obs, o, n) -> {
            if (!syncingBaseName) {
                showBaseName(n);
            }
        });
        txtOutFile.textProperty().addListener((obs, o, n) -> {
            if (!OutputBaseNameValidation.isValid(n)) {
                int caret = Math.min(txtOutFile.getCaretPosition(), o == null ? 0 : o.length());
                txtOutFile.setText(o);
                txtOutFile.positionCaret(caret);
                return;
            }
            syncingBaseName = true;
            try {
                viewModel.outBaseNameProperty().set(n == null || n.isEmpty() ? null : n);
            } finally {
                syncingBaseName = false;
            }
        });
    }

    private void showBaseName(String name) {
        syncingBaseName = true;
        try {
            txtOutFile.setText(name != null ? name : "");
        } finally {
            syncingBaseName = false;
        }
    }

    /**
     * Sets the derived output name shown as the prompt text of an empty output-file field. Called by the main
     * controller when the input file or suffix changes.
     */
    public void setOutFileNamePrompt(String derivedName) {
        txtOutFile.setPromptText(derivedName != null ? derivedName : "");
    }

    /**
     * Binds the suffix switch and field to the view model as a three-state view over {@code outSuffix}:
     * <ul>
     *   <li>switch off &rArr; {@code outSuffix == ""} — the output keeps the input name (no suffix);</li>
     *   <li>switch on, field blank &rArr; {@code outSuffix == null} — the configured {@code output.suffix} default
     *       applies, shown as the prompt text;</li>
     *   <li>switch on, field typed &rArr; the explicit suffix for this document.</li>
     * </ul>
     */
    private void bindOutSuffix() {
        showSuffix(viewModel.outSuffixProperty().get());
        refreshDefaultSuffix();
        viewModel.outSuffixProperty().addListener((obs, o, n) -> {
            if (!syncingSuffix) {
                showSuffix(n);
            }
        });
        chkUseSuffix.selectedProperty().addListener((obs, was, on) -> {
            if (syncingSuffix) {
                return;
            }
            syncingSuffix = true;
            try {
                if (Boolean.TRUE.equals(on)) {
                    txtOutSuffix.setDisable(false);
                    String t = txtOutSuffix.getText();
                    viewModel.outSuffixProperty().set(t == null || t.isEmpty() ? null : t);
                } else {
                    viewModel.outSuffixProperty().set("");
                    txtOutSuffix.setDisable(true);
                }
            } finally {
                syncingSuffix = false;
            }
        });
        txtOutSuffix.textProperty().addListener((obs, o, n) -> {
            if (syncingSuffix) {
                return;
            }
            if (!OutputSuffixValidation.isValid(n)) {
                // Keep the caret put, so editing mid-value does not jump to the end on a refused character.
                int caret = Math.min(txtOutSuffix.getCaretPosition(), o == null ? 0 : o.length());
                txtOutSuffix.setText(o);
                txtOutSuffix.positionCaret(caret);
                return;
            }
            syncingSuffix = true;
            try {
                viewModel.outSuffixProperty().set(n == null || n.isEmpty() ? null : n);
            } finally {
                syncingSuffix = false;
            }
        });
    }

    /**
     * Refreshes the configured defaults shown as the prompt text of an empty field. The same field feeds both the
     * "Sign" and the "Timestamp" operation, each falling back to its own configured suffix, so both are shown.
     * Called after the Preferences dialog changes {@code output.suffix} / {@code output.suffix.timestamp}.
     */
    public void refreshDefaultSuffix() {
        txtOutSuffix.setPromptText(AppConfig.defaultOutSuffix() + " / " + AppConfig.defaultTimestampSuffix());
    }

    private void showSuffix(String suffix) {
        syncingSuffix = true;
        try {
            boolean on = !"".equals(suffix); // null or explicit text => switch on; "" => off
            chkUseSuffix.setSelected(on);
            txtOutSuffix.setDisable(!on);
            txtOutSuffix.setText(on && suffix != null ? suffix : "");
        } finally {
            syncingSuffix = false;
        }
    }

    /**
     * Gates the "append signature" checkbox against the active engine's capabilities. Unchecking the box
     * requests an overwrite (non-incremental rewrite), which needs {@link Capability#OVERWRITE_MODE}.
     * Engines that lack it (e.g. the DSS/PAdES engine) always append, so the box is forced on and disabled
     * with the shared "not supported" tooltip while such an engine is active. This mirrors the CLI
     * fail-soft handled by {@link net.sf.jsignpdf.engine.EngineMismatchValidator}.
     *
     * <p>Enforcement also guards the {@code append} property directly, not just engine changes: the
     * persisted options are loaded after this wiring runs, and a stored {@code append=false} would
     * otherwise survive when the startup engine is already a non-overwrite one (no change event fires).
     *
     * @param caps the capability source driving the gating; must be wired after {@link #setViewModel}
     */
    public void gateCapabilities(EngineCapabilities caps) {
        caps.gate(chkAppend, Capability.OVERWRITE_MODE);
        enforceAppendForEngine(caps.activeEngineProperty().get());
        caps.activeEngineProperty().addListener((obs, oldEngine, newEngine) -> enforceAppendForEngine(newEngine));
        viewModel.appendProperty().addListener((obs, was, isAppend) -> {
            if (Boolean.FALSE.equals(isAppend)) {
                enforceAppendForEngine(caps.activeEngineProperty().get());
            }
        });
    }

    private void enforceAppendForEngine(SigningEngine engine) {
        if (engine != null && !engine.capabilities().contains(Capability.OVERWRITE_MODE)) {
            viewModel.appendProperty().set(true);
        }
    }

    @FXML
    private void onBrowseOutFile() {
        File file = new NativeFileChooser()
                .setTitle(RES.get("jfx.gui.dialog.selectOutputPdf"))
                .addFilter(ExtensionFilter.of("PDF Files", "*.pdf"))
                .showSaveDialog(txtOutFile.getScene().getWindow());
        if (file != null) {
            viewModel.outPathProperty().set(file.getParent());
            viewModel.outBaseNameProperty().set(file.getName());
        }
    }

    @FXML
    private void onBrowseOutDir() {
        File dir = new NativeFileChooser()
                .setTitle(RES.get("jfx.gui.dialog.selectOutputDir"))
                .showOpenDirectoryDialog(txtOutDir.getScene().getWindow());
        if (dir != null) viewModel.outPathProperty().set(dir.getAbsolutePath());
    }
}
