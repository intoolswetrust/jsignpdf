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
import javafx.stage.FileChooser;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.types.ServerAuthentication;

import java.net.Proxy;

/**
 * Controller for TSA, OCSP, CRL, and proxy settings.
 */
public class TsaSettingsController {

    @FXML private CheckBox chkTsaEnabled;
    @FXML private TextField txtTsaUrl;
    @FXML private ComboBox<ServerAuthentication> cmbTsaAuthn;
    @FXML private TextField txtTsaUser;
    @FXML private PasswordField txtTsaPassword;
    @FXML private TextField txtTsaCertFileType;
    @FXML private TextField txtTsaCertFile;
    @FXML private PasswordField txtTsaCertFilePassword;
    @FXML private TextField txtTsaPolicy;
    @FXML private TextField txtTsaHashAlg;
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
        cmbProxyType.setItems(FXCollections.observableArrayList(Proxy.Type.values()));

        // Toggle TSA details visibility
        tsaDetailsPane.managedProperty().bind(tsaDetailsPane.visibleProperty());
        chkTsaEnabled.selectedProperty().addListener((obs, o, n) ->
                tsaDetailsPane.setVisible(n));
        tsaDetailsPane.setVisible(false);

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
        txtTsaCertFileType.textProperty().bindBidirectional(viewModel.tsaCertFileTypeProperty());
        txtTsaCertFile.textProperty().bindBidirectional(viewModel.tsaCertFileProperty());
        txtTsaCertFilePassword.textProperty().bindBidirectional(viewModel.tsaCertFilePasswordProperty());
        txtTsaPolicy.textProperty().bindBidirectional(viewModel.tsaPolicyProperty());
        txtTsaHashAlg.textProperty().bindBidirectional(viewModel.tsaHashAlgProperty());

        chkOcspEnabled.selectedProperty().bindBidirectional(viewModel.ocspEnabledProperty());
        txtOcspServerUrl.textProperty().bindBidirectional(viewModel.ocspServerUrlProperty());
        chkCrlEnabled.selectedProperty().bindBidirectional(viewModel.crlEnabledProperty());

        cmbProxyType.valueProperty().bindBidirectional(viewModel.proxyTypeProperty());
        txtProxyHost.textProperty().bindBidirectional(viewModel.proxyHostProperty());

        // Proxy port: String <-> int
        viewModel.proxyPortProperty().addListener((obs, o, n) ->
                txtProxyPort.setText(String.valueOf(n.intValue())));
        txtProxyPort.setOnAction(e -> {
            try {
                viewModel.proxyPortProperty().set(Integer.parseInt(txtProxyPort.getText()));
            } catch (NumberFormatException ignored) {
            }
        });

        // Update visibility from initial loaded values
        tsaDetailsPane.setVisible(viewModel.tsaEnabledProperty().get());
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

    @FXML
    private void onBrowseTsaCertFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle(RES.get("jfx.gui.dialog.selectTsaCertFile"));
        File file = fc.showOpenDialog(txtTsaCertFile.getScene().getWindow());
        if (file != null) txtTsaCertFile.setText(file.getAbsolutePath());
    }
}
