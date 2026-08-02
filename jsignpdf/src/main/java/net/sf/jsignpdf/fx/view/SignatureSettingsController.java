package net.sf.jsignpdf.fx.view;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import static net.sf.jsignpdf.Constants.RES;

import javafx.animation.PauseTransition;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.fx.util.NativeFileChooser;
import net.sf.jsignpdf.fx.util.NativeFileChooser.ExtensionFilter;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.types.SignatureFieldInfo;

/**
 * Controller for the visible-signature appearance section.
 * Exposes only the signature text (multiline) and a background image
 * with a live preview. All other visible-signature knobs are normalized
 * to canonical values by the view-model (see design-doc/3.0.0-simplify-visible.md).
 */
public class SignatureSettingsController {

    @FXML private CheckBox chkVisibleSig;
    @FXML private ComboBox<SignatureFieldInfo> cmbSigField;
    @FXML private VBox visibleSigPane;
    @FXML private TextArea txtL2Text;
    @FXML private TextField txtFontSize;
    @FXML private TextField txtBgImgPath;
    @FXML private ImageView bgImgPreview;
    @FXML private Label bgImgPreviewPlaceholder;

    private SigningOptionsViewModel viewModel;
    private final PauseTransition bgImgDebounce = new PauseTransition(Duration.millis(150));

    /** True while the loaded document offers no blank signature field to pick. */
    private final BooleanProperty noBlankFields = new SimpleBooleanProperty(true);
    /** True while no document is loaded, i.e. there is nothing to place a visible signature on. */
    private final BooleanProperty noDocument = new SimpleBooleanProperty(false);
    private Consumer<SignatureFieldInfo> onSigFieldSelected = field -> {
    };
    private boolean updatingSigFields;

    @FXML
    private void initialize() {
        // Keep the panel laid out even when visible-signature is off; just disable it
        // so users can still see the current text/preview.
        visibleSigPane.disableProperty().bind(chkVisibleSig.selectedProperty().not());
        // Signing into an existing field always draws the appearance into that field's rectangle, so the
        // toggle has nothing left to decide - it is forced on and disabled while a field is selected.
        chkVisibleSig.disableProperty().bind(noDocument.or(cmbSigField.valueProperty().isNotNull()));

        cmbSigField.setConverter(new StringConverter<>() {
            @Override
            public String toString(SignatureFieldInfo field) {
                return field == null ? RES.get("jfx.gui.sig.field.newField") : describe(field);
            }

            @Override
            public SignatureFieldInfo fromString(String string) {
                return null;
            }
        });
        cmbSigField.valueProperty().addListener((obs, o, field) -> onSigFieldChanged(field));

        bgImgDebounce.setOnFinished(e -> updateBgImgPreview(txtBgImgPath.getText()));
        txtBgImgPath.textProperty().addListener((obs, o, n) -> bgImgDebounce.playFromStart());
        updateBgImgPreview(txtBgImgPath.getText());
    }

    /**
     * Replaces the offered signature fields - called whenever a document is opened or closed. The selection is
     * always reset to "create a new field": a field name only means something for the document it came from, so
     * carrying a previous choice over to the next document would either fail or, worse, hit a same-named field
     * somewhere else.
     *
     * @param fields the blank signature fields of the loaded document (empty when there are none)
     */
    public void setSignatureFields(List<SignatureFieldInfo> fields) {
        updatingSigFields = true;
        try {
            cmbSigField.getItems().setAll(fields);
            // The "(create new field)" entry is the null value, rendered by the converter above.
            cmbSigField.getItems().add(0, null);
            cmbSigField.setValue(null);
        } finally {
            updatingSigFields = false;
        }
        noBlankFields.set(fields.isEmpty());
        onSigFieldChanged(null);
    }

    /**
     * Registers the callback fired when the selected signature field changes ({@code null} = create a new
     * field). The main controller uses it to highlight the field and to stop the placement overlay from
     * offering a drag that would be ignored.
     */
    public void setOnSigFieldSelected(Consumer<SignatureFieldInfo> handler) {
        this.onSigFieldSelected = handler != null ? handler : field -> {
        };
    }

    /**
     * Disables the field combo while the given binding is true (the active engine can't sign existing fields),
     * or while the document has no blank field to offer.
     */
    public void gateSigFieldCombo(BooleanBinding unsupportedByEngine) {
        cmbSigField.disableProperty().bind(unsupportedByEngine.or(noBlankFields));
    }

    private void onSigFieldChanged(SignatureFieldInfo field) {
        if (updatingSigFields) {
            return;
        }
        if (viewModel != null) {
            viewModel.sigFieldNameProperty().set(field == null ? null : field.name());
            if (field != null) {
                viewModel.visibleProperty().set(true);
            }
        }
        onSigFieldSelected.accept(field);
    }

    private static String describe(SignatureFieldInfo field) {
        final String label = RES.get("jfx.gui.sig.field.item", Constants.SIG_FIELD_SELECTOR_NUMBER_PREFIX + field.number(),
                field.name(), String.valueOf(field.page()));
        return field.hasVisibleRect() ? label : label + " " + RES.get("jfx.gui.sig.field.invisible");
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
        noDocument.set(disabled);
    }

    private void bindToViewModel() {
        // A field name is only meaningful for the document it came from, and the properties file happily
        // remembers one. Drop whatever was persisted; the user picks a field from the loaded document.
        viewModel.sigFieldNameProperty().set(null);
        chkVisibleSig.selectedProperty().bindBidirectional(viewModel.visibleProperty());
        txtL2Text.textProperty().bindBidirectional(viewModel.l2TextProperty());
        txtBgImgPath.textProperty().bindBidirectional(viewModel.bgImgPathProperty());

        viewModel.l2TextFontSizeProperty().addListener((obs, o, n) ->
                txtFontSize.setText(String.valueOf(n.floatValue())));
        txtFontSize.setText(String.valueOf(viewModel.l2TextFontSizeProperty().get()));
        txtFontSize.setOnAction(e -> commitFontSize());
        txtFontSize.focusedProperty().addListener((obs, o, n) -> { if (!n) commitFontSize(); });
    }

    private void commitFontSize() {
        try {
            viewModel.l2TextFontSizeProperty().set(Float.parseFloat(txtFontSize.getText()));
        } catch (NumberFormatException ignored) {
            txtFontSize.setText(String.valueOf(viewModel.l2TextFontSizeProperty().get()));
        }
    }

    @FXML
    private void onBrowseBgImage() {
        File file = new NativeFileChooser()
                .setTitle(RES.get("jfx.gui.dialog.selectBackgroundImage"))
                .addFilter(ExtensionFilter.of("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"))
                .addFilter(ExtensionFilter.of("All Files", "*.*"))
                .showOpenDialog(txtBgImgPath.getScene().getWindow());
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
