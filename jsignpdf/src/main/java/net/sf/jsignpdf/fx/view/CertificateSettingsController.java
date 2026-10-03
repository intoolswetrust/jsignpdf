package net.sf.jsignpdf.fx.view;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.fx.util.NativeFileChooser;
import net.sf.jsignpdf.fx.util.NativeFileChooser.ExtensionFilter;
import net.sf.jsignpdf.fx.service.KeyStoreService;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.pkcs11.Pkcs11Backend;
import net.sf.jsignpdf.pkcs11.Pkcs11Profile;
import net.sf.jsignpdf.pkcs11.Pkcs11Profiles;
import net.sf.jsignpdf.pkcs11.ProfileStatus;
import net.sf.jsignpdf.utils.KeyStoreUtils;

import org.apache.commons.lang3.StringUtils;

import static net.sf.jsignpdf.Constants.LOGGER;
import static net.sf.jsignpdf.Constants.RES;

/**
 * Controller for the certificate/keystore settings panel.
 */
public class CertificateSettingsController {

    @FXML private ComboBox<KeystoreChoice> cmbKeystoreType;
    @FXML private Label lblKeystoreFile;
    @FXML private HBox boxKeystoreFile;
    @FXML private TextField txtKeystoreFile;
    @FXML private Button btnBrowseKeystore;
    @FXML private PasswordField txtKeystorePassword;
    @FXML private Button btnLoadKeys;
    @FXML private Label lblLoadKeysStatus;
    @FXML private ComboBox<String> cmbKeyAlias;
    @FXML private PasswordField txtKeyPassword;
    @FXML private CheckBox chkStorePasswords;

    private SigningOptionsViewModel viewModel;
    private final KeyStoreService keyStoreService = new KeyStoreService();
    private boolean syncing;

    @FXML
    private void initialize() {
        cmbKeystoreType.setConverter(new StringConverter<KeystoreChoice>() {
            @Override
            public String toString(KeystoreChoice c) {
                return c == null ? "" : c.display();
            }

            @Override
            public KeystoreChoice fromString(String s) {
                return null;
            }
        });
        cmbKeystoreType.setCellFactory(lv -> new ListCell<KeystoreChoice>() {
            @Override
            protected void updateItem(KeystoreChoice item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    setDisable(false);
                } else {
                    setText(item.display());
                    setDisable(!item.isEnabled());
                    setTooltip(item.note() == null ? null : new Tooltip(item.note()));
                }
            }
        });
        cmbKeystoreType.setItems(FXCollections.observableArrayList(buildChoices(null, null)));
        cmbKeystoreType.valueProperty().addListener((obs, oldV, newV) -> onChoiceSelected(newV));
        showLoadStatus(null);

        keyStoreService.setOnSucceeded(e -> {
            String[] aliases = keyStoreService.getValue();
            cmbKeyAlias.setItems(FXCollections.observableArrayList(aliases));
            if (aliases.length > 0) {
                cmbKeyAlias.getSelectionModel().selectFirst();
                showLoadStatus(RES.get("jfx.gui.cert.loadKeys.ok", String.valueOf(aliases.length)));
            } else {
                showLoadStatus(RES.get("jfx.gui.cert.loadKeys.none"));
            }
            refreshKeystoreChoices();
        });
        keyStoreService.setOnFailed(e -> {
            Throwable ex = keyStoreService.getException();
            LOGGER.log(Level.WARNING, "Failed to load key aliases", ex);
            cmbKeyAlias.getItems().clear();
            String reason = ex == null ? "" : StringUtils.defaultIfBlank(ex.getMessage(), ex.getClass().getSimpleName());
            showLoadStatus(RES.get("jfx.gui.cert.loadKeys.failed", reason));
            refreshKeystoreChoices();
        });
    }

    public void setViewModel(SigningOptionsViewModel vm) {
        this.viewModel = vm;
        bindToViewModel();
    }

    private void bindToViewModel() {
        viewModel.ksTypeProperty().addListener((obs, o, n) -> selectFromViewModel());
        viewModel.ksProviderProperty().addListener((obs, o, n) -> selectFromViewModel());
        refreshKeystoreChoices();
        txtKeystoreFile.textProperty().bindBidirectional(viewModel.ksFileProperty());
        txtKeystorePassword.textProperty().bindBidirectional(viewModel.ksPasswordProperty());
        cmbKeyAlias.valueProperty().bindBidirectional(viewModel.keyAliasProperty());
        txtKeyPassword.textProperty().bindBidirectional(viewModel.keyPasswordProperty());
        chkStorePasswords.selectedProperty().bindBidirectional(viewModel.storePasswordsProperty());
    }

    /**
     * Rebuilds the keystore-type entries from the current PKCS#11 profiles and reselects the view-model selection. A
     * selected profile renamed in Preferences is followed.
     */
    public void refreshKeystoreChoices() {
        String type = viewModel == null ? null : viewModel.ksTypeProperty().get();
        String provider = viewModel == null ? null : viewModel.ksProviderProperty().get();
        Pkcs11Profiles profiles = Pkcs11Profiles.getInstance();
        String renamed = provider == null || profiles.find(provider).isPresent() ? null : profiles.renamedTo(provider);
        if (renamed != null && viewModel != null) {
            provider = renamed;
            syncing = true;
            try {
                viewModel.ksProviderProperty().set(renamed);
            } finally {
                syncing = false;
            }
        }
        syncing = true;
        try {
            cmbKeystoreType.getItems().setAll(buildChoices(type, provider));
        } finally {
            syncing = false;
        }
        selectFromViewModel();
    }

    static List<KeystoreChoice> buildChoices(String currentType, String currentProvider) {
        Pkcs11Profiles profiles = Pkcs11Profiles.getInstance();
        List<KeystoreChoice> result = new ArrayList<>();
        for (String type : KeyStoreUtils.getKeyStores()) {
            if (!Pkcs11Profiles.isPkcs11Type(type)) {
                result.add(KeystoreChoice.plain(type));
                continue;
            }
            for (Pkcs11Profile p : profiles.list()) {
                boolean listed = p.offeredTypes().contains(type);
                boolean current = type.equalsIgnoreCase(currentType) && p.id().equalsIgnoreCase(currentProvider);
                if (!listed && !(current && p.accepts(type))) {
                    continue;
                }
                ProfileStatus st = profiles.status(p.id());
                String note = st != null && st.state() == ProfileStatus.State.FAILED
                        ? RES.get("jfx.gui.cert.pkcs11.failed", st.message()) : null;
                result.add(new KeystoreChoice(type, p.id(), p.displayLabel(), note, true));
            }
        }
        for (ProfileStatus st : profiles.statuses()) {
            if (st.state() == ProfileStatus.State.INVALID) {
                result.add(new KeystoreChoice(Pkcs11Backend.SUN.keyStoreType(), st.id(), st.id(), st.message(), false));
            }
        }
        return result;
    }

    private void selectFromViewModel() {
        if (syncing || viewModel == null) {
            return;
        }
        String type = viewModel.ksTypeProperty().get();
        String provider = viewModel.ksProviderProperty().get();
        KeystoreChoice match = null;
        String status = null;
        if (Pkcs11Profiles.isPkcs11Type(type)) {
            List<KeystoreChoice> ofType = cmbKeystoreType.getItems().stream()
                    .filter(c -> c.type().equalsIgnoreCase(type) && c.isEnabled()).toList();
            if (StringUtils.isNotEmpty(provider)) {
                match = ofType.stream().filter(c -> c.matches(type, provider)).findFirst().orElse(null);
                if (match == null) {
                    status = RES.get("jfx.gui.cert.pkcs11.missing", provider);
                }
            } else if (ofType.size() == 1) {
                match = ofType.get(0);
            } else if (!ofType.isEmpty()) {
                status = RES.get("jfx.gui.cert.pkcs11.pick");
            }
        } else {
            match = cmbKeystoreType.getItems().stream().filter(c -> c.matches(type, null)).findFirst().orElse(null);
        }
        syncing = true;
        try {
            cmbKeystoreType.setValue(match);
        } finally {
            syncing = false;
        }
        if (match != null && match.profileId() != null && !match.profileId().equals(provider)) {
            syncing = true;
            try {
                viewModel.ksProviderProperty().set(match.profileId());
            } finally {
                syncing = false;
            }
        }
        updateFileControls(match != null ? match.type() : type);
        if (status != null) {
            showLoadStatus(status);
        }
    }

    private void onChoiceSelected(KeystoreChoice choice) {
        if (syncing || viewModel == null) {
            return;
        }
        if (choice != null && !choice.isEnabled()) {
            selectFromViewModel();
            return;
        }
        syncing = true;
        try {
            viewModel.ksProviderProperty().set(choice == null ? null : choice.profileId());
            viewModel.ksTypeProperty().set(choice == null ? null : choice.type());
        } finally {
            syncing = false;
        }
        updateFileControls(choice == null ? null : choice.type());
        showLoadStatus(null);
    }

    private void updateFileControls(String type) {
        boolean fileBased = !Pkcs11Profiles.isPkcs11Type(type);
        lblKeystoreFile.setVisible(fileBased);
        lblKeystoreFile.setManaged(fileBased);
        boxKeystoreFile.setVisible(fileBased);
        boxKeystoreFile.setManaged(fileBased);
    }

    private void showLoadStatus(String text) {
        boolean show = StringUtils.isNotEmpty(text);
        lblLoadKeysStatus.setText(show ? text : "");
        lblLoadKeysStatus.setVisible(show);
        lblLoadKeysStatus.setManaged(show);
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
        KeystoreChoice choice = cmbKeystoreType.getValue();
        BasicSignerOptions tmpOpts = new BasicSignerOptions();
        tmpOpts.setKsType(choice != null ? choice.type() : viewModel.ksTypeProperty().get());
        tmpOpts.setKsProvider(choice != null ? choice.profileId() : viewModel.ksProviderProperty().get());
        tmpOpts.setKsFile(txtKeystoreFile.getText());
        tmpOpts.setKsPasswd(txtKeystorePassword.getText() != null
                ? txtKeystorePassword.getText().toCharArray() : null);

        showLoadStatus(null);
        keyStoreService.cancel();
        keyStoreService.reset();
        keyStoreService.setOptions(tmpOpts);
        keyStoreService.start();
    }
}
