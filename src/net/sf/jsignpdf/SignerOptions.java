package net.sf.jsignpdf;

import java.io.PrintWriter;

import org.bouncycastle.crypto.CryptoException;

/**
 * Options for PDF signer.
 * @author Josef Cacek
 */
public class SignerOptions {

	protected final PropertyProvider props = PropertyProvider.getInstance();
	protected final ResourceProvider res = ResourceProvider.getInstance();
	protected final JSignEncryptor encryptor = new JSignEncryptor();

	private volatile PrintWriter printWriter;
	private volatile String ksType;
	private volatile String ksFile;
	private volatile char[] ksPasswd;
	private volatile String keyAlias;
	private volatile char[] keyPasswd;
	private volatile String inFile;
	private volatile String outFile;
	private volatile String reason;
	private volatile String location;
	private volatile SignResultListener listener;
	private volatile boolean append;
	private volatile boolean advanced;
	private volatile boolean encrypted;
	private volatile boolean storePasswords;
	private volatile char[] pdfOwnerPwd;
	private volatile char[] pdfUserPwd;
	private volatile CertificationLevel certLevel;

	/**
	 * Logs localized message to PrintWriter
	 * @param aKey message key
	 */
	void log(final String aKey) {
		log(aKey, (String[]) null);
	}

	/**
	 * Logs localized message to PrintWriter
	 * @param aKey message key
	 * @param anArg message parameter
	 */
	void log(final String aKey, final String anArg) {
		log(aKey, anArg==null? null: new String[] {anArg});
	}

	/**
	 * Logs localized message to PrintWriter
	 * @param aKey message key
	 * @param anArgs message parameters
	 */
	void log(final String aKey, final String[] anArgs) {
		if (printWriter!=null) {
			printWriter.println(res.get(aKey, anArgs));
		}
	}

	private String charArrToStr(final char[] aCharArr) {
		return aCharArr==null?"":new String(aCharArr);
	}

	/**
	 * Returns decrypted property
	 * @param aProperty
	 * @return
	 */
	private String getDecrypted(String aProperty) {
		try {
			return encryptor.decryptString(props.getProperty(aProperty));
		} catch (CryptoException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Sets encrypted property
	 * @param aProperty
	 * @return
	 */
	private void setEncrypted(final String aProperty, final String aValue) {
		try {
			props.setProperty(aProperty,
					encryptor.encryptString(aValue));
		} catch (CryptoException e) {
			e.printStackTrace();
			props.removeProperty(aProperty);
		}
	}


	/**
	 * Loads options from PropertyProvider
	 */
	public void loadOptions() {
		ksType = props.getProperty(Constants.PROPERTY_KSTYPE);
		advanced = props.getAsBool(Constants.PROPERTY_ADVANCED);
		storePasswords = props.getAsBool(Constants.PROPERTY_STOREPWD);
		keyAlias = props.getProperty(Constants.PROPERTY_ALIAS);
		inFile = props.getProperty(Constants.PROPERTY_INPDF);
		outFile = props.getProperty(Constants.PROPERTY_OUTPDF);
		reason = props.getProperty(Constants.PROPERTY_REASON);
		location = props.getProperty(Constants.PROPERTY_LOCATION);
		append = props.getAsBool(Constants.PROPERTY_APPEND);
		encrypted = props.getAsBool(Constants.PROPERTY_ENCRYPTED_PDF);
		final String tmpLevel = props.getProperty(Constants.PROPERTY_CERT_LEVEL);
		if (tmpLevel != null) {
			certLevel = CertificationLevel.valueOf(tmpLevel);
		}
		if (certLevel==null) {
				certLevel = CertificationLevel.NOT_CERTIFIED;
		}
		final String tmpHome = getDecrypted(Constants.EPROPERTY_USERHOME);
		boolean tmpPasswords = storePasswords &&
			Constants.USER_HOME!=null &&
			Constants.USER_HOME.equals(tmpHome);
		if (tmpPasswords) {
			ksPasswd = getDecrypted(Constants.EPROPERTY_KS_PWD).toCharArray();
			keyPasswd = getDecrypted(Constants.EPROPERTY_KEY_PWD).toCharArray();
			if (encrypted) {
				pdfOwnerPwd = getDecrypted(Constants.EPROPERTY_OWNER_PWD).toCharArray();
				pdfUserPwd = getDecrypted(Constants.EPROPERTY_USER_PWD).toCharArray();
			}
		}
	}

	/**
	 * Stores options to PropertyProvider
	 */
	public void storeOptions() {
		props.setProperty(Constants.PROPERTY_KSTYPE, ksType);
		props.setProperty(Constants.PROPERTY_ADVANCED, advanced);
		props.setProperty(Constants.PROPERTY_STOREPWD, storePasswords);
		props.setProperty(Constants.PROPERTY_ALIAS, keyAlias);
		props.setProperty(Constants.PROPERTY_INPDF, inFile);
		props.setProperty(Constants.PROPERTY_OUTPDF, outFile);
		props.setProperty(Constants.PROPERTY_REASON, reason);
		props.setProperty(Constants.PROPERTY_LOCATION, location);
		props.setProperty(Constants.PROPERTY_APPEND, append);
		props.setProperty(Constants.PROPERTY_ENCRYPTED_PDF, encrypted);
		if (certLevel!=null) {
			props.setProperty(Constants.PROPERTY_CERT_LEVEL, certLevel.name());
		} else {
			props.removeProperty(Constants.PROPERTY_CERT_LEVEL);
		}

		setEncrypted(Constants.EPROPERTY_USERHOME, Constants.USER_HOME);
		if (storePasswords) {
			setEncrypted(Constants.EPROPERTY_KS_PWD, new String(ksPasswd));
			setEncrypted(Constants.EPROPERTY_KEY_PWD, new String(keyPasswd));
			if (encrypted) {
				setEncrypted(Constants.EPROPERTY_OWNER_PWD, new String(pdfOwnerPwd));
				setEncrypted(Constants.EPROPERTY_USER_PWD, new String(pdfUserPwd));
			}
		} else {
			props.removeProperty(Constants.EPROPERTY_KS_PWD);
			props.removeProperty(Constants.EPROPERTY_KEY_PWD);
			props.removeProperty(Constants.EPROPERTY_OWNER_PWD);
			props.removeProperty(Constants.EPROPERTY_USER_PWD);
		}
		props.saveDefault();
	}

	/**
	 * Fires event listener
	 * @param aResult
	 * @see #getListener()
	 */
	public void fireSignerFinishedEvent(boolean aResult) {
		if (listener != null) {
			listener.signerFinishedEvent(aResult);
		}
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
	public String getInFile() {
		return inFile;
	}
	public void setInFile(String inFile) {
		this.inFile = inFile;
	}
	public String getOutFile() {
		return outFile;
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
	public String getKeyPasswdStr() {
		return charArrToStr(keyPasswd);
	}
	public void setKeyPasswd(char[] keyPasswd) {
		this.keyPasswd = keyPasswd;
	}
	public String getKeyAlias() {
		return keyAlias;
	}
	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
	}
	public boolean isAppend() {
		return append;
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

	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	public boolean isStorePasswords() {
		return storePasswords;
	}

	public void setStorePasswords(boolean storePasswords) {
		this.storePasswords = storePasswords;
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

	public char[] getPdfUserPwd() {
		return pdfUserPwd;
	}
	public String getPdfUserPwdStr() {
		return charArrToStr(pdfUserPwd);
	}

	public void setPdfUserPwd(char[] pdfUserPwd) {
		this.pdfUserPwd = pdfUserPwd;
	}

	public CertificationLevel getCertLevel() {
		return certLevel;
	}

	public void setCertLevel(CertificationLevel certLevel) {
		this.certLevel = certLevel;
	}

}
