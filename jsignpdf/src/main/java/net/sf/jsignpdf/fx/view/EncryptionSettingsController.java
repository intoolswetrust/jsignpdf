package net.sf.jsignpdf.fx.view;

import java.io.File;

import static net.sf.jsignpdf.Constants.RES;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.types.PDFEncryption;
import net.sf.jsignpdf.types.PrintRight;

/**
 * Controller for PDF encryption and rights settings.
 */
public class EncryptionSettingsController {

    @FXML private ComboBox<PDFEncryption> cmbEncryption;
    @FXML private VBox encryptionDetailsPane;
    @FXML private PasswordField txtOwnerPassword;
    @FXML private PasswordField txtUserPassword;
    @FXML private TextField txtEncCertFile;

    // Rights
    @FXML private ComboBox<PrintRight> cmbPrintRight;
    @FXML private CheckBox chkCopy;
    @FXML private CheckBox chkAssembly;
    @FXML private CheckBox chkFillIn;
    @FXML private CheckBox chkScreenReaders;
    @FXML private CheckBox chkModifyAnnotations;
    @FXML private CheckBox chkModifyContents;

    private SigningOptionsViewModel viewModel;

    @FXML
    private void initialize() {
        cmbEncryption.setItems(FXCollections.observableArrayList(PDFEncryption.values()));
        cmbPrintRight.setItems(FXCollections.observableArrayList(PrintRight.values()));

        // Toggle encryption details visibility
        cmbEncryption.valueProperty().addListener((obs, o, n) ->
                encryptionDetailsPane.setVisible(n != null && n != PDFEncryption.NONE));
        encryptionDetailsPane.setVisible(false);
        encryptionDetailsPane.managedProperty().bind(encryptionDetailsPane.visibleProperty());
    }

    public void setViewModel(SigningOptionsViewModel vm) {
        this.viewModel = vm;
        bindToViewModel();
    }

    private void bindToViewModel() {
        cmbEncryption.valueProperty().bindBidirectional(viewModel.pdfEncryptionProperty());
        txtOwnerPassword.textProperty().bindBidirectional(viewModel.pdfOwnerPasswordProperty());
        txtUserPassword.textProperty().bindBidirectional(viewModel.pdfUserPasswordProperty());
        txtEncCertFile.textProperty().bindBidirectional(viewModel.pdfEncryptionCertFileProperty());

        cmbPrintRight.valueProperty().bindBidirectional(viewModel.rightPrintingProperty());
        chkCopy.selectedProperty().bindBidirectional(viewModel.rightCopyProperty());
        chkAssembly.selectedProperty().bindBidirectional(viewModel.rightAssemblyProperty());
        chkFillIn.selectedProperty().bindBidirectional(viewModel.rightFillInProperty());
        chkScreenReaders.selectedProperty().bindBidirectional(viewModel.rightScreenReadersProperty());
        chkModifyAnnotations.selectedProperty().bindBidirectional(viewModel.rightModifyAnnotationsProperty());
        chkModifyContents.selectedProperty().bindBidirectional(viewModel.rightModifyContentsProperty());

        // Update visibility from initial loaded values
        PDFEncryption enc = viewModel.pdfEncryptionProperty().get();
        encryptionDetailsPane.setVisible(enc != null && enc != PDFEncryption.NONE);
    }

    @FXML
    private void onBrowseEncCert() {
        FileChooser fc = new FileChooser();
        fc.setTitle(RES.get("jfx.gui.dialog.selectEncryptionCert"));
        File file = fc.showOpenDialog(txtEncCertFile.getScene().getWindow());
        if (file != null) txtEncCertFile.setText(file.getAbsolutePath());
    }
}
