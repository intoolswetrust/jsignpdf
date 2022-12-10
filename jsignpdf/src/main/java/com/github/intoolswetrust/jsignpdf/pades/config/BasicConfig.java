package com.github.intoolswetrust.jsignpdf.pades.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;

public class BasicConfig {

    @Parameter(converter = FileConverter.class, description = "PDF files to be signed")
    private List<File> files = new ArrayList<>();

    @Parameter(names = { "--help", "-h" }, help = true, description = "Prints this help")
    private boolean printHelp;

    @Parameter(names = { "--list-keystore-types", "-lkt" }, description = "Command listing available keystore types")
    private boolean listKeyStores;

    @Parameter(names = { "--list-keys", "-lk" }, description = "Command listing signing key aliases in the specified keystore")
    private boolean listKeys;

    @Parameter(names = { "--keystore-type", "-kst" }, description = "Keystore type to be loaded")
    private String keyStoreType;

    @Parameter(names = { "--keystore-file", "-ksf" }, converter = FileConverter.class, description = "Keystore file to be used")
    private File keyStoreFile;

    @Parameter(names = { "--keystore-password", "-ksp" }, description = "KeyStore password")
    private String keyStorePassword;

    @Parameter(names = { "--key-password", "-kp" }, description = "Key password")
    private String keyPassword;

    @Parameter(names = { "--key-alias", "-ka" }, description = "Key alias to be used for signing")
    private String keyAlias;

    @Parameter(names = { "--pades-level", "-pl" }, description = "PAdES level")
    private PadesLevel padesLevel = PadesLevel.BASELINE_B;

    @Parameter(names = { "--out-suffix", "-os" }, description = "Signed file suffix to be attached to the original name")
    private String outSuffix = "_signed";

    @Parameter(names = { "--out-directory", "-d" }, converter = FileConverter.class, description = "Directory to write the signed PDFs to. If not provided, the source directory of input PDF file is used.")
    private File outDirectory;

    @Parameter(names = "--disable-validity-check", description = "Don't check certificate validity in the keystore")
    private boolean disableValidityCheck;

    @Parameter(names = "--disable-key-usage-check", description = "Don't check certificate key-usage field in the keystore")
    private boolean disableKeyUsageCheck;

    @Parameter(names = "--disable-critical-extensions-check", description = "Don't check if all certificate critical extensions are known")
    private boolean disableCriticalExtensionsCheck;

    @Parameter(names = {"--digest-algorithm", "-da"}, description = "Digest algorithm used in the signature")
    private DigestAlgorithm digestAlgorithm = DigestAlgorithm.SHA256;

    private final TsaConfig tsaConfig = new TsaConfig();

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keystoreType) {
        this.keyStoreType = keystoreType;
    }

    public File getKeyStoreFile() {
        return keyStoreFile;
    }

    public void setKeyStoreFile(File keystoreFile) {
        this.keyStoreFile = keystoreFile;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keystorePassword) {
        this.keyStorePassword = keystorePassword;
    }

    public char[] getKeyStorePasswordAsChars() {
        return keyStorePassword == null ? null : keyStorePassword.toCharArray();
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public char[] getKeyPasswordAsChars() {
        return keyPassword == null ? null : keyPassword.toCharArray();
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public PadesLevel getPadesLevel() {
        return padesLevel;
    }

    public void setPadesLevel(PadesLevel padesLevel) {
        this.padesLevel = padesLevel;
    }

    public boolean isPrintHelp() {
        return printHelp;
    }

    public void setPrintHelp(boolean printHelp) {
        this.printHelp = printHelp;
    }

    public boolean isListKeyStores() {
        return listKeyStores;
    }

    public void setListKeyStores(boolean listKeyStores) {
        this.listKeyStores = listKeyStores;
    }

    public boolean isListKeys() {
        return listKeys;
    }

    public void setListKeys(boolean listKeys) {
        this.listKeys = listKeys;
    }

    public String getOutSuffix() {
        return outSuffix;
    }

    public void setOutSuffix(String outSuffix) {
        this.outSuffix = outSuffix;
    }

    public File getOutDirectory() {
        return outDirectory;
    }

    public void setOutDirectory(File outDirectory) {
        this.outDirectory = outDirectory;
    }

    public boolean isDisableValidityCheck() {
        return disableValidityCheck;
    }

    public void setDisableValidityCheck(boolean disableValidityCheck) {
        this.disableValidityCheck = disableValidityCheck;
    }

    public boolean isDisableKeyUsageCheck() {
        return disableKeyUsageCheck;
    }

    public void setDisableKeyUsageCheck(boolean disableKeyUsageCheck) {
        this.disableKeyUsageCheck = disableKeyUsageCheck;
    }

    public boolean isDisableCriticalExtensionsCheck() {
        return disableCriticalExtensionsCheck;
    }

    public void setDisableCriticalExtensionsCheck(boolean disableCriticalExtensionsCheck) {
        this.disableCriticalExtensionsCheck = disableCriticalExtensionsCheck;
    }

    public DigestAlgorithm getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(DigestAlgorithm digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public TsaConfig getTsaConfig() {
        return tsaConfig;
    }

}
