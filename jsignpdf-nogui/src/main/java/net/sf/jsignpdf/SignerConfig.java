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

import static net.sf.jsignpdf.Constants.*;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.beust.jcommander.Parameter;

import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PDFEncryption;
import net.sf.jsignpdf.types.PrintRight;
import net.sf.jsignpdf.types.ServerAuthentication;

import org.apache.commons.lang3.StringUtils;

/**
 * Options for PDF signer.
 *
 * @author Josef Cacek
 */
public class SignerConfig {

    // === Commands ===

    @Parameter(names = { "-" + ARG_HELP, "--" + ARG_HELP_LONG }, help = true, description = "prints this help screen")
    private boolean printHelp;

    @Parameter(names = { "-" + ARG_VERSION, "--" + ARG_VERSION_LONG }, description = "shows the application version")
    private boolean printVersion;

    @Parameter(names = { "-" + ARG_LIST_KS_TYPES,
            "--" + ARG_LIST_KS_TYPES_LONG }, description = "lists available keystore types")
    private boolean listKeyStores;

    @Parameter(names = { "-" + ARG_LIST_KEYS, "--" + ARG_LIST_KEYS_LONG }, description = "lists keys in chosen keystore")
    private boolean listKeys;

    // === Output ===

    @Parameter(names = { "-" + ARG_OPREFIX, "--" + ARG_OPREFIX_LONG }, description = "prefix for signed filename")
    private String outPrefix;

    @Parameter(names = { "-" + ARG_OSUFFIX, "--" + ARG_OSUFFIX_LONG }, description = "suffix for signed filename")
    private String outSuffix = Constants.DEFAULT_OUT_SUFFIX;

    @Parameter(names = { "-" + ARG_OUTPATH,
            "--" + ARG_OUTPATH_LONG }, description = "output directory for signed documents")
    private String outPath;

    // === Positional arguments (PDF files) ===

    @Parameter(description = "PDF files to sign")
    private List<String> filesList = new ArrayList<>();

    // === Keystore and Key ===

    @Parameter(names = { "-" + ARG_KS_TYPE, "--" + ARG_KS_TYPE_LONG }, description = "keystore type")
    private String ksType;

    @Parameter(names = { "-" + ARG_KS_FILE, "--" + ARG_KS_FILE_LONG }, description = "keystore file path")
    private String ksFile;

    @Parameter(names = { "-" + ARG_KS_PWD, "--" + ARG_KS_PWD_LONG }, description = "keystore password")
    private String ksPasswdCli;
    private char[] ksPasswd;

    @Parameter(names = { "-" + ARG_KEY_ALIAS, "--" + ARG_KEY_ALIAS_LONG }, description = "key alias in keystore")
    private String keyAlias;

    @Parameter(names = { "-" + ARG_KEY_INDEX, "--" + ARG_KEY_INDEX_LONG }, description = "key index in keystore")
    private int keyIndex = Constants.DEFVAL_KEY_INDEX;

    @Parameter(names = { "-" + ARG_KEY_PWD, "--" + ARG_KEY_PWD_LONG }, description = "key password")
    private String keyPasswdCli;
    private char[] keyPasswd;

    private String inFile;
    private String outFile;

    // === Signature info ===

    @Parameter(names = { "-" + ARG_SIGNER_NAME, "--" + ARG_SIGNER_NAME_LONG }, description = "signer name")
    private String signerName;

    @Parameter(names = { "-" + ARG_REASON, "--" + ARG_REASON_LONG }, description = "reason of signature")
    private String reason;

    @Parameter(names = { "-" + ARG_LOCATION, "--" + ARG_LOCATION_LONG }, description = "location of signature")
    private String location;

    @Parameter(names = { "-" + ARG_CONTACT, "--" + ARG_CONTACT_LONG }, description = "signer's contact details")
    private String contact;

    @Parameter(names = { "-" + ARG_APPEND,
            "--" + ARG_APPEND_LONG }, description = "add signature to existing ones instead of replacing")
    private boolean append;

    @Parameter(names = { "-" + ARG_CERT_LEVEL, "--" + ARG_CERT_LEVEL_LONG }, description = "certification level")
    private String certLevelCli;
    private CertificationLevel certLevel;

    @Parameter(names = { "-" + ARG_HASH_ALGORITHM,
            "--" + ARG_HASH_ALGORITHM_LONG }, description = "hash algorithm for signature")
    private String hashAlgorithmCli;
    private HashAlgorithm hashAlgorithm;

    @Parameter(names = { "-" + ARG_QUIET, "--" + ARG_QUIET_LONG }, description = "quiet mode - disable logging")
    private boolean quiet;

    // === Encryption ===

    @Parameter(names = { "-" + ARG_ENCRYPTED,
            "--" + ARG_ENCRYPTED_LONG }, description = "encrypt output PDF (deprecated, use --encryption PASSWORD)")
    private boolean encrypted;

    @Parameter(names = { "-" + ARG_ENCRYPTION,
            "--" + ARG_ENCRYPTION_LONG }, description = "encryption mode for output PDF")
    private String pdfEncryptionCli;
    private PDFEncryption pdfEncryption;

    @Parameter(names = { "-" + ARG_PWD_OWNER,
            "--" + ARG_PWD_OWNER_LONG }, description = "owner password for encrypted PDF")
    private String pdfOwnerPwdCli;
    private char[] pdfOwnerPwd;

    @Parameter(names = { "-" + ARG_PWD_USER,
            "--" + ARG_PWD_USER_LONG }, description = "user password for encrypted PDF")
    private String pdfUserPwdCli;
    private char[] pdfUserPwd;

    @Parameter(names = { "-" + ARG_RIGHT_PRINT,
            "--" + ARG_RIGHT_PRINT_LONG }, description = "printing rights for encrypted PDF")
    private String rightPrintCli;
    private PrintRight rightPrinting;

    @Parameter(names = "--" + ARG_DISABLE_COPY_LONG, description = "deny copy in encrypted documents")
    private boolean disableCopy;

    @Parameter(names = "--" + ARG_DISABLE_ASSEMBLY_LONG, description = "deny assembly in encrypted documents")
    private boolean disableAssembly;

    @Parameter(names = "--" + ARG_DISABLE_FILL_LONG, description = "deny fill in encrypted documents")
    private boolean disableFill;

    @Parameter(names = "--" + ARG_DISABLE_SCREEN_READERS_LONG, description = "deny screen readers in encrypted documents")
    private boolean disableScreenReaders;

    @Parameter(names = "--" + ARG_DISABLE_MODIFY_ANNOT_LONG, description = "deny modify annotations in encrypted documents")
    private boolean disableModifyAnnotations;

    @Parameter(names = "--" + ARG_DISABLE_MODIFY_CONTENT_LONG, description = "deny modify content in encrypted documents")
    private boolean disableModifyContent;

    private boolean rightCopy;
    private boolean rightAssembly;
    private boolean rightFillIn;
    private boolean rightScreanReaders;
    private boolean rightModifyAnnotations;
    private boolean rightModifyContents;

    // === Visible Signature ===

    @Parameter(names = { "-" + ARG_VISIBLE, "--" + ARG_VISIBLE_LONG }, description = "enable visible signature")
    private boolean visible;

    @Parameter(names = { "-" + ARG_PAGE, "--" + ARG_PAGE_LONG }, description = "page for visible signature")
    private int page = Constants.DEFVAL_PAGE;

    @Parameter(names = "-" + ARG_POS_LLX, description = "lower left X coordinate of visible signature")
    private float positionLLX = Constants.DEFVAL_LLX;

    @Parameter(names = "-" + ARG_POS_LLY, description = "lower left Y coordinate of visible signature")
    private float positionLLY = Constants.DEFVAL_LLY;

    @Parameter(names = "-" + ARG_POS_URX, description = "upper right X coordinate of visible signature")
    private float positionURX = Constants.DEFVAL_URX;

    @Parameter(names = "-" + ARG_POS_URY, description = "upper right Y coordinate of visible signature")
    private float positionURY = Constants.DEFVAL_URY;

    @Parameter(names = "--" + ARG_L2_TEXT_LONG, description = "visible signature text content")
    private String l2Text;

    @Parameter(names = { "-" + ARG_L2TEXT_FONT_SIZE,
            "--" + ARG_L2TEXT_FONT_SIZE_LONG }, description = "font size for visible signature text")
    private float l2TextFontSize = Constants.DEFVAL_L2_FONT_SIZE;

    @Parameter(names = "--" + ARG_BG_PATH, description = "background image path for visible signature")
    private String bgImgPath;

    @Parameter(names = "--" + ARG_DISABLE_ACRO6LAYERS, description = "disable Acrobat 6 layer mode")
    private boolean disableAcro6Layers;

    // === TSA ===

    @Parameter(names = { "-" + ARG_TSA_URL, "--" + ARG_TSA_URL_LONG }, description = "TSA server URL")
    private String tsaUrl;

    @Parameter(names = { "-" + ARG_TSA_AUTHN,
            "--" + ARG_TSA_AUTHN_LONG }, description = "TSA authentication method")
    private String tsaAuthnCli;
    private ServerAuthentication tsaServerAuthn;

    @Parameter(names = { "-" + ARG_TSA_USER, "--" + ARG_TSA_USER_LONG }, description = "TSA username")
    private String tsaUser;

    @Parameter(names = { "-" + ARG_TSA_PWD, "--" + ARG_TSA_PWD_LONG }, description = "TSA password")
    private String tsaPasswd;

    @Parameter(names = { "-" + ARG_TSA_CERT_FILE_TYPE,
            "--" + ARG_TSA_CERT_FILE_TYPE_LONG }, description = "TSA certificate file type")
    private String tsaCertFileType;

    @Parameter(names = { "-" + ARG_TSA_CERT_FILE,
            "--" + ARG_TSA_CERT_FILE_LONG }, description = "TSA certificate file path")
    private String tsaCertFile;

    @Parameter(names = { "-" + ARG_TSA_CERT_PWD,
            "--" + ARG_TSA_CERT_PWD_LONG }, description = "TSA certificate password")
    private String tsaCertFilePwd;

    @Parameter(names = "--" + ARG_TSA_POLICY_LONG, description = "TSA policy OID")
    private String tsaPolicy;

    @Parameter(names = { "-" + ARG_TSA_HASH_ALG,
            "--" + ARG_TSA_HASH_ALG_LONG }, description = "TSA hash algorithm")
    private String tsaHashAlg;

    private boolean timestamp;

    // === Certificate Validation ===

    @Parameter(names = "--" + ARG_CRL_LONG, description = "enable CRL certificate validation")
    private boolean crlEnabled;

    // === Proxy ===

    @Parameter(names = "--" + ARG_PROXY_TYPE_LONG, description = "proxy type for internet connections")
    private String proxyTypeCli;
    private Proxy.Type proxyType;

    @Parameter(names = "--" + ARG_PROXY_HOST_LONG, description = "proxy hostname")
    private String proxyHost;

    @Parameter(names = "--" + ARG_PROXY_PORT_LONG, description = "proxy port")
    private int proxyPort;

    /**
     * Post-processes JCommander-parsed fields. Call this after {@code JCommander.parse()}.
     */
    public void postParseCmdLine() {
        if (quiet) {
            LOGGER.setLevel(Level.OFF);
            Logger.getGlobal().setLevel(Level.OFF);
        }

        // Password conversions (CLI String -> char[])
        if (ksPasswdCli != null)
            setKsPasswd(ksPasswdCli);
        if (keyPasswdCli != null)
            setKeyPasswd(keyPasswdCli);
        if (pdfOwnerPwdCli != null)
            setPdfOwnerPwd(pdfOwnerPwdCli);
        if (pdfUserPwdCli != null)
            setPdfUserPwd(pdfUserPwdCli);

        // Enum conversions (CLI String -> enum)
        if (certLevelCli != null)
            setCertLevel(certLevelCli);
        if (hashAlgorithmCli != null)
            setHashAlgorithm(hashAlgorithmCli);
        if (encrypted)
            setPdfEncryption(PDFEncryption.PASSWORD);
        if (pdfEncryptionCli != null)
            setPdfEncryption(pdfEncryptionCli);
        if (rightPrintCli != null)
            setRightPrinting(rightPrintCli);
        if (tsaAuthnCli != null)
            setTsaServerAuthn(tsaAuthnCli);
        if (proxyTypeCli != null)
            setProxyType(proxyTypeCli);

        // Rights (disable flags invert to right flags)
        setRightCopy(!disableCopy);
        setRightAssembly(!disableAssembly);
        setRightFillIn(!disableFill);
        setRightScreanReaders(!disableScreenReaders);
        setRightModifyAnnotations(!disableModifyAnnotations);
        setRightModifyContents(!disableModifyContent);

        // TSA
        if (tsaUrl != null) {
            setTimestamp(true);
        }

        // Set inFile from the first positional argument
        if (filesList != null && !filesList.isEmpty()) {
            setInFile(filesList.get(0));
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
    public String getEffectiveOutFile() {
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

    public char[] getKeyPasswd() {
        return keyPasswd;
    }

    public char[] getEffectiveKeyPasswd() {
        if (keyPasswd != null && keyPasswd.length == 0) {
            keyPasswd = null;
        }
        return keyPasswd != null ? keyPasswd : ksPasswd;
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

    public void setKeyAlias(final String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public int getKeyIndex() {
        return keyIndex;
    }

    public void setKeyIndex(final int anIndex) {
        this.keyIndex = anIndex;
        if (keyIndex < 0)
            keyIndex = Constants.DEFVAL_KEY_INDEX;
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

    public void setPdfOwnerPwd(final char[] pdfOwnerPwd) {
        this.pdfOwnerPwd = pdfOwnerPwd;
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

    public String getL2Text() {
        return l2Text;
    }

    public void setL2Text(final String text) {
        l2Text = text;
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
     * @return the timestamp
     */
    public boolean isTimestamp() {
        return timestamp;
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

    public void setCrlEnabled(final boolean crlEnabled) {
        this.crlEnabled = crlEnabled;
    }

    public HashAlgorithm getHashAlgorithm() {
        if (hashAlgorithm == null) {
            hashAlgorithm = Constants.DEFVAL_HASH_ALGORITHM;
        }
        return hashAlgorithm;
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
        if (getProxyType() != Proxy.Type.DIRECT) {
            tmpResult = new Proxy(getProxyType(), new InetSocketAddress(getProxyHost(), getProxyPort()));
        }
        return tmpResult;
    }

    /**
     * @return the outPrefix
     */
    public String getOutPrefix() {
        if (outPrefix == null)
            outPrefix = "";
        return outPrefix;
    }

    /**
     * @param outPrefix the outPrefix to set
     */
    public void setOutPrefix(String outPrefix) {
        this.outPrefix = outPrefix;
    }

    /**
     * @return the outSuffix
     */
    public String getOutSuffix() {
        if (outSuffix == null)
            outSuffix = "";
        return outSuffix;
    }

    /**
     * @param outSuffix the outSuffix to set
     */
    public void setOutSuffix(String outSuffix) {
        this.outSuffix = outSuffix;
    }

    /**
     * @return the files
     */
    public String[] getFiles() {
        if (filesList == null || filesList.isEmpty()) {
            return null;
        }
        return filesList.toArray(new String[0]);
    }

    /**
     * @param files the files to set
     */
    public void setFiles(String[] files) {
        this.filesList = new ArrayList<>();
        if (files != null) {
            for (String f : files) {
                this.filesList.add(f);
            }
        }
    }

    /**
     * @return the printHelp
     */
    public boolean isPrintHelp() {
        return printHelp;
    }

    /**
     * @param printHelp the printHelp to set
     */
    public void setPrintHelp(boolean printHelp) {
        this.printHelp = printHelp;
    }

    /**
     * @return the printVersion
     */
    public boolean isPrintVersion() {
        return printVersion;
    }

    /**
     * @param printVersion the printVersion to set
     */
    public void setPrintVersion(boolean printVersion) {
        this.printVersion = printVersion;
    }

    /**
     * @return the listKeyStores
     */
    public boolean isListKeyStores() {
        return listKeyStores;
    }

    /**
     * @param listKeyStores the listKeyStores to set
     */
    public void setListKeyStores(boolean listKeyStores) {
        this.listKeyStores = listKeyStores;
    }

    /**
     * @return the listKeys
     */
    public boolean isListKeys() {
        return listKeys;
    }

    /**
     * @param listKeys the listKeys to set
     */
    public void setListKeys(boolean listKeys) {
        this.listKeys = listKeys;
    }

    /**
     * Returns output path including tailing slash character
     *
     * @return the outPath
     */
    public String getOutPath() {
        String tmpResult;
        if (StringUtils.isEmpty(outPath)) {
            tmpResult = "./";
        } else {
            tmpResult = outPath.replaceAll("\\\\", "/");
            if (!tmpResult.endsWith("/")) {
                tmpResult = tmpResult + "/";
            }
        }
        return tmpResult;
    }

    /**
     * @param outPath the outPath to set
     */
    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }

}
