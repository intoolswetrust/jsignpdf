package net.sf.jsignpdf.fx.view;

import java.io.File;
import java.util.logging.Level;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.fx.util.NativeFileChooser;
import net.sf.jsignpdf.fx.util.NativeFileChooser.ExtensionFilter;
import net.sf.jsignpdf.fx.service.KeyStoreService;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.utils.KeyStoreUtils;

import static net.sf.jsignpdf.Constants.LOGGER;
import static net.sf.jsignpdf.Constants.RES;

/**
 * Controller for the certificate/keystore settings panel.
 */
public class CertificateSettingsController {

    @FXML private ComboBox<String> cmbKeystoreType;
    @FXML private TextField txtKeystoreFile;
    @FXML private Button btnBrowseKeystore;
    @FXML private PasswordField txtKeystorePassword;
    @FXML private Button btnLoadKeys;
    @FXML private ComboBox<String> cmbKeyAlias;
    @FXML private PasswordField txtKeyPassword;
    @FXML private CheckBox chkStorePasswords;

    private SigningOptionsViewModel viewModel;
    private final KeyStoreService keyStoreService = new KeyStoreService();

    @FXML
    private void initialize() {
        // Populate keystore types
        cmbKeystoreType.setItems(FXCollections.observableArrayList(KeyStoreUtils.getKeyStores()));

        // Setup key loading service callbacks
        keyStoreService.setOnSucceeded(e -> {
            String[] aliases = keyStoreService.getValue();
            cmbKeyAlias.setItems(FXCollections.observableArrayList(aliases));
            if (aliases.length > 0) {
                cmbKeyAlias.getSelectionModel().selectFirst();
            }
        });
        keyStoreService.setOnFailed(e -> {
            LOGGER.log(Level.WARNING, "Failed to load key aliases", keyStoreService.getException());
            cmbKeyAlias.getItems().clear();
        });
    }

    public void setViewModel(SigningOptionsViewModel vm) {
        this.viewModel = vm;
        bindToViewModel();
    }

    private void bindToViewModel() {
        // Bidirectional bindings
        cmbKeystoreType.valueProperty().bindBidirectional(viewModel.ksTypeProperty());
        txtKeystoreFile.textProperty().bindBidirectional(viewModel.ksFileProperty());
        txtKeystorePassword.textProperty().bindBidirectional(viewModel.ksPasswordProperty());
        cmbKeyAlias.valueProperty().bindBidirectional(viewModel.keyAliasProperty());
        txtKeyPassword.textProperty().bindBidirectional(viewModel.keyPasswordProperty());
        chkStorePasswords.selectedProperty().bindBidirectional(viewModel.storePasswordsProperty());
    }

    @FXML
    private void onBrowseKeystore() {
        // "All Files" must remain first — load-bearing ordering for this site.
        File file = new NativeFileChooser()
                .setTitle(RES.get("jfx.gui.dialog.selectKeystoreFile"))
                .addFilter(ExtensionFilter.of("All Files", "*.*"))
                .addFilter(ExtensionFilter.of("PKCS12", "*.p12", "*.pfx"))
                .addFilter(ExtensionFilter.of("JKS", "*.jks"))
                .showOpenDialog(txtKeystoreFile.getScene().getWindow());
        if (file != null) {
            txtKeystoreFile.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void onLoadKeys() {
        // Create temporary options to load keys
        BasicSignerOptions tmpOpts = new BasicSignerOptions();
        tmpOpts.setKsType(cmbKeystoreType.getValue());
        tmpOpts.setKsFile(txtKeystoreFile.getText());
        tmpOpts.setKsPasswd(txtKeystorePassword.getText() != null
                ? txtKeystorePassword.getText().toCharArray() : null);

        keyStoreService.cancel();
        keyStoreService.reset();
        keyStoreService.setOptions(tmpOpts);
        keyStoreService.start();
    }
}
