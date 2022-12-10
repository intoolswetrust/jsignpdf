package com.github.intoolswetrust.jsignpdf.pades.config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

public class Pkcs11Config {

    @Parameter(names = { "--pkcs11-config",
            "-p11" }, converter = FileConverter.class, description = "Config file for SunPKCS11 and JSignPKCS11 providers")
    private File p11ConfigFile;

    @DynamicParameter(names = "-P11", description = "PKCS11 provider configuration parameter")
    private Map<String, String> params = new HashMap<>();

    public File getP11ConfigFile() {
        return p11ConfigFile;
    }

    public void setP11ConfigFile(File p11ConfigFile) {
        this.p11ConfigFile = p11ConfigFile;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}
