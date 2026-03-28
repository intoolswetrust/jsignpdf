package net.sf.jsignpdf.fx.view;

import java.io.File;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.RenderMode;

/**
 * Controller for signature appearance and metadata settings.
 */
public class SignatureSettingsController {

    @FXML private ComboBox<RenderMode> cmbRenderMode;
    @FXML private ComboBox<HashAlgorithm> cmbHashAlgorithm;
    @FXML private ComboBox<CertificationLevel> cmbCertLevel;
    @FXML private TextField txtSignerName;
    @FXML private TextField txtReason;
    @FXML private TextField txtLocation;
    @FXML private TextField txtContact;
    @FXML private TextField txtL2Text;
    @FXML private TextField txtL4Text;
    @FXML private TextField txtFontSize;
    @FXML private TextField txtImgPath;
    @FXML private TextField txtBgImgPath;
    @FXML private CheckBox chkAcro6Layers;
    @FXML private CheckBox chkAppend;
    @FXML private TextField txtOutFile;

    private SigningOptionsViewModel viewModel;

    @FXML
    private void initialize() {
        cmbRenderMode.setItems(FXCollections.observableArrayList(RenderMode.values()));
        cmbHashAlgorithm.setItems(FXCollections.observableArrayList(HashAlgorithm.values()));
        cmbCertLevel.setItems(FXCollections.observableArrayList(CertificationLevel.values()));
    }

    public void setViewModel(SigningOptionsViewModel vm) {
        this.viewModel = vm;
        bindToViewModel();
    }

    private void bindToViewModel() {
        cmbRenderMode.valueProperty().bindBidirectional(viewModel.renderModeProperty());
        cmbHashAlgorithm.valueProperty().bindBidirectional(viewModel.hashAlgorithmProperty());
        cmbCertLevel.valueProperty().bindBidirectional(viewModel.certLevelProperty());
        txtSignerName.textProperty().bindBidirectional(viewModel.signerNameProperty());
        txtReason.textProperty().bindBidirectional(viewModel.reasonProperty());
        txtLocation.textProperty().bindBidirectional(viewModel.locationProperty());
        txtContact.textProperty().bindBidirectional(viewModel.contactProperty());
        txtL2Text.textProperty().bindBidirectional(viewModel.l2TextProperty());
        txtL4Text.textProperty().bindBidirectional(viewModel.l4TextProperty());
        txtImgPath.textProperty().bindBidirectional(viewModel.imgPathProperty());
        txtBgImgPath.textProperty().bindBidirectional(viewModel.bgImgPathProperty());
        chkAcro6Layers.selectedProperty().bindBidirectional(viewModel.acro6LayersProperty());
        chkAppend.selectedProperty().bindBidirectional(viewModel.appendProperty());
        txtOutFile.textProperty().bindBidirectional(viewModel.outFileProperty());

        // Font size needs manual sync (String <-> float)
        viewModel.l2TextFontSizeProperty().addListener((obs, o, n) ->
                txtFontSize.setText(String.valueOf(n.floatValue())));
        txtFontSize.setOnAction(e -> {
            try {
                viewModel.l2TextFontSizeProperty().set(Float.parseFloat(txtFontSize.getText()));
            } catch (NumberFormatException ignored) {
            }
        });
    }

    @FXML
    private void onBrowseImage() {
        File file = browseImageFile("Select Signature Image");
        if (file != null) txtImgPath.setText(file.getAbsolutePath());
    }

    @FXML
    private void onBrowseBgImage() {
        File file = browseImageFile("Select Background Image");
        if (file != null) txtBgImgPath.setText(file.getAbsolutePath());
    }

    @FXML
    private void onBrowseOutFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Output PDF File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog(txtOutFile.getScene().getWindow());
        if (file != null) txtOutFile.setText(file.getAbsolutePath());
    }

    private File browseImageFile(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        return fc.showOpenDialog(txtImgPath.getScene().getWindow());
    }
}
