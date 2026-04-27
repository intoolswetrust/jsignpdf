package net.sf.jsignpdf.fx.view;

import java.io.File;

import static net.sf.jsignpdf.Constants.RES;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import net.sf.jsignpdf.fx.util.NativeFileChooser;
import net.sf.jsignpdf.fx.util.NativeFileChooser.ExtensionFilter;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;

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
    @FXML private TextField txtOutFile;

    private SigningOptionsViewModel viewModel;

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
        txtOutFile.textProperty().bindBidirectional(viewModel.outFileProperty());
    }

    @FXML
    private void onBrowseOutFile() {
        File file = new NativeFileChooser()
                .setTitle(RES.get("jfx.gui.dialog.selectOutputPdf"))
                .addFilter(ExtensionFilter.of("PDF Files", "*.pdf"))
                .showSaveDialog(txtOutFile.getScene().getWindow());
        if (file != null) txtOutFile.setText(file.getAbsolutePath());
    }
}
