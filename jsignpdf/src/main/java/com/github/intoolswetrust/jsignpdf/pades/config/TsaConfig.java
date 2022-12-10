package com.github.intoolswetrust.jsignpdf.pades.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;

public class TsaConfig {

    @Parameter(names = {"--tsa-server-url", "-ts"}, description = "Timestamp server URL")
    private String tsaServerUrl;

    @Parameter(names = {"--tsa-key-file-type", "-tskt"}, description = "KeyStore type for TSA client-certificate authentication")
    private String tsaKeyStoreFileType;
    @Parameter(names = {"--tsa-key-file", "-tskf"}, converter = FileConverter.class, description = "KeyStore file for TSA client-certificate authentication")
    private File tsaKeyStoreFile;
    @Parameter(names = {"--tsa-key-password", "-tskp"}, description = "KeyStore password for TSA client-certificate authentication")
    private String tsaKeyStorePassword;

    @Parameter(names = {"--tsa-user", "-tsu"}, description = "Username for TSA Basic authentication")
    private String tsaUser;
    @Parameter(names = {"--tsa-password", "-ts"}, description = "Password for TSA Basic authentication")
    private String tsaPassword;
    @Parameter(names = {"--tsa-policy-oid"}, description = "TSA policy OID")
    private String tsaPolicyOid;

    public String getTsaServerUrl() {
        return tsaServerUrl;
    }

    public void setTsaServerUrl(String tsaServerUrl) {
        this.tsaServerUrl = tsaServerUrl;
    }

    public String getTsaKeyStoreFileType() {
        return tsaKeyStoreFileType;
    }

    public void setTsaKeyStoreFileType(String tsaKeyStoreFileType) {
        this.tsaKeyStoreFileType = tsaKeyStoreFileType;
    }

    public File getTsaKeyStoreFile() {
        return tsaKeyStoreFile;
    }

    public void setTsaKeyStoreFile(File tsaKeyStoreFile) {
        this.tsaKeyStoreFile = tsaKeyStoreFile;
    }

    public String getTsaKeyStorePassword() {
        return tsaKeyStorePassword;
    }

    public void setTsaKeyStorePassword(String tsaKeyStorePassword) {
        this.tsaKeyStorePassword = tsaKeyStorePassword;
    }

    public String getTsaUser() {
        return tsaUser;
    }

    public void setTsaUser(String tsaUser) {
        this.tsaUser = tsaUser;
    }

    public String getTsaPassword() {
        return tsaPassword;
    }

    public void setTsaPassword(String tsaPassword) {
        this.tsaPassword = tsaPassword;
    }

    public String getTsaPolicyOid() {
        return tsaPolicyOid;
    }

    public void setTsaPolicyOid(String tsaPolicyOid) {
        this.tsaPolicyOid = tsaPolicyOid;
    }
}
