package net.sf.jsignpdf.fx.view;

import java.io.File;

import static net.sf.jsignpdf.Constants.RES;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.types.RenderMode;

/**
 * Controller for the visible-signature appearance section.
 * Holds only the fields that affect how a visible signature rectangle is
 * rendered on the PDF. Non-appearance signature settings (hash algorithm,
 * certification level, signer metadata, output file, append) live in
 * {@link SignaturePropertiesController}.
 */
public class SignatureSettingsController {

    @FXML private CheckBox chkVisibleSig;
    @FXML private VBox visibleSigPane;
    @FXML private ComboBox<RenderMode> cmbRenderMode;
    @FXML private TextField txtL2Text;
    @FXML private TextField txtL4Text;
    @FXML private TextField txtFontSize;
    @FXML private TextField txtImgPath;
    @FXML private TextField txtBgImgPath;
    @FXML private CheckBox chkAcro6Layers;

    private SigningOptionsViewModel viewModel;

    @FXML
    private void initialize() {
        cmbRenderMode.setItems(FXCollections.observableArrayList(RenderMode.values()));

        // Toggle visible signature details
        visibleSigPane.managedProperty().bind(visibleSigPane.visibleProperty());
        chkVisibleSig.selectedProperty().addListener((obs, o, n) ->
                visibleSigPane.setVisible(n));
        visibleSigPane.setVisible(false);
    }

    public void setViewModel(SigningOptionsViewModel vm) {
        this.viewModel = vm;
        bindToViewModel();
    }

    /**
     * Disables/enables the visible-signature toggle checkbox. Called by the main
     * controller when a document is (un)loaded, so the user cannot enable a
     * visible signature without a document on which to place it.
     */
    public void setVisibleSigCheckBoxDisabled(boolean disabled) {
        chkVisibleSig.setDisable(disabled);
    }

    private void bindToViewModel() {
        chkVisibleSig.selectedProperty().bindBidirectional(viewModel.visibleProperty());
        cmbRenderMode.valueProperty().bindBidirectional(viewModel.renderModeProperty());
        txtL2Text.textProperty().bindBidirectional(viewModel.l2TextProperty());
        txtL4Text.textProperty().bindBidirectional(viewModel.l4TextProperty());
        txtImgPath.textProperty().bindBidirectional(viewModel.imgPathProperty());
        txtBgImgPath.textProperty().bindBidirectional(viewModel.bgImgPathProperty());
        chkAcro6Layers.selectedProperty().bindBidirectional(viewModel.acro6LayersProperty());

        // Font size needs manual sync (String <-> float)
        viewModel.l2TextFontSizeProperty().addListener((obs, o, n) ->
                txtFontSize.setText(String.valueOf(n.floatValue())));
        txtFontSize.setOnAction(e -> {
            try {
                viewModel.l2TextFontSizeProperty().set(Float.parseFloat(txtFontSize.getText()));
            } catch (NumberFormatException ignored) {
            }
        });

        // Update visibility from initial value
        visibleSigPane.setVisible(viewModel.visibleProperty().get());
    }

    @FXML
    private void onBrowseImage() {
        File file = browseImageFile(RES.get("jfx.gui.dialog.selectSignatureImage"));
        if (file != null) txtImgPath.setText(file.getAbsolutePath());
    }

    @FXML
    private void onBrowseBgImage() {
        File file = browseImageFile(RES.get("jfx.gui.dialog.selectBackgroundImage"));
        if (file != null) txtBgImgPath.setText(file.getAbsolutePath());
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
