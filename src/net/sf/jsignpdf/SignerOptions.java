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

	private final JSignEncryptor encryptor = new JSignEncryptor();

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
	private void setEncrypted(String aProperty, String aValue) {
		try {
			props.setProperty(aProperty,
				encryptor.encryptString(props.getProperty(aProperty)));
		} catch (CryptoException e) {
			e.printStackTrace();
			props.removeProperty(aProperty);
		}
	}


	public void loadOptions() {
		final String tmpHome = getDecrypted(Constants.PROPERTY_STOREPWD);
		ksType = props.getProperty(Constants.PROPERTY_KSTYPE);
		advanced = props.getAsBool(Constants.PROPERTY_ADVANCED);
		storePasswords = props.getAsBool(Constants.PROPERTY_STOREPWD);
		boolean tmpPasswords = storePasswords &&
			Constants.USER_HOME!=null &&
			Constants.USER_HOME.equals(tmpHome);
		if (tmpPasswords) {
			//TODO load pwds here
		}

		//
//		tfKeystoreFile.setText(props.getProperty(Constants.PROPERTY_KEYSTORE));
//		cbAlias.setSelectedItem(props.getProperty(Constants.PROPERTY_ALIAS));
//		tfInPdfFile.setText(props.getProperty(Constants.PROPERTY_INPDF));
//		tfOutPdfFile.setText(props.getProperty(Constants.PROPERTY_OUTPDF));
//		tfReason.setText(props.getProperty(Constants.PROPERTY_REASON));
//		tfLocation.setText(props.getProperty(Constants.PROPERTY_LOCATION));

	}

	public void storeOptions() {
		setEncrypted(Constants.PROPERTY_USERHOME, Constants.USER_HOME);
		//TODO
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

	public void setPdfOwnerPwd(char[] pdfOwnerPwd) {
		this.pdfOwnerPwd = pdfOwnerPwd;
	}

	public char[] getPdfUserPwd() {
		return pdfUserPwd;
	}

	public void setPdfUserPwd(char[] pdfUserPwd) {
		this.pdfUserPwd = pdfUserPwd;
	}

}
