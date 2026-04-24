package net.sf.jsignpdf.fx.view;

import java.io.File;

import static net.sf.jsignpdf.Constants.RES;

import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;

/**
 * Controller for the visible-signature appearance section.
 * Exposes only the signature text (multiline) and a background image
 * with a live preview. All other visible-signature knobs are normalized
 * to canonical values by the view-model (see design-doc/3.0.0-simplify-visible.md).
 */
public class SignatureSettingsController {

    @FXML private CheckBox chkVisibleSig;
    @FXML private VBox visibleSigPane;
    @FXML private TextArea txtL2Text;
    @FXML private TextField txtBgImgPath;
    @FXML private ImageView bgImgPreview;
    @FXML private Label bgImgPreviewPlaceholder;

    private SigningOptionsViewModel viewModel;
    private final PauseTransition bgImgDebounce = new PauseTransition(Duration.millis(150));

    @FXML
    private void initialize() {
        // Keep the panel laid out even when visible-signature is off; just disable it
        // so users can still see the current text/preview.
        visibleSigPane.disableProperty().bind(chkVisibleSig.selectedProperty().not());

        bgImgDebounce.setOnFinished(e -> updateBgImgPreview(txtBgImgPath.getText()));
        txtBgImgPath.textProperty().addListener((obs, o, n) -> bgImgDebounce.playFromStart());
        updateBgImgPreview(txtBgImgPath.getText());
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
        txtL2Text.textProperty().bindBidirectional(viewModel.l2TextProperty());
        txtBgImgPath.textProperty().bindBidirectional(viewModel.bgImgPathProperty());
    }

    @FXML
    private void onBrowseBgImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle(RES.get("jfx.gui.dialog.selectBackgroundImage"));
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fc.showOpenDialog(txtBgImgPath.getScene().getWindow());
        if (file != null) txtBgImgPath.setText(file.getAbsolutePath());
    }

    @FXML
    private void onClearBgImage() {
        txtBgImgPath.setText("");
    }

    private void updateBgImgPreview(String path) {
        if (path == null || path.trim().isEmpty()) {
            showPreviewPlaceholder(RES.get("jfx.gui.sig.bgImage.none"));
            return;
        }
        File f = new File(path);
        if (!f.isFile()) {
            showPreviewPlaceholder(RES.get("jfx.gui.sig.bgImage.error"));
            return;
        }
        Image img = new Image(f.toURI().toString(), 260, 160, true, true, true);
        img.errorProperty().addListener((ChangeListener<Boolean>) (obs, o, n) -> {
            if (Boolean.TRUE.equals(n)) {
                showPreviewPlaceholder(RES.get("jfx.gui.sig.bgImage.error"));
            }
        });
        if (img.isError()) {
            showPreviewPlaceholder(RES.get("jfx.gui.sig.bgImage.error"));
            return;
        }
        bgImgPreview.setImage(img);
        bgImgPreview.setVisible(true);
        bgImgPreviewPlaceholder.setVisible(false);
    }

    private void showPreviewPlaceholder(String message) {
        bgImgPreview.setImage(null);
        bgImgPreview.setVisible(false);
        bgImgPreviewPlaceholder.setText(message);
        bgImgPreviewPlaceholder.setVisible(true);
    }
}
