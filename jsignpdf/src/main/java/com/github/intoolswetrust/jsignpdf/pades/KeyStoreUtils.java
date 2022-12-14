package com.github.intoolswetrust.jsignpdf.pades;

import static com.github.intoolswetrust.jsignpdf.pades.Constants.LOGGER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.github.intoolswetrust.jsignpdf.pades.config.BasicConfig;

import eu.europa.esig.dss.token.KeyStoreSignatureTokenConnection;

/**
 * This class provides methods for KeyStore(s) handling.
 */
public class KeyStoreUtils {

    /**
     * Returns known KeyStore types
     */
    public static SortedSet<String> getKeyStores() {
        TreeSet<String> result = new TreeSet<String>(Security.getAlgorithms("KeyStore"));
        return result;
    }

    public static String[] getKeyAliases(BasicConfig config) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore tmpKs = loadKeyStore(config.getKeyStoreType(), config.getKeyStoreFile(), config.getKeyStorePassword());
        List<String> tmpResult = getAliasesList(tmpKs, config);

        return tmpResult.toArray(new String[tmpResult.size()]);
    }

    /**
     * Returns list of key aliases in given keystore.
     *
     * @param aKs
     * @param options
     * @return
     * @throws KeyStoreException 
     */
    private static List<String> getAliasesList(KeyStore aKs, BasicConfig options) throws KeyStoreException {
        final List<String> tmpResult = new ArrayList<String>();
        Enumeration<String> tmpAliases = aKs.aliases();
        while (tmpAliases.hasMoreElements()) {
            String tmpAlias = tmpAliases.nextElement();
            if (aKs.isKeyEntry(tmpAlias)) {
                final Certificate tmpCert = aKs.getCertificate(tmpAlias);
                boolean tmpAddAlias = true;
                if (tmpCert instanceof X509Certificate) {
                    final X509Certificate tmpX509 = (X509Certificate) tmpCert;
                    if (!options.isDisableValidityCheck()) {
                        try {
                            tmpX509.checkValidity();
                        } catch (CertificateExpiredException e) {
                            LOGGER.fine("Expired certificate: " + tmpAlias);
                            tmpAddAlias = false;
                        } catch (CertificateNotYetValidException e) {
                            LOGGER.fine("Not yet valid certificate: " + tmpAlias);
                            tmpAddAlias = false;
                        }
                    }
                    if (!options.isDisableKeyUsageCheck()) {
                        // check if the certificate is supposed to be
                        // used for digital signatures
                        final boolean keyUsage[] = tmpX509.getKeyUsage();
                        if (keyUsage != null && keyUsage.length > 0) {
                            // KeyUsage ::= BIT STRING {
                            // digitalSignature (0),
                            // nonRepudiation (1),
                            // keyEncipherment (2),
                            // dataEncipherment (3),
                            // keyAgreement (4),
                            // keyCertSign (5),
                            // cRLSign (6),
                            // encipherOnly (7),
                            // decipherOnly (8) }
                            if (!(keyUsage[0] || keyUsage[1])) {
                                LOGGER.fine("Certificate is not intended for signing: " + tmpAlias);
                                tmpAddAlias = false;
                            }
                        }
                    }
                    // check critical extensions
                    if (!options.isDisableCriticalExtensionsCheck()) {
                        final Set<String> criticalExtensionOIDs = tmpX509.getCriticalExtensionOIDs();
                        if (criticalExtensionOIDs != null) {
                            for (String oid : criticalExtensionOIDs) {
                                if (!Constants.SUPPORTED_CRITICAL_EXTENSION_OIDS.contains(oid)) {
                                    LOGGER.fine("Certificate '" + tmpAlias + "' contains a critical extension (" + oid
                                            + ") which is not supported");
                                    tmpAddAlias = false;
                                }
                            }
                        }
                    }
                }
                if (tmpAddAlias) {
                    tmpResult.add(tmpAlias);
                }
            }
        }
        return tmpResult;
    }

    /**
     * Loads certificate names (aliases) from the given keystore
     *
     * @return array of certificate aliases
     */
    public static String[] getCertAliases(KeyStore tmpKs) {
        if (tmpKs == null)
            return null;
        final List<String> tmpResult = new ArrayList<String>();
        try {
            final Enumeration<String> tmpAliases = tmpKs.aliases();
            while (tmpAliases.hasMoreElements()) {
                final String tmpAlias = tmpAliases.nextElement();
                if (tmpKs.isCertificateEntry(tmpAlias)) {
                    tmpResult.add(tmpAlias);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return tmpResult.toArray(new String[tmpResult.size()]);
    }

    public static KeyStore loadKeyStore(String aKsType, File aKsFile, String aKsPasswd) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        char[] tmpPass = null;
        if (aKsPasswd != null) {
            tmpPass = aKsPasswd.toCharArray();
        }
        return loadKeyStore(aKsType, aKsFile, tmpPass);
    }

    public static KeyStore loadKeyStore(String aKsType, final File aKsFile, final char[] aKsPasswd) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        aKsType = getOrDefaultKeyStoreType(aKsType);

        KeyStore tmpKs = null;
        tmpKs = KeyStore.getInstance(aKsType);
        
        if (aKsFile != null) {
            try (InputStream tmpIS = new FileInputStream(aKsFile)) {
                tmpKs.load(tmpIS, aKsPasswd);
            }
        } else {
            tmpKs.load(null, aKsPasswd);
        }
        return tmpKs;
    }

    public static KeyStoreSignatureTokenConnection createKeyStoreSignatureTokenConnection(BasicConfig config) throws IOException {
        PasswordProtection ksPassword = new PasswordProtection(config.getKeyStorePasswordAsChars());
        File keyStoreFile = config.getKeyStoreFile();
        KeyStoreSignatureTokenConnection result = null;
        if (keyStoreFile != null) {
            try (FileInputStream fis = new FileInputStream(keyStoreFile)) {
                result =  new KeyStoreSignatureTokenConnection(fis, getOrDefaultKeyStoreType(config.getKeyStoreType()),
                        ksPassword);
            }
        } else {
            result =  new KeyStoreSignatureTokenConnection((InputStream) null, getOrDefaultKeyStoreType(config.getKeyStoreType()),
                    ksPassword);
        }
        return result;
    }

    public static String getOrDefaultKeyStoreType(String ksType) {
        if (StringUtils.isEmpty(ksType)) {
            ksType = KeyStore.getDefaultType();
        }
        return ksType;
    }

}
