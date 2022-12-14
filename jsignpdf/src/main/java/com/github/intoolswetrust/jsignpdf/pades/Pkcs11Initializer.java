package com.github.intoolswetrust.jsignpdf.pades;

import static com.github.intoolswetrust.jsignpdf.pades.Constants.LOGGER;
import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Provider;
import java.security.Security;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import com.github.intoolswetrust.jsignpdf.pades.config.Pkcs11Config;

public class Pkcs11Initializer implements Closeable {

    public static volatile Provider SUN_PROVIDER;
    public static volatile Provider JSIGN_PROVIDER;

    private volatile File tempConfigFile;

    public Pkcs11Initializer(Pkcs11Config p11config) throws IOException {
        File file = requireNonNull(p11config).getP11ConfigFile();
        Map<String, String> params = p11config.getParams();
        if (file != null && params != null && !params.isEmpty()) {
            throw new IllegalArgumentException(
                    "Providing both the PKCS11 config file, and the PKCS11 parameters is not allowed. Use either the path to config file, or the parameters.");
        }
        if (file == null && !params.isEmpty()) {
            file = createPkcs11Config(params);
            tempConfigFile = file;
        }
        if (file != null) {
            registerProviders(file);
        }
    }

    private File createPkcs11Config(Map<String, String> params) throws IOException {
        File tmpFile = Files.createTempFile("jsignpdf", "p11.properties").toFile();
        Properties props = new Properties();
        props.putAll(params);
        try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
            props.store(fos, "Generated PKCS11 config file");
        }
        return tmpFile;
    }

    @Override
    public void close() throws IOException {
        unregisterProviders();
        File tmpFile = tempConfigFile;
        tempConfigFile = null;
        if (tmpFile != null) {
            boolean deleted = tmpFile.delete();
            LOGGER.fine("Temporary PKCS11 config file was " + (deleted ? "" : "not ") + "deleted.");
        }
    }

    private static void registerProviders(final File cfgFile) {
        String absolutePath = cfgFile.getAbsolutePath();
        if (cfgFile.isFile()) {
            SUN_PROVIDER = initPkcs11Provider(absolutePath, "sun.security.pkcs11.SunPKCS11");
            JSIGN_PROVIDER = initPkcs11Provider(absolutePath, "com.github.kwart.jsign.pkcs11.JSignPKCS11");
        } else {
            throw new IllegalArgumentException(
                    "The PKCS#11 provider is not registered. Configuration file doesn't exist: " + absolutePath);
        }
    }

    /**
     * Unregisters PKCS11 security provider registered by {@link #registerProviders(String)} method.
     * <p>
     * Some tokens/card-readers hangs during second usage of the program, they have to be unplugged and plugged again following
     * code should prevent this issue.
     * </p>
     *
     * @param providerName
     */
    private static void unregisterProviders() {
        SUN_PROVIDER = unregisterProvider(SUN_PROVIDER);
        JSIGN_PROVIDER = unregisterProvider(JSIGN_PROVIDER);
        // we should wait a little bit to de-register provider correctly (is it a driver
        // issue?)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOGGER.fine("Sleep interrupted. Who cares?");
        }
    }

    private static Provider initPkcs11Provider(String configPath, String className) {
        Provider pkcs11Provider = null;
        try {
            Class<?> sunPkcs11Cls = Class.forName(className);
            try {
                pkcs11Provider = (Provider) sunPkcs11Cls.getConstructor(String.class).newInstance(configPath);
            } catch (NoSuchMethodException e) {
                pkcs11Provider = (Provider) sunPkcs11Cls.getConstructor().newInstance();
                Class<Provider> provCls = Provider.class;
                pkcs11Provider = (Provider) provCls.getMethod("configure", String.class).invoke(pkcs11Provider, configPath);
            }
            Security.addProvider(pkcs11Provider);
            final String name = pkcs11Provider.getName();
            LOGGER.fine("PKCS11 provider registered with name " + name);
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "Unable to register " + className + " security provider.", e);
        }
        return pkcs11Provider;
    }

    private static Provider unregisterProvider(Provider provider) {
        if (provider == null) {
            return null;
        }
        String providerName = provider.getName();
        LOGGER.fine("Removing security provider with name " + providerName);
        try {
            Security.removeProvider(providerName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Removing provider failed", e);
        }
        return null;
    }
}
