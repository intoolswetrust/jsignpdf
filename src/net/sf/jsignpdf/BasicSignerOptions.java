package net.sf.jsignpdf;

import java.io.PrintWriter;

/**
 * Options for PDF signer.
 * @author Josef Cacek
 */
public class BasicSignerOptions {

	protected final static ResourceProvider res = ResourceProvider.getInstance();

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
	private volatile char[] pdfOwnerPwd;
	private volatile char[] pdfUserPwd;
	private volatile CertificationLevel certLevel;

	//options from rights dialog
	private volatile PrintRight rightPrinting;
	private volatile boolean rightCopy;
	private volatile boolean rightAssembly;
	private volatile boolean rightFillIn;
	private volatile boolean rightScreanReaders;
	private volatile boolean rightModifyAnnotations;
	private volatile boolean rightModifyContents;


	//options from visible signature settings dialog
	private volatile boolean visible;
	private volatile int page = Constants.DEFVAL_PAGE;
	private volatile float positionLLX = Constants.DEFVAL_LLX;
	private volatile float positionLLY = Constants.DEFVAL_LLY;
	private volatile float positionURX = Constants.DEFVAL_URX;
	private volatile float positionURY = Constants.DEFVAL_URY;
	private volatile float bgImgScale = Constants.DEFVAL_BG_SCALE;
	private volatile RenderMode renderMode;
	private volatile String l2Text;
	private volatile String l4Text;
	private volatile String imgPath;
	private volatile String bgImgPath;


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
			printWriter.flush();
		}
	}

	/**
	 * Fires event listener
	 * @param aResult
	 * @see #getListener()
	 */
	protected void fireSignerFinishedEvent(boolean aResult) {
		if (listener != null) {
			listener.signerFinishedEvent(aResult);
		}
	}

	/**
	 * Converts array of characters to String. If array is null, empty string is returned
	 * @param aCharArr char array
	 * @return not null string
	 */
	private String charArrToStr(final char[] aCharArr) {
		return aCharArr==null?"":new String(aCharArr);
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
		setKsPasswd(aPasswd==null?null:aPasswd.toCharArray());
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
	public char[] getKeyPasswdX() {
		if (keyPasswd == null || keyPasswd.length==0) return null;
		return advanced?keyPasswd:ksPasswd;
	}
	public String getKeyPasswdStr() {
		return charArrToStr(keyPasswd);
	}
	public void setKeyPasswd(char[] keyPasswd) {
		this.keyPasswd = keyPasswd;
	}
	public void setKeyPasswd(String aPasswd) {
		setKeyPasswd(aPasswd==null?null:aPasswd.toCharArray());
	}

	public String getKeyAlias() {
		return keyAlias;
	}
	public String getKeyAliasX() {
		return advanced?keyAlias:null;
	}
	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
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
		setPdfOwnerPwd(aPasswd==null?null:aPasswd.toCharArray());
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
		setPdfUserPwd(aPasswd==null?null:aPasswd.toCharArray());
	}

	public CertificationLevel getCertLevel() {
		if (certLevel==null) {
			certLevel = CertificationLevel.NOT_CERTIFIED;
		}
		return certLevel;
	}
	public CertificationLevel getCertLevelX() {
		return advanced?getCertLevel():CertificationLevel.NOT_CERTIFIED;
	}

	public void setCertLevel(CertificationLevel aCertLevel) {
		this.certLevel = aCertLevel;
	}

	public void setCertLevel(String aCertLevel) {
		setCertLevel(aCertLevel==null?null:CertificationLevel.valueOf(aCertLevel.toUpperCase()));
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
		if (getRightPrinting()==null) {
			rightPrinting = PrintRight.ALLOW_PRINTING;
		}
		this.rightPrinting = rightPrinting;
	}
	public void setRightPrinting(String aValue) {
		setRightPrinting(aValue==null?null:PrintRight.valueOf(aValue.toUpperCase()));
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
		if (aPage<1) {
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
		if (renderMode==null) {
			renderMode = RenderMode.DESCRTIPTION_ONLY;
		}
		return renderMode;
	}

	public void setRenderMode(RenderMode renderMode) {
		this.renderMode = renderMode;
	}

	public void setRenderMode(String aValue) {
		setRenderMode(aValue==null?null:RenderMode.valueOf(aValue.toUpperCase()));
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

}
