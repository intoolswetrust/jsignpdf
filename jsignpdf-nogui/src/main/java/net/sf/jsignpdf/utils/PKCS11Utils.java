/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is 'JSignPdf, a free application for PDF signing'.
 *
 * The Initial Developer of the Original Code is Josef Cacek.
 * Portions created by Josef Cacek are Copyright (C) Josef Cacek. All Rights Reserved.
 *
 * Contributor(s): Josef Cacek.
 *
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU Lesser General Public License, version 2.1 (the  "LGPL License"), in which case the
 * provisions of LGPL License are applicable instead of those
 * above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the LGPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the LGPL License.
 */
package net.sf.jsignpdf.utils;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.File;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;

/**
 * Methods for handling PKCS#11 security providers.
 *
 * @author Josef Cacek
 */
public class PKCS11Utils {

    public static volatile Provider SUN_PROVIDER;
    public static volatile Provider JSIGN_PROVIDER;

    /**
     * Tries to register the sun.security.pkcs11.SunPKCS11 provider with configuration provided in the given file.
     *
     * @param configPath path to PKCS#11 provider configuration file
     * @return newly registered PKCS#11 provider name if provider successfully registered; <code>null</code> otherwise
     */
    public static void registerProviders(final String configPath) {
        if (StringUtils.isEmpty(configPath)) {
            return;
        }
        LOGGER.fine("Registering SunPKCS11 provider from configuration in " + configPath);
        final File cfgFile = IOUtils.findFile(configPath);
        final String absolutePath = cfgFile.getAbsolutePath();
        if (cfgFile.isFile()) {
            SUN_PROVIDER = initPkcs11Provider(absolutePath, "sun.security.pkcs11.SunPKCS11");
            JSIGN_PROVIDER = initPkcs11Provider(absolutePath, "com.github.kwart.jsign.pkcs11.JSignPKCS11");
        } else {
            System.err.println("The PKCS#11 provider is not registered. Configuration file doesn't exist: " + absolutePath);
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
    public static void unregisterProviders() {
        SUN_PROVIDER = unregisterProvider(SUN_PROVIDER);
        JSIGN_PROVIDER = unregisterProvider(JSIGN_PROVIDER);
        // we should wait a little bit to de-register provider correctly (is it a driver
        // issue?)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getProviderNameForKeystoreType(String type) {
        if (type == null) {
            return null;
        }
        String name = getProviderNameImpl(type, SUN_PROVIDER);
        if (name == null) {
            name = getProviderNameImpl(type, JSIGN_PROVIDER);
        }
        return name;
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
            LOGGER.log(Level.SEVERE, "Unable to register SunPKCS11 security provider.", e);
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

    private static String getProviderNameImpl(String type, Provider provider) {
        if (provider == null || type == null) {
            return null;
        }
        String providerName = provider.getName();
        try {
            KeyStore.getInstance(type, provider);
            LOGGER.fine("KeyStore type " + type + " is supported by the provider " + providerName);
            return provider.getName();
        } catch (Exception e) {
            LOGGER.fine("KeyStore type " + type + " is not supported by the provider " + providerName);
        }
        return null;
    }

}
