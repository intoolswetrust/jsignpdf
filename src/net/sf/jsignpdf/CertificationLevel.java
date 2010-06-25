package net.sf.jsignpdf;

import com.lowagie.text.pdf.PdfSignatureAppearance;

/**
 * Enum of possible certification levels used to Sign PDF.
 * @author Josef Cacek
 */
public enum CertificationLevel {

	NOT_CERTIFIED ("certificationLevel.notCertified", PdfSignatureAppearance.NOT_CERTIFIED),
	CERTIFIED_NO_CHANGES_ALLOWED ("certificationLevel.noChanges", PdfSignatureAppearance.CERTIFIED_NO_CHANGES_ALLOWED),
	CERTIFIED_FORM_FILLING ("certificationLevel.formFill", PdfSignatureAppearance.CERTIFIED_FORM_FILLING),
	CERTIFIED_FORM_FILLING_AND_ANNOTATIONS ("certificationLevel.formFillAnnot", PdfSignatureAppearance.CERTIFIED_FORM_FILLING_AND_ANNOTATIONS);

	private String msgKey;
	private int level;

	CertificationLevel(final String aMsgKey, final int aLevel) {
		msgKey = aMsgKey;
		level = aLevel;
	}

	/**
	 * Returns internationalized description of a level.
	 */
	public String toString() {
		return ResourceProvider.getBundleBean().get(msgKey);
	}

	/**
	 * Returns Level as defined in iText.
	 * @return
	 * @see PdfSignatureAppearance#setCertificationLevel(int)
	 */
	public int getLevel() {
		return level;
	}

}
