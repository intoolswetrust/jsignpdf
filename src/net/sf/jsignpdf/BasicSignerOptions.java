package net.sf.jsignpdf;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;

import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PrintRight;
import net.sf.jsignpdf.types.RenderMode;

import org.bouncycastle.crypto.CryptoException;

/**
 * Options for PDF signer.
 * 
 * @author Josef Cacek
 */
public class BasicSignerOptions {

	protected final static ResourceProvider res = ResourceProvider.getInstance();
	protected final PropertyProvider props = PropertyProvider.getInstance();
	protected final JSignEncryptor encryptor = new JSignEncryptor();

	private PrintWriter printWriter;
	private String ksType;
	private String ksFile;
	private char[] ksPasswd;
	private String keyAlias;
	private int keyIndex = Constants.DEFVAL_KEY_INDEX;
	private char[] keyPasswd;
	private String inFile;
	private String outFile;
	private String reason;
	private String location;
	private String contact;
	private SignResultListener listener;
	private boolean append;
	private boolean advanced;
	private boolean encrypted;
	private char[] pdfOwnerPwd;
	private char[] pdfUserPwd;
	private CertificationLevel certLevel;
	private HashAlgorithm hashAlgorithm;

	protected boolean storePasswords;

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

	// options for timestamps (provided by external TSA)
	private boolean timestamp;
	private String tsaUrl;
	private String tsaUser;
	private String tsaPasswd;

	// options for certificate validation
	private boolean ocspEnabled;
	private boolean crlEnabled;

	// Proxy connection
	private Proxy.Type proxyType;
	private String proxyHost;
	private int proxyPort;

	/**
	 * Loads options from PropertyProvider
	 */
	public void loadOptions() {
		setKsType(props.getProperty(Constants.PROPERTY_KSTYPE));
		setAdvanced(props.getAsBool(Constants.PROPERTY_ADVANCED));
		setKsFile(props.getProperty(Constants.PROPERTY_KEYSTORE));
		setKeyAlias(props.getProperty(Constants.PROPERTY_ALIAS));
		setKeyIndex(props.getAsInt(Constants.PROPERTY_KEY_INDEX, Constants.DEFVAL_KEY_INDEX));
		setInFile(props.getProperty(Constants.PROPERTY_INPDF));
		setOutFile(props.getProperty(Constants.PROPERTY_OUTPDF));
		setReason(props.getProperty(Constants.PROPERTY_REASON));
		setLocation(props.getProperty(Constants.PROPERTY_LOCATION));
		setContact(props.getProperty(Constants.PROPERTY_CONTACT));
		setAppend(props.getAsBool(Constants.PROPERTY_APPEND));
		setEncrypted(props.getAsBool(Constants.PROPERTY_ENCRYPTED_PDF));
		setCertLevel(props.getProperty(Constants.PROPERTY_CERT_LEVEL));
		setHashAlgorithm(props.getProperty(Constants.PROPERTY_HASH_ALGORITHM));

		setRightPrinting(props.getProperty(Constants.PROPERTY_RIGHT_PRINT));
		setRightCopy(props.getAsBool(Constants.PROPERTY_RIGHT_COPY));
		setRightAssembly(props.getAsBool(Constants.PROPERTY_RIGHT_ASSEMBLY));
		setRightFillIn(props.getAsBool(Constants.PROPERTY_RIGHT_FILL_IN));
		setRightScreanReaders(props.getAsBool(Constants.PROPERTY_RIGHT_SCR_READ));
		setRightModifyAnnotations(props.getAsBool(Constants.PROPERTY_RIGHT_MOD_ANNOT));
		setRightModifyContents(props.getAsBool(Constants.PROPERTY_RIGHT_MOD_CONT));

		// visible signature options
		setVisible(props.getAsBool(Constants.PROPERTY_VISIBLE_ENABLED));
		setPage(props.getAsInt(Constants.PROPERTY_VISIBLE_PAGE, Constants.DEFVAL_PAGE));
		setPositionLLX(props.getAsFloat(Constants.PROPERTY_VISIBLE_POS_LLX, Constants.DEFVAL_LLX));
		setPositionLLY(props.getAsFloat(Constants.PROPERTY_VISIBLE_POS_LLY, Constants.DEFVAL_LLY));
		setPositionURX(props.getAsFloat(Constants.PROPERTY_VISIBLE_POS_URX, Constants.DEFVAL_URX));
		setPositionURY(props.getAsFloat(Constants.PROPERTY_VISIBLE_POS_URY, Constants.DEFVAL_URY));
		setBgImgScale(props.getAsFloat(Constants.PROPERTY_VISIBLE_BGSCALE, Constants.DEFVAL_BG_SCALE));
		setRenderMode(props.getProperty(Constants.PROPERTY_VISIBLE_RENDER));
		setL2Text(props.getPropNullSensitive(Constants.PROPERTY_VISIBLE_L2TEXT));
		setL2TextFontSize(props.getAsFloat(Constants.PROPERTY_VISIBLE_L2TEXT_FONT_SIZE, Constants.DEFVAL_L2_FONT_SIZE));
		setL4Text(props.getPropNullSensitive(Constants.PROPERTY_VISIBLE_L4TEXT));
		setImgPath(props.getProperty(Constants.PROPERTY_VISIBLE_IMG));
		setBgImgPath(props.getProperty(Constants.PROPERTY_VISIBLE_BGIMG));

		// TSA
		setTimestamp(props.getAsBool(Constants.PROPERTY_TSA_ENABLED));
		setTsaUrl(props.getProperty(Constants.PROPERTY_TSA_URL));
		setTsaUser(props.getProperty(Constants.PROPERTY_TSA_USER));

		// OCSP & CRL
		setOcspEnabled(props.getAsBool(Constants.PROPERTY_OCSP_ENABLED));
		setCrlEnabled(props.getAsBool(Constants.PROPERTY_CRL_ENABLED));

		// proxy
		setProxyType(props.getProperty(Constants.PROPERTY_PROXY_TYPE));
		setProxyHost(props.getProperty(Constants.PROPERTY_PROXY_HOST));
		setProxyPort(props.getAsInt(Constants.PROPERTY_PROXY_PORT, Constants.DEFVAL_PROXY_PORT));

		// passwords
		storePasswords = props.getAsBool(Constants.PROPERTY_STOREPWD);
		final String tmpHome = getDecrypted(Constants.EPROPERTY_USERHOME);
		boolean tmpPasswords = storePasswords && Constants.USER_HOME != null && Constants.USER_HOME.equals(tmpHome);
		if (tmpPasswords) {
			setKsPasswd(getDecrypted(Constants.EPROPERTY_KS_PWD));
			setKeyPasswd(getDecrypted(Constants.EPROPERTY_KEY_PWD));
			if (isEncrypted()) {
				setPdfOwnerPwd(getDecrypted(Constants.EPROPERTY_OWNER_PWD));
				setPdfUserPwd(getDecrypted(Constants.EPROPERTY_USER_PWD));
			}
			setTsaPasswd(getDecrypted(Constants.EPROPERTY_TSA_PWD));
		}

	}

	/**
	 * Stores options to PropertyProvider
	 */
	public void storeOptions() {
		props.setProperty(Constants.PROPERTY_KSTYPE, getKsType());
		props.setProperty(Constants.PROPERTY_ADVANCED, isAdvanced());
		props.setProperty(Constants.PROPERTY_KEYSTORE, getKsFile());
		props.setProperty(Constants.PROPERTY_ALIAS, getKeyAlias());
		props.setProperty(Constants.PROPERTY_KEY_INDEX, getKeyIndex());
		props.setProperty(Constants.PROPERTY_INPDF, getInFile());
		props.setProperty(Constants.PROPERTY_OUTPDF, getOutFile());
		props.setProperty(Constants.PROPERTY_REASON, getReason());
		props.setProperty(Constants.PROPERTY_LOCATION, getLocation());
		props.setProperty(Constants.PROPERTY_CONTACT, getContact());
		props.setProperty(Constants.PROPERTY_APPEND, isAppend());
		props.setProperty(Constants.PROPERTY_ENCRYPTED_PDF, isEncrypted());
		props.setProperty(Constants.PROPERTY_CERT_LEVEL, getCertLevel().name());
		props.setProperty(Constants.PROPERTY_HASH_ALGORITHM, getHashAlgorithm().name());

		props.setProperty(Constants.PROPERTY_RIGHT_PRINT, getRightPrinting().name());
		props.setProperty(Constants.PROPERTY_RIGHT_COPY, isRightCopy());
		props.setProperty(Constants.PROPERTY_RIGHT_ASSEMBLY, isRightAssembly());
		props.setProperty(Constants.PROPERTY_RIGHT_FILL_IN, isRightFillIn());
		props.setProperty(Constants.PROPERTY_RIGHT_SCR_READ, isRightScreanReaders());
		props.setProperty(Constants.PROPERTY_RIGHT_MOD_ANNOT, isRightModifyAnnotations());
		props.setProperty(Constants.PROPERTY_RIGHT_MOD_CONT, isRightModifyContents());

		// visible signature options
		props.setProperty(Constants.PROPERTY_VISIBLE_ENABLED, isVisible());
		props.setProperty(Constants.PROPERTY_VISIBLE_PAGE, getPage());
		props.setProperty(Constants.PROPERTY_VISIBLE_POS_LLX, getPositionLLX());
		props.setProperty(Constants.PROPERTY_VISIBLE_POS_LLY, getPositionLLY());
		props.setProperty(Constants.PROPERTY_VISIBLE_POS_URX, getPositionURX());
		props.setProperty(Constants.PROPERTY_VISIBLE_POS_URY, getPositionURY());
		props.setProperty(Constants.PROPERTY_VISIBLE_BGSCALE, getBgImgScale());
		props.setProperty(Constants.PROPERTY_VISIBLE_RENDER, getRenderMode().name());
		props.setPropNullSensitive(Constants.PROPERTY_VISIBLE_L2TEXT, getL2Text());
		props.setProperty(Constants.PROPERTY_VISIBLE_L2TEXT_FONT_SIZE, getL2TextFontSize());
		props.setPropNullSensitive(Constants.PROPERTY_VISIBLE_L4TEXT, getL4Text());
		props.setProperty(Constants.PROPERTY_VISIBLE_IMG, getImgPath());
		props.setProperty(Constants.PROPERTY_VISIBLE_BGIMG, getBgImgPath());

		props.setProperty(Constants.PROPERTY_TSA_ENABLED, isTimestamp());
		props.setProperty(Constants.PROPERTY_TSA_URL, getTsaUrl());
		props.setProperty(Constants.PROPERTY_TSA_USER, getTsaUser());
		props.setProperty(Constants.PROPERTY_OCSP_ENABLED, isOcspEnabled());
		props.setProperty(Constants.PROPERTY_CRL_ENABLED, isCrlEnabled());

		props.setProperty(Constants.PROPERTY_PROXY_TYPE, getProxyType().name());
		props.setProperty(Constants.PROPERTY_PROXY_HOST, getProxyHost());
		props.setProperty(Constants.PROPERTY_PROXY_PORT, getProxyPort());

		props.setProperty(Constants.PROPERTY_STOREPWD, isStorePasswords());
		setEncrypted(Constants.EPROPERTY_USERHOME, Constants.USER_HOME);
		if (isStorePasswords()) {
			setEncrypted(Constants.EPROPERTY_KS_PWD, new String(getKsPasswd()));
			setEncrypted(Constants.EPROPERTY_KEY_PWD, new String(getKeyPasswd()));
			if (isEncrypted()) {
				setEncrypted(Constants.EPROPERTY_OWNER_PWD, new String(getPdfOwnerPwd()));
				setEncrypted(Constants.EPROPERTY_USER_PWD, new String(getPdfUserPwd()));
			}
			setEncrypted(Constants.EPROPERTY_TSA_PWD, getTsaPasswd());
		} else {
			props.removeProperty(Constants.EPROPERTY_KS_PWD);
			props.removeProperty(Constants.EPROPERTY_KEY_PWD);
			props.removeProperty(Constants.EPROPERTY_OWNER_PWD);
			props.removeProperty(Constants.EPROPERTY_USER_PWD);
			props.removeProperty(Constants.EPROPERTY_TSA_PWD);
		}

		props.saveDefault();
	}

	/**
	 * Logs localized message to PrintWriter
	 * 
	 * @param aKey
	 *            message key
	 * @param anArgs
	 *            message parameters
	 */
	public void log(final String aKey, final String... anArgs) {
		if (printWriter != null) {
			printWriter.println(res.get(aKey, anArgs));
			printWriter.flush();
		}
	}

	/**
	 * Fires event listener
	 * 
	 * @param aResult
	 * @see #getListener()
	 */
	protected void fireSignerFinishedEvent(Exception aResult) {
		if (listener != null) {
			listener.signerFinishedEvent(aResult);
		}
	}

	/**
	 * Converts array of characters to String. If array is null, empty string is
	 * returned
	 * 
	 * @param aCharArr
	 *            char array
	 * @return not null string
	 */
	private String charArrToStr(final char[] aCharArr) {
		return aCharArr == null ? "" : new String(aCharArr);
	}

	public PrintWriter getPrintWriter() {
		return printWriter;
	}

	public void setPrintWriter(PrintWriter outWriter) {
		this.printWriter = outWriter;
	}

	public String getKsType() {
		return ksType;
	}

	public void setKsType(String ksType) {
		this.ksType = ksType;
	}

	public String getKsFile() {
		return ksFile;
	}

	public void setKsFile(String ksFile) {
		this.ksFile = ksFile;
	}

	public char[] getKsPasswd() {
		return ksPasswd;
	}

	public String getKsPasswdStr() {
		return charArrToStr(ksPasswd);
	}

	public void setKsPasswd(char[] passwd) {
		this.ksPasswd = passwd;
	}

	public void setKsPasswd(String aPasswd) {
		setKsPasswd(aPasswd == null ? null : aPasswd.toCharArray());
	}

	public String getInFile() {
		return inFile;
	}

	public void setInFile(String inFile) {
		this.inFile = inFile;
	}

	public String getOutFile() {
		return outFile;
	}

	/**
	 * Returns output file name if filled or input file name with default output
	 * suffix ("_signed")
	 * 
	 * @return
	 */
	public String getOutFileX() {
		String tmpOut = StringUtils.emptyNull(outFile);
		if (tmpOut == null) {
			String tmpExtension = "";
			String tmpNameBase = StringUtils.emptyNull(getInFile());
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

	public void setOutFile(String outFile) {
		this.outFile = outFile;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public SignResultListener getListener() {
		return listener;
	}

	public void setListener(SignResultListener listener) {
		this.listener = listener;
	}

	public char[] getKeyPasswd() {
		return keyPasswd;
	}

	public char[] getKeyPasswdX() {
		if (keyPasswd != null && keyPasswd.length == 0) {
			keyPasswd = null;
		}
		return advanced ? keyPasswd : ksPasswd;
	}

	public String getKeyPasswdStr() {
		return charArrToStr(keyPasswd);
	}

	public void setKeyPasswd(char[] keyPasswd) {
		this.keyPasswd = keyPasswd;
	}

	public void setKeyPasswd(String aPasswd) {
		setKeyPasswd(aPasswd == null ? null : aPasswd.toCharArray());
	}

	public String getKeyAlias() {
		return keyAlias;
	}

	public String getKeyAliasX() {
		return advanced ? keyAlias : null;
	}

	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
	}

	public int getKeyIndex() {
		return keyIndex;
	}

	public int getKeyIndexX() {
		return advanced ? keyIndex : Constants.DEFVAL_KEY_INDEX;
	}

	public void setKeyIndex(int anIndex) {
		this.keyIndex = anIndex;
		if (keyIndex < 0)
			keyIndex = Constants.DEFVAL_KEY_INDEX;
	}

	public boolean isAppend() {
		return append;
	}

	public boolean isAppendX() {
		return advanced && append && !encrypted;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}

	public boolean isAdvanced() {
		return advanced;
	}

	public void setAdvanced(boolean advanced) {
		this.advanced = advanced;
	}

	public boolean isEncrypted() {
		return encrypted;
	}

	public boolean isEncryptedX() {
		return advanced && encrypted;
	}

	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	public char[] getPdfOwnerPwd() {
		return pdfOwnerPwd;
	}

	public String getPdfOwnerPwdStr() {
		return charArrToStr(pdfOwnerPwd);
	}

	public void setPdfOwnerPwd(char[] pdfOwnerPwd) {
		this.pdfOwnerPwd = pdfOwnerPwd;
	}

	public void setPdfOwnerPwd(String aPasswd) {
		setPdfOwnerPwd(aPasswd == null ? null : aPasswd.toCharArray());
	}

	public char[] getPdfUserPwd() {
		return pdfUserPwd;
	}

	public String getPdfUserPwdStr() {
		return charArrToStr(pdfUserPwd);
	}

	public void setPdfUserPwd(char[] pdfUserPwd) {
		this.pdfUserPwd = pdfUserPwd;
	}

	public void setPdfUserPwd(String aPasswd) {
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

	public void setCertLevel(CertificationLevel aCertLevel) {
		this.certLevel = aCertLevel;
	}

	public void setCertLevel(String aCertLevel) {
		setCertLevel(aCertLevel == null ? null : CertificationLevel.valueOf(aCertLevel.toUpperCase()));
	}

	public boolean isRightCopy() {
		return rightCopy;
	}

	public void setRightCopy(boolean rightCopy) {
		this.rightCopy = rightCopy;
	}

	public boolean isRightAssembly() {
		return rightAssembly;
	}

	public void setRightAssembly(boolean rightAssembly) {
		this.rightAssembly = rightAssembly;
	}

	public boolean isRightFillIn() {
		return rightFillIn;
	}

	public void setRightFillIn(boolean rightFillIn) {
		this.rightFillIn = rightFillIn;
	}

	public boolean isRightScreanReaders() {
		return rightScreanReaders;
	}

	public void setRightScreanReaders(boolean rightScreanReaders) {
		this.rightScreanReaders = rightScreanReaders;
	}

	public boolean isRightModifyAnnotations() {
		return rightModifyAnnotations;
	}

	public void setRightModifyAnnotations(boolean rightModifyAnnotations) {
		this.rightModifyAnnotations = rightModifyAnnotations;
	}

	public boolean isRightModifyContents() {
		return rightModifyContents;
	}

	public void setRightModifyContents(boolean rightModifyContents) {
		this.rightModifyContents = rightModifyContents;
	}

	public PrintRight getRightPrinting() {
		return rightPrinting;
	}

	public void setRightPrinting(PrintRight rightPrinting) {
		if (getRightPrinting() == null) {
			rightPrinting = PrintRight.ALLOW_PRINTING;
		}
		this.rightPrinting = rightPrinting;
	}

	public void setRightPrinting(String aValue) {
		setRightPrinting(aValue == null ? null : PrintRight.valueOf(aValue.toUpperCase()));
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int aPage) {
		if (aPage < 1) {
			aPage = 1;
		}
		this.page = aPage;
	}

	public float getPositionLLX() {
		return positionLLX;
	}

	public void setPositionLLX(float positionLLX) {
		this.positionLLX = positionLLX;
	}

	public float getPositionLLY() {
		return positionLLY;
	}

	public void setPositionLLY(float positionLLY) {
		this.positionLLY = positionLLY;
	}

	public float getPositionURX() {
		return positionURX;
	}

	public void setPositionURX(float positionURX) {
		this.positionURX = positionURX;
	}

	public float getPositionURY() {
		return positionURY;
	}

	public void setPositionURY(float positionURY) {
		this.positionURY = positionURY;
	}

	public float getBgImgScale() {
		return bgImgScale;
	}

	public void setBgImgScale(float bgImgScale) {
		this.bgImgScale = bgImgScale;
	}

	public RenderMode getRenderMode() {
		if (renderMode == null) {
			renderMode = RenderMode.DESCRTIPTION_ONLY;
		}
		return renderMode;
	}

	public void setRenderMode(RenderMode renderMode) {
		this.renderMode = renderMode;
	}

	public void setRenderMode(String aValue) {
		setRenderMode(aValue == null ? null : RenderMode.valueOf(aValue.toUpperCase()));
	}

	public String getL2Text() {
		return l2Text;
	}

	public void setL2Text(String text) {
		l2Text = text;
	}

	public String getL4Text() {
		return l4Text;
	}

	public void setL4Text(String text) {
		l4Text = text;
	}

	public String getImgPath() {
		return (imgPath = StringUtils.emptyNull(imgPath));
	}

	public void setImgPath(String imgPath) {
		this.imgPath = imgPath;
	}

	public String getBgImgPath() {
		return (bgImgPath = StringUtils.emptyNull(bgImgPath));
	}

	public void setBgImgPath(String bgImgPath) {
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
	 * @param textFontSize
	 *            the l2TextFontSize to set
	 */
	public void setL2TextFontSize(float textFontSize) {
		l2TextFontSize = textFontSize;
	}

	/**
	 * Returns decrypted property
	 * 
	 * @param aProperty
	 * @return
	 */
	protected String getDecrypted(String aProperty) {
		try {
			return encryptor.decryptString(props.getProperty(aProperty));
		} catch (CryptoException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Sets encrypted property
	 * 
	 * @param aProperty
	 * @return
	 */
	protected void setEncrypted(final String aProperty, final String aValue) {
		try {
			props.setProperty(aProperty, encryptor.encryptString(aValue));
		} catch (CryptoException e) {
			e.printStackTrace();
			props.removeProperty(aProperty);
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
	 * @param timestamp
	 *            the timestamp to set
	 */
	public void setTimestamp(boolean timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * @return the tsaUrl
	 */
	public String getTsaUrl() {
		return tsaUrl;
	}

	/**
	 * @param tsaUrl
	 *            the tsaUrl to set
	 */
	public void setTsaUrl(String tsaUrl) {
		this.tsaUrl = tsaUrl;
	}

	/**
	 * @return the tsaUser
	 */
	public String getTsaUser() {
		return tsaUser;
	}

	/**
	 * @param tsaUser
	 *            the tsaUser to set
	 */
	public void setTsaUser(String tsaUser) {
		this.tsaUser = tsaUser;
	}

	/**
	 * @return the tsaPasswd
	 */
	public String getTsaPasswd() {
		return tsaPasswd;
	}

	/**
	 * @param tsaPasswd
	 *            the tsaPasswd to set
	 */
	public void setTsaPasswd(String tsaPasswd) {
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
	 * @param ocspEnabled
	 *            the ocspEnabled to set
	 */
	public void setOcspEnabled(boolean ocspEnabled) {
		this.ocspEnabled = ocspEnabled;
	}

	public boolean isStorePasswords() {
		return storePasswords;
	}

	public void setStorePasswords(boolean storePasswords) {
		this.storePasswords = storePasswords;
	}

	/**
	 * @return the contact
	 */
	public String getContact() {
		return contact;
	}

	/**
	 * @param contact
	 *            the contact to set
	 */
	public void setContact(String contact) {
		this.contact = contact;
	}

	public boolean isCrlEnabled() {
		return crlEnabled;
	}

	public boolean isCrlEnabledX() {
		return advanced && crlEnabled;
	}

	public void setCrlEnabled(boolean crlEnabled) {
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

	public void setHashAlgorithm(HashAlgorithm hashAlgorithm) {
		this.hashAlgorithm = hashAlgorithm;
	}

	public void setHashAlgorithm(String aStrValue) {
		setHashAlgorithm(StringUtils.isEmpty(aStrValue) ? null : HashAlgorithm.valueOf(aStrValue));
	}

	public Proxy.Type getProxyType() {
		if (proxyType == null) {
			proxyType = Constants.DEFVAL_PROXY_TYPE;
		}
		return proxyType;
	}

	public void setProxyType(Proxy.Type proxyType) {
		this.proxyType = proxyType;
	}

	public void setProxyType(final String aStrValue) {
		setProxyType(StringUtils.isEmpty(aStrValue) ? null : Proxy.Type.valueOf(aStrValue));
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	/**
	 * Creates and returns Proxy object, which should be used for URL
	 * connections in JSignPdf.
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
}
