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
package net.sf.jsignpdf;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PDFEncryption;
import net.sf.jsignpdf.types.PrintRight;
import net.sf.jsignpdf.types.RenderMode;
import net.sf.jsignpdf.types.ServerAuthentication;
import net.sf.jsignpdf.utils.PropertyProvider;
import net.sf.jsignpdf.utils.PropertyStoreFactory;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.CryptoException;

/**
 * Options for PDF signer.
 */
public class BasicSignerOptions {

    protected final PropertyProvider props = PropertyStoreFactory.getInstance().mainConfig();
    protected final JSignEncryptor encryptor = new JSignEncryptor();

    private String propertiesFilePath;

    private String ksType;
    private String ksFile;
    private char[] ksPasswd;
    private String keyAlias;
    private int keyIndex = Constants.DEFVAL_KEY_INDEX;
    private char[] keyPasswd;
    private String inFile;
    private String outFile;
    private String signerName;
    private String reason;
    private String location;
    private String contact;
    private SignResultListener listener;
    private boolean append = Constants.DEFVAL_APPEND;
    private boolean advanced;
    private PDFEncryption pdfEncryption;
    private char[] pdfOwnerPwd;
    private char[] pdfUserPwd;
    private String pdfEncryptionCertFile;
    private CertificationLevel certLevel;
    private HashAlgorithm hashAlgorithm;

    protected boolean storePasswords = Constants.DEFVAL_STOREPWD;

    // options from rights dialog
    private PrintRight rightPrinting;
    private boolean rightCopy;
    private boolean rightAssembly;
    private boolean rightFillIn;
    private boolean rightScreanReaders;
    private boolean rightModifyAnnotations;
    private boolean rightModifyContents;

    // options from visible signature settings dialog
    private boolean visible;
    private int page = Constants.DEFVAL_PAGE;
    private float positionLLX = Constants.DEFVAL_LLX;
    private float positionLLY = Constants.DEFVAL_LLY;
    private float positionURX = Constants.DEFVAL_URX;
    private float positionURY = Constants.DEFVAL_URY;
    private float bgImgScale = Constants.DEFVAL_BG_SCALE;
    private RenderMode renderMode;
    private String l2Text;
    private String l4Text;
    private float l2TextFontSize = Constants.DEFVAL_L2_FONT_SIZE;
    private String imgPath;
    private String bgImgPath;
    private boolean acro6Layers = Constants.DEFVAL_ACRO6LAYERS;

    // options for timestamps (provided by external TSA)
    private boolean timestamp;
    private String tsaUrl;
    private ServerAuthentication tsaServerAuthn;
    private String tsaUser;
    private String tsaPasswd;
    private String tsaCertFileType;
    private String tsaCertFile;
    private String tsaCertFilePwd;
    private String tsaPolicy;
    private String tsaHashAlg;

    // options for certificate validation
    private boolean ocspEnabled;
    private String ocspServerUrl;
    private boolean crlEnabled;

    // Proxy connection
    private Proxy.Type proxyType;
    private String proxyHost;
    private int proxyPort;

    private String[] cmdLine;

    /**
     * Loads options from PropertyProvider
     */
    public void loadOptions() {
        if (propertiesFilePath != null) {
            props.loadProperties(propertiesFilePath);
        }
        loadFromStore(props, true);
    }

    /**
     * Loads the signing-configuration subset of options from a preset store. Session-only state (input/output paths) is
     * intentionally skipped — the live values stay in place. Passwords are loaded only when the preset was saved with
     * {@code storePasswords=true} on a machine with a matching {@code user.home}; otherwise the in-memory password fields
     * are cleared so stale credentials from a previously loaded preset cannot bleed into this one.
     *
     * @param store the preset property store to read from
     */
    public void loadFromPreset(PropertyProvider store) {
        loadFromStore(store, false);
    }

    /**
     * Common per-field load. When {@code includeAllConfig} is {@code true} the full main configuration is loaded (including
     * session state); when {@code false} the method loads only the preset subset — session state (input/output paths) is
     * skipped and password fields are cleared on the preset path unless the preset legitimately carries them for this user.
     */
    private void loadFromStore(PropertyProvider store, boolean includeAllConfig) {
        setKsType(store.getProperty(Constants.PROPERTY_KSTYPE));
        setAdvanced(store.getAsBool(Constants.PROPERTY_ADVANCED));
        setKsFile(store.getProperty(Constants.PROPERTY_KEYSTORE));
        setKeyAlias(store.getProperty(Constants.PROPERTY_ALIAS));
        setKeyIndex(store.getAsInt(Constants.PROPERTY_KEY_INDEX, Constants.DEFVAL_KEY_INDEX));
        if (includeAllConfig) {
            setInFile(store.getProperty(Constants.PROPERTY_INPDF));
            setOutFile(store.getProperty(Constants.PROPERTY_OUTPDF));
        }
        setReason(store.getProperty(Constants.PROPERTY_REASON));
        setLocation(store.getProperty(Constants.PROPERTY_LOCATION));
        setContact(store.getProperty(Constants.PROPERTY_CONTACT));
        setAppend(store.getAsBool(Constants.PROPERTY_APPEND, Constants.DEFVAL_APPEND));
        // backward compatibility
        setPdfEncryption(store.getProperty(Constants.PROPERTY_PDF_ENCRYPTION));
        if (pdfEncryption == null && store.getAsBool(Constants.PROPERTY_ENCRYPTED_PDF)) {
            setPdfEncryption(PDFEncryption.PASSWORD);
        }
        setPdfEncryptionCertFile(store.getProperty(Constants.PROPERTY_PDF_ENCRYPTION_CERT_FILE));
        setCertLevel(store.getProperty(Constants.PROPERTY_CERT_LEVEL));
        setHashAlgorithm(store.getProperty(Constants.PROPERTY_HASH_ALGORITHM));

        setRightPrinting(store.getProperty(Constants.PROPERTY_RIGHT_PRINT));
        setRightCopy(store.getAsBool(Constants.PROPERTY_RIGHT_COPY));
        setRightAssembly(store.getAsBool(Constants.PROPERTY_RIGHT_ASSEMBLY));
        setRightFillIn(store.getAsBool(Constants.PROPERTY_RIGHT_FILL_IN));
        setRightScreanReaders(store.getAsBool(Constants.PROPERTY_RIGHT_SCR_READ));
        setRightModifyAnnotations(store.getAsBool(Constants.PROPERTY_RIGHT_MOD_ANNOT));
        setRightModifyContents(store.getAsBool(Constants.PROPERTY_RIGHT_MOD_CONT));

        // visible signature options
        setVisible(store.getAsBool(Constants.PROPERTY_VISIBLE_ENABLED));
        setPage(store.getAsInt(Constants.PROPERTY_VISIBLE_PAGE, Constants.DEFVAL_PAGE));
        setPositionLLX(store.getAsFloat(Constants.PROPERTY_VISIBLE_POS_LLX, Constants.DEFVAL_LLX));
        setPositionLLY(store.getAsFloat(Constants.PROPERTY_VISIBLE_POS_LLY, Constants.DEFVAL_LLY));
        setPositionURX(store.getAsFloat(Constants.PROPERTY_VISIBLE_POS_URX, Constants.DEFVAL_URX));
        setPositionURY(store.getAsFloat(Constants.PROPERTY_VISIBLE_POS_URY, Constants.DEFVAL_URY));
        setBgImgScale(store.getAsFloat(Constants.PROPERTY_VISIBLE_BGSCALE, Constants.DEFVAL_BG_SCALE));
        setRenderMode(store.getProperty(Constants.PROPERTY_VISIBLE_RENDER));
        setL2Text(store.getPropNullSensitive(Constants.PROPERTY_VISIBLE_L2TEXT));
        setL2TextFontSize(store.getAsFloat(Constants.PROPERTY_VISIBLE_L2TEXT_FONT_SIZE, Constants.DEFVAL_L2_FONT_SIZE));
        setL4Text(store.getPropNullSensitive(Constants.PROPERTY_VISIBLE_L4TEXT));
        setImgPath(store.getProperty(Constants.PROPERTY_VISIBLE_IMG));
        setBgImgPath(store.getProperty(Constants.PROPERTY_VISIBLE_BGIMG));
        setAcro6Layers(!store.exists(Constants.PROPERTY_VISIBLE_ACRO6LAYERS)
                || store.getAsBool(Constants.PROPERTY_VISIBLE_ACRO6LAYERS));

        // TSA
        setTimestamp(store.getAsBool(Constants.PROPERTY_TSA_ENABLED));
        setTsaUrl(store.getProperty(Constants.PROPERTY_TSA_URL));
        setTsaUser(store.getProperty(Constants.PROPERTY_TSA_USER));
        // backward compatibility
        setTsaServerAuthn(store.getProperty(Constants.PROPERTY_TSA_SERVER_AUTHN));
        if (tsaServerAuthn == null && StringUtils.isNotEmpty(tsaUser)) {
            setTsaServerAuthn(ServerAuthentication.PASSWORD);
        }
        setTsaCertFileType(store.getProperty(Constants.PROPERTY_TSA_CERT_FILE_TYPE));
        setTsaCertFile(store.getProperty(Constants.PROPERTY_TSA_CERT_FILE));
        setTsaPolicy(store.getProperty(Constants.PROPERTY_TSA_POLICY));
        setTsaHashAlg(store.getProperty(Constants.PROPERTY_TSA_HASH_ALG));

        // OCSP & CRL
        setOcspEnabled(store.getAsBool(Constants.PROPERTY_OCSP_ENABLED));
        setOcspServerUrl(store.getProperty(Constants.PROPERTY_OCSP_SERVER_URL));
        setCrlEnabled(store.getAsBool(Constants.PROPERTY_CRL_ENABLED));

        // proxy
        setProxyType(store.getProperty(Constants.PROPERTY_PROXY_TYPE));
        setProxyHost(store.getProperty(Constants.PROPERTY_PROXY_HOST));
        setProxyPort(store.getAsInt(Constants.PROPERTY_PROXY_PORT, Constants.DEFVAL_PROXY_PORT));

        // passwords — gated by storePasswords and a matching user.home
        storePasswords = store.getAsBool(Constants.PROPERTY_STOREPWD, Constants.DEFVAL_STOREPWD);
        final String tmpHome = getDecrypted(store, Constants.EPROPERTY_USERHOME);
        final boolean tmpPasswords = storePasswords && Constants.USER_HOME != null && Constants.USER_HOME.equals(tmpHome);
        if (tmpPasswords) {
            setKsPasswd(getDecrypted(store, Constants.EPROPERTY_KS_PWD));
            setKeyPasswd(getDecrypted(store, Constants.EPROPERTY_KEY_PWD));
            setPdfOwnerPwd(getDecrypted(store, Constants.EPROPERTY_OWNER_PWD));
            setPdfUserPwd(getDecrypted(store, Constants.EPROPERTY_USER_PWD));
            setTsaPasswd(getDecrypted(store, Constants.EPROPERTY_TSA_PWD));
            setTsaCertFilePwd(getDecrypted(store, Constants.EPROPERTY_TSA_CERT_PWD));
        } else if (!includeAllConfig) {
            // Preset load: clear any stale passwords carried over from a previously
            // loaded preset. Otherwise a user who switches to a password-less or
            // foreign-host preset would silently sign with the earlier preset's
            // credentials. The main-config path keeps the in-memory values so an
            // interactive password entry on a fresh session is still possible.
            setKsPasswd((String) null);
            setKeyPasswd((String) null);
            setPdfOwnerPwd((String) null);
            setPdfUserPwd((String) null);
            setTsaPasswd(null);
            setTsaCertFilePwd(null);
        }
    }

    /**
     * Stores options to PropertyProvider
     */
    public void storeOptions() {
        storeToStore(props, true);
        if (propertiesFilePath != null) {
            props.saveProperties(propertiesFilePath);
        } else {
            props.save();
        }
    }

    /**
     * Writes the signing-configuration subset of options into a preset store. Session-only state (input/output paths) is
     * skipped. Passwords are written encrypted when {@code storePasswords} is {@code true}; otherwise the password keys are
     * removed from the store. The caller is responsible for persisting the store via {@link PropertyProvider#save()}.
     *
     * @param store the preset property store to write to
     */
    public void storeToPreset(PropertyProvider store) {
        storeToStore(store, false);
    }

    /**
     * Common per-field store. When {@code includeAllConfig} is {@code true} the full main configuration is written (including
     * session state); when {@code false} the method writes only the preset subset (no session state). Password fields are
     * written encrypted in either case when {@code storePasswords} is set, and otherwise cleared from the target store.
     */
    private void storeToStore(PropertyProvider store, boolean includeAllConfig) {
        store.setProperty(Constants.PROPERTY_KSTYPE, getKsType());
        store.setProperty(Constants.PROPERTY_ADVANCED, isAdvanced());
        store.setProperty(Constants.PROPERTY_KEYSTORE, getKsFile());
        store.setProperty(Constants.PROPERTY_ALIAS, getKeyAlias());
        store.setProperty(Constants.PROPERTY_KEY_INDEX, getKeyIndex());
        if (includeAllConfig) {
            store.setProperty(Constants.PROPERTY_INPDF, getInFile());
            store.setProperty(Constants.PROPERTY_OUTPDF, getOutFile());
        }
        store.setProperty(Constants.PROPERTY_REASON, getReason());
        store.setProperty(Constants.PROPERTY_LOCATION, getLocation());
        store.setProperty(Constants.PROPERTY_CONTACT, getContact());
        store.setProperty(Constants.PROPERTY_APPEND, isAppend());
        store.setProperty(Constants.PROPERTY_PDF_ENCRYPTION, getPdfEncryption().name());
        store.setProperty(Constants.PROPERTY_PDF_ENCRYPTION_CERT_FILE, getPdfEncryptionCertFile());
        store.setProperty(Constants.PROPERTY_CERT_LEVEL, getCertLevel().name());
        store.setProperty(Constants.PROPERTY_HASH_ALGORITHM, getHashAlgorithm().name());

        store.setProperty(Constants.PROPERTY_RIGHT_PRINT, getRightPrinting().name());
        store.setProperty(Constants.PROPERTY_RIGHT_COPY, isRightCopy());
        store.setProperty(Constants.PROPERTY_RIGHT_ASSEMBLY, isRightAssembly());
        store.setProperty(Constants.PROPERTY_RIGHT_FILL_IN, isRightFillIn());
        store.setProperty(Constants.PROPERTY_RIGHT_SCR_READ, isRightScreanReaders());
        store.setProperty(Constants.PROPERTY_RIGHT_MOD_ANNOT, isRightModifyAnnotations());
        store.setProperty(Constants.PROPERTY_RIGHT_MOD_CONT, isRightModifyContents());

        // visible signature options
        store.setProperty(Constants.PROPERTY_VISIBLE_ENABLED, isVisible());
        store.setProperty(Constants.PROPERTY_VISIBLE_PAGE, getPage());
        store.setProperty(Constants.PROPERTY_VISIBLE_POS_LLX, getPositionLLX());
        store.setProperty(Constants.PROPERTY_VISIBLE_POS_LLY, getPositionLLY());
        store.setProperty(Constants.PROPERTY_VISIBLE_POS_URX, getPositionURX());
        store.setProperty(Constants.PROPERTY_VISIBLE_POS_URY, getPositionURY());
        store.setProperty(Constants.PROPERTY_VISIBLE_BGSCALE, getBgImgScale());
        store.setProperty(Constants.PROPERTY_VISIBLE_RENDER, getRenderMode().name());
        store.setPropNullSensitive(Constants.PROPERTY_VISIBLE_L2TEXT, getL2Text());
        store.setProperty(Constants.PROPERTY_VISIBLE_L2TEXT_FONT_SIZE, getL2TextFontSize());
        store.setPropNullSensitive(Constants.PROPERTY_VISIBLE_L4TEXT, getL4Text());
        store.setProperty(Constants.PROPERTY_VISIBLE_IMG, getImgPath());
        store.setProperty(Constants.PROPERTY_VISIBLE_BGIMG, getBgImgPath());
        store.setProperty(Constants.PROPERTY_VISIBLE_ACRO6LAYERS, isAcro6Layers());

        store.setProperty(Constants.PROPERTY_TSA_ENABLED, isTimestamp());
        store.setProperty(Constants.PROPERTY_TSA_URL, getTsaUrl());
        store.setProperty(Constants.PROPERTY_TSA_USER, getTsaUser());
        store.setProperty(Constants.PROPERTY_TSA_CERT_FILE_TYPE, getTsaCertFileType());
        store.setProperty(Constants.PROPERTY_TSA_CERT_FILE, getTsaCertFile());
        store.setProperty(Constants.PROPERTY_TSA_SERVER_AUTHN, getTsaServerAuthn().name());
        store.setProperty(Constants.PROPERTY_TSA_POLICY, getTsaPolicy());
        store.setProperty(Constants.PROPERTY_TSA_HASH_ALG, getTsaHashAlg());
        store.setProperty(Constants.PROPERTY_OCSP_ENABLED, isOcspEnabled());
        store.setProperty(Constants.PROPERTY_OCSP_SERVER_URL, getOcspServerUrl());
        store.setProperty(Constants.PROPERTY_CRL_ENABLED, isCrlEnabled());

        store.setProperty(Constants.PROPERTY_PROXY_TYPE, getProxyType().name());
        store.setProperty(Constants.PROPERTY_PROXY_HOST, getProxyHost());
        store.setProperty(Constants.PROPERTY_PROXY_PORT, getProxyPort());

        store.setProperty(Constants.PROPERTY_STOREPWD, isStorePasswords());
        setEncrypted(store, Constants.EPROPERTY_USERHOME, Constants.USER_HOME);
        if (isStorePasswords()) {
            setEncrypted(store, Constants.EPROPERTY_KS_PWD, getKsPasswdStr());
            setEncrypted(store, Constants.EPROPERTY_KEY_PWD, getKeyPasswdStr());
            setEncrypted(store, Constants.EPROPERTY_OWNER_PWD, getPdfOwnerPwdStr());
            setEncrypted(store, Constants.EPROPERTY_USER_PWD, getPdfUserPwdStr());
            setEncrypted(store, Constants.EPROPERTY_TSA_PWD, getTsaPasswd());
            setEncrypted(store, Constants.EPROPERTY_TSA_CERT_PWD, getTsaCertFilePwd());
        } else {
            store.removeProperty(Constants.EPROPERTY_KS_PWD);
            store.removeProperty(Constants.EPROPERTY_KEY_PWD);
            store.removeProperty(Constants.EPROPERTY_OWNER_PWD);
            store.removeProperty(Constants.EPROPERTY_USER_PWD);
            store.removeProperty(Constants.EPROPERTY_TSA_PWD);
            store.removeProperty(Constants.EPROPERTY_TSA_CERT_PWD);
        }
    }

    public void loadCmdLine() throws Exception {
    }

    /**
     * Fires event listener
     *
     * @param aResult
     * @see #getListener()
     */
    protected void fireSignerFinishedEvent(final Throwable aResult) {
        if (listener != null) {
            listener.signerFinishedEvent(aResult);
        }
    }

    /**
     * Converts array of characters to String. If array is null, empty string is returned
     *
     * @param aCharArr char array
     * @return not null string
     */
    private String charArrToStr(final char[] aCharArr) {
        return aCharArr == null ? "" : new String(aCharArr);
    }

    /**
     * @return the propertiesFilePath
     */
    public String getPropertiesFilePath() {
        return propertiesFilePath;
    }

    /**
     * @param propertiesFilePath the propertiesFilePath to set
     */
    public void setPropertiesFilePath(final String propertiesFilePath) {
        this.propertiesFilePath = propertiesFilePath;
    }

    public String getKsType() {
        return ksType;
    }

    public void setKsType(final String ksType) {
        this.ksType = ksType;
    }

    public String getKsFile() {
        return ksFile;
    }

    public void setKsFile(final String ksFile) {
        this.ksFile = ksFile;
    }

    public char[] getKsPasswd() {
        return ksPasswd;
    }

    public String getKsPasswdStr() {
        return charArrToStr(ksPasswd);
    }

    public void setKsPasswd(final char[] passwd) {
        this.ksPasswd = passwd;
    }

    public void setKsPasswd(final String aPasswd) {
        setKsPasswd(aPasswd == null ? null : aPasswd.toCharArray());
    }

    public String getInFile() {
        return inFile;
    }

    public void setInFile(final String inFile) {
        this.inFile = inFile;
    }

    public String getOutFile() {
        return outFile;
    }

    /**
     * Returns output file name if filled or input file name with default output suffix ("_signed")
     *
     * @return
     */
    public String getOutFileX() {
        String tmpOut = StringUtils.defaultIfBlank(outFile, null);
        if (tmpOut == null) {
            String tmpExtension = "";
            String tmpNameBase = StringUtils.defaultIfBlank(getInFile(), null);
            if (tmpNameBase == null) {
                tmpOut = "signed.pdf";
            } else {
                if (tmpNameBase.toLowerCase().endsWith(".pdf")) {
                    final int tmpBaseLen = tmpNameBase.length() - 4;
                    tmpExtension = tmpNameBase.substring(tmpBaseLen);
                    tmpNameBase = tmpNameBase.substring(0, tmpBaseLen);
                }
                tmpOut = tmpNameBase + Constants.DEFAULT_OUT_SUFFIX + tmpExtension;
            }
        }
        return tmpOut;
    }

    public void setOutFile(final String outFile) {
        this.outFile = outFile;
    }

    public String getSignerName() {
        return signerName;
    }

    public void setSignerName(final String signerName) {
        this.signerName = signerName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public SignResultListener getListener() {
        return listener;
    }

    public void setListener(final SignResultListener listener) {
        this.listener = listener;
    }

    public char[] getKeyPasswd() {
        return keyPasswd;
    }

    public char[] getKeyPasswdX() {
        if (keyPasswd != null && keyPasswd.length == 0) {
            keyPasswd = null;
        }
        return (advanced && keyPasswd != null) ? keyPasswd : ksPasswd;
    }

    public String getKeyPasswdStr() {
        return charArrToStr(keyPasswd);
    }

    public void setKeyPasswd(final char[] keyPasswd) {
        this.keyPasswd = keyPasswd;
    }

    public void setKeyPasswd(final String aPasswd) {
        setKeyPasswd(aPasswd == null ? null : aPasswd.toCharArray());
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public String getKeyAliasX() {
        return advanced ? keyAlias : null;
    }

    public void setKeyAlias(final String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public int getKeyIndex() {
        return keyIndex;
    }

    public int getKeyIndexX() {
        return advanced ? keyIndex : Constants.DEFVAL_KEY_INDEX;
    }

    public void setKeyIndex(final int anIndex) {
        this.keyIndex = anIndex;
        if (keyIndex < 0)
            keyIndex = Constants.DEFVAL_KEY_INDEX;
    }

    public boolean isAppend() {
        return append;
    }

    public boolean isAppendX() {
        return (getPdfEncryption() == PDFEncryption.NONE)
                && ((!Constants.DEFVAL_APPEND && advanced && append) || (Constants.DEFVAL_APPEND && (append || !advanced)));
    }

    public void setAppend(final boolean append) {
        this.append = append;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public void setAdvanced(final boolean advanced) {
        this.advanced = advanced;
    }

    /**
     * @return the pdfEncryption
     */
    public PDFEncryption getPdfEncryption() {
        if (pdfEncryption == null) {
            pdfEncryption = PDFEncryption.NONE;
        }
        return pdfEncryption;
    }

    /**
     * @param pdfEncryption the pdfEncryption to set
     */
    public void setPdfEncryption(final PDFEncryption pdfEncryption) {
        this.pdfEncryption = pdfEncryption;
    }

    public void setPdfEncryption(final String aValue) {
        PDFEncryption enumInstance = null;
        if (aValue != null) {
            try {
                enumInstance = PDFEncryption.valueOf(aValue.toUpperCase(Locale.ENGLISH));
            } catch (final Exception e) {
                // probably illegal value - fallback to a default (i.e. null)
            }
        }
        setPdfEncryption(enumInstance);
    }

    public char[] getPdfOwnerPwd() {
        return pdfOwnerPwd;
    }

    public String getPdfOwnerPwdStr() {
        return charArrToStr(pdfOwnerPwd);
    }

    public String getPdfOwnerPwdStrX() {
        return charArrToStr(advanced ? pdfOwnerPwd : null);
    }

    public void setPdfOwnerPwd(final char[] pdfOwnerPwd) {
        this.pdfOwnerPwd = pdfOwnerPwd;
    }

    /**
     * @return the pdfEncryptionCertFile
     */
    public String getPdfEncryptionCertFile() {
        return pdfEncryptionCertFile;
    }

    /**
     * @param pdfEncryptionCertFile the pdfEncryptionCertFile to set
     */
    public void setPdfEncryptionCertFile(final String pdfEncryptionCertFile) {
        this.pdfEncryptionCertFile = pdfEncryptionCertFile;
    }

    public void setPdfOwnerPwd(final String aPasswd) {
        setPdfOwnerPwd(aPasswd == null ? null : aPasswd.toCharArray());
    }

    public char[] getPdfUserPwd() {
        return pdfUserPwd;
    }

    public String getPdfUserPwdStr() {
        return charArrToStr(pdfUserPwd);
    }

    public void setPdfUserPwd(final char[] pdfUserPwd) {
        this.pdfUserPwd = pdfUserPwd;
    }

    public void setPdfUserPwd(final String aPasswd) {
        setPdfUserPwd(aPasswd == null ? null : aPasswd.toCharArray());
    }

    public CertificationLevel getCertLevel() {
        if (certLevel == null) {
            certLevel = CertificationLevel.NOT_CERTIFIED;
        }
        return certLevel;
    }

    public CertificationLevel getCertLevelX() {
        return advanced ? getCertLevel() : CertificationLevel.NOT_CERTIFIED;
    }

    public void setCertLevel(final CertificationLevel aCertLevel) {
        this.certLevel = aCertLevel;
    }

    public void setCertLevel(final String aValue) {
        CertificationLevel certLevel = null;
        if (aValue != null) {
            try {
                certLevel = CertificationLevel.valueOf(aValue.toUpperCase(Locale.ENGLISH));
            } catch (final Exception e) {
                // probably illegal value - fallback to a default (i.e. null)
            }
        }
        setCertLevel(certLevel);
    }

    public boolean isRightCopy() {
        return rightCopy;
    }

    public void setRightCopy(final boolean rightCopy) {
        this.rightCopy = rightCopy;
    }

    public boolean isRightAssembly() {
        return rightAssembly;
    }

    public void setRightAssembly(final boolean rightAssembly) {
        this.rightAssembly = rightAssembly;
    }

    public boolean isRightFillIn() {
        return rightFillIn;
    }

    public void setRightFillIn(final boolean rightFillIn) {
        this.rightFillIn = rightFillIn;
    }

    public boolean isRightScreanReaders() {
        return rightScreanReaders;
    }

    public void setRightScreanReaders(final boolean rightScreanReaders) {
        this.rightScreanReaders = rightScreanReaders;
    }

    public boolean isRightModifyAnnotations() {
        return rightModifyAnnotations;
    }

    public void setRightModifyAnnotations(final boolean rightModifyAnnotations) {
        this.rightModifyAnnotations = rightModifyAnnotations;
    }

    public boolean isRightModifyContents() {
        return rightModifyContents;
    }

    public void setRightModifyContents(final boolean rightModifyContents) {
        this.rightModifyContents = rightModifyContents;
    }

    public PrintRight getRightPrinting() {
        if (rightPrinting == null) {
            rightPrinting = PrintRight.ALLOW_PRINTING;
        }
        return rightPrinting;
    }

    public void setRightPrinting(PrintRight rightPrinting) {
        this.rightPrinting = rightPrinting;
    }

    public void setRightPrinting(final String aValue) {
        PrintRight printRight = null;
        if (aValue != null) {
            try {
                printRight = PrintRight.valueOf(aValue.toUpperCase(Locale.ENGLISH));
            } catch (final Exception e) {
                // probably illegal value - fallback to a default (i.e. null)
            }
        }
        setRightPrinting(printRight);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int aPage) {
        this.page = aPage;
    }

    public float getPositionLLX() {
        return positionLLX;
    }

    public void setPositionLLX(final float positionLLX) {
        this.positionLLX = positionLLX;
    }

    public float getPositionLLY() {
        return positionLLY;
    }

    public void setPositionLLY(final float positionLLY) {
        this.positionLLY = positionLLY;
    }

    public float getPositionURX() {
        return positionURX;
    }

    public void setPositionURX(final float positionURX) {
        this.positionURX = positionURX;
    }

    public float getPositionURY() {
        return positionURY;
    }

    public void setPositionURY(final float positionURY) {
        this.positionURY = positionURY;
    }

    public float getBgImgScale() {
        return bgImgScale;
    }

    public void setBgImgScale(final float bgImgScale) {
        this.bgImgScale = bgImgScale;
    }

    public RenderMode getRenderMode() {
        if (renderMode == null) {
            renderMode = RenderMode.DESCRIPTION_ONLY;
        }
        return renderMode;
    }

    public void setRenderMode(final RenderMode renderMode) {
        this.renderMode = renderMode;
    }

    public void setRenderMode(final String aValue) {
        RenderMode renderMode = null;
        if (aValue != null) {
            try {
                renderMode = RenderMode.valueOf(aValue.toUpperCase(Locale.ENGLISH));
            } catch (final Exception e) {
                // probably illegal value - fallback to default (i.e. null)
            }
        }
        setRenderMode(renderMode);
    }

    public String getL2Text() {
        return l2Text;
    }

    public void setL2Text(final String text) {
        l2Text = text;
    }

    public String getL4Text() {
        return l4Text;
    }

    public void setL4Text(final String text) {
        l4Text = text;
    }

    public String getImgPath() {
        return (imgPath = StringUtils.defaultIfBlank(imgPath, null));
    }

    public void setImgPath(final String imgPath) {
        this.imgPath = imgPath;
    }

    public String getBgImgPath() {
        return (bgImgPath = StringUtils.defaultIfBlank(bgImgPath, null));
    }

    public void setBgImgPath(final String bgImgPath) {
        this.bgImgPath = bgImgPath;
    }

    /**
     * @return the l2TextFontSize
     */
    public float getL2TextFontSize() {
        if (l2TextFontSize <= 0f) {
            l2TextFontSize = Constants.DEFVAL_L2_FONT_SIZE;
        }
        return l2TextFontSize;
    }

    /**
     * @param textFontSize the l2TextFontSize to set
     */
    public void setL2TextFontSize(final float textFontSize) {
        l2TextFontSize = textFontSize;
    }

    /**
     * @return the acro6Layers
     */
    public boolean isAcro6Layers() {
        return acro6Layers;
    }

    /**
     * @param acro6Layers the acro6Layers to set
     */
    public void setAcro6Layers(final boolean acro6Layers) {
        this.acro6Layers = acro6Layers;
    }

    /**
     * Returns decrypted property from the backing main-config store.
     */
    protected String getDecrypted(final String aProperty) {
        return getDecrypted(props, aProperty);
    }

    /**
     * Returns decrypted property from the given store.
     */
    protected String getDecrypted(final PropertyProvider store, final String aProperty) {
        try {
            return encryptor.decryptString(store.getProperty(aProperty));
        } catch (final CryptoException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sets encrypted property on the backing main-config store.
     */
    protected void setEncrypted(final String aProperty, final String aValue) {
        setEncrypted(props, aProperty, aValue);
    }

    /**
     * Sets encrypted property on the given store.
     */
    protected void setEncrypted(final PropertyProvider store, final String aProperty, final String aValue) {
        try {
            store.setProperty(aProperty, encryptor.encryptString(aValue));
        } catch (final CryptoException e) {
            e.printStackTrace();
            store.removeProperty(aProperty);
        }
    }

    /**
     * @return the timestamp
     */
    public boolean isTimestamp() {
        return timestamp;
    }

    public boolean isTimestampX() {
        return advanced && timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(final boolean timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the tsaUrl
     */
    public String getTsaUrl() {
        return tsaUrl;
    }

    /**
     * @param tsaUrl the tsaUrl to set
     */
    public void setTsaUrl(final String tsaUrl) {
        this.tsaUrl = tsaUrl;
    }

    /**
     * @return the tsaUser
     */
    public String getTsaUser() {
        return tsaUser;
    }

    /**
     * @param tsaUser the tsaUser to set
     */
    public void setTsaUser(final String tsaUser) {
        this.tsaUser = tsaUser;
    }

    /**
     * @return the tsaServerAuthn
     */
    public ServerAuthentication getTsaServerAuthn() {
        if (tsaServerAuthn == null) {
            tsaServerAuthn = ServerAuthentication.NONE;
        }
        return tsaServerAuthn;
    }

    /**
     * @param tsaServerAuthn the tsaServerAuthn to set
     */
    public void setTsaServerAuthn(final ServerAuthentication tsaServerAuthn) {
        this.tsaServerAuthn = tsaServerAuthn;
    }

    /**
     * @param aValue
     */
    public void setTsaServerAuthn(final String aValue) {
        ServerAuthentication enumInstance = null;
        if (aValue != null) {
            try {
                enumInstance = ServerAuthentication.valueOf(aValue.toUpperCase(Locale.ENGLISH));
            } catch (final Exception e) {
                // probably illegal value - fallback to a default (i.e. null)
            }
        }
        setTsaServerAuthn(enumInstance);
    }

    /**
     * @return the tsaCertFileType
     */
    public String getTsaCertFileType() {
        return tsaCertFileType;
    }

    /**
     * @param tsaCertFileType the tsaCertFileType to set
     */
    public void setTsaCertFileType(String tsaCertFileType) {
        this.tsaCertFileType = tsaCertFileType;
    }

    /**
     * @return the tsaCertFile
     */
    public String getTsaCertFile() {
        return tsaCertFile;
    }

    /**
     * @param tsaCertFile the tsaCertFile to set
     */
    public void setTsaCertFile(final String tsaCertFile) {
        this.tsaCertFile = tsaCertFile;
    }

    /**
     * @return the tsaCertFilePwd
     */
    public String getTsaCertFilePwd() {
        return tsaCertFilePwd;
    }

    /**
     * @param tsaCertFilePwd the tsaCertFilePwd to set
     */
    public void setTsaCertFilePwd(final String tsaCertFilePwd) {
        this.tsaCertFilePwd = tsaCertFilePwd;
    }

    /**
     * @return the tsaPolicy
     */
    public String getTsaPolicy() {
        return tsaPolicy;
    }

    /**
     * @param tsaPolicy the tsaPolicy to set
     */
    public void setTsaPolicy(final String tsaPolicy) {
        this.tsaPolicy = tsaPolicy;
    }

    /**
     * @return the tsaHashAlg
     */
    public String getTsaHashAlg() {
        return tsaHashAlg;
    }

    /**
     * @return
     */
    public String getTsaHashAlgWithFallback() {
        return StringUtils.defaultIfBlank(tsaHashAlg, Constants.DEFVAL_TSA_HASH_ALG);
    }

    /**
     * @param tsaHashAlg the tsaHashAlg to set
     */
    public void setTsaHashAlg(String tsaHashAlg) {
        this.tsaHashAlg = tsaHashAlg;
    }

    /**
     * @return the tsaPasswd
     */
    public String getTsaPasswd() {
        return tsaPasswd;
    }

    /**
     * @param tsaPasswd the tsaPasswd to set
     */
    public void setTsaPasswd(final String tsaPasswd) {
        this.tsaPasswd = tsaPasswd;
    }

    /**
     * @return the ocspEnabled
     */
    public boolean isOcspEnabled() {
        return ocspEnabled;
    }

    public boolean isOcspEnabledX() {
        return advanced && ocspEnabled;
    }

    /**
     * @param ocspEnabled the ocspEnabled to set
     */
    public void setOcspEnabled(final boolean ocspEnabled) {
        this.ocspEnabled = ocspEnabled;
    }

    /**
     * @return the ocspServerUrl
     */
    public String getOcspServerUrl() {
        return ocspServerUrl;
    }

    /**
     * @param ocspServerUrl the ocspServerUrl to set
     */
    public void setOcspServerUrl(final String ocspServerUrl) {
        this.ocspServerUrl = ocspServerUrl;
    }

    public boolean isStorePasswords() {
        return storePasswords;
    }

    public void setStorePasswords(final boolean storePasswords) {
        this.storePasswords = storePasswords;
    }

    /**
     * @return the contact
     */
    public String getContact() {
        return contact;
    }

    /**
     * @param contact the contact to set
     */
    public void setContact(final String contact) {
        this.contact = contact;
    }

    public boolean isCrlEnabled() {
        return crlEnabled;
    }

    public boolean isCrlEnabledX() {
        return advanced && crlEnabled;
    }

    public void setCrlEnabled(final boolean crlEnabled) {
        this.crlEnabled = crlEnabled;
    }

    public HashAlgorithm getHashAlgorithm() {
        if (hashAlgorithm == null) {
            hashAlgorithm = Constants.DEFVAL_HASH_ALGORITHM;
        }
        return hashAlgorithm;
    }

    public HashAlgorithm getHashAlgorithmX() {
        if (!advanced) {
            return Constants.DEFVAL_HASH_ALGORITHM;
        }
        return getHashAlgorithm();
    }

    public void setHashAlgorithm(final HashAlgorithm hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public void setHashAlgorithm(final String aValue) {
        HashAlgorithm hashAlg = null;
        if (StringUtils.isNotEmpty(aValue)) {
            try {
                hashAlg = HashAlgorithm.valueOf(aValue.toUpperCase(Locale.ENGLISH));
            } catch (final Exception e) {
                // probably illegal value - fallback to a default (i.e. null)
            }
        }
        setHashAlgorithm(hashAlg);
    }

    public Proxy.Type getProxyType() {
        if (proxyType == null) {
            proxyType = Constants.DEFVAL_PROXY_TYPE;
        }
        return proxyType;
    }

    public void setProxyType(final Proxy.Type proxyType) {
        this.proxyType = proxyType;
    }

    public void setProxyType(final String aValue) {
        Proxy.Type proxy = null;
        if (StringUtils.isNotEmpty(aValue)) {
            try {
                proxy = Proxy.Type.valueOf(aValue.toUpperCase(Locale.ENGLISH));
            } catch (final Exception e) {
                // probably illegal value - fallback to a default (i.e. null)
            }
        }
        setProxyType(proxy);
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(final String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(final int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * Creates and returns Proxy object, which should be used for URL connections in JSignPdf.
     *
     * @return initialized Proxy object.
     */
    public Proxy createProxy() {
        Proxy tmpResult = Proxy.NO_PROXY;
        if (isAdvanced() && getProxyType() != Proxy.Type.DIRECT) {
            tmpResult = new Proxy(getProxyType(), new InetSocketAddress(getProxyHost(), getProxyPort()));
        }
        return tmpResult;
    }

    protected String[] getCmdLine() {
        return cmdLine;
    }

    protected void setCmdLine(String[] cmdLine) {
        this.cmdLine = cmdLine;
    }

    /**
     * Creates a shallow copy of this options instance. Enum and String fields are
     * effectively immutable, so shallow copy is sufficient for thread-safety.
     * char[] fields are defensively copied.
     *
     * @return a new BasicSignerOptions with the same field values
     */
    public BasicSignerOptions createCopy() {
        BasicSignerOptions copy = new BasicSignerOptions();
        copy.setKsType(getKsType());
        copy.setKsFile(getKsFile());
        copy.setKsPasswd(getKsPasswd() != null ? getKsPasswd().clone() : null);
        copy.setKeyAlias(getKeyAlias());
        copy.setKeyIndex(getKeyIndex());
        copy.setKeyPasswd(getKeyPasswd() != null ? getKeyPasswd().clone() : null);
        copy.setInFile(getInFile());
        copy.setOutFile(getOutFile());
        copy.setSignerName(getSignerName());
        copy.setReason(getReason());
        copy.setLocation(getLocation());
        copy.setContact(getContact());
        copy.setListener(getListener());
        copy.setAppend(isAppend());
        copy.setAdvanced(isAdvanced());
        copy.setPdfEncryption(getPdfEncryption());
        copy.setPdfOwnerPwd(getPdfOwnerPwd() != null ? getPdfOwnerPwd().clone() : null);
        copy.setPdfUserPwd(getPdfUserPwd() != null ? getPdfUserPwd().clone() : null);
        copy.setPdfEncryptionCertFile(getPdfEncryptionCertFile());
        copy.setCertLevel(getCertLevel());
        copy.setHashAlgorithm(getHashAlgorithm());
        copy.setStorePasswords(isStorePasswords());
        copy.setRightPrinting(getRightPrinting());
        copy.setRightCopy(isRightCopy());
        copy.setRightAssembly(isRightAssembly());
        copy.setRightFillIn(isRightFillIn());
        copy.setRightScreanReaders(isRightScreanReaders());
        copy.setRightModifyAnnotations(isRightModifyAnnotations());
        copy.setRightModifyContents(isRightModifyContents());
        copy.setVisible(isVisible());
        copy.setPage(getPage());
        copy.setPositionLLX(getPositionLLX());
        copy.setPositionLLY(getPositionLLY());
        copy.setPositionURX(getPositionURX());
        copy.setPositionURY(getPositionURY());
        copy.setBgImgScale(getBgImgScale());
        copy.setRenderMode(getRenderMode());
        copy.setL2Text(getL2Text());
        copy.setL4Text(getL4Text());
        copy.setL2TextFontSize(getL2TextFontSize());
        copy.setImgPath(getImgPath());
        copy.setBgImgPath(getBgImgPath());
        copy.setAcro6Layers(isAcro6Layers());
        copy.setTimestamp(isTimestamp());
        copy.setTsaUrl(getTsaUrl());
        copy.setTsaServerAuthn(getTsaServerAuthn());
        copy.setTsaUser(getTsaUser());
        copy.setTsaPasswd(getTsaPasswd());
        copy.setTsaCertFileType(getTsaCertFileType());
        copy.setTsaCertFile(getTsaCertFile());
        copy.setTsaCertFilePwd(getTsaCertFilePwd());
        copy.setTsaPolicy(getTsaPolicy());
        copy.setTsaHashAlg(getTsaHashAlg());
        copy.setOcspEnabled(isOcspEnabled());
        copy.setOcspServerUrl(getOcspServerUrl());
        copy.setCrlEnabled(isCrlEnabled());
        copy.setProxyType(getProxyType());
        copy.setProxyHost(getProxyHost());
        copy.setProxyPort(getProxyPort());
        return copy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(cmdLine);
        result = prime * result + Arrays.hashCode(keyPasswd);
        result = prime * result + Arrays.hashCode(ksPasswd);
        result = prime * result + Arrays.hashCode(pdfOwnerPwd);
        result = prime * result + Arrays.hashCode(pdfUserPwd);
        result = prime * result + Objects.hash(acro6Layers, advanced, append, bgImgPath, bgImgScale, certLevel, contact,
                crlEnabled, encryptor, hashAlgorithm, imgPath, inFile, keyAlias, keyIndex, ksFile, ksType, l2Text,
                l2TextFontSize, l4Text, listener, location, ocspEnabled, ocspServerUrl, outFile, page, pdfEncryption,
                pdfEncryptionCertFile, positionLLX, positionLLY, positionURX, positionURY, propertiesFilePath, props, proxyHost,
                proxyPort, proxyType, reason, renderMode, rightAssembly, rightCopy, rightFillIn, rightModifyAnnotations,
                rightModifyContents, rightPrinting, rightScreanReaders, signerName, storePasswords, timestamp, tsaCertFile,
                tsaCertFilePwd, tsaCertFileType, tsaHashAlg, tsaPasswd, tsaPolicy, tsaServerAuthn, tsaUrl, tsaUser, visible);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BasicSignerOptions other = (BasicSignerOptions) obj;
        return acro6Layers == other.acro6Layers && advanced == other.advanced && append == other.append
                && Objects.equals(bgImgPath, other.bgImgPath)
                && Float.floatToIntBits(bgImgScale) == Float.floatToIntBits(other.bgImgScale) && certLevel == other.certLevel
                && Arrays.equals(cmdLine, other.cmdLine) && Objects.equals(contact, other.contact)
                && crlEnabled == other.crlEnabled && Objects.equals(encryptor, other.encryptor)
                && hashAlgorithm == other.hashAlgorithm && Objects.equals(imgPath, other.imgPath)
                && Objects.equals(inFile, other.inFile) && Objects.equals(keyAlias, other.keyAlias)
                && keyIndex == other.keyIndex && Arrays.equals(keyPasswd, other.keyPasswd)
                && Objects.equals(ksFile, other.ksFile) && Arrays.equals(ksPasswd, other.ksPasswd)
                && Objects.equals(ksType, other.ksType) && Objects.equals(l2Text, other.l2Text)
                && Float.floatToIntBits(l2TextFontSize) == Float.floatToIntBits(other.l2TextFontSize)
                && Objects.equals(l4Text, other.l4Text) && Objects.equals(listener, other.listener)
                && Objects.equals(location, other.location) && ocspEnabled == other.ocspEnabled
                && Objects.equals(ocspServerUrl, other.ocspServerUrl) && Objects.equals(outFile, other.outFile)
                && page == other.page && pdfEncryption == other.pdfEncryption
                && Objects.equals(pdfEncryptionCertFile, other.pdfEncryptionCertFile)
                && Arrays.equals(pdfOwnerPwd, other.pdfOwnerPwd) && Arrays.equals(pdfUserPwd, other.pdfUserPwd)
                && Float.floatToIntBits(positionLLX) == Float.floatToIntBits(other.positionLLX)
                && Float.floatToIntBits(positionLLY) == Float.floatToIntBits(other.positionLLY)
                && Float.floatToIntBits(positionURX) == Float.floatToIntBits(other.positionURX)
                && Float.floatToIntBits(positionURY) == Float.floatToIntBits(other.positionURY)
                && Objects.equals(propertiesFilePath, other.propertiesFilePath) && Objects.equals(props, other.props)
                && Objects.equals(proxyHost, other.proxyHost) && proxyPort == other.proxyPort && proxyType == other.proxyType
                && Objects.equals(reason, other.reason) && renderMode == other.renderMode
                && rightAssembly == other.rightAssembly && rightCopy == other.rightCopy && rightFillIn == other.rightFillIn
                && rightModifyAnnotations == other.rightModifyAnnotations && rightModifyContents == other.rightModifyContents
                && rightPrinting == other.rightPrinting && rightScreanReaders == other.rightScreanReaders
                && Objects.equals(signerName, other.signerName) && storePasswords == other.storePasswords
                && timestamp == other.timestamp && Objects.equals(tsaCertFile, other.tsaCertFile)
                && Objects.equals(tsaCertFilePwd, other.tsaCertFilePwd)
                && Objects.equals(tsaCertFileType, other.tsaCertFileType) && Objects.equals(tsaHashAlg, other.tsaHashAlg)
                && Objects.equals(tsaPasswd, other.tsaPasswd) && Objects.equals(tsaPolicy, other.tsaPolicy)
                && tsaServerAuthn == other.tsaServerAuthn && Objects.equals(tsaUrl, other.tsaUrl)
                && Objects.equals(tsaUser, other.tsaUser) && visible == other.visible;
    }
}
