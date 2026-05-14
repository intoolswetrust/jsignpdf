package net.sf.jsignpdf.fx.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PDFEncryption;
import net.sf.jsignpdf.types.PrintRight;
import net.sf.jsignpdf.types.RenderMode;
import net.sf.jsignpdf.types.ServerAuthentication;
import net.sf.jsignpdf.utils.AppConfig;

import java.net.Proxy;
import java.security.KeyStore;

/**
 * JavaFX property adapter wrapping BasicSignerOptions.
 * Syncs bidirectionally via syncToOptions() / syncFromOptions().
 */
public class SigningOptionsViewModel {

    // Certificate settings
    private final StringProperty ksType = new SimpleStringProperty();
    private final StringProperty ksFile = new SimpleStringProperty();
    private final StringProperty ksPassword = new SimpleStringProperty();
    private final StringProperty keyAlias = new SimpleStringProperty();
    private final IntegerProperty keyIndex = new SimpleIntegerProperty(Constants.DEFVAL_KEY_INDEX);
    private final StringProperty keyPassword = new SimpleStringProperty();
    private final BooleanProperty storePasswords = new SimpleBooleanProperty(Constants.DEFVAL_STOREPWD);

    // File settings
    private final StringProperty outFile = new SimpleStringProperty();
    private final BooleanProperty append = new SimpleBooleanProperty(Constants.DEFVAL_APPEND);

    // Signature metadata
    private final StringProperty signerName = new SimpleStringProperty();
    private final StringProperty reason = new SimpleStringProperty();
    private final StringProperty location = new SimpleStringProperty();
    private final StringProperty contact = new SimpleStringProperty();

    // Certification & hash
    private final ObjectProperty<CertificationLevel> certLevel = new SimpleObjectProperty<>();
    private final ObjectProperty<HashAlgorithm> hashAlgorithm = new SimpleObjectProperty<>();

    // Visible signature
    private final BooleanProperty visible = new SimpleBooleanProperty(false);
    private final IntegerProperty page = new SimpleIntegerProperty(Constants.DEFVAL_PAGE);
    private final FloatProperty positionLLX = new SimpleFloatProperty(Constants.DEFVAL_LLX);
    private final FloatProperty positionLLY = new SimpleFloatProperty(Constants.DEFVAL_LLY);
    private final FloatProperty positionURX = new SimpleFloatProperty(Constants.DEFVAL_URX);
    private final FloatProperty positionURY = new SimpleFloatProperty(Constants.DEFVAL_URY);
    private final FloatProperty bgImgScale = new SimpleFloatProperty(Constants.DEFVAL_BG_SCALE);
    private final ObjectProperty<RenderMode> renderMode = new SimpleObjectProperty<>();
    private final StringProperty l2Text = new SimpleStringProperty();
    private final StringProperty l4Text = new SimpleStringProperty();
    private final FloatProperty l2TextFontSize = new SimpleFloatProperty(Constants.DEFVAL_L2_FONT_SIZE);
    private final StringProperty imgPath = new SimpleStringProperty();
    private final StringProperty bgImgPath = new SimpleStringProperty();
    private final BooleanProperty acro6Layers = new SimpleBooleanProperty(Constants.DEFVAL_ACRO6LAYERS);

    // PDF Encryption
    private final ObjectProperty<PDFEncryption> pdfEncryption = new SimpleObjectProperty<>();
    private final StringProperty pdfOwnerPassword = new SimpleStringProperty();
    private final StringProperty pdfUserPassword = new SimpleStringProperty();
    private final StringProperty pdfEncryptionCertFile = new SimpleStringProperty();

    // Rights
    private final ObjectProperty<PrintRight> rightPrinting = new SimpleObjectProperty<>();
    private final BooleanProperty rightCopy = new SimpleBooleanProperty(true);
    private final BooleanProperty rightAssembly = new SimpleBooleanProperty(true);
    private final BooleanProperty rightFillIn = new SimpleBooleanProperty(true);
    private final BooleanProperty rightScreenReaders = new SimpleBooleanProperty(true);
    private final BooleanProperty rightModifyAnnotations = new SimpleBooleanProperty(true);
    private final BooleanProperty rightModifyContents = new SimpleBooleanProperty(true);

    // TSA
    private final BooleanProperty tsaEnabled = new SimpleBooleanProperty(false);
    private final StringProperty tsaUrl = new SimpleStringProperty();
    private final ObjectProperty<ServerAuthentication> tsaServerAuthn = new SimpleObjectProperty<>();
    private final StringProperty tsaUser = new SimpleStringProperty();
    private final StringProperty tsaPassword = new SimpleStringProperty();
    private final StringProperty tsaCertFileType = new SimpleStringProperty();
    private final StringProperty tsaCertFile = new SimpleStringProperty();
    private final StringProperty tsaCertFilePassword = new SimpleStringProperty();
    private final StringProperty tsaPolicy = new SimpleStringProperty();
    private final ObjectProperty<HashAlgorithm> tsaHashAlg = new SimpleObjectProperty<>();

    // OCSP/CRL
    private final BooleanProperty ocspEnabled = new SimpleBooleanProperty(false);
    private final StringProperty ocspServerUrl = new SimpleStringProperty();
    private final BooleanProperty crlEnabled = new SimpleBooleanProperty(false);

    // Proxy
    private final ObjectProperty<Proxy.Type> proxyType = new SimpleObjectProperty<>(Constants.DEFVAL_PROXY_TYPE);
    private final StringProperty proxyHost = new SimpleStringProperty();
    private final IntegerProperty proxyPort = new SimpleIntegerProperty(Constants.DEFVAL_PROXY_PORT);

    /**
     * Sync values from this ViewModel into a BasicSignerOptions instance.
     */
    public void syncToOptions(BasicSignerOptions opts) {
        // The JavaFX UI always exposes all settings (no simple/advanced toggle),
        // so options must be in advanced mode for all features to take effect.
        opts.setAdvanced(true);
        opts.setKsType(ksType.get());
        opts.setKsFile(ksFile.get());
        opts.setKsPasswd(toCharArray(ksPassword.get()));
        opts.setKeyAlias(keyAlias.get());
        opts.setKeyIndex(keyIndex.get());
        opts.setKeyPasswd(toCharArray(keyPassword.get()));
        opts.setStorePasswords(storePasswords.get());
        opts.setOutFile(outFile.get());
        opts.setAppend(append.get());
        opts.setSignerName(signerName.get());
        opts.setReason(reason.get());
        opts.setLocation(location.get());
        opts.setContact(contact.get());
        opts.setCertLevel(certLevel.get());
        opts.setHashAlgorithm(hashAlgorithm.get());

        // Visible signature
        opts.setVisible(visible.get());
        opts.setPage(page.get());
        opts.setPositionLLX(positionLLX.get());
        opts.setPositionLLY(positionLLY.get());
        opts.setPositionURX(positionURX.get());
        opts.setPositionURY(positionURY.get());
        opts.setBgImgScale(bgImgScale.get());
        // Normalize empty string to null so SignerLogic's null-check (blank rectangle intent) is preserved.
        String l2TextValue = l2Text.get();
        opts.setL2Text(l2TextValue != null && l2TextValue.isEmpty() ? null : l2TextValue);
        opts.setBgImgPath(bgImgPath.get());
        // The simplified JavaFX UI exposes l2Text, l2TextFontSize, and bgImgPath for visible
        // signatures; every other appearance knob is reset to its canonical default
        // so saved settings stay deterministic. See design-doc/3.0.0-simplify-visible.md.
        opts.setRenderMode(RenderMode.DESCRIPTION_ONLY);
        opts.setImgPath(null);
        opts.setL4Text(null);
        opts.setL2TextFontSize(l2TextFontSize.get());
        opts.setAcro6Layers(Constants.DEFVAL_ACRO6LAYERS);

        // Encryption
        opts.setPdfEncryption(pdfEncryption.get());
        opts.setPdfOwnerPwd(toCharArray(pdfOwnerPassword.get()));
        opts.setPdfUserPwd(toCharArray(pdfUserPassword.get()));
        opts.setPdfEncryptionCertFile(pdfEncryptionCertFile.get());

        // Rights
        opts.setRightPrinting(rightPrinting.get());
        opts.setRightCopy(rightCopy.get());
        opts.setRightAssembly(rightAssembly.get());
        opts.setRightFillIn(rightFillIn.get());
        opts.setRightScreanReaders(rightScreenReaders.get());
        opts.setRightModifyAnnotations(rightModifyAnnotations.get());
        opts.setRightModifyContents(rightModifyContents.get());

        // TSA
        opts.setTimestamp(tsaEnabled.get());
        opts.setTsaUrl(tsaUrl.get());
        opts.setTsaServerAuthn(tsaServerAuthn.get());
        opts.setTsaUser(tsaUser.get());
        opts.setTsaPasswd(tsaPassword.get());
        opts.setTsaCertFileType(tsaCertFileType.get());
        opts.setTsaCertFile(tsaCertFile.get());
        opts.setTsaCertFilePwd(tsaCertFilePassword.get());
        opts.setTsaPolicy(tsaPolicy.get());
        HashAlgorithm tsaHa = tsaHashAlg.get();
        opts.setTsaHashAlg(tsaHa != null ? tsaHa.getAlgorithmName() : null);

        // OCSP/CRL
        opts.setOcspEnabled(ocspEnabled.get());
        opts.setOcspServerUrl(ocspServerUrl.get());
        opts.setCrlEnabled(crlEnabled.get());

        // Proxy
        opts.setProxyType(proxyType.get());
        opts.setProxyHost(proxyHost.get());
        opts.setProxyPort(proxyPort.get());
    }

    /**
     * Sync values from a BasicSignerOptions instance into this ViewModel.
     */
    public void syncFromOptions(BasicSignerOptions opts) {
        ksType.set(opts.getKsType());
        ksFile.set(opts.getKsFile());
        ksPassword.set(fromCharArray(opts.getKsPasswd()));
        keyAlias.set(opts.getKeyAlias());
        keyIndex.set(opts.getKeyIndex());
        keyPassword.set(fromCharArray(opts.getKeyPasswd()));
        storePasswords.set(opts.isStorePasswords());
        outFile.set(opts.getOutFile());
        append.set(opts.isAppend());
        signerName.set(opts.getSignerName());
        reason.set(opts.getReason());
        location.set(opts.getLocation());
        contact.set(opts.getContact());
        certLevel.set(opts.getCertLevelX());
        hashAlgorithm.set(opts.getHashAlgorithmX());

        visible.set(opts.isVisible());
        page.set(opts.getPage());
        positionLLX.set(opts.getPositionLLX());
        positionLLY.set(opts.getPositionLLY());
        positionURX.set(opts.getPositionURX());
        positionURY.set(opts.getPositionURY());
        bgImgScale.set(opts.getBgImgScale());
        renderMode.set(opts.getRenderMode());
        l2Text.set(opts.getL2Text());
        l4Text.set(opts.getL4Text());
        l2TextFontSize.set(opts.getL2TextFontSize());
        imgPath.set(opts.getImgPath());
        bgImgPath.set(opts.getBgImgPath());
        acro6Layers.set(opts.isAcro6Layers());

        pdfEncryption.set(opts.getPdfEncryption());
        pdfOwnerPassword.set(opts.getPdfOwnerPwdStr());
        pdfUserPassword.set(opts.getPdfUserPwdStr());
        pdfEncryptionCertFile.set(opts.getPdfEncryptionCertFile());

        rightPrinting.set(opts.getRightPrinting());
        rightCopy.set(opts.isRightCopy());
        rightAssembly.set(opts.isRightAssembly());
        rightFillIn.set(opts.isRightFillIn());
        rightScreenReaders.set(opts.isRightScreanReaders());
        rightModifyAnnotations.set(opts.isRightModifyAnnotations());
        rightModifyContents.set(opts.isRightModifyContents());

        tsaUrl.set(opts.getTsaUrl());
        tsaEnabled.set(opts.isTimestamp());
        tsaServerAuthn.set(opts.getTsaServerAuthn());
        tsaUser.set(opts.getTsaUser());
        tsaPassword.set(opts.getTsaPasswd());
        tsaCertFileType.set(resolveTsaCertFileType(opts.getTsaCertFileType()));
        tsaCertFile.set(opts.getTsaCertFile());
        tsaCertFilePassword.set(opts.getTsaCertFilePwd());
        tsaPolicy.set(opts.getTsaPolicy());
        tsaHashAlg.set(resolveTsaHashAlg(opts.getTsaHashAlg()));

        ocspEnabled.set(opts.isOcspEnabled());
        ocspServerUrl.set(opts.getOcspServerUrl());
        crlEnabled.set(opts.isCrlEnabled());

        proxyType.set(opts.getProxyType());
        proxyHost.set(opts.getProxyHost());
        proxyPort.set(opts.getProxyPort());
    }

    /**
     * Resets all ViewModel properties to their default values.
     */
    public void resetToDefaults() {
        // Certificate settings
        ksType.set(KeyStore.getDefaultType());
        ksFile.set(null);
        ksPassword.set(null);
        keyAlias.set(null);
        keyIndex.set(Constants.DEFVAL_KEY_INDEX);
        keyPassword.set(null);
        storePasswords.set(Constants.DEFVAL_STOREPWD);

        // File & metadata
        outFile.set(null);
        append.set(Constants.DEFVAL_APPEND);
        signerName.set(null);
        reason.set(null);
        location.set(null);
        contact.set(null);
        certLevel.set(CertificationLevel.NOT_CERTIFIED);
        hashAlgorithm.set(Constants.DEFVAL_HASH_ALGORITHM);

        // Visible signature
        visible.set(false);
        page.set(Constants.DEFVAL_PAGE);
        positionLLX.set(Constants.DEFVAL_LLX);
        positionLLY.set(Constants.DEFVAL_LLY);
        positionURX.set(Constants.DEFVAL_URX);
        positionURY.set(Constants.DEFVAL_URY);
        bgImgScale.set(Constants.DEFVAL_BG_SCALE);
        renderMode.set(RenderMode.DESCRIPTION_ONLY);
        l2Text.set(null);
        l4Text.set(null);
        l2TextFontSize.set(Constants.DEFVAL_L2_FONT_SIZE);
        imgPath.set(null);
        bgImgPath.set(null);
        acro6Layers.set(Constants.DEFVAL_ACRO6LAYERS);

        // Encryption & rights
        pdfEncryption.set(PDFEncryption.NONE);
        pdfOwnerPassword.set(null);
        pdfUserPassword.set(null);
        pdfEncryptionCertFile.set(null);
        rightPrinting.set(PrintRight.ALLOW_PRINTING);
        rightCopy.set(true);
        rightAssembly.set(true);
        rightFillIn.set(true);
        rightScreenReaders.set(true);
        rightModifyAnnotations.set(true);
        rightModifyContents.set(true);

        // TSA
        tsaEnabled.set(false);
        tsaUrl.set(null);
        tsaServerAuthn.set(ServerAuthentication.NONE);
        tsaUser.set(null);
        tsaPassword.set(null);
        tsaCertFileType.set(resolveTsaCertFileType(null));
        tsaCertFile.set(null);
        tsaCertFilePassword.set(null);
        tsaPolicy.set(null);
        tsaHashAlg.set(resolveTsaHashAlg(null));

        // OCSP/CRL
        ocspEnabled.set(false);
        ocspServerUrl.set(null);
        crlEnabled.set(false);

        // Proxy
        proxyType.set(Constants.DEFVAL_PROXY_TYPE);
        proxyHost.set(null);
        proxyPort.set(Constants.DEFVAL_PROXY_PORT);
    }

    /**
     * Defaults a blank TSA cert file type to PKCS12 — the TSA panel always
     * exposes a concrete keystore-type selection, matching the CLI default
     * (see {@link net.sf.jsignpdf.ssl.SSLInitializer}).
     */
    private static String resolveTsaCertFileType(String stored) {
        if (stored == null || stored.trim().isEmpty()) {
            return "PKCS12";
        }
        return stored;
    }

    /**
     * Resolves a persisted TSA hash algorithm name to the matching enum value.
     * Falls back to the configured default when the stored value is blank or
     * not recognised — the TSA panel always exposes a concrete selection.
     */
    private static HashAlgorithm resolveTsaHashAlg(String stored) {
        HashAlgorithm ha = HashAlgorithm.fromAlgorithmName(stored);
        if (ha != null) {
            return ha;
        }
        HashAlgorithm fallback = HashAlgorithm.fromAlgorithmName(AppConfig.defaultTsaHashAlg());
        return fallback != null ? fallback : HashAlgorithm.SHA256;
    }

    private static char[] toCharArray(String s) {
        return s != null ? s.toCharArray() : null;
    }

    private static String fromCharArray(char[] c) {
        return c != null ? new String(c) : null;
    }

    // --- Property accessors ---
    public StringProperty ksTypeProperty() { return ksType; }
    public StringProperty ksFileProperty() { return ksFile; }
    public StringProperty ksPasswordProperty() { return ksPassword; }
    public StringProperty keyAliasProperty() { return keyAlias; }
    public IntegerProperty keyIndexProperty() { return keyIndex; }
    public StringProperty keyPasswordProperty() { return keyPassword; }
    public BooleanProperty storePasswordsProperty() { return storePasswords; }
    public StringProperty outFileProperty() { return outFile; }
    public BooleanProperty appendProperty() { return append; }
    public StringProperty signerNameProperty() { return signerName; }
    public StringProperty reasonProperty() { return reason; }
    public StringProperty locationProperty() { return location; }
    public StringProperty contactProperty() { return contact; }
    public ObjectProperty<CertificationLevel> certLevelProperty() { return certLevel; }
    public ObjectProperty<HashAlgorithm> hashAlgorithmProperty() { return hashAlgorithm; }
    public BooleanProperty visibleProperty() { return visible; }
    public IntegerProperty pageProperty() { return page; }
    public FloatProperty positionLLXProperty() { return positionLLX; }
    public FloatProperty positionLLYProperty() { return positionLLY; }
    public FloatProperty positionURXProperty() { return positionURX; }
    public FloatProperty positionURYProperty() { return positionURY; }
    public FloatProperty bgImgScaleProperty() { return bgImgScale; }
    public ObjectProperty<RenderMode> renderModeProperty() { return renderMode; }
    public StringProperty l2TextProperty() { return l2Text; }
    public StringProperty l4TextProperty() { return l4Text; }
    public FloatProperty l2TextFontSizeProperty() { return l2TextFontSize; }
    public StringProperty imgPathProperty() { return imgPath; }
    public StringProperty bgImgPathProperty() { return bgImgPath; }
    public BooleanProperty acro6LayersProperty() { return acro6Layers; }
    public ObjectProperty<PDFEncryption> pdfEncryptionProperty() { return pdfEncryption; }
    public StringProperty pdfOwnerPasswordProperty() { return pdfOwnerPassword; }
    public StringProperty pdfUserPasswordProperty() { return pdfUserPassword; }
    public StringProperty pdfEncryptionCertFileProperty() { return pdfEncryptionCertFile; }
    public ObjectProperty<PrintRight> rightPrintingProperty() { return rightPrinting; }
    public BooleanProperty rightCopyProperty() { return rightCopy; }
    public BooleanProperty rightAssemblyProperty() { return rightAssembly; }
    public BooleanProperty rightFillInProperty() { return rightFillIn; }
    public BooleanProperty rightScreenReadersProperty() { return rightScreenReaders; }
    public BooleanProperty rightModifyAnnotationsProperty() { return rightModifyAnnotations; }
    public BooleanProperty rightModifyContentsProperty() { return rightModifyContents; }
    public BooleanProperty tsaEnabledProperty() { return tsaEnabled; }
    public StringProperty tsaUrlProperty() { return tsaUrl; }
    public ObjectProperty<ServerAuthentication> tsaServerAuthnProperty() { return tsaServerAuthn; }
    public StringProperty tsaUserProperty() { return tsaUser; }
    public StringProperty tsaPasswordProperty() { return tsaPassword; }
    public StringProperty tsaCertFileTypeProperty() { return tsaCertFileType; }
    public StringProperty tsaCertFileProperty() { return tsaCertFile; }
    public StringProperty tsaCertFilePasswordProperty() { return tsaCertFilePassword; }
    public StringProperty tsaPolicyProperty() { return tsaPolicy; }
    public ObjectProperty<HashAlgorithm> tsaHashAlgProperty() { return tsaHashAlg; }
    public BooleanProperty ocspEnabledProperty() { return ocspEnabled; }
    public StringProperty ocspServerUrlProperty() { return ocspServerUrl; }
    public BooleanProperty crlEnabledProperty() { return crlEnabled; }
    public ObjectProperty<Proxy.Type> proxyTypeProperty() { return proxyType; }
    public StringProperty proxyHostProperty() { return proxyHost; }
    public IntegerProperty proxyPortProperty() { return proxyPort; }
}
