package net.sf.jsignpdf.fx.view;

import java.io.File;

import static net.sf.jsignpdf.Constants.RES;

import javafx.beans.value.ObservableValue;
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

    private static final String STYLE_VALIDATION_ERROR = "-fx-border-color: red; -fx-border-width: 1;";

    @FXML private ComboBox<PDFEncryption> cmbEncryption;
    @FXML private VBox encryptionDetailsPane;
    @FXML private PasswordField txtOwnerPassword;
    @FXML private PasswordField txtUserPassword;
    @FXML private TextField txtEncCertFile;

    // Rights
    @FXML private VBox rightsPane;
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

        // Toggle encryption details and rights visibility together. Rights
        // only apply to encrypted output, so there's no point showing them
        // when encryption is off.
        cmbEncryption.valueProperty().addListener((obs, o, n) -> {
            boolean encOn = n != null && n != PDFEncryption.NONE;
            encryptionDetailsPane.setVisible(encOn);
            rightsPane.setVisible(encOn);
            updatePasswordValidation();
        });
        encryptionDetailsPane.setVisible(false);
        encryptionDetailsPane.managedProperty().bind(encryptionDetailsPane.visibleProperty());
        rightsPane.setVisible(false);
        rightsPane.managedProperty().bind(rightsPane.visibleProperty());

        // Live validation on password fields
        txtOwnerPassword.textProperty().addListener((ObservableValue<? extends String> obs, String o, String n) ->
                updatePasswordValidation());
        txtUserPassword.textProperty().addListener((ObservableValue<? extends String> obs, String o, String n) ->
                updatePasswordValidation());
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
        boolean encOn = enc != null && enc != PDFEncryption.NONE;
        encryptionDetailsPane.setVisible(encOn);
        rightsPane.setVisible(encOn);
        updatePasswordValidation();
    }

    private void updatePasswordValidation() {
        if (!isPasswordEncryptionSelected()) {
            txtOwnerPassword.setStyle(null);
            txtUserPassword.setStyle(null);
            return;
        }
        txtOwnerPassword.setStyle(isBlank(txtOwnerPassword.getText()) ? STYLE_VALIDATION_ERROR : null);
        txtUserPassword.setStyle(isBlank(txtUserPassword.getText()) ? STYLE_VALIDATION_ERROR : null);
    }

    /**
     * Returns true if password encryption is selected and both passwords are filled in.
     * Used by the main controller to gate the Sign action.
     */
    public boolean isEncryptionConfigValid() {
        if (!isPasswordEncryptionSelected()) {
            return true;
        }
        return !isBlank(txtOwnerPassword.getText()) && !isBlank(txtUserPassword.getText());
    }

    private boolean isPasswordEncryptionSelected() {
        PDFEncryption enc = cmbEncryption.getValue();
        return enc == PDFEncryption.PASSWORD;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @FXML
    private void onBrowseEncCert() {
        FileChooser fc = new FileChooser();
        fc.setTitle(RES.get("jfx.gui.dialog.selectEncryptionCert"));
        File file = fc.showOpenDialog(txtEncCertFile.getScene().getWindow());
        if (file != null) txtEncCertFile.setText(file.getAbsolutePath());
    }
}
