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
    @FXML private VBox passwordPane;
    @FXML private VBox certPane;
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

        // Toggle encryption details and rights visibility based on selection.
        // Rights only apply to encrypted output, and within the details pane
        // we show only the subset of fields relevant to the chosen type:
        // - PASSWORD:    owner/user passwords
        // - CERTIFICATE: encryption certificate
        // - NONE:        nothing (whole details pane and rights hidden)
        cmbEncryption.valueProperty().addListener((obs, o, n) -> {
            applyEncryptionVisibility(n);
            updatePasswordValidation();
        });
        encryptionDetailsPane.managedProperty().bind(encryptionDetailsPane.visibleProperty());
        rightsPane.managedProperty().bind(rightsPane.visibleProperty());
        passwordPane.managedProperty().bind(passwordPane.visibleProperty());
        certPane.managedProperty().bind(certPane.visibleProperty());
        applyEncryptionVisibility(null);

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
        applyEncryptionVisibility(viewModel.pdfEncryptionProperty().get());
        updatePasswordValidation();
    }

    private void applyEncryptionVisibility(PDFEncryption enc) {
        boolean encOn = enc != null && enc != PDFEncryption.NONE;
        encryptionDetailsPane.setVisible(encOn);
        rightsPane.setVisible(encOn);
        passwordPane.setVisible(enc == PDFEncryption.PASSWORD);
        certPane.setVisible(enc == PDFEncryption.CERTIFICATE);
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
