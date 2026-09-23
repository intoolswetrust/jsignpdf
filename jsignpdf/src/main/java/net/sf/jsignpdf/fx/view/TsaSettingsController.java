package net.sf.jsignpdf.fx.view;

import java.io.File;

import static net.sf.jsignpdf.Constants.RES;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import net.sf.jsignpdf.fx.util.NativeFileChooser;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.ServerAuthentication;
import net.sf.jsignpdf.utils.KeyStoreUtils;

import java.net.Proxy;

/**
 * Controller for TSA, OCSP, CRL, and proxy settings.
 */
public class TsaSettingsController {

    private static final String STYLE_VALIDATION_ERROR = "-fx-border-color: red; -fx-border-width: 1;";
    private static final int MIN_PROXY_PORT = 1;
    private static final int MAX_PROXY_PORT = 65535;

    @FXML private CheckBox chkTsaEnabled;
    @FXML private TextField txtTsaUrl;
    @FXML private ComboBox<ServerAuthentication> cmbTsaAuthn;
    @FXML private VBox tsaUserPane;
    @FXML private TextField txtTsaUser;
    @FXML private PasswordField txtTsaPassword;
    @FXML private VBox tsaCertPane;
    @FXML private ComboBox<String> cmbTsaCertFileType;
    @FXML private TextField txtTsaCertFile;
    @FXML private PasswordField txtTsaCertFilePassword;
    @FXML private TextField txtTsaPolicy;
    @FXML private ComboBox<HashAlgorithm> cmbTsaHashAlg;
    @FXML private VBox tsaDetailsPane;

    @FXML private CheckBox chkOcspEnabled;
    @FXML private Label lblOcspServerUrl;
    @FXML private TextField txtOcspServerUrl;
    @FXML private CheckBox chkCrlEnabled;

    @FXML private ComboBox<Proxy.Type> cmbProxyType;
    @FXML private Label lblProxyHost;
    @FXML private TextField txtProxyHost;
    @FXML private Label lblProxyPort;
    @FXML private TextField txtProxyPort;

    private SigningOptionsViewModel viewModel;

    @FXML
    private void initialize() {
        cmbTsaAuthn.setItems(FXCollections.observableArrayList(ServerAuthentication.values()));
        cmbTsaHashAlg.setItems(FXCollections.observableArrayList(HashAlgorithm.values()));
        cmbTsaCertFileType.setItems(FXCollections.observableArrayList(KeyStoreUtils.getKeyStores()));
        cmbProxyType.setItems(FXCollections.observableArrayList(Proxy.Type.values()));

        // Toggle TSA details visibility
        tsaDetailsPane.managedProperty().bind(tsaDetailsPane.visibleProperty());
        chkTsaEnabled.selectedProperty().addListener((obs, o, n) -> {
            tsaDetailsPane.setVisible(n);
            updateValidation();
        });
        tsaDetailsPane.setVisible(false);

        // Live validation: TSA URL is required when TSA is enabled.
        txtTsaUrl.textProperty().addListener((obs, o, n) -> updateValidation());

        // Toggle auth-dependent panes: show only the inputs relevant to the
        // selected authentication method (user/password vs certificate file).
        tsaUserPane.managedProperty().bind(tsaUserPane.visibleProperty());
        tsaCertPane.managedProperty().bind(tsaCertPane.visibleProperty());
        cmbTsaAuthn.valueProperty().addListener((obs, o, n) -> applyAuthVisibility(n));
        applyAuthVisibility(null);

        // Toggle OCSP URL visibility
        chkOcspEnabled.selectedProperty().addListener((obs, o, n) -> {
            lblOcspServerUrl.setVisible(n);
            txtOcspServerUrl.setVisible(n);
            lblOcspServerUrl.setManaged(n);
            txtOcspServerUrl.setManaged(n);
        });
        lblOcspServerUrl.setVisible(false);
        txtOcspServerUrl.setVisible(false);
        lblOcspServerUrl.setManaged(false);
        txtOcspServerUrl.setManaged(false);

        // Toggle proxy details visibility
        cmbProxyType.valueProperty().addListener((obs, o, n) -> {
            boolean showDetails = n != null && n != Proxy.Type.DIRECT;
            lblProxyHost.setVisible(showDetails);
            txtProxyHost.setVisible(showDetails);
            lblProxyPort.setVisible(showDetails);
            txtProxyPort.setVisible(showDetails);
            lblProxyHost.setManaged(showDetails);
            txtProxyHost.setManaged(showDetails);
            lblProxyPort.setManaged(showDetails);
            txtProxyPort.setManaged(showDetails);
        });
        lblProxyHost.setVisible(false);
        txtProxyHost.setVisible(false);
        lblProxyPort.setVisible(false);
        txtProxyPort.setVisible(false);
        lblProxyHost.setManaged(false);
        txtProxyHost.setManaged(false);
        lblProxyPort.setManaged(false);
        txtProxyPort.setManaged(false);
    }

    public void setViewModel(SigningOptionsViewModel vm) {
        this.viewModel = vm;
        bindToViewModel();
    }

    private void bindToViewModel() {
        chkTsaEnabled.selectedProperty().bindBidirectional(viewModel.tsaEnabledProperty());
        txtTsaUrl.textProperty().bindBidirectional(viewModel.tsaUrlProperty());
        cmbTsaAuthn.valueProperty().bindBidirectional(viewModel.tsaServerAuthnProperty());
        txtTsaUser.textProperty().bindBidirectional(viewModel.tsaUserProperty());
        txtTsaPassword.textProperty().bindBidirectional(viewModel.tsaPasswordProperty());
        cmbTsaCertFileType.valueProperty().bindBidirectional(viewModel.tsaCertFileTypeProperty());
        txtTsaCertFile.textProperty().bindBidirectional(viewModel.tsaCertFileProperty());
        txtTsaCertFilePassword.textProperty().bindBidirectional(viewModel.tsaCertFilePasswordProperty());
        txtTsaPolicy.textProperty().bindBidirectional(viewModel.tsaPolicyProperty());
        cmbTsaHashAlg.valueProperty().bindBidirectional(viewModel.tsaHashAlgProperty());

        chkOcspEnabled.selectedProperty().bindBidirectional(viewModel.ocspEnabledProperty());
        txtOcspServerUrl.textProperty().bindBidirectional(viewModel.ocspServerUrlProperty());
        chkCrlEnabled.selectedProperty().bindBidirectional(viewModel.crlEnabledProperty());

        cmbProxyType.valueProperty().bindBidirectional(viewModel.proxyTypeProperty());
        txtProxyHost.textProperty().bindBidirectional(viewModel.proxyHostProperty());

        // Proxy port: String <-> int
        viewModel.proxyPortProperty().addListener((obs, o, n) ->
                txtProxyPort.setText(String.valueOf(n.intValue())));
        txtProxyPort.setText(String.valueOf(viewModel.proxyPortProperty().get()));
        txtProxyPort.setOnAction(e -> commitProxyPort());
        txtProxyPort.focusedProperty().addListener((obs, o, isFocused) -> {
            if (!isFocused) {
                commitProxyPort();
            }
        });

        // Update visibility from initial loaded values
        tsaDetailsPane.setVisible(viewModel.tsaEnabledProperty().get());
        updateValidation();
        applyAuthVisibility(viewModel.tsaServerAuthnProperty().get());
        boolean ocspOn = viewModel.ocspEnabledProperty().get();
        lblOcspServerUrl.setVisible(ocspOn);
        txtOcspServerUrl.setVisible(ocspOn);
        lblOcspServerUrl.setManaged(ocspOn);
        txtOcspServerUrl.setManaged(ocspOn);
        Proxy.Type pt = viewModel.proxyTypeProperty().get();
        boolean proxyOn = pt != null && pt != Proxy.Type.DIRECT;
        lblProxyHost.setVisible(proxyOn);
        txtProxyHost.setVisible(proxyOn);
        lblProxyPort.setVisible(proxyOn);
        txtProxyPort.setVisible(proxyOn);
        lblProxyHost.setManaged(proxyOn);
        txtProxyHost.setManaged(proxyOn);
        lblProxyPort.setManaged(proxyOn);
        txtProxyPort.setManaged(proxyOn);
    }

    /**
     * Writes the proxy port field to the view model. Enter and focus loss already do this, but
     * neither fires for a menu accelerator or the window close request, both of which persist
     * the view model straight away.
     */
    public void commitPendingEdits() {
        commitProxyPort();
    }

    private void commitProxyPort() {
        int port = parseProxyPort(txtProxyPort.getText());
        if (port < 0) {
            // Revert rather than leave a value on screen that was never persisted.
            txtProxyPort.setText(String.valueOf(viewModel.proxyPortProperty().get()));
            return;
        }
        viewModel.proxyPortProperty().set(port);
    }

    private static int parseProxyPort(String text) {
        try {
            int port = Integer.parseInt(text.trim());
            return port >= MIN_PROXY_PORT && port <= MAX_PROXY_PORT ? port : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void applyAuthVisibility(ServerAuthentication authn) {
        tsaUserPane.setVisible(authn == ServerAuthentication.PASSWORD);
        tsaCertPane.setVisible(authn == ServerAuthentication.CERTIFICATE);
    }

    /**
     * Applies or clears a red-border style on the TSA URL field. The URL is
     * required whenever TSA is enabled.
     */
    private void updateValidation() {
        boolean invalid = chkTsaEnabled.isSelected() && isBlank(txtTsaUrl.getText());
        txtTsaUrl.setStyle(invalid ? STYLE_VALIDATION_ERROR : null);
    }

    /**
     * Returns true if TSA is either disabled or has a non-blank server URL.
     * Used by the main controller to gate Sign.
     */
    public boolean isTsaConfigValid() {
        return !chkTsaEnabled.isSelected() || !isBlank(txtTsaUrl.getText());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @FXML
    private void onBrowseTsaCertFile() {
        File file = new NativeFileChooser()
                .setTitle(RES.get("jfx.gui.dialog.selectTsaCertFile"))
                .showOpenDialog(txtTsaCertFile.getScene().getWindow());
        if (file != null) txtTsaCertFile.setText(file.getAbsolutePath());
    }
}
