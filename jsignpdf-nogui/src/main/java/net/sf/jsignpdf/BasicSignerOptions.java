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
import java.util.Locale;

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
public class BasicSignerOptions {

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
    private PDFEncryption pdfEncryption;
    private char[] pdfOwnerPwd;
    private char[] pdfUserPwd;
    private CertificationLevel certLevel;
    private HashAlgorithm hashAlgorithm;

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
    private String l2Text;
    private float l2TextFontSize = Constants.DEFVAL_L2_FONT_SIZE;
    private String bgImgPath;

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
    private boolean crlEnabled;

    // Proxy connection
    private Proxy.Type proxyType;
    private String proxyHost;
    private int proxyPort;

    private String[] cmdLine;

    public void loadCmdLine() throws Exception {
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

    protected String[] getCmdLine() {
        return cmdLine;
    }

    protected void setCmdLine(String[] cmdLine) {
        this.cmdLine = cmdLine;
    }

}
