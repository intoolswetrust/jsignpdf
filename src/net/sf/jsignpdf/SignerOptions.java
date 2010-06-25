package net.sf.jsignpdf;

/**
 * Domain object for JSignPdf
 * 
 * @author Josef Cacek
 */
public class SignerOptions extends AbstractBean {

	public static final String PROPERTY_KEYSTORE_TYPE = "ksType";
	public static final String PROPERTY_KEYSTORE_FILE = "ksFile";
	public static final String PROPERTY_KEYSTORE_PWD = "ksPasswd";
	public static final String PROPERTY_KEY_ALIAS = "keyAlias";
	public static final String PROPERTY_KEY_INDEX = "keyIndex";
	public static final String PROPERTY_KEY_PASSWD = "keyPasswd";
	public static final String PROPERTY_IN_FILE = "inFile";
	public static final String PROPERTY_IN_FILE_PWD = "inFileOwnerPwd";
	public static final String PROPERTY_OUT_FILE = "outFile";
	public static final String PROPERTY_REASON = "reason";
	public static final String PROPERTY_LOCATION = "location";
	public static final String PROPERTY_CONTACT = "contact";
	public static final String PROPERTY_APPEND = "append";
	public static final String PROPERTY_OUT_OWNER_PWD = "pdfOwnerPwd";
	public static final String PROPERTY_OUT_USER_PWD = "pdfUserPwd";
	public static final String PROPERTY_CERT_LEVEL = "certLevel";
	public static final String PROPERTY_RIGHT_PRINTING = "rightPrinting";
	public static final String PROPERTY_RIGHT_COPY = "rightCopy";
	public static final String PROPERTY_RIGHT_ASSEMBLY = "rightAssembly";
	public static final String PROPERTY_RIGHT_FILLIN = "rightFillIn";
	public static final String PROPERTY_RIGHT_SCREENREADERS = "rightScreanReaders";
	public static final String PROPERTY_RIGHT_MODIFYANNOT = "rightModifyAnnotations";
	public static final String PROPERTY_RIGHT_MODIFYCONT = "rightModifyContents";
	public static final String PROPERTY_VISIBLE_ENABLED = "visible";
	public static final String PROPERTY_PAGE = "page";
	public static final String PROPERTY_POSITION_LLX = "positionLLX";
	public static final String PROPERTY_POSITION_LLY = "positionLLY";
	public static final String PROPERTY_POSITION_URX = "positionURX";
	public static final String PROPERTY_POSITION_URY = "positionURY";
	public static final String PROPERTY_BGIMG_SCALE = "bgImgScale";
	public static final String PROPERTY_RENDER_MODE = "renderMode";
	public static final String PROPERTY_L2TEXT = "l2Text";
	public static final String PROPERTY_L4TEXT = "l4Text";
	public static final String PROPERTY_L2FONT_SIZE = "l2TextFontSize";
	public static final String PROPERTY_IMG_PATH = "imgPath";
	public static final String PROPERTY_BGIMG_PATH = "bgImgPath";
	public static final String PROPERTY_TSA_URL = "tsaUrl";
	public static final String PROPERTY_TSA_USER = "tsaUser";
	public static final String PROPERTY_TSA_PWD = "tsaPasswd";
	public static final String PROPERTY_OCSP_ENABLED = "ocspEnabled";

	private String ksType;
	private String ksFile;
	private String ksPasswd;
	private String keyAlias;
	private int keyIndex;
	private String keyPasswd;
	private String inFile;
	private String inFileOwnerPwd;

	private String outFile;
	private String reason;
	private String location;
	private String contact;
	private boolean append;
	private String pdfOwnerPwd;
	private String pdfUserPwd;
	private CertificationLevel certLevel;

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
	private String tsaUrl;
	private String tsaUser;
	private String tsaPasswd;

	// options for timestamps (provided by external TSA)
	private boolean ocspEnabled;

	/**
	 * @return the ksType
	 */
	public String getKsType() {
		return ksType;
	}

	/**
	 * @param ksType
	 *            the ksType to set
	 */
	public void setKsType(String ksType) {
		Object oldValue = this.ksType;
		this.ksType = ksType;
		firePropertyChange(PROPERTY_KEYSTORE_TYPE, oldValue, this.ksType);
	}

	/**
	 * @return the ksFile
	 */
	public String getKsFile() {
		return ksFile;
	}

	/**
	 * @param ksFile
	 *            the ksFile to set
	 */
	public void setKsFile(String ksFile) {
		Object oldValue = this.ksFile;
		this.ksFile = ksFile;
		firePropertyChange(PROPERTY_KEYSTORE_FILE, oldValue, this.ksFile);
	}

	/**
	 * @return the ksPasswd
	 */
	public String getKsPasswd() {
		return ksPasswd;
	}

	/**
	 * @param ksPasswd
	 *            the ksPasswd to set
	 */
	public void setKsPasswd(String ksPasswd) {
		Object oldValue = this.ksPasswd;
		this.ksPasswd = ksPasswd;
		firePropertyChange(PROPERTY_KEYSTORE_PWD, oldValue, this.ksPasswd);
	}

	/**
	 * @return the keyAlias
	 */
	public String getKeyAlias() {
		return keyAlias;
	}

	/**
	 * @param keyAlias
	 *            the keyAlias to set
	 */
	public void setKeyAlias(String keyAlias) {
		Object oldValue = this.keyAlias;
		this.keyAlias = keyAlias;
		firePropertyChange(PROPERTY_KEY_ALIAS, oldValue, this.keyAlias);
	}

	/**
	 * @return the keyIndex
	 */
	public int getKeyIndex() {
		return keyIndex;
	}

	/**
	 * @param keyIndex
	 *            the keyIndex to set
	 */
	public void setKeyIndex(int keyIndex) {
		int oldValue = this.keyIndex;
		this.keyIndex = keyIndex;
		firePropertyChange(PROPERTY_KEY_INDEX, oldValue, this.keyIndex);
	}

	/**
	 * @return the keyPasswd
	 */
	public String getKeyPasswd() {
		return keyPasswd;
	}

	/**
	 * @param keyPasswd
	 *            the keyPasswd to set
	 */
	public void setKeyPasswd(String keyPasswd) {
		Object oldValue = this.keyPasswd;
		this.keyPasswd = keyPasswd;
		firePropertyChange(PROPERTY_KEY_PASSWD, oldValue, this.keyPasswd);
	}

	/**
	 * @return the inFile
	 */
	public String getInFile() {
		return inFile;
	}

	/**
	 * @param inFile
	 *            the inFile to set
	 */
	public void setInFile(String inFile) {
		Object oldValue = this.inFile;
		this.inFile = inFile;
		firePropertyChange(PROPERTY_IN_FILE, oldValue, this.inFile);
	}

	/**
	 * @return the inFileOwnerPwd
	 */
	public String getInFileOwnerPwd() {
		return inFileOwnerPwd;
	}

	/**
	 * @param inFileOwnerPwd
	 *            the inFileOwnerPwd to set
	 */
	public void setInFileOwnerPwd(String inFileOwnerPwd) {
		Object oldValue = this.inFileOwnerPwd;
		this.inFileOwnerPwd = inFileOwnerPwd;
		firePropertyChange(PROPERTY_IN_FILE_PWD, oldValue, this.inFileOwnerPwd);
	}

	/**
	 * @return the outFile
	 */
	public String getOutFile() {
		return outFile;
	}

	/**
	 * @param outFile
	 *            the outFile to set
	 */
	public void setOutFile(String outFile) {
		Object oldValue = this.outFile;
		this.outFile = outFile;
		firePropertyChange(PROPERTY_OUT_FILE, oldValue, this.outFile);
	}

	/**
	 * @return the reason
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * @param reason
	 *            the reason to set
	 */
	public void setReason(String reason) {
		Object oldValue = this.reason;
		this.reason = reason;
		firePropertyChange(PROPERTY_REASON, oldValue, this.reason);
	}

	/**
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @param location
	 *            the location to set
	 */
	public void setLocation(String location) {
		Object oldValue = this.location;
		this.location = location;
		firePropertyChange(PROPERTY_LOCATION, oldValue, this.location);
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
		Object oldValue = this.contact;
		this.contact = contact;
		firePropertyChange(PROPERTY_CONTACT, oldValue, this.contact);
	}

	/**
	 * @return the append
	 */
	public boolean isAppend() {
		return append;
	}

	/**
	 * @param append
	 *            the append to set
	 */
	public void setAppend(boolean append) {
		boolean oldValue = this.append;
		this.append = append;
		firePropertyChange(PROPERTY_APPEND, oldValue, this.append);
	}

	/**
	 * @return the pdfOwnerPwd
	 */
	public String getPdfOwnerPwd() {
		return pdfOwnerPwd;
	}

	/**
	 * @param pdfOwnerPwd
	 *            the pdfOwnerPwd to set
	 */
	public void setPdfOwnerPwd(String pdfOwnerPwd) {
		Object oldValue = this.pdfOwnerPwd;
		this.pdfOwnerPwd = pdfOwnerPwd;
		firePropertyChange(PROPERTY_OUT_OWNER_PWD, oldValue, this.pdfOwnerPwd);
	}

	/**
	 * @return the pdfUserPwd
	 */
	public String getPdfUserPwd() {
		return pdfUserPwd;
	}

	/**
	 * @param pdfUserPwd
	 *            the pdfUserPwd to set
	 */
	public void setPdfUserPwd(String pdfUserPwd) {
		Object oldValue = this.pdfUserPwd;
		this.pdfUserPwd = pdfUserPwd;
		firePropertyChange(PROPERTY_OUT_USER_PWD, oldValue, this.pdfUserPwd);
	}

	/**
	 * @return the certLevel
	 */
	public CertificationLevel getCertLevel() {
		return certLevel;
	}

	/**
	 * @param certLevel
	 *            the certLevel to set
	 */
	public void setCertLevel(CertificationLevel certLevel) {
		Object oldValue = this.certLevel;
		this.certLevel = certLevel;
		firePropertyChange(PROPERTY_CERT_LEVEL, oldValue, this.certLevel);
	}

	/**
	 * @return the rightPrinting
	 */
	public PrintRight getRightPrinting() {
		return rightPrinting;
	}

	/**
	 * @param rightPrinting
	 *            the rightPrinting to set
	 */
	public void setRightPrinting(PrintRight rightPrinting) {
		Object oldValue = this.rightPrinting;
		this.rightPrinting = rightPrinting;
		firePropertyChange(PROPERTY_RIGHT_PRINTING, oldValue, this.rightPrinting);
	}

	/**
	 * @return the rightCopy
	 */
	public boolean isRightCopy() {
		return rightCopy;
	}

	/**
	 * @param rightCopy
	 *            the rightCopy to set
	 */
	public void setRightCopy(boolean rightCopy) {
		boolean oldValue = this.rightCopy;
		this.rightCopy = rightCopy;
		firePropertyChange(PROPERTY_RIGHT_COPY, oldValue, this.rightCopy);
	}

	/**
	 * @return the rightAssembly
	 */
	public boolean isRightAssembly() {
		return rightAssembly;
	}

	/**
	 * @param rightAssembly
	 *            the rightAssembly to set
	 */
	public void setRightAssembly(boolean rightAssembly) {
		boolean oldValue = this.rightAssembly;
		this.rightAssembly = rightAssembly;
		firePropertyChange(PROPERTY_RIGHT_ASSEMBLY, oldValue, this.rightAssembly);
	}

	/**
	 * @return the rightFillIn
	 */
	public boolean isRightFillIn() {
		return rightFillIn;
	}

	/**
	 * @param rightFillIn
	 *            the rightFillIn to set
	 */
	public void setRightFillIn(boolean rightFillIn) {
		boolean oldValue = this.rightFillIn;
		this.rightFillIn = rightFillIn;
		firePropertyChange(PROPERTY_RIGHT_FILLIN, oldValue, this.rightFillIn);
	}

	/**
	 * @return the rightScreanReaders
	 */
	public boolean isRightScreanReaders() {
		return rightScreanReaders;
	}

	/**
	 * @param rightScreanReaders
	 *            the rightScreanReaders to set
	 */
	public void setRightScreanReaders(boolean rightScreanReaders) {
		boolean oldValue = this.rightScreanReaders;
		this.rightScreanReaders = rightScreanReaders;
		firePropertyChange(PROPERTY_RIGHT_SCREENREADERS, oldValue, this.rightScreanReaders);
	}

	/**
	 * @return the rightModifyAnnotations
	 */
	public boolean isRightModifyAnnotations() {
		return rightModifyAnnotations;
	}

	/**
	 * @param rightModifyAnnotations
	 *            the rightModifyAnnotations to set
	 */
	public void setRightModifyAnnotations(boolean rightModifyAnnotations) {
		boolean oldValue = this.rightModifyAnnotations;
		this.rightModifyAnnotations = rightModifyAnnotations;
		firePropertyChange(PROPERTY_RIGHT_MODIFYANNOT, oldValue, this.rightModifyAnnotations);
	}

	/**
	 * @return the rightModifyContents
	 */
	public boolean isRightModifyContents() {
		return rightModifyContents;
	}

	/**
	 * @param rightModifyContents
	 *            the rightModifyContents to set
	 */
	public void setRightModifyContents(boolean rightModifyContents) {
		boolean oldValue = this.rightModifyContents;
		this.rightModifyContents = rightModifyContents;
		firePropertyChange(PROPERTY_RIGHT_MODIFYCONT, oldValue, this.rightModifyContents);
	}

	/**
	 * @return the visible
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * @param visible
	 *            the visible to set
	 */
	public void setVisible(boolean visible) {
		boolean oldValue = this.visible;
		this.visible = visible;
		firePropertyChange(PROPERTY_VISIBLE_ENABLED, oldValue, this.visible);
	}

	/**
	 * @return the page
	 */
	public int getPage() {
		return page;
	}

	/**
	 * @param page
	 *            the page to set
	 */
	public void setPage(int page) {
		int oldValue = this.page;
		this.page = page;
		firePropertyChange(PROPERTY_PAGE, oldValue, this.page);
	}

	/**
	 * @return the positionLLX
	 */
	public float getPositionLLX() {
		return positionLLX;
	}

	/**
	 * @param positionLLX
	 *            the positionLLX to set
	 */
	public void setPositionLLX(float positionLLX) {
		Object oldValue = this.positionLLX;
		this.positionLLX = positionLLX;
		firePropertyChange(PROPERTY_POSITION_LLX, oldValue, this.positionLLX);
	}

	/**
	 * @return the positionLLY
	 */
	public float getPositionLLY() {
		return positionLLY;
	}

	/**
	 * @param positionLLY
	 *            the positionLLY to set
	 */
	public void setPositionLLY(float positionLLY) {
		Object oldValue = this.positionLLY;
		this.positionLLY = positionLLY;
		firePropertyChange(PROPERTY_POSITION_LLY, oldValue, this.positionLLY);
	}

	/**
	 * @return the positionURX
	 */
	public float getPositionURX() {
		return positionURX;
	}

	/**
	 * @param positionURX
	 *            the positionURX to set
	 */
	public void setPositionURX(float positionURX) {
		Object oldValue = this.positionURX;
		this.positionURX = positionURX;
		firePropertyChange(PROPERTY_POSITION_URX, oldValue, this.positionURX);
	}

	/**
	 * @return the positionURY
	 */
	public float getPositionURY() {
		return positionURY;
	}

	/**
	 * @param positionURY
	 *            the positionURY to set
	 */
	public void setPositionURY(float positionURY) {
		Object oldValue = this.positionURY;
		this.positionURY = positionURY;
		firePropertyChange(PROPERTY_POSITION_URY, oldValue, this.positionURY);
	}

	/**
	 * @return the bgImgScale
	 */
	public float getBgImgScale() {
		return bgImgScale;
	}

	/**
	 * @param bgImgScale
	 *            the bgImgScale to set
	 */
	public void setBgImgScale(float bgImgScale) {
		Object oldValue = this.bgImgScale;
		this.bgImgScale = bgImgScale;
		firePropertyChange(PROPERTY_BGIMG_SCALE, oldValue, this.bgImgScale);
	}

	/**
	 * @return the renderMode
	 */
	public RenderMode getRenderMode() {
		return renderMode;
	}

	/**
	 * @param renderMode
	 *            the renderMode to set
	 */
	public void setRenderMode(RenderMode renderMode) {
		Object oldValue = this.renderMode;
		this.renderMode = renderMode;
		firePropertyChange(PROPERTY_RENDER_MODE, oldValue, this.renderMode);
	}

	/**
	 * @return the l2Text
	 */
	public String getL2Text() {
		return l2Text;
	}

	/**
	 * @param l2Text
	 *            the l2Text to set
	 */
	public void setL2Text(String l2Text) {
		Object oldValue = this.l2Text;
		this.l2Text = l2Text;
		firePropertyChange(PROPERTY_L2TEXT, oldValue, this.l2Text);
	}

	/**
	 * @return the l4Text
	 */
	public String getL4Text() {
		return l4Text;
	}

	/**
	 * @param l4Text
	 *            the l4Text to set
	 */
	public void setL4Text(String l4Text) {
		Object oldValue = this.l4Text;
		this.l4Text = l4Text;
		firePropertyChange(PROPERTY_L4TEXT, oldValue, this.l4Text);
	}

	/**
	 * @return the l2TextFontSize
	 */
	public float getL2TextFontSize() {
		return l2TextFontSize;
	}

	/**
	 * @param l2TextFontSize
	 *            the l2TextFontSize to set
	 */
	public void setL2TextFontSize(float l2TextFontSize) {
		Object oldValue = this.l2TextFontSize;
		this.l2TextFontSize = l2TextFontSize;
		firePropertyChange(PROPERTY_L2FONT_SIZE, oldValue, this.l2TextFontSize);
	}

	/**
	 * @return the imgPath
	 */
	public String getImgPath() {
		return imgPath;
	}

	/**
	 * @param imgPath
	 *            the imgPath to set
	 */
	public void setImgPath(String imgPath) {
		Object oldValue = this.imgPath;
		this.imgPath = imgPath;
		firePropertyChange(PROPERTY_IMG_PATH, oldValue, this.imgPath);
	}

	/**
	 * @return the bgImgPath
	 */
	public String getBgImgPath() {
		return bgImgPath;
	}

	/**
	 * @param bgImgPath
	 *            the bgImgPath to set
	 */
	public void setBgImgPath(String bgImgPath) {
		Object oldValue = this.bgImgPath;
		this.bgImgPath = bgImgPath;
		firePropertyChange(PROPERTY_BGIMG_PATH, oldValue, this.bgImgPath);
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
		Object oldValue = this.tsaUrl;
		this.tsaUrl = tsaUrl;
		firePropertyChange(PROPERTY_TSA_URL, oldValue, this.tsaUrl);
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
		Object oldValue = this.tsaUser;
		this.tsaUser = tsaUser;
		firePropertyChange(PROPERTY_TSA_USER, oldValue, this.tsaUser);
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
		Object oldValue = this.tsaPasswd;
		this.tsaPasswd = tsaPasswd;
		firePropertyChange(PROPERTY_TSA_PWD, oldValue, this.tsaPasswd);
	}

	/**
	 * @return the ocspEnabled
	 */
	public boolean isOcspEnabled() {
		return ocspEnabled;
	}

	/**
	 * @param ocspEnabled
	 *            the ocspEnabled to set
	 */
	public void setOcspEnabled(boolean ocspEnabled) {
		boolean oldValue = this.ocspEnabled;
		this.ocspEnabled = ocspEnabled;
		firePropertyChange(PROPERTY_OCSP_ENABLED, oldValue, this.ocspEnabled);
	}

}
