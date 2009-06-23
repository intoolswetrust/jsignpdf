package net.sf.jsignpdf;

import org.bouncycastle.crypto.CryptoException;

/**
 * Options for PDF signer.
 * @author Josef Cacek
 */
public class SignerOptions extends BasicSignerOptions {

	protected final PropertyProvider props = PropertyProvider.getInstance();
	protected final JSignEncryptor encryptor = new JSignEncryptor();

	protected volatile boolean storePasswords;


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
		setKsType(props.getProperty(Constants.PROPERTY_KSTYPE));
		setAdvanced(props.getAsBool(Constants.PROPERTY_ADVANCED));
		setKsFile(props.getProperty(Constants.PROPERTY_KEYSTORE));
		storePasswords = props.getAsBool(Constants.PROPERTY_STOREPWD);
		setKeyAlias(props.getProperty(Constants.PROPERTY_ALIAS));
		setInFile(props.getProperty(Constants.PROPERTY_INPDF));
		setOutFile(props.getProperty(Constants.PROPERTY_OUTPDF));
		setReason(props.getProperty(Constants.PROPERTY_REASON));
		setLocation(props.getProperty(Constants.PROPERTY_LOCATION));
		setAppend(props.getAsBool(Constants.PROPERTY_APPEND));
		setEncrypted(props.getAsBool(Constants.PROPERTY_ENCRYPTED_PDF));
		setCertLevel(props.getProperty(Constants.PROPERTY_CERT_LEVEL));

		final String tmpHome = getDecrypted(Constants.EPROPERTY_USERHOME);
		boolean tmpPasswords = storePasswords &&
			Constants.USER_HOME!=null &&
			Constants.USER_HOME.equals(tmpHome);
		if (tmpPasswords) {
			setKsPasswd(getDecrypted(Constants.EPROPERTY_KS_PWD));
			setKeyPasswd(getDecrypted(Constants.EPROPERTY_KEY_PWD));
			if (isEncrypted()) {
				setPdfOwnerPwd(getDecrypted(Constants.EPROPERTY_OWNER_PWD));
				setPdfUserPwd(getDecrypted(Constants.EPROPERTY_USER_PWD));
			}
		}

		setRightPrinting(props.getProperty(Constants.PROPERTY_RIGHT_PRINT));
		setRightCopy(props.getAsBool(Constants.PROPERTY_RIGHT_COPY));
		setRightAssembly(props.getAsBool(Constants.PROPERTY_RIGHT_ASSEMBLY));
		setRightFillIn(props.getAsBool(Constants.PROPERTY_RIGHT_FILL_IN));
		setRightScreanReaders(props.getAsBool(Constants.PROPERTY_RIGHT_SCR_READ));
		setRightModifyAnnotations(props.getAsBool(Constants.PROPERTY_RIGHT_MOD_ANNOT));
		setRightModifyContents(props.getAsBool(Constants.PROPERTY_RIGHT_MOD_CONT));

		//visible signature options
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

	}

	/**
	 * Stores options to PropertyProvider
	 */
	public void storeOptions() {
		props.setProperty(Constants.PROPERTY_KSTYPE, getKsType());
		props.setProperty(Constants.PROPERTY_ADVANCED, isAdvanced());
		props.setProperty(Constants.PROPERTY_KEYSTORE, getKsFile());
		props.setProperty(Constants.PROPERTY_STOREPWD, isStorePasswords());
		props.setProperty(Constants.PROPERTY_ALIAS, getKeyAlias());
		props.setProperty(Constants.PROPERTY_INPDF, getInFile());
		props.setProperty(Constants.PROPERTY_OUTPDF, getOutFile());
		props.setProperty(Constants.PROPERTY_REASON, getReason());
		props.setProperty(Constants.PROPERTY_LOCATION, getLocation());
		props.setProperty(Constants.PROPERTY_APPEND, isAppend());
		props.setProperty(Constants.PROPERTY_ENCRYPTED_PDF, isEncrypted());
		props.setProperty(Constants.PROPERTY_CERT_LEVEL, getCertLevel().name());

		setEncrypted(Constants.EPROPERTY_USERHOME, Constants.USER_HOME);
		if (isStorePasswords()) {
			setEncrypted(Constants.EPROPERTY_KS_PWD, new String(getKsPasswd()));
			setEncrypted(Constants.EPROPERTY_KEY_PWD, new String(getKeyPasswd()));
			if (isEncrypted()) {
				setEncrypted(Constants.EPROPERTY_OWNER_PWD, new String(getPdfOwnerPwd()));
				setEncrypted(Constants.EPROPERTY_USER_PWD, new String(getPdfUserPwd()));
			}
		} else {
			props.removeProperty(Constants.EPROPERTY_KS_PWD);
			props.removeProperty(Constants.EPROPERTY_KEY_PWD);
			props.removeProperty(Constants.EPROPERTY_OWNER_PWD);
			props.removeProperty(Constants.EPROPERTY_USER_PWD);
		}

		props.setProperty(Constants.PROPERTY_RIGHT_PRINT, getRightPrinting().name());
		props.setProperty(Constants.PROPERTY_RIGHT_COPY, isRightCopy());
		props.setProperty(Constants.PROPERTY_RIGHT_ASSEMBLY, isRightAssembly());
		props.setProperty(Constants.PROPERTY_RIGHT_FILL_IN, isRightFillIn());
		props.setProperty(Constants.PROPERTY_RIGHT_SCR_READ, isRightScreanReaders());
		props.setProperty(Constants.PROPERTY_RIGHT_MOD_ANNOT, isRightModifyAnnotations());
		props.setProperty(Constants.PROPERTY_RIGHT_MOD_CONT, isRightModifyContents());

		//visible signature options
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

		props.saveDefault();
	}

	public boolean isStorePasswords() {
		return storePasswords;
	}

	public void setStorePasswords(boolean storePasswords) {
		this.storePasswords = storePasswords;
	}

}
